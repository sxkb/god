package god.bai.god.shen.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 可视化颜色选择器
 */
public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final String currentValue;
    private final Consumer<String> callback;

    private int selectedR = 255, selectedG = 0, selectedB = 0;
    private EditBox rInput, gInput, bInput;

    private static final int[][] PRESET_COLORS = {
        {255, 0, 0}, {220, 20, 60}, {178, 34, 34}, {139, 0, 0},
        {255, 165, 0}, {255, 140, 0}, {255, 127, 80}, {210, 105, 30},
        {255, 255, 0}, {255, 215, 0}, {255, 255, 153}, {218, 165, 32},
        {173, 255, 47}, {154, 205, 50}, {128, 128, 0}, {107, 142, 35},
        {0, 255, 0}, {50, 205, 50}, {34, 139, 34}, {0, 128, 0},
        {0, 255, 127}, {64, 224, 208}, {0, 206, 209}, {0, 128, 128},
        {0, 255, 255}, {0, 204, 255}, {30, 144, 255}, {70, 130, 180},
        {0, 0, 255}, {65, 105, 225}, {0, 0, 205}, {25, 25, 112},
        {128, 0, 255}, {138, 43, 226}, {147, 112, 219}, {75, 0, 130},
        {255, 0, 255}, {255, 20, 147}, {218, 112, 214}, {153, 50, 204},
        {255, 182, 193}, {255, 105, 180}, {255, 192, 203}, {199, 21, 133},
        {255, 255, 255}, {192, 192, 192}, {128, 128, 128}, {0, 0, 0},
    };

    private static final String[] COLOR_NAMES = {
        "纯红", "猩红", "栗色", "暗红",
        "橙色", "深橙", "珊瑚", "棕色",
        "纯黄", "金黄", "浅黄", "暗金",
        "黄绿", "草绿", "橄榄", "森林绿",
        "纯绿", "春绿", "森林", "深绿",
        "春绿", "青绿", "深青", "鸭绿",
        "纯青", "天蓝", "道奇蓝", "钢蓝",
        "纯蓝", "皇家蓝", "中蓝", "深蓝",
        "紫色", "紫罗兰", "中紫", "靛蓝",
        "品红", "深粉", "兰紫", "暗紫",
        "粉红", "热粉", "浅粉", "紫红",
        "白色", "银灰", "灰色", "黑色",
    };

    private static String getColorName(int r, int g, int b) {
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i][0] == r && PRESET_COLORS[i][1] == g && PRESET_COLORS[i][2] == b) {
                return COLOR_NAMES[i];
            }
        }
        return null;
    }

    public ColorPickerScreen(Screen parent, ShenParamHelper.ParamDef def,
                             String currentValue, Consumer<String> callback) {
        super(Component.literal("选择颜色"));
        this.parent = parent;
        this.currentValue = currentValue;
        this.callback = callback;

        parseCurrentColor(currentValue);
    }

    private void parseCurrentColor(String value) {
        try {
            String[] parts = value.split(",");
            if (parts.length >= 3) {
                selectedR = clamp(Integer.parseInt(parts[0].trim()), 0, 255);
                selectedG = clamp(Integer.parseInt(parts[1].trim()), 0, 255);
                selectedB = clamp(Integer.parseInt(parts[2].trim()), 0, 255);
            } else if (parts.length == 1 && value.startsWith("#")) {
                int hex = Integer.parseInt(value.substring(1), 16);
                selectedR = (hex >> 16) & 0xFF;
                selectedG = (hex >> 8) & 0xFF;
                selectedB = hex & 0xFF;
            }
        } catch (Exception ignored) {
        }
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    @Override
    protected void init() {
        int sw = this.width;

        this.addRenderableWidget(Button.builder(
            Component.literal("§c← 返回"),
            button -> {
                if (this.minecraft != null) this.minecraft.setScreen(parent);
            }
        ).bounds(10, 8, 60, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("§6§l选择粒子颜色"),
            b -> {}
        ).bounds(80, 8, 160, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("§a✔ 确定"),
            button -> {
                callback.accept(selectedR + "," + selectedG + "," + selectedB);
                if (this.minecraft != null) this.minecraft.setScreen(parent);
            }
        ).bounds(sw - 80, 8, 70, 20).build());

        int cols = 8;
        int cellSize = Math.min(28, (sw - 40) / cols);
        int gridStartX = (sw - cols * cellSize) / 2;
        int gridStartY = 50;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = gridStartX + col * cellSize;
            int y = gridStartY + row * cellSize;
            final int r = PRESET_COLORS[i][0];
            final int g = PRESET_COLORS[i][1];
            final int b = PRESET_COLORS[i][2];

            this.addRenderableWidget(Button.builder(
                Component.literal(""),
                button -> {
                    selectedR = r;
                    selectedG = g;
                    selectedB = b;
                    if (rInput != null) rInput.setValue(String.valueOf(r));
                    if (gInput != null) gInput.setValue(String.valueOf(g));
                    if (bInput != null) bInput.setValue(String.valueOf(b));
                }
            ).bounds(x, y, cellSize, cellSize).build());
        }

        int infoY = gridStartY + ((PRESET_COLORS.length + cols - 1) / cols) * cellSize + 15;

        int inputY = infoY + 35;
        int inputW = Math.min(60, (sw - 100) / 3);

        rInput = new EditBox(this.font, 50, inputY, inputW, 20, Component.literal("R"));
        rInput.setMaxLength(3);
        rInput.setValue(String.valueOf(selectedR));
        rInput.setResponder(text -> {
            try {
                selectedR = clamp(Integer.parseInt(text), 0, 255);
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(rInput);

        gInput = new EditBox(this.font, 50 + inputW + 10, inputY, inputW, 20, Component.literal("G"));
        gInput.setMaxLength(3);
        gInput.setValue(String.valueOf(selectedG));
        gInput.setResponder(text -> {
            try {
                selectedG = clamp(Integer.parseInt(text), 0, 255);
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(gInput);

        bInput = new EditBox(this.font, 50 + (inputW + 10) * 2, inputY, inputW, 20, Component.literal("B"));
        bInput.setMaxLength(3);
        bInput.setValue(String.valueOf(selectedB));
        bInput.setResponder(text -> {
            try {
                selectedB = clamp(Integer.parseInt(text), 0, 255);
            } catch (NumberFormatException ignored) {}
        });
        this.addRenderableWidget(bInput);

        this.addRenderableWidget(Button.builder(
            Component.literal("§cR"), b -> {}
        ).bounds(38, inputY, 12, 20).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("§aG"), b -> {}
        ).bounds(38 + inputW + 10, inputY, 12, 20).build());
        this.addRenderableWidget(Button.builder(
            Component.literal("§9B"), b -> {}
        ).bounds(38 + (inputW + 10) * 2, inputY, 12, 20).build());

        if (sw > 300) {
            this.addRenderableWidget(Button.builder(
                Component.literal("📋 复制"),
                button -> {
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(selectedR + "," + selectedG + "," + selectedB);
                    }
                }
            ).bounds(sw - 140, inputY, 60, 20).build());

            this.addRenderableWidget(Button.builder(
                Component.literal("📋 粘贴"),
                button -> {
                    if (this.minecraft != null) {
                        String clip = this.minecraft.keyboardHandler.getClipboard();
                        if (clip != null) {
                            parseCurrentColor(clip);
                            if (rInput != null) rInput.setValue(String.valueOf(selectedR));
                            if (gInput != null) gInput.setValue(String.valueOf(selectedG));
                            if (bInput != null) bInput.setValue(String.valueOf(selectedB));
                        }
                    }
                }
            ).bounds(sw - 75, inputY, 65, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int sw = this.width;

        int cols = 8;
        int cellSize = Math.min(28, (sw - 40) / cols);
        int gridStartX = (sw - cols * cellSize) / 2;
        int gridStartY = 50;

        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int col = i % cols;
            int row = i / cols;
            int x = gridStartX + col * cellSize;
            int y = gridStartY + row * cellSize;

            int r = PRESET_COLORS[i][0];
            int g = PRESET_COLORS[i][1];
            int b = PRESET_COLORS[i][2];
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;

            context.fill(x + 1, y + 1, x + cellSize - 1, y + cellSize - 1, color);

            boolean isSelected = (selectedR == r && selectedG == g && selectedB == b);
            if (isSelected) {
                context.fill(x, y, x + cellSize, y + 1, 0xFFDAA520);
                context.fill(x, y + cellSize - 1, x + cellSize, y + cellSize, 0xFFDAA520);
                context.fill(x, y, x + 1, y + cellSize, 0xFFDAA520);
                context.fill(x + cellSize - 1, y, x + cellSize, y + cellSize, 0xFFDAA520);
            } else {
                context.fill(x, y, x + cellSize, y + 1, 0x55555555);
                context.fill(x, y + cellSize - 1, x + cellSize, y + cellSize, 0x55555555);
                context.fill(x, y, x + 1, y + cellSize, 0x55555555);
                context.fill(x + cellSize - 1, y, x + cellSize, y + cellSize, 0x55555555);
            }
        }

        int gridEndY = gridStartY + ((PRESET_COLORS.length + cols - 1) / cols) * cellSize;
        int previewY = gridEndY + 10;

        String hexStr = String.format("#%02X%02X%02X", selectedR, selectedG, selectedB);
        String colorName = getColorName(selectedR, selectedG, selectedB);
        if (colorName == null) colorName = "自定义颜色";

        int previewColor = 0xFF000000 | (selectedR << 16) | (selectedG << 8) | selectedB;
        int previewSize = 24;
        int previewX = (sw - previewSize) / 2;
        context.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, previewColor);
        context.fill(previewX, previewY, previewX + previewSize, previewY + 1, 0xFF888888);
        context.fill(previewX, previewY + previewSize - 1, previewX + previewSize, previewY + previewSize, 0xFF888888);
        context.fill(previewX, previewY, previewX + 1, previewY + previewSize, 0xFF888888);
        context.fill(previewX + previewSize - 1, previewY, previewX + previewSize, previewY + previewSize, 0xFF888888);

        String info = "§f" + colorName + " §7(" + selectedR + ", " + selectedG + ", " + selectedB + ") §8" + hexStr;
        context.drawString(this.font, Component.literal(info),
            previewX + previewSize + 8, previewY + 6, 0xFFFFFF, true);

        int hintY = previewY + 40;
        String hintText = "§7点击上方颜色块选择，或在下方手动输入RGB值";
        context.drawString(this.font, Component.literal(hintText),
            (sw - this.font.width(hintText)) / 2,
            hintY, 0x888888, false);
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

    public boolean shouldPause() {
        return false;
    }
}
