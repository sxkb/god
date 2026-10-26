package god.bai.god.shen.ui;

import god.bai.god.shen.ShenLaw;
import god.bai.god.shen.ShenNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ShenImportScreen extends Screen {
    private final Screen parent;
    private EditBox inputField;

    public ShenImportScreen(Screen parent) {
        super(Component.literal("导入法则"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int fieldW = Math.min(this.width - 40, 500);
        int fieldX = centerX - fieldW / 2;
        int fieldY = this.height / 2 - 30;

        inputField = new EditBox(this.font, fieldX, fieldY, fieldW, 20, Component.literal(""));
        inputField.setMaxLength(Integer.MAX_VALUE);
        inputField.setFilter(s -> true);
        this.addRenderableWidget(inputField);
        this.setFocused(inputField);

        this.addRenderableWidget(Button.builder(
            Component.literal("§a导入"),
            btn -> {
                String text = inputField.getValue().trim();
                if (text.isEmpty()) {
                    if (this.minecraft != null && this.minecraft.player != null)
                        this.minecraft.player.sendSystemMessage(Component.literal("§c[言出法随] 输入内容为空"));
                    return;
                }
                try {
                    ShenLaw imported = ShenNetwork.GSON.fromJson(text, ShenLaw.class);
                    if (imported == null || imported.name == null || imported.name.isEmpty()) {
                        if (this.minecraft != null && this.minecraft.player != null)
                            this.minecraft.player.sendSystemMessage(Component.literal("§c[言出法随] 无效的法则数据"));
                        return;
                    }
                    ShenNetwork.sendSaveLaw(imported);
                    if (this.minecraft != null && this.minecraft.player != null)
                        this.minecraft.player.sendSystemMessage(Component.literal("§a[言出法随] §e导入 §b" + imported.name + " §e成功"));
                    if (this.minecraft != null) this.minecraft.setScreen(parent);
                } catch (Exception e) {
                    if (this.minecraft != null && this.minecraft.player != null)
                        this.minecraft.player.sendSystemMessage(Component.literal("§c[言出法随] 导入失败: " + e.getMessage()));
                }
            }
        ).bounds(centerX - 84, fieldY + 30, 80, 20).build());

        this.addRenderableWidget(Button.builder(
            Component.literal("§7取消"),
            btn -> {
                if (this.minecraft != null) this.minecraft.setScreen(parent);
            }
        ).bounds(centerX + 4, fieldY + 30, 80, 20).build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        int centerX = this.width / 2;
        context.drawCenteredString(this.font, Component.literal("§d导入法则 - 粘贴JSON数据"), centerX, this.height / 2 - 50, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft != null && hasControlDown() && keyCode == 86) {
            String clipboard = this.minecraft.keyboardHandler.getClipboard();
            if (clipboard != null && inputField != null) {
                String text = inputField.getValue();
                int cursor = Math.min(inputField.getCursorPosition(), text.length());
                String newText = text.substring(0, cursor) + clipboard + text.substring(cursor);
                if (newText.length() <= Integer.MAX_VALUE) {
                    inputField.setValue(newText);
                    inputField.setCursorPosition(cursor + clipboard.length());
                }
            }
            return true;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;

        if (this.minecraft != null && hasControlDown()) {
            if (keyCode == 65) {
                if (inputField != null) {
                    inputField.setCursorPosition(0);
                    inputField.setHighlightPos(inputField.getValue().length());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.getFocused() == inputField) {
            String text = inputField.getValue();
            if (text.length() >= Integer.MAX_VALUE) return false;
            int cursor = Math.min(inputField.getCursorPosition(), text.length());
            String newText = text.substring(0, cursor) + chr + text.substring(cursor);
            inputField.setValue(newText);
            inputField.setCursorPosition(cursor + 1);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }
}
