package god.bai.god.shen.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * 参数配置独立页面
 */
public class ShenParamConfigScreen extends Screen {
    private final Screen parent;
    private final String title;
    private final List<ShenParamHelper.ParamDef> paramDefs;
    private final Map<String, String> params;
    private final Runnable onSave;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private String dropdownCustomKey = "";

    public ShenParamConfigScreen(Screen parent, String title,
                                  List<ShenParamHelper.ParamDef> paramDefs,
                                  Map<String, String> params,
                                  Runnable onSave) {
        super(Component.literal("配置 " + title));
        this.parent = parent;
        this.title = title;
        this.paramDefs = paramDefs;
        this.params = params;
        this.onSave = onSave;
    }

    private void rebuild() {
        this.clearWidgets();
        this.scrollOffset = 0;
        this.maxScroll = 0;
        this.init();
    }

    @Override
    protected void init() {
        super.init();

        int sw = this.width;
        int sh = this.height;

        int marginX = 10;
        int contentWidth = sw - marginX * 2;

        this.addRenderableWidget(Button.builder(
            Component.literal("§c← 返回"),
            button -> {
                if (this.minecraft != null) this.minecraft.setScreen(parent);
            }
        ).bounds(marginX, 8, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("§6§l配置参数: " + title),
            b -> {}
        ).bounds(80, 8, Math.min(300, sw - 90), 20).build());

        int widgetStartY = 36;
        int widgetSpacing = 24;
        int bottomReserve = 45;

        int visibleHeight = sh - widgetStartY - bottomReserve;
        int totalHeight = paramDefs.size() * widgetSpacing;

        maxScroll = Math.max(0, totalHeight - visibleHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int widgetY = widgetStartY - scrollOffset;
        for (ShenParamHelper.ParamDef def : paramDefs) {
            boolean aboveTop = (widgetY + widgetSpacing < 0);
            boolean belowBottom = (widgetY > sh);
            if (!aboveTop && !belowBottom) {
                renderParamWidget(def, marginX, widgetY, contentWidth);
            }
            widgetY += widgetSpacing;
        }

        int bottomY = sh - 28;
        int btnW = Math.min(120, (sw - 30) / 2);
        this.addRenderableWidget(Button.builder(
            Component.literal("§a✓ 完成"),
            button -> {
                onSave.run();
                if (this.minecraft != null) this.minecraft.setScreen(parent);
            }
        ).bounds(sw / 2 - btnW / 2, bottomY, btnW, 20).build());
    }

    private void renderParamWidget(ShenParamHelper.ParamDef def, int x, int y, int contentWidth) {
        String currentValue = params.getOrDefault(def.key, def.defaultValue);
        int labelWidth = Math.min(this.font.width(def.label + ": ") + 5, contentWidth / 4);

        switch (def.widgetType) {
            case DROPDOWN: {
                if (def.registryType != null) {
                    renderRegistryDropdown(def, x, y, currentValue, contentWidth);
                    break;
                }

                String displayName = getOptionDisplayName(def.options, currentValue);
                boolean isCustomValue = def.allowCustom && !isValueInOptions(def.options, currentValue);

                this.addRenderableWidget(Button.builder(
                    Component.literal("§7" + def.label + ":"),
                    b -> {}
                ).bounds(x, y, labelWidth, 20).build());

                if (def.allowCustom && dropdownCustomKey.equals(def.key)) {
                    int btnWidth = Math.min(160, contentWidth - labelWidth - 25);
                    if (btnWidth < 40) btnWidth = 40;
                    EditBox customInput = new EditBox(this.font,
                        x + labelWidth, y, btnWidth, 20, Component.literal("输入ID..."));
                    customInput.setMaxLength(128);
                    customInput.setValue(currentValue);
                    customInput.setResponder(text -> params.put(def.key, text));
                    customInput.setFocused(true);
                    this.addRenderableWidget(customInput);

                    this.addRenderableWidget(Button.builder(
                        Component.literal("§a✓"),
                        button -> {
                            dropdownCustomKey = "";
                            rebuild();
                        }
                    ).bounds(x + labelWidth + btnWidth + 2, y, 20, 20).build());
                    break;
                }

                int btnWidth = Math.min(160, contentWidth - labelWidth - 25);
                if (btnWidth < 40) btnWidth = 40;
                this.addRenderableWidget(Button.builder(
                    Component.literal((isCustomValue ? "§d" : "§f") + displayName + " §7[选择]"),
                    button -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new RegistrySelectionScreen(
                                this, def, currentValue, selectedValue -> {
                                    params.put(def.key, selectedValue);
                                }
                            ));
                        }
                    }
                ).bounds(x + labelWidth, y, btnWidth, 20).build());

                if (def.allowCustom && btnWidth > 60) {
                    this.addRenderableWidget(Button.builder(
                        Component.literal("✏️"),
                        button -> {
                            dropdownCustomKey = def.key;
                            rebuild();
                        }
                    ).bounds(x + labelWidth + btnWidth + 2, y, 20, 20).build());
                }
                break;
            }

            case SLIDER_PRESET: {
                int btnWidth = Math.min(80, Math.max(36, (contentWidth - labelWidth - 30) / def.options.length));
                if (btnWidth < 36) btnWidth = 36;

                this.addRenderableWidget(Button.builder(
                    Component.literal("§7" + def.label + ":"),
                    b -> {}
                ).bounds(x, y, labelWidth, 20).build());

                int btnX = x + labelWidth;
                int sw = this.width;
                for (String[] option : def.options) {
                    boolean selected = option[1].equals(currentValue);
                    String optionLabel = (sw < 300 && option[0].length() > 3)
                        ? option[0].substring(0, Math.min(2, btnWidth / 10)) : option[0];
                    this.addRenderableWidget(Button.builder(
                        Component.literal((selected ? "§a" : "§7") + optionLabel),
                        button -> {
                            params.put(def.key, option[1]);
                            rebuild();
                        }
                    ).bounds(btnX, y, btnWidth, 20).build());
                    btnX += btnWidth + 3;
                    if (btnX + btnWidth > this.width - 5) break;
                }
                break;
            }

            case BOOLEAN: {
                boolean value = "true".equalsIgnoreCase(currentValue);
                int boolW = Math.min(200, contentWidth);
                this.addRenderableWidget(Button.builder(
                    Component.literal("§7" + def.label + ": " + (value ? "§a✅ 是" : "§c❌ 否")),
                    button -> {
                        boolean newVal = !"true".equalsIgnoreCase(params.getOrDefault(def.key, def.defaultValue));
                        params.put(def.key, newVal ? "true" : "false");
                        rebuild();
                    }
                ).bounds(x, y, boolW, 20).build());
                break;
            }

            case NUMBER: {
                this.addRenderableWidget(Button.builder(
                    Component.literal("§7" + def.label + ":"),
                    b -> {}
                ).bounds(x, y, labelWidth, 20).build());

                int inputX = x + labelWidth;
                int sw = this.width;

                if (def.operable) {
                    String currentOp = params.getOrDefault(def.key + "_op", "set");
                    String[][] opModes = {
                        {"set", "§e=", "设置"},
                        {"add", "§a+", "增加"},
                        {"subtract", "§c−", "减少"},
                        {"multiply", "§d×", "乘"},
                        {"divide", "§5÷", "除"},
                    };
                    int curIdx = 0;
                    for (int i = 0; i < opModes.length; i++) {
                        if (opModes[i][0].equals(currentOp)) { curIdx = i; break; }
                    }
                    int opW = 18;
                    this.addRenderableWidget(Button.builder(
                        Component.literal(opModes[curIdx][1]),
                        button -> {
                            String cur = params.getOrDefault(def.key + "_op", "set");
                            int nextIdx = 0;
                            for (int i = 0; i < opModes.length; i++) {
                                if (opModes[i][0].equals(cur)) { nextIdx = (i + 1) % opModes.length; break; }
                            }
                            params.put(def.key + "_op", opModes[nextIdx][0]);
                            rebuild();
                        }
                    ).bounds(inputX, y, opW, 20).build());
                    inputX += opW + 2;
                }

                int remaining = contentWidth - (inputX - x) - 55;
                int inputWidth = Math.min(80, Math.max(30, remaining / 2));
                EditBox numInput = new EditBox(this.font, inputX, y, inputWidth, 20, Component.literal(def.label));
                numInput.setMaxLength(32);
                numInput.setValue(currentValue);
                numInput.setResponder(text -> params.put(def.key, text));
                this.addRenderableWidget(numInput);

                int stepBtnX = inputX + inputWidth + 2;
                this.addRenderableWidget(Button.builder(Component.literal("-1"), button -> {
                    try {
                        int val = Integer.parseInt(params.getOrDefault(def.key, def.defaultValue));
                        val = Math.max(def.min, val - 1);
                        params.put(def.key, String.valueOf(val));
                        rebuild();
                    } catch (NumberFormatException ignored) {}
                }).bounds(stepBtnX, y, Math.min(22, (sw - stepBtnX) / 3), 20).build());

                this.addRenderableWidget(Button.builder(Component.literal("+1"), button -> {
                    try {
                        int val = Integer.parseInt(params.getOrDefault(def.key, def.defaultValue));
                        val = Math.min(def.max, val + 1);
                        params.put(def.key, String.valueOf(val));
                        rebuild();
                    } catch (NumberFormatException ignored) {}
                }).bounds(stepBtnX + Math.min(24, (sw - stepBtnX - 24) / 2), y, Math.min(24, (sw - stepBtnX) / 3), 20).build());

                if ((def.min != 0 || def.max != 0) && sw > 350) {
                    String rangeText = "§7[" + def.min + "~" + def.max + "]";
                    this.addRenderableWidget(Button.builder(
                        Component.literal(rangeText),
                        b -> {}
                    ).bounds(stepBtnX + 50, y, this.font.width(rangeText) + 6, 20).build());
                }
                break;
            }

            case TEXT: {
                this.addRenderableWidget(Button.builder(
                    Component.literal("§7" + def.label + ":"),
                    b -> {}
                ).bounds(x, y, labelWidth, 20).build());

                int inputWidth = Math.min(300, Math.max(40, contentWidth - labelWidth - 20));
                EditBox textInput = new EditBox(this.font, x + labelWidth, y, inputWidth, 20, Component.literal(def.label));
                textInput.setMaxLength(512);
                textInput.setValue(currentValue);
                textInput.setResponder(text -> params.put(def.key, text));
                this.addRenderableWidget(textInput);
                break;
            }
            case COLOR: {
                this.addRenderableWidget(Button.builder(
                    Component.literal("§7" + def.label + ":"),
                    b -> {}
                ).bounds(x, y, labelWidth, 20).build());

                int r = 255, g = 0, b = 0;
                try {
                    String[] parts = currentValue.split(",");
                    if (parts.length >= 3) {
                        r = Math.max(0, Math.min(255, Integer.parseInt(parts[0].trim())));
                        g = Math.max(0, Math.min(255, Integer.parseInt(parts[1].trim())));
                        b = Math.max(0, Math.min(255, Integer.parseInt(parts[2].trim())));
                    }
                } catch (Exception ignored) {}

                int previewColor = 0xFF000000 | (r << 16) | (g << 8) | b;
                String displayText = String.format("§7[%d, %d, %d] #%02X%02X%02X §r[选择颜色]", r, g, b, r, g, b);

                int btnX = x + labelWidth;
                int btnWidth = contentWidth - labelWidth - 48;
                this.addRenderableWidget(Button.builder(
                    Component.literal(displayText),
                    button -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new ColorPickerScreen(
                                this, def, currentValue, selected -> {
                                    params.put(def.key, selected);
                                }
                            ));
                        }
                    }
                ).bounds(btnX, y, btnWidth, 20).build());

                boolean isDefault = def.defaultValue.equals(currentValue);
                this.addRenderableWidget(Button.builder(
                    Component.literal(isDefault ? "§8↺" : "§c↺"),
                    button -> {
                        params.put(def.key, def.defaultValue);
                        rebuild();
                    }
                ).bounds(btnX + btnWidth + 2, y, 24, 20).build());
                break;
            }
        }
    }

    private void renderRegistryDropdown(ShenParamHelper.ParamDef def, int x, int y, String currentValue, int contentWidth) {
        int btnX = x + 50;
        int btnWidth = Math.min(200, Math.max(40, contentWidth - 50 - 10));

        this.addRenderableWidget(Button.builder(
            Component.literal("§7" + def.label + ":"), b -> {}
        ).bounds(x, y, 48, 20).build());

        if (dropdownCustomKey.equals(def.key)) {
            EditBox customInput = new EditBox(this.font,
                btnX, y, btnWidth, 20, Component.literal("输入ID..."));
            customInput.setMaxLength(128);
            customInput.setValue(currentValue);
            customInput.setResponder(text -> params.put(def.key, text));
            customInput.setFocused(true);
            this.addRenderableWidget(customInput);

            this.addRenderableWidget(Button.builder(
                Component.literal("§a✓"),
                button -> {
                    dropdownCustomKey = "";
                    rebuild();
                }
            ).bounds(btnX + btnWidth + 2, y, 20, 20).build());
            return;
        }

        String displayName = tryGetDisplayName(def.registryType, currentValue);
        this.addRenderableWidget(Button.builder(
            Component.literal("§e🔍 " + displayName + " §7[点击浏览]"),
            button -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new RegistrySelectionScreen(
                        this, def, currentValue, selectedValue -> {
                            params.put(def.key, selectedValue);
                        }
                    ));
                }
            }
        ).bounds(btnX, y, btnWidth, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("✏️"),
            button -> {
                dropdownCustomKey = def.key;
                rebuild();
            }
        ).bounds(btnX + btnWidth + 2, y, 20, 20).build());
    }

    private String tryGetDisplayName(ShenParamHelper.RegistryType type, String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        if (id == null) return "✏️ " + idStr;
        switch (type) {
            case STATUS_EFFECT: {
                var effect = BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect != null) {
                    String name = I18n.get(effect.getDescriptionId());
                    if (!name.startsWith("effect.")) return name;
                }
                return "✏️ " + idStr;
            }
            case ENTITY_TYPE: {
                var et = BuiltInRegistries.ENTITY_TYPE.get(id);
                if (et != null) {
                    String name = I18n.get(et.getDescriptionId());
                    if (!name.startsWith("entity.")) return name;
                }
                return "✏️ " + idStr;
            }
            case ITEM: {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null) {
                    String name = I18n.get(item.getDescriptionId());
                    if (!name.startsWith("item.") && !name.startsWith("block.")) return name;
                }
                return "✏️ " + idStr;
            }
            case BLOCK: {
                Block block = BuiltInRegistries.BLOCK.get(id);
                if (block != null) {
                    String name = I18n.get(block.getDescriptionId());
                    if (!name.startsWith("block.")) return name;
                }
                return "✏️ " + idStr;
            }
            case PARTICLE: {
                String path = id.getPath().replace('_', ' ').replace(".", " ");
                StringBuilder sb = new StringBuilder();
                boolean nextUpper = true;
                for (char c : path.toCharArray()) {
                    if (c == ' ') { nextUpper = true; sb.append(c); }
                    else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
                    else sb.append(c);
                }
                return sb.toString();
            }
            case SOUND_EVENT: {
                var sound = BuiltInRegistries.SOUND_EVENT.get(id);
                if (sound != null) {
                    String subtitleKey = "subtitles." + id.getPath().replace('/', '.');
                    String name = I18n.get(subtitleKey);
                    if (!name.equals(subtitleKey)) return name;
                }
                return "✏️ " + idStr;
            }
            case DIMENSION: {
                return ShenParamHelper.tryTranslateDimension(id);
            }
            case DAMAGE_TYPE: {
                return ShenParamHelper.tryTranslateDamageType(id);
            }
            case ATTRIBUTE: {
                var attr = BuiltInRegistries.ATTRIBUTE.get(id);
                if (attr != null) return ShenParamHelper.tryTranslateAttribute(id, attr);
                return "✏️ " + idStr;
            }
            case ENCHANTMENT: {
                var ench = BuiltInRegistries.ENCHANTMENT.get(id);
                if (ench != null) return ShenParamHelper.tryTranslateEnchantment(id, ench);
                return "✏️ " + idStr;
            }
            case NBT_VARIABLE: {
                return ShenParamHelper.tryTranslateNbtVariable(idStr);
            }
        }
        return "✏️ " + idStr;
    }

    private String getOptionDisplayName(String[][] options, String value) {
        if (options == null) return value;
        for (String[] opt : options) {
            if (opt[1].equals(value)) return opt[0];
        }
        return value;
    }

    private boolean isValueInOptions(String[][] options, String value) {
        if (options == null) return false;
        for (String[] opt : options) {
            if (opt[1].equals(value)) return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        if (maxScroll > 0) {
            String scrollHint = scrollOffset > 0
                ? "§7▲ 滚轮上下滚动 ▼"
                : "§7▼ 滚轮向下滚动 ▼";
            context.drawCenteredString(this.font,
                Component.literal(scrollHint),
                this.width / 2, this.height - 10, 0x666666);
        }
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
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (verticalAmount != 0) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 20));
            this.clearWidgets();
            this.init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    public boolean shouldPause() {
        return false;
    }
}
