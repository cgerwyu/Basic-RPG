package net.cgerwyu.basicrpgclasses.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.cgerwyu.basicrpgclasses.BasicRPGClasses;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(BasicRPGClasses.id("controls"));

    public static final KeyMapping OPEN_PROGRESSION = new KeyMapping(
            "key.basicrpgclasses.open_progression",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            CATEGORY
    );
    public static final KeyMapping TOGGLE_COMBAT_MODE = new KeyMapping(
            "key.basicrpgclasses.toggle_combat_mode",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );
    public static final KeyMapping ACTIVATE_SELECTED_SKILL = new KeyMapping(
            "key.basicrpgclasses.activate_selected_skill",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    private ModKeyMappings() {
    }
}
