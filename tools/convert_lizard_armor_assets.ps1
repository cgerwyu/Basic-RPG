param(
    [string]$SourceDirectory = 'C:\Users\cdald\Desktop\references\ARMOR\lizards',
    [string]$ProjectDirectory = 'C:\Users\cdald\Desktop\basicrpgclasses-26.2'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$itemOutput = Join-Path $ProjectDirectory 'src\main\resources\assets\basicrpgclasses\textures\item'
$armorOutput = Join-Path $ProjectDirectory 'src\main\resources\assets\basicrpgclasses\textures\entity\armor'
$equipmentOutput = Join-Path $ProjectDirectory 'src\main\resources\assets\basicrpgclasses\textures\entity\equipment'
$equipmentHumanoidOutput = Join-Path $equipmentOutput 'humanoid'
$equipmentLeggingsOutput = Join-Path $equipmentOutput 'humanoid_leggings'
$equipmentBabyOutput = Join-Path $equipmentOutput 'humanoid_baby'
New-Item -ItemType Directory -Force -Path `
    $itemOutput, `
    $armorOutput, `
    $equipmentHumanoidOutput, `
    $equipmentLeggingsOutput, `
    $equipmentBabyOutput | Out-Null

$ordinarySprites = [ordered]@{
    'basic helmet.png' = 'stone_fang_helmet.png'
    'basic chestplate.png' = 'stone_fang_chestplate.png'
    'basic leggings.png' = 'stone_fang_leggings.png'
    'basic boots.png' = 'stone_fang_boots.png'
}

foreach ($entry in $ordinarySprites.GetEnumerator()) {
    Copy-Item -LiteralPath (Join-Path $SourceDirectory $entry.Key) -Destination (Join-Path $itemOutput $entry.Value) -Force
}

function Convert-AnimatedItemSheet {
    param(
        [string]$Source,
        [string]$Destination,
        [int]$CellSize = 64
    )

    $sourceBitmap = [System.Drawing.Bitmap]::FromFile($Source)
    try {
        $sheetSize = $CellSize * 3
        if ($sourceBitmap.Width -ne $sheetSize -or $sourceBitmap.Height -ne $sheetSize) {
            throw "Expected a ${sheetSize}x${sheetSize} sprite sheet: $Source"
        }

        $outputBitmap = New-Object System.Drawing.Bitmap $CellSize, ($CellSize * 8), ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($outputBitmap)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                for ($frame = 0; $frame -lt 8; $frame++) {
                    $sourceCell = $frame + 1
                    $sourceX = ($sourceCell % 3) * $CellSize
                    $sourceY = [Math]::Floor($sourceCell / 3) * $CellSize
                    $sourceRect = New-Object System.Drawing.Rectangle $sourceX, $sourceY, $CellSize, $CellSize
                    $destinationRect = New-Object System.Drawing.Rectangle 0, ($frame * $CellSize), $CellSize, $CellSize
                    $graphics.DrawImage($sourceBitmap, $destinationRect, $sourceRect, [System.Drawing.GraphicsUnit]::Pixel)
                }
            }
            finally {
                $graphics.Dispose()
            }
            $outputBitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $outputBitmap.Dispose()
        }
    }
    finally {
        $sourceBitmap.Dispose()
    }
}

$animatedSprites = [ordered]@{
    'pixellab-The-green-gem-embedded-in-the--1788182730378.png' = 'firstfang_helmet.png'
    'Boss chestplate animated.png' = 'firstfang_chestplate.png'
    'Boss leggings animated.png' = 'firstfang_leggings.png'
    'Boss boots animated.png' = 'firstfang_boots.png'
}

foreach ($entry in $animatedSprites.GetEnumerator()) {
    Convert-AnimatedItemSheet `
        -Source (Join-Path $SourceDirectory $entry.Key) `
        -Destination (Join-Path $itemOutput $entry.Value)
}

$gemDirectory = Join-Path (Split-Path $SourceDirectory -Parent) '..\GEMS\lizards'
$gemDirectory = [System.IO.Path]::GetFullPath($gemDirectory)
Convert-AnimatedItemSheet `
    -Source (Join-Path $gemDirectory 'gem animated.png') `
    -Destination (Join-Path $itemOutput 'firstfang_heart.png') `
    -CellSize 32
Copy-Item `
    -LiteralPath (Join-Path $gemDirectory 'shard.png') `
    -Destination (Join-Path $itemOutput 'firstfang_sovereign_shard.png') `
    -Force

function Get-GreenColor {
    param([int]$X, [int]$Y, [bool]$Elevated)

    if ($Elevated) {
        $deep = [System.Drawing.Color]::FromArgb(255, 5, 27, 20)
        $shadow = [System.Drawing.Color]::FromArgb(255, 8, 44, 28)
        $base = [System.Drawing.Color]::FromArgb(255, 14, 68, 37)
        $mid = [System.Drawing.Color]::FromArgb(255, 24, 94, 45)
        $highlight = [System.Drawing.Color]::FromArgb(255, 56, 135, 66)
    }
    else {
        $deep = [System.Drawing.Color]::FromArgb(255, 7, 31, 23)
        $shadow = [System.Drawing.Color]::FromArgb(255, 11, 48, 31)
        $base = [System.Drawing.Color]::FromArgb(255, 18, 69, 39)
        $mid = [System.Drawing.Color]::FromArgb(255, 31, 91, 47)
        $highlight = [System.Drawing.Color]::FromArgb(255, 67, 126, 66)
    }

    # Repeating forged-scale tile: dark seams, a narrow hammered highlight and
    # broad uniform green metal. It intentionally avoids random checker noise,
    # which read as grass camouflage once magnified on the player model.
    $localX = (($X % 16) + 16) % 16
    $localY = (($Y % 16) + 16) % 16
    if ($localX -eq 0 -or $localY -eq 15) {
        return $deep
    }
    if ($localY -eq 0 -or $localY -eq 1) {
        return $highlight
    }
    if ($localX -eq 1 -or $localX -eq 14 -or $localY -eq 13 -or $localY -eq 14) {
        return $shadow
    }
    if ($localX -eq 6 -or $localX -eq 7) {
        return $mid
    }
    if ((($localX + [Math]::Floor($localY / 2)) % 11) -eq 0) {
        return $mid
    }
    return $base
}

function New-ArmorTexture {
    param(
        [string]$Destination,
        [bool]$Elevated
    )

    $bitmap = New-Object System.Drawing.Bitmap 128, 128, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 128; $y++) {
            for ($x = 0; $x -lt 128; $x++) {
                if ($x -ge 64 -and $y -ge 96) {
                    $boneStep = (($x + $y) % 5)
                    $bonePalette = @(
                        [System.Drawing.Color]::FromArgb(255, 91, 82, 58),
                        [System.Drawing.Color]::FromArgb(255, 139, 126, 88),
                        [System.Drawing.Color]::FromArgb(255, 183, 168, 119),
                        [System.Drawing.Color]::FromArgb(255, 220, 207, 157),
                        [System.Drawing.Color]::FromArgb(255, 244, 234, 190)
                    )
                    $color = $bonePalette[$boneStep]
                }
                elseif ($x -ge 64 -and $x -lt 96 -and $y -ge 40 -and $y -lt 80) {
                    if (-not $Elevated) {
                        $color = [System.Drawing.Color]::Transparent
                        $bitmap.SetPixel($x, $y, $color)
                        continue
                    }

                    $localX = ($x - 64) % 16
                    $localY = ($y - 40) % 16
                    $distance = [Math]::Abs($localX - 7.5) + [Math]::Abs($localY - 7.5)
                    if ($distance -gt 10) {
                        $color = [System.Drawing.Color]::FromArgb(255, 35, 9, 50)
                    }
                    elseif ($distance -gt 7) {
                        $color = [System.Drawing.Color]::FromArgb(255, 91, 25, 130)
                    }
                    elseif (($localX + $localY) % 5 -eq 0) {
                        $color = [System.Drawing.Color]::FromArgb(255, 205, 100, 246)
                    }
                    else {
                        $color = [System.Drawing.Color]::FromArgb(255, 130, 42, 184)
                    }
                }
                elseif ($x -ge 96 -and $y -ge 40 -and $y -lt 80) {
                    if (-not $Elevated) {
                        $color = [System.Drawing.Color]::Transparent
                        $bitmap.SetPixel($x, $y, $color)
                        continue
                    }

                    $localX = ($x - 96) % 16
                    $localY = ($y - 40) % 16
                    $distance = [Math]::Abs($localX - 7.5) + [Math]::Abs($localY - 7.5)
                    if ($localX -le 4 -and $localY -le 4) {
                        $color = [System.Drawing.Color]::FromArgb(255, 232, 255, 226)
                    }
                    elseif ($distance -lt 3) {
                        $color = [System.Drawing.Color]::FromArgb(255, 130, 255, 139)
                    }
                    elseif ($distance -lt 7) {
                        $color = [System.Drawing.Color]::FromArgb(255, 41, 191, 78)
                    }
                    else {
                        $color = [System.Drawing.Color]::FromArgb(255, 13, 91, 44)
                    }
                }
                else {
                    $sampleX = $x
                    $sampleY = $y
                    if ($x -lt 64 -and $y -ge 64 -and $y -lt 96) {
                        $sampleX += ([Math]::Floor($x / 16) * 3)
                        $sampleY += ([Math]::Floor(($y - 64) / 8) * 5)
                    }
                    $color = Get-GreenColor -X $sampleX -Y $sampleY -Elevated $Elevated
                }
                $bitmap.SetPixel($x, $y, $color)
            }
        }
        $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

New-ArmorTexture -Destination (Join-Path $armorOutput 'stone_fang.png') -Elevated $false
New-ArmorTexture -Destination (Join-Path $armorOutput 'firstfang.png') -Elevated $true

$blockbenchHelmetTexture = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
try {
    for ($y = 0; $y -lt 64; $y++) {
        for ($x = 0; $x -lt 64; $x++) {
            $color = Get-GreenColor -X $x -Y $y -Elevated $false
            $blockbenchHelmetTexture.SetPixel($x, $y, $color)
        }
    }
    $blockbenchHelmetTexture.Save(
        (Join-Path $armorOutput 'stone_fang_helmet_blockbench.png'),
        [System.Drawing.Imaging.ImageFormat]::Png
    )
}
finally {
    $blockbenchHelmetTexture.Dispose()
}

$auraStrength = @(52, 78, 116, 170, 230, 178, 118, 72)
for ($frame = 0; $frame -lt $auraStrength.Count; $frame++) {
    $bitmap = New-Object System.Drawing.Bitmap 128, 128, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 40; $y -lt 80; $y++) {
            for ($x = 64; $x -lt 96; $x++) {
                $checker = (($x + $y + $frame) % 4)
                $alpha = [Math]::Min(255, $auraStrength[$frame] + ($checker * 7))
                if ((($x * 3 + $y + $frame) % 11) -eq 0) {
                    $color = [System.Drawing.Color]::FromArgb($alpha, 241, 177, 255)
                }
                elseif ($checker -eq 0) {
                    $color = [System.Drawing.Color]::FromArgb($alpha, 196, 79, 255)
                }
                else {
                    $color = [System.Drawing.Color]::FromArgb($alpha, 120, 34, 206)
                }
                $bitmap.SetPixel($x, $y, $color)
            }
        }
        $destination = Join-Path $armorOutput ("firstfang_aura_{0}.png" -f $frame)
        $bitmap.Save($destination, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

foreach ($equipmentDirectory in @($equipmentHumanoidOutput, $equipmentLeggingsOutput, $equipmentBabyOutput)) {
    $transparentBitmap = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($transparentBitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::Transparent)
        }
        finally {
            $graphics.Dispose()
        }
        $transparentBitmap.Save(
            (Join-Path $equipmentDirectory 'transparent.png'),
            [System.Drawing.Imaging.ImageFormat]::Png
        )
    }
    finally {
        $transparentBitmap.Dispose()
    }
}
