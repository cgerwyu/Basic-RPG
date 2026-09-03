param(
    [string]$ProjectDirectory = 'C:\Users\cdald\Desktop\basicrpgclasses-26.2',
    [string]$MinecraftClientJar = 'C:\Users\cdald\.gradle\caches\neoformruntime\artifacts\minecraft_26.2_client.jar'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$textureRoot = Join-Path $ProjectDirectory 'src\main\resources\assets\basicrpgclasses\textures\entity\equipment'
$humanoidOutput = Join-Path $textureRoot 'humanoid'
$leggingsOutput = Join-Path $textureRoot 'humanoid_leggings'
$babyOutput = Join-Path $textureRoot 'humanoid_baby'
$auraOutput = Join-Path $ProjectDirectory 'src\main\resources\assets\basicrpgclasses\textures\entity\armor'
$artOutput = Join-Path $ProjectDirectory 'art\lizard_armor_2d'
$itemTextureRoot = Join-Path $ProjectDirectory 'src\main\resources\assets\basicrpgclasses\textures\item'
New-Item -ItemType Directory -Force -Path $humanoidOutput, $leggingsOutput, $babyOutput, $auraOutput, $artOutput | Out-Null

function Read-ZipBitmap {
    param(
        [System.IO.Compression.ZipArchive]$Archive,
        [string]$EntryName
    )

    $entry = $Archive.GetEntry($EntryName)
    if ($null -eq $entry) {
        throw "Missing Minecraft texture: $EntryName"
    }

    $stream = $entry.Open()
    try {
        $source = [System.Drawing.Bitmap]::FromStream($stream)
        try {
            $copy = [System.Drawing.Bitmap]::new(
                $source.Width,
                $source.Height,
                [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
            )
            $graphics = [System.Drawing.Graphics]::FromImage($copy)
            try {
                $graphics.DrawImageUnscaled($source, 0, 0)
            }
            finally {
                $graphics.Dispose()
            }
            return $copy
        }
        finally {
            $source.Dispose()
        }
    }
    finally {
        $stream.Dispose()
    }
}

function Read-FileBitmap {
    param([string]$Path)

    $source = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $copy = [System.Drawing.Bitmap]::new(
            $source.Width,
            $source.Height,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        $graphics = [System.Drawing.Graphics]::FromImage($copy)
        try {
            $graphics.DrawImageUnscaled($source, 0, 0)
        }
        finally {
            $graphics.Dispose()
        }
        return $copy
    }
    finally {
        $source.Dispose()
    }
}

function Get-ArmorPalette {
    param([bool]$Elevated)

    if ($Elevated) {
        return @(
            [System.Drawing.Color]::FromArgb(255, 3, 18, 13),
            [System.Drawing.Color]::FromArgb(255, 7, 39, 25),
            [System.Drawing.Color]::FromArgb(255, 13, 68, 36),
            [System.Drawing.Color]::FromArgb(255, 27, 101, 49),
            [System.Drawing.Color]::FromArgb(255, 72, 151, 71),
            [System.Drawing.Color]::FromArgb(255, 132, 207, 101)
        )
    }

    return @(
        [System.Drawing.Color]::FromArgb(255, 5, 24, 17),
        [System.Drawing.Color]::FromArgb(255, 10, 46, 29),
        [System.Drawing.Color]::FromArgb(255, 18, 72, 39),
        [System.Drawing.Color]::FromArgb(255, 33, 103, 51),
        [System.Drawing.Color]::FromArgb(255, 81, 146, 72),
        [System.Drawing.Color]::FromArgb(255, 139, 196, 100)
    )
}

function Test-AlphaBoundary {
    param(
        [System.Drawing.Bitmap]$Mask,
        [int]$X,
        [int]$Y
    )

    foreach ($offset in @(@(-1, 0), @(1, 0), @(0, -1), @(0, 1))) {
        $sampleX = $X + $offset[0]
        $sampleY = $Y + $offset[1]
        if ($sampleX -lt 0 -or $sampleY -lt 0 -or $sampleX -ge $Mask.Width -or $sampleY -ge $Mask.Height) {
            return $true
        }
        if ($Mask.GetPixel($sampleX, $sampleY).A -eq 0) {
            return $true
        }
    }
    return $false
}

function Convert-ArmorMask {
    param(
        [System.Drawing.Bitmap]$Mask,
        [bool]$Elevated
    )

    $palette = Get-ArmorPalette -Elevated $Elevated
    $output = [System.Drawing.Bitmap]::new(64, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 32; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
            $source = $Mask.GetPixel($x, $y)
            if ($source.A -eq 0) {
                $output.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                continue
            }

            if (Test-AlphaBoundary -Mask $Mask -X $x -Y $y) {
                $output.SetPixel($x, $y, $palette[0])
                continue
            }

            $luma = [Math]::Round(($source.R * 0.2126) + ($source.G * 0.7152) + ($source.B * 0.0722))
            $index = if ($luma -lt 70) { 1 } elseif ($luma -lt 120) { 2 } elseif ($luma -lt 175) { 3 } elseif ($luma -lt 220) { 4 } else { 5 }
            $output.SetPixel($x, $y, $palette[$index])
        }
    }
    return $output
}

function Set-IfOpaque {
    param(
        [System.Drawing.Bitmap]$Texture,
        [System.Drawing.Bitmap]$Mask,
        [int]$X,
        [int]$Y,
        [System.Drawing.Color]$Color
    )

    if ($X -ge 0 -and $Y -ge 0 -and $X -lt 64 -and $Y -lt 32 -and $Mask.GetPixel($X, $Y).A -gt 0) {
        $Texture.SetPixel($X, $Y, $Color)
    }
}

function Add-PlateAccents {
    param(
        [System.Drawing.Bitmap]$Texture,
        [System.Drawing.Bitmap]$Mask,
        [bool]$Elevated,
        [bool]$Leggings
    )

    $palette = Get-ArmorPalette -Elevated $Elevated
    $deep = $palette[0]
    $shadow = $palette[1]
    $highlight = $palette[5]

    if ($Leggings) {
        # Strong belt line and layered thigh/shin bands.
        foreach ($x in 16..39) {
            Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 27 -Color $deep
        }
        foreach ($x in 0..15) {
            Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 23 -Color $highlight
            Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 24 -Color $shadow
            Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 28 -Color $deep
        }
        return
    }

    # Helmet brow: a coherent V-shaped jade plate, with the vanilla visor opening retained.
    foreach ($point in @(@(8, 9), @(9, 9), @(10, 10), @(11, 10), @(12, 10), @(13, 10), @(14, 9), @(15, 9))) {
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $point[0] -Y $point[1] -Color $highlight
    }

    # Breastplate chevrons. These replace checker noise with large readable forged plates.
    foreach ($point in @(
        @(20, 21), @(27, 21), @(21, 22), @(26, 22), @(22, 23), @(25, 23), @(23, 24), @(24, 24),
        @(20, 26), @(27, 26), @(21, 27), @(26, 27), @(22, 28), @(25, 28), @(23, 29), @(24, 29)
    )) {
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $point[0] -Y $point[1] -Color $highlight
    }
    foreach ($x in 16..39) {
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 25 -Color $deep
    }

    # Bracers and greaves receive horizontal metal seams rather than grass-like mottling.
    foreach ($x in 40..55) {
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 24 -Color $shadow
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 25 -Color $highlight
    }
    foreach ($x in 0..15) {
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 28 -Color $deep
    }

    # The same right-leg UV is mirrored onto both feet, so this creates exactly three toe plates per boot.
    $boneDark = [System.Drawing.Color]::FromArgb(255, 133, 119, 80)
    $boneLight = [System.Drawing.Color]::FromArgb(255, 239, 226, 172)
    foreach ($x in @(4, 5, 7)) {
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 30 -Color $boneDark
        Set-IfOpaque -Texture $Texture -Mask $Mask -X $x -Y 31 -Color $boneLight
    }
    Set-IfOpaque -Texture $Texture -Mask $Mask -X 6 -Y 30 -Color $deep
    Set-IfOpaque -Texture $Texture -Mask $Mask -X 6 -Y 31 -Color $deep
}

function Get-PoisonColors {
    param([int]$Frame)

    $dark = @(
        [System.Drawing.Color]::FromArgb(255, 49, 12, 75),
        [System.Drawing.Color]::FromArgb(255, 61, 15, 91),
        [System.Drawing.Color]::FromArgb(255, 76, 18, 112),
        [System.Drawing.Color]::FromArgb(255, 94, 22, 139),
        [System.Drawing.Color]::FromArgb(255, 113, 31, 162),
        [System.Drawing.Color]::FromArgb(255, 91, 22, 137),
        [System.Drawing.Color]::FromArgb(255, 72, 18, 106),
        [System.Drawing.Color]::FromArgb(255, 56, 14, 84)
    )
    $bright = @(
        [System.Drawing.Color]::FromArgb(255, 117, 49, 159),
        [System.Drawing.Color]::FromArgb(255, 139, 57, 186),
        [System.Drawing.Color]::FromArgb(255, 164, 69, 211),
        [System.Drawing.Color]::FromArgb(255, 191, 84, 235),
        [System.Drawing.Color]::FromArgb(255, 224, 128, 255),
        [System.Drawing.Color]::FromArgb(255, 194, 88, 235),
        [System.Drawing.Color]::FromArgb(255, 158, 66, 207),
        [System.Drawing.Color]::FromArgb(255, 132, 53, 177)
    )
    return @($dark[$Frame], $bright[$Frame])
}

function Add-FirstfangGems {
    param(
        [System.Drawing.Bitmap]$Texture,
        [int]$Frame,
        [bool]$Leggings
    )

    $poison = Get-PoisonColors -Frame $Frame
    $purpleDark = $poison[0]
    $purpleBright = $poison[1]
    $gemDark = [System.Drawing.Color]::FromArgb(255, 8, 91, 40)
    $gemMid = [System.Drawing.Color]::FromArgb(255, 31, 190, 72)
    $gemLight = [System.Drawing.Color]::FromArgb(255, 167, 255, 151)
    $white = [System.Drawing.Color]::FromArgb(255, 235, 255, 226)

    if ($Leggings) {
        # One small belt shard on the front of the leggings; no stones on thighs or knees.
        foreach ($point in @(@(23, 28), @(24, 28), @(22, 29), @(25, 29), @(23, 31), @(24, 31))) {
            $Texture.SetPixel($point[0], $point[1], $purpleDark)
        }
        $Texture.SetPixel(23, 29, $gemMid)
        $Texture.SetPixel(24, 29, $gemLight)
        $Texture.SetPixel(23, 30, $gemDark)
        $Texture.SetPixel(24, 30, $gemMid)
        if ($Frame -eq 4) {
            $Texture.SetPixel(22, 28, $purpleBright)
            $Texture.SetPixel(25, 28, $purpleBright)
        }
        return
    }

    # Forehead shard, restricted to the brow so the face aperture stays open.
    foreach ($point in @(@(11, 8), @(12, 8), @(10, 9), @(13, 9), @(11, 11), @(12, 11))) {
        $Texture.SetPixel($point[0], $point[1], $purpleDark)
    }
    $Texture.SetPixel(11, 9, $gemMid)
    $Texture.SetPixel(12, 9, $gemLight)
    $Texture.SetPixel(11, 10, $gemDark)
    $Texture.SetPixel(12, 10, $gemMid)

    # One whole boss gem in the center of the chestplate.
    foreach ($point in @(
        @(23, 21), @(24, 21), @(22, 22), @(25, 22),
        @(22, 27), @(25, 27), @(23, 28), @(24, 28)
    )) {
        $Texture.SetPixel($point[0], $point[1], $purpleDark)
    }
    foreach ($y in 23..26) {
        $Texture.SetPixel(22, $y, $purpleBright)
        $Texture.SetPixel(25, $y, $purpleDark)
        $Texture.SetPixel(23, $y, $gemMid)
        $Texture.SetPixel(24, $y, $gemDark)
    }
    $Texture.SetPixel(24, 23, $gemLight)
    $Texture.SetPixel(23, 24, $white)

    # One shard on the front of each boot. The shared leg UV mirrors it onto both boots.
    $Texture.SetPixel(5, 26, $purpleDark)
    $Texture.SetPixel(6, 26, $purpleBright)
    $Texture.SetPixel(4, 27, $purpleDark)
    $Texture.SetPixel(7, 27, $purpleDark)
    $Texture.SetPixel(5, 27, $gemMid)
    $Texture.SetPixel(6, 27, $gemLight)
    $Texture.SetPixel(5, 28, $gemDark)
    $Texture.SetPixel(6, 28, $gemMid)
    $Texture.SetPixel(5, 29, $purpleDark)
    $Texture.SetPixel(6, 29, $purpleBright)
}

function Get-AdjustedColor {
    param(
        [System.Drawing.Color]$Color,
        [double]$Factor
    )

    return [System.Drawing.Color]::FromArgb(
        $Color.A,
        [Math]::Min(255, [Math]::Max(0, [Math]::Round($Color.R * $Factor))),
        [Math]::Min(255, [Math]::Max(0, [Math]::Round($Color.G * $Factor))),
        [Math]::Min(255, [Math]::Max(0, [Math]::Round($Color.B * $Factor)))
    )
}

function Convert-ToHdArmor {
    param(
        [System.Drawing.Bitmap]$Source,
        [bool]$Elevated
    )

    $scale = 4
    $output = [System.Drawing.Bitmap]::new(256, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for ($y = 0; $y -lt 128; $y++) {
        for ($x = 0; $x -lt 256; $x++) {
            $sourceX = [Math]::Floor($x / $scale)
            $sourceY = [Math]::Floor($y / $scale)
            $sourceColor = $Source.GetPixel($sourceX, $sourceY)
            if ($sourceColor.A -eq 0) { continue }

            # A quiet forged base. The old version outlined every 4x4 source pixel,
            # which created the unwanted horizontal/grid-like pattern in game.
            $factor = 0.92 + ((($x + ($y * 2)) % 17) / 170.0)
            if ($Elevated) { $factor += 0.035 }
            $output.SetPixel($x, $y, (Get-AdjustedColor -Color $sourceColor -Factor $factor))
        }
    }

    # One-pixel dark silhouette edge makes the armor read as a constructed object
    # without turning every source texel into a stripe.
    $edge = [System.Drawing.Color]::FromArgb(255, 2, 13, 9)
    for ($y = 1; $y -lt 127; $y++) {
        for ($x = 1; $x -lt 255; $x++) {
            if ($output.GetPixel($x, $y).A -eq 0) { continue }
            if (
                $output.GetPixel($x - 1, $y).A -eq 0 -or
                $output.GetPixel($x + 1, $y).A -eq 0 -or
                $output.GetPixel($x, $y - 1).A -eq 0 -or
                $output.GetPixel($x, $y + 1).A -eq 0
            ) {
                $output.SetPixel($x, $y, $edge)
            }
        }
    }
    return $output
}

function Set-HdPixelIfOpaque {
    param(
        [System.Drawing.Bitmap]$Texture,
        [int]$X,
        [int]$Y,
        [System.Drawing.Color]$Color
    )

    if ($X -ge 0 -and $Y -ge 0 -and $X -lt 256 -and $Y -lt 128 -and $Texture.GetPixel($X, $Y).A -gt 0) {
        $Texture.SetPixel($X, $Y, $Color)
    }
}

function Add-HdRivet {
    param(
        [System.Drawing.Bitmap]$Texture,
        [int]$CenterX,
        [int]$CenterY,
        [bool]$Elevated
    )

    $rim = if ($Elevated) {
        [System.Drawing.Color]::FromArgb(255, 110, 91, 34)
    } else {
        [System.Drawing.Color]::FromArgb(255, 79, 72, 36)
    }
    $center = if ($Elevated) {
        [System.Drawing.Color]::FromArgb(255, 224, 202, 94)
    } else {
        [System.Drawing.Color]::FromArgb(255, 167, 157, 76)
    }
    foreach ($point in @(@(-1, 0), @(0, -1), @(1, 0), @(0, 1))) {
        Set-HdPixelIfOpaque -Texture $Texture -X ($CenterX + $point[0]) -Y ($CenterY + $point[1]) -Color $rim
    }
    Set-HdPixelIfOpaque -Texture $Texture -X $CenterX -Y $CenterY -Color $center
}

function Get-HandPaintPalette {
    param([bool]$Elevated)

    if ($Elevated) {
        return @{
            Outline = [System.Drawing.Color]::FromArgb(255, 2, 15, 10)
            Recess = [System.Drawing.Color]::FromArgb(255, 5, 31, 20)
            Shadow = [System.Drawing.Color]::FromArgb(255, 9, 54, 30)
            Mid = [System.Drawing.Color]::FromArgb(255, 20, 91, 42)
            Face = [System.Drawing.Color]::FromArgb(255, 40, 132, 55)
            Edge = [System.Drawing.Color]::FromArgb(255, 99, 194, 79)
            Shine = [System.Drawing.Color]::FromArgb(255, 164, 232, 111)
            Rivet = [System.Drawing.Color]::FromArgb(255, 193, 174, 72)
            BoneDark = [System.Drawing.Color]::FromArgb(255, 115, 99, 61)
            Bone = [System.Drawing.Color]::FromArgb(255, 220, 207, 156)
            BoneLight = [System.Drawing.Color]::FromArgb(255, 255, 245, 199)
        }
    }

    return @{
        Outline = [System.Drawing.Color]::FromArgb(255, 3, 17, 12)
        Recess = [System.Drawing.Color]::FromArgb(255, 7, 35, 23)
        Shadow = [System.Drawing.Color]::FromArgb(255, 12, 57, 33)
        Mid = [System.Drawing.Color]::FromArgb(255, 23, 86, 41)
        Face = [System.Drawing.Color]::FromArgb(255, 42, 119, 51)
        Edge = [System.Drawing.Color]::FromArgb(255, 89, 164, 69)
        Shine = [System.Drawing.Color]::FromArgb(255, 139, 199, 91)
        Rivet = [System.Drawing.Color]::FromArgb(255, 145, 132, 63)
        BoneDark = [System.Drawing.Color]::FromArgb(255, 119, 103, 68)
        Bone = [System.Drawing.Color]::FromArgb(255, 214, 200, 148)
        BoneLight = [System.Drawing.Color]::FromArgb(255, 243, 232, 184)
    }
}

function New-LeafPoints {
    param(
        [int]$CenterX,
        [int]$CenterY,
        [int]$Width,
        [int]$Height
    )

    $halfW = [Math]::Max(2, [Math]::Floor($Width / 2))
    $halfH = [Math]::Max(2, [Math]::Floor($Height / 2))
    return [System.Drawing.Point[]]@(
        [System.Drawing.Point]::new($CenterX, $CenterY - $halfH),
        [System.Drawing.Point]::new($CenterX - $halfW, $CenterY - [Math]::Floor($halfH / 3)),
        [System.Drawing.Point]::new($CenterX - [Math]::Floor($halfW * 0.82), $CenterY + [Math]::Floor($halfH * 0.34)),
        [System.Drawing.Point]::new($CenterX, $CenterY + $halfH),
        [System.Drawing.Point]::new($CenterX + [Math]::Floor($halfW * 0.82), $CenterY + [Math]::Floor($halfH * 0.34)),
        [System.Drawing.Point]::new($CenterX + $halfW, $CenterY - [Math]::Floor($halfH / 3))
    )
}

function Draw-LeafScale {
    param(
        [System.Drawing.Bitmap]$Layer,
        [hashtable]$Palette,
        [int]$CenterX,
        [int]$CenterY,
        [int]$Width,
        [int]$Height,
        [bool]$Elevated
    )

    $graphics = [System.Drawing.Graphics]::FromImage($Layer)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
    $outlineBrush = [System.Drawing.SolidBrush]::new($Palette.Outline)
    $shadowBrush = [System.Drawing.SolidBrush]::new($Palette.Shadow)
    $faceBrush = [System.Drawing.SolidBrush]::new($Palette.Face)
    $edgePen = [System.Drawing.Pen]::new($Palette.Edge, $(if ($Elevated) { 2 } else { 1 }))
    $shinePen = [System.Drawing.Pen]::new($Palette.Shine, 1)
    try {
        $outer = New-LeafPoints -CenterX $CenterX -CenterY $CenterY -Width $Width -Height $Height
        $middle = New-LeafPoints -CenterX $CenterX -CenterY ($CenterY - 1) -Width ([Math]::Max(4, $Width - 4)) -Height ([Math]::Max(5, $Height - 4))
        $inner = New-LeafPoints -CenterX ($CenterX - 1) -CenterY ($CenterY - 2) -Width ([Math]::Max(3, $Width - 8)) -Height ([Math]::Max(4, $Height - 8))
        $graphics.FillPolygon($outlineBrush, $outer)
        $graphics.FillPolygon($shadowBrush, $middle)
        $graphics.FillPolygon($faceBrush, $inner)
        $graphics.DrawLine($edgePen, $outer[0], $outer[1])
        $graphics.DrawLine($edgePen, $outer[0], $outer[5])
        $graphics.DrawLine($shinePen, $CenterX - 1, $CenterY - [Math]::Floor($Height / 3), $CenterX - 1, $CenterY + [Math]::Floor($Height / 5))
        if ($Elevated -and $Width -ge 12) {
            $graphics.DrawLine($shinePen, $CenterX + 1, $CenterY - [Math]::Floor($Height / 4), $CenterX + [Math]::Floor($Width / 5), $CenterY)
        }
    }
    finally {
        $outlineBrush.Dispose()
        $shadowBrush.Dispose()
        $faceBrush.Dispose()
        $edgePen.Dispose()
        $shinePen.Dispose()
        $graphics.Dispose()
    }
}

function Draw-SegmentedBand {
    param(
        [System.Drawing.Bitmap]$Layer,
        [hashtable]$Palette,
        [int]$Left,
        [int]$Top,
        [int]$Right,
        [int]$Bottom,
        [bool]$Elevated
    )

    $graphics = [System.Drawing.Graphics]::FromImage($Layer)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $outlineBrush = [System.Drawing.SolidBrush]::new($Palette.Outline)
    $midBrush = [System.Drawing.SolidBrush]::new($Palette.Mid)
    $edgePen = [System.Drawing.Pen]::new($Palette.Edge, $(if ($Elevated) { 2 } else { 1 }))
    try {
        $outer = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($Left + 1, $Top),
            [System.Drawing.Point]::new($Right - 1, $Top),
            [System.Drawing.Point]::new($Right, $Top + 2),
            [System.Drawing.Point]::new($Right - 2, $Bottom),
            [System.Drawing.Point]::new($Left + 2, $Bottom),
            [System.Drawing.Point]::new($Left, $Top + 2)
        )
        $inner = [System.Drawing.Rectangle]::FromLTRB($Left + 2, $Top + 2, $Right - 1, $Bottom - 1)
        $graphics.FillPolygon($outlineBrush, $outer)
        if ($inner.Width -gt 0 -and $inner.Height -gt 0) { $graphics.FillRectangle($midBrush, $inner) }
        $graphics.DrawLine($edgePen, $Left + 2, $Top + 1, $Right - 2, $Top + 1)

        $segmentWidth = [Math]::Max(5, [Math]::Floor(($Right - $Left) / 3))
        for ($x = $Left + $segmentWidth; $x -lt $Right - 2; $x += $segmentWidth) {
            $graphics.DrawLine($edgePen, $x, $Top + 2, $x - 1, $Bottom - 2)
        }
    }
    finally {
        $outlineBrush.Dispose()
        $midBrush.Dispose()
        $edgePen.Dispose()
        $graphics.Dispose()
    }
}

function Draw-Claw {
    param(
        [System.Drawing.Bitmap]$Layer,
        [hashtable]$Palette,
        [int]$CenterX,
        [int]$Top,
        [int]$Bottom
    )

    $graphics = [System.Drawing.Graphics]::FromImage($Layer)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $darkBrush = [System.Drawing.SolidBrush]::new($Palette.BoneDark)
    $boneBrush = [System.Drawing.SolidBrush]::new($Palette.Bone)
    $lightPen = [System.Drawing.Pen]::new($Palette.BoneLight, 1)
    try {
        $outer = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($CenterX - 3, $Top + 1),
            [System.Drawing.Point]::new($CenterX + 2, $Top),
            [System.Drawing.Point]::new($CenterX + 2, $Bottom - 3),
            [System.Drawing.Point]::new($CenterX, $Bottom),
            [System.Drawing.Point]::new($CenterX - 2, $Bottom - 2)
        )
        $inner = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($CenterX - 1, $Top + 2),
            [System.Drawing.Point]::new($CenterX + 1, $Top + 1),
            [System.Drawing.Point]::new($CenterX + 1, $Bottom - 3),
            [System.Drawing.Point]::new($CenterX, $Bottom - 1),
            [System.Drawing.Point]::new($CenterX - 1, $Bottom - 3)
        )
        $graphics.FillPolygon($darkBrush, $outer)
        $graphics.FillPolygon($boneBrush, $inner)
        $graphics.DrawLine($lightPen, $CenterX, $Top + 2, $CenterX, $Bottom - 4)
    }
    finally {
        $darkBrush.Dispose()
        $boneBrush.Dispose()
        $lightPen.Dispose()
        $graphics.Dispose()
    }
}

function Draw-BoneHorn {
    param(
        [System.Drawing.Bitmap]$Layer,
        [hashtable]$Palette,
        [int]$BaseX,
        [int]$BaseY,
        [int]$Direction
    )

    $graphics = [System.Drawing.Graphics]::FromImage($Layer)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $darkBrush = [System.Drawing.SolidBrush]::new($Palette.BoneDark)
    $boneBrush = [System.Drawing.SolidBrush]::new($Palette.Bone)
    try {
        $outer = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($BaseX - (3 * $Direction), $BaseY + 7),
            [System.Drawing.Point]::new($BaseX + (3 * $Direction), $BaseY + 6),
            [System.Drawing.Point]::new($BaseX + (2 * $Direction), $BaseY + 1),
            [System.Drawing.Point]::new($BaseX, $BaseY - 5),
            [System.Drawing.Point]::new($BaseX - $Direction, $BaseY + 2)
        )
        $inner = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($BaseX - $Direction, $BaseY + 5),
            [System.Drawing.Point]::new($BaseX + (2 * $Direction), $BaseY + 4),
            [System.Drawing.Point]::new($BaseX + $Direction, $BaseY + 1),
            [System.Drawing.Point]::new($BaseX, $BaseY - 3)
        )
        $graphics.FillPolygon($darkBrush, $outer)
        $graphics.FillPolygon($boneBrush, $inner)
    }
    finally {
        $darkBrush.Dispose()
        $boneBrush.Dispose()
        $graphics.Dispose()
    }
}

function Merge-HdLayer {
    param(
        [System.Drawing.Bitmap]$Texture,
        [System.Drawing.Bitmap]$Layer
    )

    for ($y = 0; $y -lt 128; $y++) {
        for ($x = 0; $x -lt 256; $x++) {
            if ($Texture.GetPixel($x, $y).A -eq 0) { continue }
            $paint = $Layer.GetPixel($x, $y)
            if ($paint.A -gt 0) { $Texture.SetPixel($x, $y, $paint) }
        }
    }
}

function Get-FirstItemFrame {
    param([System.Drawing.Bitmap]$Sheet)

    $frameSize = $Sheet.Width
    return $Sheet.Clone(
        [System.Drawing.Rectangle]::new(0, 0, $frameSize, $frameSize),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
}

function New-NormalizedCrop {
    param(
        [System.Drawing.Bitmap]$Image,
        [double]$Left,
        [double]$Top,
        [double]$Right,
        [double]$Bottom
    )

    $x = [Math]::Max(0, [Math]::Floor($Image.Width * $Left))
    $y = [Math]::Max(0, [Math]::Floor($Image.Height * $Top))
    $rightPixel = [Math]::Min($Image.Width, [Math]::Ceiling($Image.Width * $Right))
    $bottomPixel = [Math]::Min($Image.Height, [Math]::Ceiling($Image.Height * $Bottom))
    return [System.Drawing.Rectangle]::FromLTRB($x, $y, $rightPixel, $bottomPixel)
}

function Paint-ItemCrop {
    param(
        [System.Drawing.Bitmap]$Texture,
        [System.Drawing.Bitmap]$ClipMask,
        [System.Drawing.Bitmap]$Item,
        [System.Drawing.Rectangle]$SourceRectangle,
        [System.Drawing.Rectangle]$DestinationRectangle,
        [bool]$ClearDestination,
        [double]$Brightness = 1.0
    )

    if ($ClearDestination) {
        for ($y = $DestinationRectangle.Top; $y -lt $DestinationRectangle.Bottom; $y++) {
            for ($x = $DestinationRectangle.Left; $x -lt $DestinationRectangle.Right; $x++) {
                if ($x -ge 0 -and $y -ge 0 -and $x -lt 256 -and $y -lt 128) {
                    $Texture.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
                }
            }
        }
    }

    $layer = [System.Drawing.Bitmap]::new(256, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($layer)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $graphics.DrawImage(
            $Item,
            $DestinationRectangle,
            $SourceRectangle.X,
            $SourceRectangle.Y,
            $SourceRectangle.Width,
            $SourceRectangle.Height,
            [System.Drawing.GraphicsUnit]::Pixel
        )
    }
    finally {
        $graphics.Dispose()
    }

    try {
        for ($y = [Math]::Max(0, $DestinationRectangle.Top); $y -lt [Math]::Min(128, $DestinationRectangle.Bottom); $y++) {
            for ($x = [Math]::Max(0, $DestinationRectangle.Left); $x -lt [Math]::Min(256, $DestinationRectangle.Right); $x++) {
                if ($ClipMask.GetPixel($x, $y).A -eq 0) { continue }
                $paint = $layer.GetPixel($x, $y)
                if ($paint.A -eq 0) { continue }
                if ([Math]::Abs($Brightness - 1.0) -gt 0.001) {
                    $paint = Get-AdjustedColor -Color $paint -Factor $Brightness
                }
                $Texture.SetPixel($x, $y, $paint)
            }
        }
    }
    finally {
        $layer.Dispose()
    }
}

function Apply-ItemReferenceRedraw {
    param(
        [System.Drawing.Bitmap]$Texture,
        [System.Drawing.Bitmap]$HelmetSheet,
        [System.Drawing.Bitmap]$ChestSheet,
        [System.Drawing.Bitmap]$LeggingsSheet,
        [System.Drawing.Bitmap]$BootsSheet,
        [bool]$LeggingsLayer
    )

    $clipMask = $Texture.Clone(
        [System.Drawing.Rectangle]::new(0, 0, 256, 128),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $helmet = Get-FirstItemFrame -Sheet $HelmetSheet
    $chest = Get-FirstItemFrame -Sheet $ChestSheet
    $leggings = Get-FirstItemFrame -Sheet $LeggingsSheet
    $boots = Get-FirstItemFrame -Sheet $BootsSheet
    try {
        if ($LeggingsLayer) {
            # The waist and each visible leg face come from different, intentional
            # areas of the supplied leggings item instead of one repeating scale.
            $waist = New-NormalizedCrop -Image $leggings -Left 0.14 -Top 0.05 -Right 0.86 -Bottom 0.46
            $crotch = New-NormalizedCrop -Image $leggings -Left 0.23 -Top 0.28 -Right 0.77 -Bottom 0.68
            $leftLeg = New-NormalizedCrop -Image $leggings -Left 0.12 -Top 0.27 -Right 0.50 -Bottom 0.96
            $rightLeg = New-NormalizedCrop -Image $leggings -Left 0.50 -Top 0.27 -Right 0.88 -Bottom 0.96
            $leftEdge = New-NormalizedCrop -Image $leggings -Left 0.06 -Top 0.22 -Right 0.36 -Bottom 0.94
            $rightEdge = New-NormalizedCrop -Image $leggings -Left 0.64 -Top 0.22 -Right 0.94 -Bottom 0.94

            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $waist -DestinationRectangle ([System.Drawing.Rectangle]::new(80, 80, 32, 22)) -ClearDestination $true
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $crotch -DestinationRectangle ([System.Drawing.Rectangle]::new(80, 98, 32, 30)) -ClearDestination $false
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $leftLeg -DestinationRectangle ([System.Drawing.Rectangle]::new(16, 80, 16, 48)) -ClearDestination $true
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $leftEdge -DestinationRectangle ([System.Drawing.Rectangle]::new(0, 80, 16, 48)) -ClearDestination $false -Brightness 0.84
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $rightEdge -DestinationRectangle ([System.Drawing.Rectangle]::new(32, 80, 16, 48)) -ClearDestination $false -Brightness 0.91
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $rightLeg -DestinationRectangle ([System.Drawing.Rectangle]::new(48, 80, 16, 48)) -ClearDestination $false -Brightness 0.76

            # Rear girdle: side lames meet at a plain forged spine; the front gem
            # cannot be duplicated on the back.
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $leftEdge -DestinationRectangle ([System.Drawing.Rectangle]::new(128, 80, 16, 48)) -ClearDestination $false -Brightness 0.73
            Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $leggings -SourceRectangle $rightEdge -DestinationRectangle ([System.Drawing.Rectangle]::new(144, 80, 16, 48)) -ClearDestination $false -Brightness 0.78
            return
        }

        # Helmet front: copy the actual crown, cheek guards and face opening.
        # This is the key identity surface and must not be replaced with a generic motif.
        $helmetFull = New-NormalizedCrop -Image $helmet -Left 0.10 -Top 0.01 -Right 0.90 -Bottom 0.97
        $helmetLeft = New-NormalizedCrop -Image $helmet -Left 0.09 -Top 0.10 -Right 0.44 -Bottom 0.95
        $helmetRight = New-NormalizedCrop -Image $helmet -Left 0.56 -Top 0.10 -Right 0.91 -Bottom 0.95
        $helmetLeftBack = New-NormalizedCrop -Image $helmet -Left 0.09 -Top 0.18 -Right 0.31 -Bottom 0.92
        $helmetRightBack = New-NormalizedCrop -Image $helmet -Left 0.69 -Top 0.18 -Right 0.91 -Bottom 0.92
        $helmetCrown = New-NormalizedCrop -Image $helmet -Left 0.24 -Top 0.01 -Right 0.76 -Bottom 0.42

        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $helmet -SourceRectangle $helmetFull -DestinationRectangle ([System.Drawing.Rectangle]::new(32, 32, 32, 32)) -ClearDestination $true
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $helmet -SourceRectangle $helmetLeft -DestinationRectangle ([System.Drawing.Rectangle]::new(0, 32, 32, 32)) -ClearDestination $false -Brightness 0.87
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $helmet -SourceRectangle $helmetRight -DestinationRectangle ([System.Drawing.Rectangle]::new(64, 32, 32, 32)) -ClearDestination $false -Brightness 0.94
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $helmet -SourceRectangle $helmetLeftBack -DestinationRectangle ([System.Drawing.Rectangle]::new(96, 32, 16, 32)) -ClearDestination $false -Brightness 0.72
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $helmet -SourceRectangle $helmetRightBack -DestinationRectangle ([System.Drawing.Rectangle]::new(112, 32, 16, 32)) -ClearDestination $false -Brightness 0.78
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $helmet -SourceRectangle $helmetCrown -DestinationRectangle ([System.Drawing.Rectangle]::new(32, 0, 32, 32)) -ClearDestination $false -Brightness 0.93

        # Chest front uses the exact breastplate sprite. Side lames, shoulders,
        # arm guards and back are derived from distinct source zones.
        $chestCenter = New-NormalizedCrop -Image $chest -Left 0.18 -Top 0.03 -Right 0.82 -Bottom 0.95
        $chestLeft = New-NormalizedCrop -Image $chest -Left 0.03 -Top 0.04 -Right 0.34 -Bottom 0.94
        $chestRight = New-NormalizedCrop -Image $chest -Left 0.66 -Top 0.04 -Right 0.97 -Bottom 0.94
        $leftShoulder = New-NormalizedCrop -Image $chest -Left 0.02 -Top 0.02 -Right 0.38 -Bottom 0.55
        $rightShoulder = New-NormalizedCrop -Image $chest -Left 0.62 -Top 0.02 -Right 0.98 -Bottom 0.55
        $leftLower = New-NormalizedCrop -Image $chest -Left 0.03 -Top 0.39 -Right 0.37 -Bottom 0.97
        $rightLower = New-NormalizedCrop -Image $chest -Left 0.63 -Top 0.39 -Right 0.97 -Bottom 0.97

        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestCenter -DestinationRectangle ([System.Drawing.Rectangle]::new(80, 80, 32, 48)) -ClearDestination $true
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestLeft -DestinationRectangle ([System.Drawing.Rectangle]::new(64, 80, 16, 48)) -ClearDestination $false -Brightness 0.84
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestRight -DestinationRectangle ([System.Drawing.Rectangle]::new(112, 80, 16, 48)) -ClearDestination $false -Brightness 0.90
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestLeft -DestinationRectangle ([System.Drawing.Rectangle]::new(128, 80, 16, 48)) -ClearDestination $false -Brightness 0.72
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestRight -DestinationRectangle ([System.Drawing.Rectangle]::new(144, 80, 16, 48)) -ClearDestination $false -Brightness 0.77

        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $leftShoulder -DestinationRectangle ([System.Drawing.Rectangle]::new(176, 80, 16, 24)) -ClearDestination $false
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $leftLower -DestinationRectangle ([System.Drawing.Rectangle]::new(176, 100, 16, 28)) -ClearDestination $false -Brightness 0.90
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestLeft -DestinationRectangle ([System.Drawing.Rectangle]::new(160, 80, 16, 48)) -ClearDestination $false -Brightness 0.78
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $chestRight -DestinationRectangle ([System.Drawing.Rectangle]::new(192, 80, 16, 48)) -ClearDestination $false -Brightness 0.84
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $rightShoulder -DestinationRectangle ([System.Drawing.Rectangle]::new(208, 80, 16, 24)) -ClearDestination $false -Brightness 0.82
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $chest -SourceRectangle $rightLower -DestinationRectangle ([System.Drawing.Rectangle]::new(208, 100, 16, 28)) -ClearDestination $false -Brightness 0.76

        # Boots: only the front receives the toe/claw silhouette and boot gem.
        # Other faces use shaft material cropped above the toes, so claws never
        # repeat around the entire leg cube.
        $bootFront = New-NormalizedCrop -Image $boots -Left 0.01 -Top 0.04 -Right 0.50 -Bottom 0.99
        $bootOuter = New-NormalizedCrop -Image $boots -Left 0.08 -Top 0.05 -Right 0.42 -Bottom 0.79
        $bootInner = New-NormalizedCrop -Image $boots -Left 0.58 -Top 0.05 -Right 0.92 -Bottom 0.79
        $bootBack = New-NormalizedCrop -Image $boots -Left 0.28 -Top 0.03 -Right 0.72 -Bottom 0.76
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $boots -SourceRectangle $bootFront -DestinationRectangle ([System.Drawing.Rectangle]::new(16, 80, 16, 48)) -ClearDestination $true
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $boots -SourceRectangle $bootOuter -DestinationRectangle ([System.Drawing.Rectangle]::new(0, 80, 16, 48)) -ClearDestination $false -Brightness 0.82
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $boots -SourceRectangle $bootInner -DestinationRectangle ([System.Drawing.Rectangle]::new(32, 80, 16, 48)) -ClearDestination $false -Brightness 0.89
        Paint-ItemCrop -Texture $Texture -ClipMask $clipMask -Item $boots -SourceRectangle $bootBack -DestinationRectangle ([System.Drawing.Rectangle]::new(48, 80, 16, 48)) -ClearDestination $false -Brightness 0.72
    }
    finally {
        $helmet.Dispose()
        $chest.Dispose()
        $leggings.Dispose()
        $boots.Dispose()
        $clipMask.Dispose()
    }
}

function Add-HdSurfaceDetails {
    param(
        [System.Drawing.Bitmap]$Texture,
        [bool]$Elevated,
        [bool]$Leggings
    )

    $palette = Get-HandPaintPalette -Elevated $Elevated
    $layer = [System.Drawing.Bitmap]::new(256, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        if ($Leggings) {
            # Waist: one constructed segmented girdle, followed by broad fauld plates.
            Draw-SegmentedBand -Layer $layer -Palette $palette -Left 80 -Top 80 -Right 111 -Bottom 90 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX 95 -CenterY 97 -Width 25 -Height 18 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX 87 -CenterY 112 -Width 14 -Height 18 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX 103 -CenterY 112 -Width 14 -Height 18 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX 95 -CenterY 121 -Width 17 -Height 14 -Elevated $Elevated

            # Leg faces: three large fitted plates rather than wallpaper rows.
            foreach ($faceCenter in @(7, 23, 39, 55)) {
                Draw-LeafScale -Layer $layer -Palette $palette -CenterX $faceCenter -CenterY 87 -Width 13 -Height 13 -Elevated $Elevated
                Draw-LeafScale -Layer $layer -Palette $palette -CenterX $faceCenter -CenterY 101 -Width 12 -Height 15 -Elevated $Elevated
                Draw-LeafScale -Layer $layer -Palette $palette -CenterX $faceCenter -CenterY 117 -Width 13 -Height 18 -Elevated $Elevated
            }
            foreach ($sideCenter in @(71, 119, 135, 151)) {
                Draw-LeafScale -Layer $layer -Palette $palette -CenterX $sideCenter -CenterY 94 -Width 12 -Height 19 -Elevated $Elevated
                Draw-LeafScale -Layer $layer -Palette $palette -CenterX $sideCenter -CenterY 115 -Width 12 -Height 20 -Elevated $Elevated
            }

            Merge-HdLayer -Texture $Texture -Layer $layer
            foreach ($point in @(@(83, 85), @(107, 85), @(18, 88), @(28, 88), @(18, 118), @(28, 118))) {
                Add-HdRivet -Texture $Texture -CenterX $point[0] -CenterY $point[1] -Elevated $Elevated
            }
            return
        }

        # Helmet crown and face frame. The mask preserves the large open face.
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 48 -CenterY 14 -Width 18 -Height 25 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 48 -CenterY 37 -Width 20 -Height 13 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 38 -CenterY 45 -Width 13 -Height 17 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 58 -CenterY 45 -Width 13 -Height 17 -Elevated $Elevated
        foreach ($center in @(@(8, 41), @(23, 46), @(72, 46), @(87, 41), @(104, 40), @(120, 40), @(112, 49), @(103, 58), @(121, 58))) {
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $center[0] -CenterY $center[1] -Width 13 -Height 15 -Elevated $Elevated
        }

        # Chest front: recognizable stacked breast scales from the supplied item sprite.
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 87 -CenterY 86 -Width 17 -Height 14 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 103 -CenterY 86 -Width 17 -Height 14 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 95 -CenterY 96 -Width 22 -Height 17 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 86 -CenterY 106 -Width 16 -Height 17 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 104 -CenterY 106 -Width 16 -Height 17 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 95 -CenterY 118 -Width 22 -Height 19 -Elevated $Elevated

        # Back: a central protective spine with paired scapular plates.
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 143 -CenterY 86 -Width 16 -Height 14 -Elevated $Elevated
        Draw-LeafScale -Layer $layer -Palette $palette -CenterX 151 -CenterY 86 -Width 16 -Height 14 -Elevated $Elevated
        foreach ($y in @(96, 108, 120)) {
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX 143 -CenterY $y -Width 18 -Height 16 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX 152 -CenterY $y -Width 14 -Height 15 -Elevated $Elevated
        }

        # Side torso lames.
        foreach ($x in @(71, 119)) {
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 91 -Width 13 -Height 18 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 109 -Width 13 -Height 20 -Elevated $Elevated
            Draw-SegmentedBand -Layer $layer -Palette $palette -Left ($x - 7) -Top 118 -Right ($x + 7) -Bottom 126 -Elevated $Elevated
        }

        # Arms: rounded scale pauldrons, elbow plate and a built bracer.
        foreach ($x in @(167, 183, 199, 215)) {
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 84 -Width 14 -Height 13 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 96 -Width 13 -Height 15 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 110 -Width 14 -Height 16 -Elevated $Elevated
            Draw-SegmentedBand -Layer $layer -Palette $palette -Left ($x - 7) -Top 116 -Right ($x + 7) -Bottom 126 -Elevated $Elevated
        }

        # Boots: broad shin plates and an articulated toe cap.
        foreach ($x in @(7, 23, 39, 55)) {
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 86 -Width 13 -Height 13 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 101 -Width 13 -Height 17 -Elevated $Elevated
            Draw-LeafScale -Layer $layer -Palette $palette -CenterX $x -CenterY 115 -Width 14 -Height 14 -Elevated $Elevated
        }
        Draw-Claw -Layer $layer -Palette $palette -CenterX 18 -Top 118 -Bottom 127
        Draw-Claw -Layer $layer -Palette $palette -CenterX 23 -Top 117 -Bottom 127
        Draw-Claw -Layer $layer -Palette $palette -CenterX 28 -Top 118 -Bottom 127

        if ($Elevated) {
            # Ivory shoulder-horn inlays from the boss chestplate item.
            Draw-BoneHorn -Layer $layer -Palette $palette -BaseX 179 -BaseY 84 -Direction -1
            Draw-BoneHorn -Layer $layer -Palette $palette -BaseX 188 -BaseY 84 -Direction 1
            Draw-BoneHorn -Layer $layer -Palette $palette -BaseX 211 -BaseY 84 -Direction -1
            Draw-BoneHorn -Layer $layer -Palette $palette -BaseX 220 -BaseY 84 -Direction 1
        }

        Merge-HdLayer -Texture $Texture -Layer $layer
        foreach ($point in @(
            @(36, 36), @(60, 36), @(83, 84), @(107, 84), @(67, 120), @(123, 120),
            @(179, 88), @(188, 88), @(211, 88), @(220, 88), @(18, 88), @(28, 88)
        )) {
            Add-HdRivet -Texture $Texture -CenterX $point[0] -CenterY $point[1] -Elevated $Elevated
        }
    }
    finally {
        $layer.Dispose()
    }
}

function Add-HdGem {
    param(
        [System.Drawing.Bitmap]$Texture,
        [int]$CenterX,
        [int]$CenterY,
        [int]$HalfWidth,
        [int]$HalfHeight
    )

    $bezelDark = [System.Drawing.Color]::FromArgb(255, 39, 7, 58)
    $bezelMid = [System.Drawing.Color]::FromArgb(255, 105, 28, 151)
    $bezelLight = [System.Drawing.Color]::FromArgb(255, 211, 113, 247)
    $gemDeep = [System.Drawing.Color]::FromArgb(255, 2, 67, 27)
    $gemMid = [System.Drawing.Color]::FromArgb(255, 22, 170, 65)
    $gemLight = [System.Drawing.Color]::FromArgb(255, 134, 255, 126)
    $specular = [System.Drawing.Color]::FromArgb(255, 241, 255, 225)

    $graphics = [System.Drawing.Graphics]::FromImage($Texture)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
    $darkBrush = [System.Drawing.SolidBrush]::new($bezelDark)
    $midBrush = [System.Drawing.SolidBrush]::new($bezelMid)
    $gemBrush = [System.Drawing.SolidBrush]::new($gemMid)
    $deepBrush = [System.Drawing.SolidBrush]::new($gemDeep)
    $lightBrush = [System.Drawing.SolidBrush]::new($gemLight)
    $specBrush = [System.Drawing.SolidBrush]::new($specular)
    $bezelPen = [System.Drawing.Pen]::new($bezelLight, 1)
    $facetPen = [System.Drawing.Pen]::new($gemLight, 1)
    try {
        $outer = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($CenterX, $CenterY - $HalfHeight),
            [System.Drawing.Point]::new($CenterX - $HalfWidth, $CenterY - 1),
            [System.Drawing.Point]::new($CenterX, $CenterY + $HalfHeight),
            [System.Drawing.Point]::new($CenterX + $HalfWidth, $CenterY - 1)
        )
        $rim = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($CenterX, $CenterY - $HalfHeight + 2),
            [System.Drawing.Point]::new($CenterX - $HalfWidth + 2, $CenterY),
            [System.Drawing.Point]::new($CenterX, $CenterY + $HalfHeight - 2),
            [System.Drawing.Point]::new($CenterX + $HalfWidth - 2, $CenterY)
        )
        $core = [System.Drawing.Point[]]@(
            [System.Drawing.Point]::new($CenterX, $CenterY - $HalfHeight + 4),
            [System.Drawing.Point]::new($CenterX - [Math]::Max(2, $HalfWidth - 3), $CenterY),
            [System.Drawing.Point]::new($CenterX, $CenterY + $HalfHeight - 4),
            [System.Drawing.Point]::new($CenterX + [Math]::Max(2, $HalfWidth - 3), $CenterY)
        )
        $leftFacet = [System.Drawing.Point[]]@($core[0], $core[1], $core[2], [System.Drawing.Point]::new($CenterX, $CenterY))
        $upperFacet = [System.Drawing.Point[]]@($core[0], $core[3], [System.Drawing.Point]::new($CenterX, $CenterY))

        $graphics.FillPolygon($darkBrush, $outer)
        $graphics.FillPolygon($midBrush, $rim)
        $graphics.FillPolygon($gemBrush, $core)
        $graphics.FillPolygon($deepBrush, $leftFacet)
        $graphics.FillPolygon($lightBrush, $upperFacet)
        $graphics.DrawPolygon($bezelPen, $rim)
        $graphics.DrawLine($facetPen, $core[0], $core[2])
        $graphics.FillRectangle($specBrush, $CenterX - 1, $CenterY - [Math]::Floor($HalfHeight / 2), 2, 2)
    }
    finally {
        $darkBrush.Dispose()
        $midBrush.Dispose()
        $gemBrush.Dispose()
        $deepBrush.Dispose()
        $lightBrush.Dispose()
        $specBrush.Dispose()
        $bezelPen.Dispose()
        $facetPen.Dispose()
        $graphics.Dispose()
    }
}

function Add-FirstfangGemsHd {
    param(
        [System.Drawing.Bitmap]$Texture,
        [bool]$Leggings
    )

    if ($Leggings) {
        Add-HdGem -Texture $Texture -CenterX 95 -CenterY 118 -HalfWidth 5 -HalfHeight 7
        return
    }

    Add-HdGem -Texture $Texture -CenterX 47 -CenterY 39 -HalfWidth 5 -HalfHeight 8
    Add-HdGem -Texture $Texture -CenterX 95 -CenterY 99 -HalfWidth 7 -HalfHeight 12
    Add-HdGem -Texture $Texture -CenterX 23 -CenterY 110 -HalfWidth 5 -HalfHeight 7
}

function Set-PoisonPixel {
    param(
        [System.Drawing.Bitmap]$Texture,
        [int]$X,
        [int]$Y,
        [System.Drawing.Color]$Color,
        [int]$MinX,
        [int]$MinY,
        [int]$MaxX,
        [int]$MaxY
    )

    if ($X -ge $MinX -and $X -le $MaxX -and $Y -ge $MinY -and $Y -le $MaxY) {
        $Texture.SetPixel($X, $Y, $Color)
    }
}

function Add-HdPoisonAura {
    param(
        [System.Drawing.Bitmap]$Texture,
        [int]$Frame,
        [int]$CenterX,
        [int]$CenterY,
        [int]$HalfWidth,
        [int]$HalfHeight,
        [int]$MinX,
        [int]$MinY,
        [int]$MaxX,
        [int]$MaxY
    )

    $radius = @(1, 3, 6, 9, 13, 15, 10, 5)[$Frame]
    $alpha = @(70, 105, 150, 205, 245, 255, 185, 110)[$Frame]
    $dark = [System.Drawing.Color]::FromArgb($alpha, 93, 20, 153)
    $bright = [System.Drawing.Color]::FromArgb([Math]::Min(255, $alpha + 18), 225, 100, 255)
    $spark = [System.Drawing.Color]::FromArgb(255, 244, 194, 255)

    $outerWidth = $HalfWidth + $radius
    $outerHeight = $HalfHeight + $radius
    for ($y = [Math]::Max($MinY, $CenterY - $outerHeight); $y -le [Math]::Min($MaxY, $CenterY + $outerHeight); $y++) {
        for ($x = [Math]::Max($MinX, $CenterX - $outerWidth); $x -le [Math]::Min($MaxX, $CenterX + $outerWidth); $x++) {
            $dx = ($x - $CenterX) / [double]$outerWidth
            $dy = ($y - $CenterY) / [double]$outerHeight
            $distance = ($dx * $dx) + ($dy * $dy)
            if ($distance -ge 0.67 -and $distance -le 1.03) {
                $color = if ((($x * 3) + $y + $Frame) % 5 -le 1) { $bright } else { $dark }
                $Texture.SetPixel($x, $y, $color)
            }
        }
    }

    # The green core itself flashes at the crest, so animation remains readable at normal camera distance.
    $coreAlpha = @(0, 0, 55, 110, 185, 245, 125, 35)[$Frame]
    if ($coreAlpha -gt 0) {
        $coreGreen = [System.Drawing.Color]::FromArgb($coreAlpha, 119, 255, 111)
        $coreWhite = [System.Drawing.Color]::FromArgb([Math]::Min(255, $coreAlpha + 10), 239, 255, 222)
        for ($y = $CenterY - $HalfHeight + 3; $y -le $CenterY + $HalfHeight - 3; $y++) {
            for ($x = $CenterX - $HalfWidth + 2; $x -le $CenterX + $HalfWidth - 2; $x++) {
                $dx = ($x - $CenterX) / [double][Math]::Max(1, $HalfWidth - 2)
                $dy = ($y - $CenterY) / [double][Math]::Max(1, $HalfHeight - 3)
                if ((($dx * $dx) + ($dy * $dy)) -le 1.0) {
                    $Texture.SetPixel($x, $y, $(if ((($x + $y + $Frame) % 7) -eq 0) { $coreWhite } else { $coreGreen }))
                }
            }
        }
    }

    # Frames 3-6 grow moving jagged poison tongues and motes; other frames visibly retract.
    $wispLength = @(0, 1, 4, 8, 13, 16, 10, 4)[$Frame]
    for ($step = 1; $step -le $wispLength; $step++) {
        $jitter = (($step + $Frame) % 3) - 1
        Set-PoisonPixel -Texture $Texture -X ($CenterX + $jitter) -Y ($CenterY - $HalfHeight - $radius - $step) -Color $bright -MinX $MinX -MinY $MinY -MaxX $MaxX -MaxY $MaxY
        Set-PoisonPixel -Texture $Texture -X ($CenterX + $HalfWidth + $radius + $step) -Y ($CenterY + $jitter) -Color $dark -MinX $MinX -MinY $MinY -MaxX $MaxX -MaxY $MaxY
        if ($Frame -ge 4) {
            Set-PoisonPixel -Texture $Texture -X ($CenterX - $HalfWidth - $radius - $step) -Y ($CenterY - $jitter) -Color $bright -MinX $MinX -MinY $MinY -MaxX $MaxX -MaxY $MaxY
        }
    }

    if ($Frame -ge 2 -and $Frame -le 6) {
        foreach ($point in @(
            @(($CenterX + $HalfWidth + $radius + 2), ($CenterY - $HalfHeight + $Frame)),
            @(($CenterX - $HalfWidth - $radius - 2), ($CenterY + 2 - $Frame)),
            @(($CenterX + ($Frame - 4)), ($CenterY - $HalfHeight - $radius - 3))
        )) {
            Set-PoisonPixel -Texture $Texture -X $point[0] -Y $point[1] -Color $spark -MinX $MinX -MinY $MinY -MaxX $MaxX -MaxY $MaxY
            Set-PoisonPixel -Texture $Texture -X ($point[0] + 1) -Y $point[1] -Color $bright -MinX $MinX -MinY $MinY -MaxX $MaxX -MaxY $MaxY
            Set-PoisonPixel -Texture $Texture -X $point[0] -Y ($point[1] + 1) -Color $bright -MinX $MinX -MinY $MinY -MaxX $MaxX -MaxY $MaxY
        }
    }
}

function New-HdAuraFrame {
    param(
        [int]$Frame,
        [bool]$Leggings
    )

    $texture = [System.Drawing.Bitmap]::new(256, 128, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    if ($Leggings) {
        Add-HdPoisonAura -Texture $texture -Frame $Frame -CenterX 95 -CenterY 118 -HalfWidth 5 -HalfHeight 7 -MinX 80 -MinY 108 -MaxX 111 -MaxY 127
    }
    else {
        # Keep the forehead pulse on the brow plate. Letting this aura use the
        # entire front-face rectangle creates an unwanted violet necklace over
        # the open face aperture at the peak frame.
        Add-HdPoisonAura -Texture $texture -Frame $Frame -CenterX 47 -CenterY 39 -HalfWidth 5 -HalfHeight 8 -MinX 34 -MinY 27 -MaxX 61 -MaxY 47
        Add-HdPoisonAura -Texture $texture -Frame $Frame -CenterX 95 -CenterY 99 -HalfWidth 7 -HalfHeight 12 -MinX 80 -MinY 80 -MaxX 111 -MaxY 127
        Add-HdPoisonAura -Texture $texture -Frame $Frame -CenterX 23 -CenterY 110 -HalfWidth 5 -HalfHeight 7 -MinX 16 -MinY 100 -MaxX 31 -MaxY 127
    }
    return $texture
}

function New-AnimatedSheet {
    param(
        [System.Drawing.Bitmap]$BaseTexture,
        [bool]$Leggings
    )

    $sheet = [System.Drawing.Bitmap]::new(64, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($sheet)
    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        for ($frame = 0; $frame -lt 8; $frame++) {
            $frameBitmap = $BaseTexture.Clone(
                [System.Drawing.Rectangle]::new(0, 0, 64, 32),
                [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
            )
            try {
                Add-FirstfangGems -Texture $frameBitmap -Frame $frame -Leggings $Leggings
                $graphics.DrawImageUnscaled($frameBitmap, 0, $frame * 32)
            }
            finally {
                $frameBitmap.Dispose()
            }
        }
    }
    finally {
        $graphics.Dispose()
    }
    return $sheet
}

function Save-NearestPreview {
    param(
        [System.Drawing.Bitmap]$Outer,
        [System.Drawing.Bitmap]$Leggings,
        [string]$Destination
    )

    $scale = 8
    $preview = [System.Drawing.Bitmap]::new(1024, 256, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($preview)
    try {
        $graphics.Clear([System.Drawing.Color]::FromArgb(255, 24, 24, 28))
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.DrawImage($Outer, [System.Drawing.Rectangle]::new(0, 0, 512, 256))
        $graphics.DrawImage($Leggings, [System.Drawing.Rectangle]::new(512, 0, 512, 256))
    }
    finally {
        $graphics.Dispose()
    }
    try {
        $preview.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $preview.Dispose()
    }
}

function Save-HumanoidWithoutHelmetUv {
    param(
        [System.Drawing.Bitmap]$Texture,
        [string]$Destination
    )

    # The normal and elevated sets now use the shared Blockbench helmet.
    # Clear the vanilla humanoid head area so the old cuboid helmet does not
    # render underneath it. Baby armor keeps the original texture because the
    # custom player render layer is not installed on baby humanoid renderers.
    $copy = $Texture.Clone(
        [System.Drawing.Rectangle]::new(0, 0, $Texture.Width, $Texture.Height),
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        for ($y = 0; $y -lt 64; $y++) {
            for ($x = 0; $x -lt 128; $x++) {
                $copy.SetPixel($x, $y, [System.Drawing.Color]::Transparent)
            }
        }
        $copy.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $copy.Dispose()
    }
}

$zip = [System.IO.Compression.ZipFile]::OpenRead($MinecraftClientJar)
try {
    $outerMask = Read-ZipBitmap -Archive $zip -EntryName 'assets/minecraft/textures/entity/equipment/humanoid/diamond.png'
    $leggingsMask = Read-ZipBitmap -Archive $zip -EntryName 'assets/minecraft/textures/entity/equipment/humanoid_leggings/diamond.png'
}
finally {
    $zip.Dispose()
}

try {
    $stoneOuter = Convert-ArmorMask -Mask $outerMask -Elevated $false
    $stoneLeggings = Convert-ArmorMask -Mask $leggingsMask -Elevated $false
    $firstfangOuterBase = Convert-ArmorMask -Mask $outerMask -Elevated $true
    $firstfangLeggingsBase = Convert-ArmorMask -Mask $leggingsMask -Elevated $true
    try {
        $stoneOuterHd = Convert-ToHdArmor -Source $stoneOuter -Elevated $false
        $stoneLeggingsHd = Convert-ToHdArmor -Source $stoneLeggings -Elevated $false
        $firstfangOuter = Convert-ToHdArmor -Source $firstfangOuterBase -Elevated $true
        $firstfangLeggings = Convert-ToHdArmor -Source $firstfangLeggingsBase -Elevated $true
        $stoneHelmetItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'stone_fang_helmet.png')
        $stoneChestItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'stone_fang_chestplate.png')
        $stoneLeggingsItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'stone_fang_leggings.png')
        $stoneBootsItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'stone_fang_boots.png')
        $firstfangHelmetItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'firstfang_helmet.png')
        $firstfangChestItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'firstfang_chestplate.png')
        $firstfangLeggingsItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'firstfang_leggings.png')
        $firstfangBootsItem = Read-FileBitmap -Path (Join-Path $itemTextureRoot 'firstfang_boots.png')
        try {
            Apply-ItemReferenceRedraw -Texture $stoneOuterHd -HelmetSheet $stoneHelmetItem -ChestSheet $stoneChestItem -LeggingsSheet $stoneLeggingsItem -BootsSheet $stoneBootsItem -LeggingsLayer $false
            Apply-ItemReferenceRedraw -Texture $stoneLeggingsHd -HelmetSheet $stoneHelmetItem -ChestSheet $stoneChestItem -LeggingsSheet $stoneLeggingsItem -BootsSheet $stoneBootsItem -LeggingsLayer $true
            Apply-ItemReferenceRedraw -Texture $firstfangOuter -HelmetSheet $firstfangHelmetItem -ChestSheet $firstfangChestItem -LeggingsSheet $firstfangLeggingsItem -BootsSheet $firstfangBootsItem -LeggingsLayer $false
            Apply-ItemReferenceRedraw -Texture $firstfangLeggings -HelmetSheet $firstfangHelmetItem -ChestSheet $firstfangChestItem -LeggingsSheet $firstfangLeggingsItem -BootsSheet $firstfangBootsItem -LeggingsLayer $true

            Save-HumanoidWithoutHelmetUv -Texture $stoneOuterHd -Destination (Join-Path $humanoidOutput 'stone_fang.png')
            $stoneOuterHd.Save((Join-Path $babyOutput 'stone_fang.png'), [System.Drawing.Imaging.ImageFormat]::Png)
            $stoneLeggingsHd.Save((Join-Path $leggingsOutput 'stone_fang.png'), [System.Drawing.Imaging.ImageFormat]::Png)
            Save-HumanoidWithoutHelmetUv -Texture $firstfangOuter -Destination (Join-Path $humanoidOutput 'firstfang.png')
            $firstfangOuter.Save((Join-Path $babyOutput 'firstfang.png'), [System.Drawing.Imaging.ImageFormat]::Png)
            $firstfangLeggings.Save((Join-Path $leggingsOutput 'firstfang.png'), [System.Drawing.Imaging.ImageFormat]::Png)

            for ($frame = 0; $frame -lt 8; $frame++) {
                $auraOuter = New-HdAuraFrame -Frame $frame -Leggings $false
                $auraLeggings = New-HdAuraFrame -Frame $frame -Leggings $true
                try {
                    $auraOuter.Save(
                        (Join-Path $auraOutput ("firstfang_aura_outer_{0}.png" -f $frame)),
                        [System.Drawing.Imaging.ImageFormat]::Png
                    )
                    $auraLeggings.Save(
                        (Join-Path $auraOutput ("firstfang_aura_leggings_{0}.png" -f $frame)),
                        [System.Drawing.Imaging.ImageFormat]::Png
                    )
                }
                finally {
                    $auraOuter.Dispose()
                    $auraLeggings.Dispose()
                }
            }

            Save-NearestPreview -Outer $stoneOuterHd -Leggings $stoneLeggingsHd -Destination (Join-Path $artOutput 'stone_fang_uv_preview.png')
            Save-NearestPreview -Outer $firstfangOuter -Leggings $firstfangLeggings -Destination (Join-Path $artOutput 'firstfang_uv_preview.png')
        }
        finally {
            $stoneOuterHd.Dispose()
            $stoneLeggingsHd.Dispose()
            $firstfangOuter.Dispose()
            $firstfangLeggings.Dispose()
            $stoneHelmetItem.Dispose()
            $stoneChestItem.Dispose()
            $stoneLeggingsItem.Dispose()
            $stoneBootsItem.Dispose()
            $firstfangHelmetItem.Dispose()
            $firstfangChestItem.Dispose()
            $firstfangLeggingsItem.Dispose()
            $firstfangBootsItem.Dispose()
        }
    }
    finally {
        $stoneOuter.Dispose()
        $stoneLeggings.Dispose()
        $firstfangOuterBase.Dispose()
        $firstfangLeggingsBase.Dispose()
    }
}
finally {
    $outerMask.Dispose()
    $leggingsMask.Dispose()
}

Write-Output "Generated vanilla 2D armor layers in $textureRoot"
