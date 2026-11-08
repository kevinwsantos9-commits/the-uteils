package dev.nivek.utility.keybind;

import dev.nivek.utility.NivekUtility;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class Keybinds {
    public static final List<KeyBinding> ALL = new ArrayList<>();
    public static KeyBinding OPEN_GUI;
    public static KeyBinding VEIN_HOLD;
    public static KeyBinding TOGGLE_AREA;
    public static KeyBinding TOGGLE_TREE;
    public static KeyBinding TORCH;
    public static KeyBinding EAT;
    public static KeyBinding CRAFTING;
    public static KeyBinding WAYPOINTS;

    private Keybinds() {
    }

    public static void register() {
        OPEN_GUI = reg("open_gui", GLFW.GLFW_KEY_N);
        VEIN_HOLD = reg("vein_hold", GLFW.GLFW_KEY_V);
        TOGGLE_AREA = reg("toggle_area", GLFW.GLFW_KEY_B);
        TOGGLE_TREE = reg("toggle_tree", GLFW.GLFW_KEY_UNKNOWN);
        TORCH = reg("torch", GLFW.GLFW_KEY_G);
        EAT = reg("eat", GLFW.GLFW_KEY_H);
        CRAFTING = reg("crafting", GLFW.GLFW_KEY_K);
        WAYPOINTS = reg("waypoints", GLFW.GLFW_KEY_J);
    }

    private static KeyBinding reg(String id, int key) {
        KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + NivekUtility.MOD_ID + "." + id, InputUtil.Type.KEYSYM, key, "key.categories." + NivekUtility.MOD_ID));
        ALL.add(kb);
        return kb;
    }

    public static void resetAll() {
        for (KeyBinding kb : ALL) kb.setBoundKey(kb.getDefaultKey());
        KeyBinding.updateKeysByCode();
        net.minecraft.client.MinecraftClient.getInstance().options.write();
    }
}
