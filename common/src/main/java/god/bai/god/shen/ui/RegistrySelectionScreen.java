package god.bai.god.shen.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 通用选择全屏页面
 */
public class RegistrySelectionScreen extends Screen {
    private final Screen parent;
    private final ShenParamHelper.ParamDef def;
    private final String currentValue;
    private final Consumer<String> callback;
    private final String[][] allOptions;

    private EditBox searchInput;
    private String searchText = "";
    private int scrollOffset = 0;

    private final List<String[]> filteredOptions = new ArrayList<>();

    public RegistrySelectionScreen(Screen parent, ShenParamHelper.ParamDef def,
                                   String currentValue, Consumer<String> callback) {
        super(Component.literal("选择 " + def.label));
        this.parent = parent;
        this.def = def;
        this.currentValue = currentValue;
        this.callback = callback;
        String[][] builtOptions;
        if (def.registryType != null) {
            builtOptions = ShenParamHelper.buildRegistryOptions(def.registryType);
            if (def.registryType == ShenParamHelper.RegistryType.DIMENSION) {
                String[][] dynamicOptions = tryLoadDimensionOptions();
                if (dynamicOptions != null && dynamicOptions.length > 0) {
                    builtOptions = dynamicOptions;
                }
            } else if (def.registryType == ShenParamHelper.RegistryType.DAMAGE_TYPE) {
                String[][] dynamicOptions = tryLoadDamageTypeOptions();
                if (dynamicOptions != null && dynamicOptions.length > 0) {
                    builtOptions = dynamicOptions;
                }
            }
        } else if (def.options != null) {
            builtOptions = def.options;
        } else {
            builtOptions = new String[0][0];
        }
        this.allOptions = builtOptions;
        updateFilteredOptions();
    }

    private String[][] tryLoadDimensionOptions() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return null;
        try {
            RegistryAccess registryManager = client.level.registryAccess();
            return ShenParamHelper.buildDimensionOptionsFromRegistry(registryManager);
        } catch (Exception e) {
            return null;
        }
    }

    private String[][] tryLoadDamageTypeOptions() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) return null;
        try {
            RegistryAccess registryManager = client.level.registryAccess();
            return ShenParamHelper.buildDamageTypeOptionsFromRegistry(registryManager);
        } catch (Exception e) {
            return null;
        }
    }

    public RegistrySelectionScreen(Screen parent, String title, String[][] options,
                                   String currentValue, Consumer<String> callback) {
        super(Component.literal("选择 " + title));
        this.parent = parent;
        this.def = null;
        this.currentValue = currentValue;
        this.callback = callback;
        this.allOptions = options;
        updateFilteredOptions();
    }

    private void updateFilteredOptions() {
        filteredOptions.clear();
        String search = searchText.toLowerCase().trim();
        for (String[] opt : allOptions) {
            if (search.isEmpty() ||
                opt[0].toLowerCase().contains(search) ||
                opt[1].toLowerCase().contains(search)) {
                filteredOptions.add(opt);
            }
        }
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
            Component.literal("§c← 返回"),
            button -> {
                if (this.minecraft != null) this.minecraft.setScreen(parent);
            }
        ).bounds(10, 8, 60, 20).build());

        String titleLabel = (def != null) ? def.label : "选项";
        this.addRenderableWidget(Button.builder(
            Component.literal("§6§l选择 " + titleLabel),
            b -> {}
        ).bounds(80, 8, 200, 20).build());

        int totalCount = allOptions.length;
        this.addRenderableWidget(Button.builder(
            Component.literal("§7共 " + totalCount + " 项"),
            b -> {}
        ).bounds(this.width - 120, 8, 110, 20).build());

        if (searchInput == null) {
            searchInput = new EditBox(this.font,
                10, 36, this.width - 20, 20, Component.literal("搜索中文名或ID..."));
            searchInput.setMaxLength(64);
            searchInput.setValue(searchText);
            searchInput.setResponder(text -> {
                searchText = text;
                scrollOffset = 0;
                updateFilteredOptions();
            });
        }
        this.addRenderableWidget(searchInput);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int startY = 65;
        int itemHeight = 24;
        int availableHeight = this.height - startY - 50;
        int maxVisible = Math.max(1, availableHeight / itemHeight);

        int end = Math.min(scrollOffset + maxVisible, filteredOptions.size());
        int actualListHeight = (end - scrollOffset) * itemHeight;
        context.fill(8, startY - 2, this.width - 8, startY + actualListHeight + 2, 0x33000000);

        for (int i = scrollOffset; i < end; i++) {
            String[] opt = filteredOptions.get(i);
            boolean selected = opt[1].equals(currentValue);
            int y = startY + (i - scrollOffset) * itemHeight;

            boolean hovered = mouseX >= 10 && mouseX <= this.width - 10 && mouseY >= y && mouseY <= y + itemHeight - 1;

            int bgColor = selected ? 0x4400AA00 : (hovered ? 0x44555555 : 0x00000000);
            if (bgColor != 0) {
                context.fill(10, y, this.width - 10, y + itemHeight - 1, bgColor);
            }

            String numStr = String.format("%-" + String.valueOf(filteredOptions.size()).length() + "d", i + 1);
            String prefix = selected ? "§a▶ " : (hovered ? "§e→ " : "  ");
            String display = prefix + "§7" + numStr + " §f" + opt[0];
            String idHint = " §8(" + opt[1] + ")";

            context.drawString(this.font,
                Component.literal(display),
                15, y + 5, 0xFFFFFF, true);

            int idWidth = this.font.width(idHint);
            context.drawString(this.font,
                Component.literal(idHint),
                this.width - 15 - idWidth, y + 5, 0x888888, false);
        }

        if (scrollOffset > 0) {
            String upText = "▲ 还有 " + scrollOffset + " 项 (滚轮向上)";
            int tw = this.font.width(upText);
            context.drawString(this.font, upText,
                (this.width - tw) / 2, startY - 12, 0x888888, false);
        }
        if (end < filteredOptions.size()) {
            String downText = "▼ 还有 " + (filteredOptions.size() - end) + " 项 (滚轮向下)";
            int tw = this.font.width(downText);
            context.drawString(this.font, downText,
                (this.width - tw) / 2, startY + actualListHeight + 6, 0x888888, false);
        }

        if (filteredOptions.isEmpty() && !searchText.isEmpty()) {
            Component msg = Component.literal("§c未找到匹配 \"" + searchText + "\"");
            context.drawString(this.font, msg,
                this.width / 2 - this.font.width(msg) / 2,
                startY + 10, 0xFFFFFF, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int startY = 65;
        int itemHeight = 24;
        int availableHeight = this.height - startY - 50;
        int maxVisible = Math.max(1, availableHeight / itemHeight);

        int end = Math.min(scrollOffset + maxVisible, filteredOptions.size());
        for (int i = scrollOffset; i < end; i++) {
            int y = startY + (i - scrollOffset) * itemHeight;
            int yEnd = y + itemHeight - 1;

            if (mouseX >= 10 && mouseX <= this.width - 10 && mouseY >= y && mouseY <= yEnd) {
                String selectedValue = filteredOptions.get(i)[1];
                callback.accept(selectedValue);
                if (this.minecraft != null) this.minecraft.setScreen(parent);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && hasControlDown()) {
            var focused = this.getFocused();
            if (focused instanceof EditBox tf) {
                if (keyCode == 86) {
                    String clipboard = this.minecraft.keyboardHandler.getClipboard();
                    if (clipboard != null) {
                        pasteIntoTextField(tf, clipboard);
                    }
                    return true;
                } else if (keyCode == 67) {
                    String selected = getSelectedText(tf);
                    if (selected != null && !selected.isEmpty()) {
                        this.minecraft.keyboardHandler.setClipboard(selected);
                    }
                    return true;
                } else if (keyCode == 88) {
                    String selected = getSelectedText(tf);
                    if (selected != null && !selected.isEmpty()) {
                        this.minecraft.keyboardHandler.setClipboard(selected);
                        pasteIntoTextField(tf, "");
                    }
                    return true;
                } else if (keyCode == 65) {
                    tf.setCursorPosition(0);
                    tf.setHighlightPos(tf.getValue().length());
                    return true;
                }
            }
        }

        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return false;
    }

    private static String getSelectedText(EditBox tf) {
        return tf.getHighlighted();
    }

    private void pasteIntoTextField(EditBox tf, String content) {
        tf.insertText(content);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int startY = 65;
        int itemHeight = 24;
        int availableHeight = this.height - startY - 50;
        int maxVisible = Math.max(1, availableHeight / itemHeight);

        int oldOffset = scrollOffset;
        scrollOffset += (int) amount;
        scrollOffset = Math.max(0, Math.min(filteredOptions.size() - maxVisible, scrollOffset));

        return oldOffset != scrollOffset;
    }
}
