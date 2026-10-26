package god.bai.god.shen.ui;

import god.bai.god.shen.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import god.bai.god.platform.Platforms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShenScreen extends Screen {
    private final boolean limited;
    private List<ShenLaw> laws;
    private int selectedIndex = -1;
    private boolean loaded = false;
    private boolean renaming = false;
    private EditBox nameField;

    private int layout_rightX, layout_btnW, layout_sectionY, layout_presetEndY;
    private int layout_rightPanelW;

    /** 游戏内导出缓冲区：兜底系统剪贴板不可用时使用 */
    private static String lastExportedJson = null;
    private static String lastExportedName = null;
    private static final File EXPORT_CACHE_FILE = Platforms.get().configDir()
            .resolve("god").resolve("last_export.json").toFile();

    static {
        loadExportCache();
    }

    private static void loadExportCache() {
        try {
            if (EXPORT_CACHE_FILE.exists()) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(EXPORT_CACHE_FILE), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
                lastExportedJson = sb.toString();
                try {
                    ShenLaw law = ShenNetwork.GSON.fromJson(lastExportedJson, ShenLaw.class);
                    if (law != null && law.name != null) lastExportedName = law.name;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static void saveExportCache(String json) {
        try {
            EXPORT_CACHE_FILE.getParentFile().mkdirs();
            try (Writer writer = new OutputStreamWriter(
                    new FileOutputStream(EXPORT_CACHE_FILE), StandardCharsets.UTF_8)) {
                writer.write(json);
            }
        } catch (Exception ignored) {}
    }

    private static final PresetTemplate[] PRESETS = {
        new PresetTemplate("⚡ 潜行发光", ShenEventType.PLAYER_SNEAK, ShenTargetType.EVENT_SOURCE, ShenEffectType.APPLY_EFFECT,
            map("effect", "minecraft:glowing", "duration", "200", "amplifier", "0", "showParticles", "true")),
        new PresetTemplate("🔥 攻击点燃", ShenEventType.PLAYER_ATTACK, ShenTargetType.LINE_OF_SIGHT, ShenEffectType.SET_FIRE,
            map("ticks", "200")),
        new PresetTemplate("🗡️ 攻击秒杀", ShenEventType.PLAYER_ATTACK, ShenTargetType.LINE_OF_SIGHT, ShenEffectType.KILL_ENTITY,
            map()),
        new PresetTemplate("💎 右键钻石", ShenEventType.PLAYER_RIGHT_CLICK, ShenTargetType.EVENT_SOURCE, ShenEffectType.GIVE_ITEM,
            map("item", "minecraft:diamond", "count", "1")),
        new PresetTemplate("🌩️ 跳跃闪电", ShenEventType.PLAYER_JUMP, ShenTargetType.EVENT_SOURCE, ShenEffectType.SUMMON_ENTITY,
            map("entityType", "minecraft:lightning_bolt", "count", "1")),
        new PresetTemplate("💊 受伤回血", ShenEventType.PLAYER_HURT, ShenTargetType.EVENT_SOURCE, ShenEffectType.MODIFY_HEALTH,
            map("amount", "20", "percent", "false")),
        new PresetTemplate("☀️ 切换晴天", ShenEventType.PLAYER_SNEAK, ShenTargetType.EVENT_SOURCE, ShenEffectType.MODIFY_WEATHER,
            map("weather", "clear", "duration", "6000")),
        new PresetTemplate("🌙 切换夜晚", ShenEventType.PLAYER_SNEAK, ShenTargetType.EVENT_SOURCE, ShenEffectType.MODIFY_TIME,
            map("time", "18000", "absolute", "true")),
        new PresetTemplate("💣 攻击爆炸", ShenEventType.PLAYER_ATTACK, ShenTargetType.LINE_OF_SIGHT, ShenEffectType.EXPLOSION,
            map("power", "4.0", "destroyBlocks", "false", "createFire", "false")),
    };

    public ShenScreen() {
        this(false);
    }

    public ShenScreen(boolean limited) {
        super(Component.literal("言出法随" + (limited ? "（阉割）" : "")));
        this.laws = new ArrayList<>();
        this.limited = limited;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();

        int sw = this.width;
        int sh = this.height;

        int panelMinW = (sw < 400) ? 60 : 80;
        layout_rightPanelW = Math.min(130, Math.max(panelMinW, sw / 5));
        layout_rightX = sw - layout_rightPanelW - 6;
        layout_btnW = layout_rightPanelW - 8;

        int leftW = layout_rightX - 10;

        boolean isNarrow = (sw < 400);
        int presetBtnW = Math.min(100, Math.max(isNarrow ? 50 : 65, (leftW - 20) / 3));
        int presetGap = presetBtnW + 3;
        int presetBtnY = 44;

        int btnX = 10;
        for (int i = 0; i < PRESETS.length; i++) {
            final PresetTemplate p = PRESETS[i];
            String label = p.displayName;
            if (isNarrow && label.length() > 6) {
                label = label.substring(0, Math.min(5, presetBtnW / 10)) + "..";
            }
            this.addRenderableWidget(Button.builder(
                Component.literal(label),
                button -> applyPreset(p)
            ).bounds(btnX, presetBtnY, presetBtnW, 16).build());
            btnX += presetGap;
            if (btnX + presetGap > layout_rightX) {
                btnX = 10;
                presetBtnY += 20;
            }
        }
        layout_presetEndY = presetBtnY + 20;

        layout_sectionY = layout_presetEndY + 6;

        buildRightPanel();

        if (!loaded) {
            loaded = true;
            ShenNetwork.sendRequestLaws();
        }
    }

    private void buildRightPanel() {
        int rx = layout_rightX;
        int bw = layout_btnW;
        int sy = layout_sectionY;
        int sh = this.height;

        int maxBottomY = sh - 35;
        int currentY = sy;

        int availableH = maxBottomY - sy;
        int buttonStep = availableH >= 160 ? 20 : (availableH >= 120 ? 16 : 14);
        int btnH = buttonStep >= 18 ? 16 : 14;

        if (currentY + buttonStep > maxBottomY) currentY = sy;

        if (limited) {
            if (currentY + btnH > maxBottomY) currentY = maxBottomY - btnH;
            this.addRenderableWidget(Button.builder(Component.literal("§b导出"), button -> {
                if (selectedIndex >= 0 && selectedIndex < laws.size()) {
                    String json = ShenNetwork.GSON.toJson(laws.get(selectedIndex));
                    lastExportedJson = json;
                    lastExportedName = laws.get(selectedIndex).name;
                    saveExportCache(json);
                    if (this.minecraft != null) {
                        this.minecraft.keyboardHandler.setClipboard(json);
                        if (this.minecraft.player != null)
                            this.minecraft.player.sendSystemMessage(
                                Component.literal("§a[言出法随] §e已导出 §b" + laws.get(selectedIndex).name + " §e到剪贴板（同时已存入本地缓存）"));
                    }
                }
            }).bounds(rx, currentY, bw, btnH).build());
            currentY += buttonStep;

            if (selectedIndex >= 0 && selectedIndex < laws.size()) {
                if (currentY + btnH > maxBottomY) currentY = maxBottomY - btnH;
                ShenLaw sel = laws.get(selectedIndex);
                String lbl = sel.enabled ? "§c[禁用]" : "§a[启用]";
                this.addRenderableWidget(Button.builder(Component.literal(lbl), button -> {
                    sel.enabled = !sel.enabled;
                    ShenNetwork.sendSaveLaw(sel);
                    init();
                }).bounds(rx, currentY, bw, btnH).build());
                currentY += buttonStep;
            }

            if (currentY + btnH > maxBottomY) currentY = maxBottomY - btnH;
            this.addRenderableWidget(Button.builder(Component.literal("§d导入"), button -> {
                importFromClipboard();
            }).bounds(rx, currentY, bw, btnH).build());
            currentY += buttonStep;

            if (currentY + btnH > maxBottomY) {
                currentY = maxBottomY - btnH;
            }
            this.addRenderableWidget(Button.builder(Component.literal("§7关闭"), button -> {
                this.onClose();
            }).bounds(rx, currentY, bw, btnH).build());
            return;
        }

        this.addRenderableWidget(Button.builder(Component.literal("§a+ 新建"), button -> {
            ShenLaw law = new ShenLaw("新法则 " + (this.laws.size() + 1));
            ShenNetwork.sendSaveLaw(law);
        }).bounds(rx, currentY, bw, btnH).build());
        currentY += buttonStep;

        this.addRenderableWidget(Button.builder(Component.literal("§e编辑规则"), button -> {
            if (selectedIndex >= 0 && selectedIndex < laws.size()) {
                if (this.minecraft != null && this.minecraft.player != null)
                    this.minecraft.setScreen(new ShenRuleEditScreen(this, laws.get(selectedIndex)));
            }
        }).bounds(rx, currentY, bw, btnH).build());
        currentY += buttonStep;

        if (selectedIndex >= 0 && selectedIndex < laws.size()) {
            if (currentY + btnH > maxBottomY) return;
            ShenLaw sel = laws.get(selectedIndex);
            String lbl = sel.enabled ? "§c[禁用]" : "§a[启用]";
            this.addRenderableWidget(Button.builder(Component.literal(lbl), button -> {
                sel.enabled = !sel.enabled;
                ShenNetwork.sendSaveLaw(sel);
                init();
            }).bounds(rx, currentY, bw, btnH).build());
            currentY += buttonStep;
        }

        if (selectedIndex >= 0 && selectedIndex < laws.size()) {
            if (currentY + btnH > maxBottomY) return;
            if (!renaming) {
                this.addRenderableWidget(Button.builder(Component.literal("§d重命名"), button -> {
                    renaming = true;
                    init();
                }).bounds(rx, currentY, bw, btnH).build());
                currentY += buttonStep;
            } else {
                if (nameField == null) {
                    nameField = new EditBox(this.font,
                        rx, currentY, bw, btnH, Component.literal("新名称"));
                    nameField.setValue(laws.get(selectedIndex).name);
                    nameField.setMaxLength(64);
                }
                nameField.setWidth(bw);
                this.addRenderableWidget(nameField);
                currentY += btnH + 2;
                if (currentY + btnH <= maxBottomY) {
                    int hw = (bw - 2) / 2;
                    this.addRenderableWidget(Button.builder(Component.literal("§a✓"), button -> {
                        if (selectedIndex >= 0 && selectedIndex < laws.size()) {
                            String nn = nameField.getValue().trim();
                            if (!nn.isEmpty()) {
                                ShenLaw l = laws.get(selectedIndex);
                                if (!l.name.equals(nn)) {
                                    ShenNetwork.sendDeleteLaw(l.name);
                                    l.name = nn;
                                }
                                ShenNetwork.sendSaveLaw(l);
                            }
                        }
                        renaming = false; nameField = null; init();
                    }).bounds(rx, currentY, hw, btnH).build());
                    this.addRenderableWidget(Button.builder(Component.literal("§c✗"), button -> {
                        renaming = false; nameField = null; init();
                    }).bounds(rx + hw + 2, currentY, hw, btnH).build());
                    currentY += buttonStep;
                } else {
                    renaming = false; nameField = null;
                }
            }
        }

        if (currentY + btnH > maxBottomY) return;
        this.addRenderableWidget(Button.builder(Component.literal("§c删除"), button -> {
            if (selectedIndex >= 0 && selectedIndex < laws.size()) {
                ShenNetwork.sendDeleteLaw(laws.get(selectedIndex).name);
                laws.remove(selectedIndex);
                selectedIndex = -1; renaming = false; nameField = null;
                init();
            }
        }).bounds(rx, currentY, bw, btnH).build());
        currentY += buttonStep;

        if (currentY + btnH > maxBottomY) return;
        this.addRenderableWidget(Button.builder(Component.literal("§b导出"), button -> {
            if (selectedIndex >= 0 && selectedIndex < laws.size()) {
                String json = ShenNetwork.GSON.toJson(laws.get(selectedIndex));
                lastExportedJson = json;
                lastExportedName = laws.get(selectedIndex).name;
                saveExportCache(json);
                if (this.minecraft != null) {
                    this.minecraft.keyboardHandler.setClipboard(json);
                    if (this.minecraft.player != null)
                        this.minecraft.player.sendSystemMessage(
                            Component.literal("§a[言出法随] §e已导出 §b" + laws.get(selectedIndex).name + " §e到剪贴板（同时已存入本地缓存）"));
                }
            }
        }).bounds(rx, currentY, bw, btnH).build());
        currentY += buttonStep;

        if (currentY + btnH > maxBottomY) return;
        this.addRenderableWidget(Button.builder(Component.literal("§d导入"), button -> {
            importFromClipboard();
        }).bounds(rx, currentY, bw, btnH).build());
        currentY += buttonStep;

        if (currentY + btnH > maxBottomY) {
            currentY = maxBottomY - btnH;
        }
        this.addRenderableWidget(Button.builder(Component.literal("§7关闭"), button -> {
            this.onClose();
        }).bounds(rx, currentY, bw, btnH).build());
    }

    private void importFromClipboard() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ShenImportScreen(this));
        }
    }

    private void applyPreset(PresetTemplate preset) {
        ShenLaw tempLaw = new ShenLaw(preset.displayName);
        ShenRule rule = new ShenRule(preset.eventType, preset.targetType, preset.effectType);
        rule.params.putAll(preset.params);
        tempLaw.addRule(rule);
        ShenNetwork.sendSaveLaw(tempLaw);
        if (limited) {
            if (this.minecraft != null && this.minecraft.player != null)
                this.minecraft.player.sendSystemMessage(
                    Component.literal("§a[言出法随] §e已添加推荐模板 §b" + preset.displayName + " §e到我的法则"));
        } else if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.setScreen(new ShenRuleEditScreen(this, tempLaw));
        }
    }

    public void onSyncLaws(List<ShenLaw> syncedLaws) {
        this.laws = new ArrayList<>(syncedLaws);
        this.selectedIndex = -1;
        if (this.minecraft != null) this.init();
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        int sw = this.width;
        int listTop = layout_sectionY;
        int listBottom = this.height - 35;
        int rowH = 14;

        ctx.drawCenteredString(this.font, this.title, sw / 2, 10, 0xFFFFFF);

        ctx.drawCenteredString(this.font,
            Component.literal("§6✦ 推荐模板 ✦"), sw / 2, 28, 0xFFAA00);

        int titleY = layout_sectionY - 12;
        String sectionTitle = (sw < 350) ? "§e我的法则" : "§e── 我的法则 ──";
        ctx.drawCenteredString(this.font,
            Component.literal(sectionTitle), sw / 2, titleY, 0xFFAA00);

        int y = listTop;
        int listRight = layout_rightX - 5;
        int textAvailableWidth = layout_rightX - 25;

        for (int i = 0; i < laws.size(); i++) {
            if (y + rowH > listBottom) break;

            ShenLaw law = laws.get(i);
            int bgColor = 0x00000000;
            int textColor = 0xCCCCCC;

            if (i == selectedIndex) {
                bgColor = 0x444444AA;
                textColor = 0xFFAA00;
            } else if (mouseX >= 8 && mouseX <= listRight && mouseY >= y && mouseY < y + rowH) {
                bgColor = 0x44555555;
            }

            if (bgColor != 0) {
                ctx.fill(8, y, listRight, y + rowH, bgColor);
            }

            String status = law.enabled ? "§a[●]" : "§c[○]";
            String highlight = (i == selectedIndex) ? "§e▶ " : "  ";
            String fullText = highlight + status + " " + law.name + " §7(" + law.rules.size() + "条)";
            if (textAvailableWidth < 120 && law.name.length() > 8) {
                String shortName = law.name.substring(0, Math.min(6, textAvailableWidth / 8)) + "..";
                fullText = highlight + status + " " + shortName + " §7(" + law.rules.size() + "条)";
            }
            ctx.drawString(this.font,
                Component.literal(fullText),
                12, y + 2, textColor);
            y += rowH;
        }

        if (selectedIndex >= 0 && selectedIndex < laws.size()) {
            ShenLaw sel = laws.get(selectedIndex);
            String st = sel.enabled ? "§a已启用" : "§c已禁用";
            int statusY = Math.max(y + 8, this.height - 50);
            String statusText = "§7选中: §f" + sel.name + " " + st + " §7(" + sel.rules.size() + "条规则)";
            if (sw < 300 && sel.name.length() > 6) {
                statusText = "§7" + sel.name.substring(0, Math.min(5, textAvailableWidth / 8)) + ".. " + st;
            }
            ctx.drawString(this.font,
                Component.literal(statusText),
                10, statusY, 0xCCCCCC);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int listTop = layout_sectionY;
        int listBottom = this.height - 35;
        int listRight = layout_rightX - 5;
        int rowH = 14;
        int y = listTop;

        for (int i = 0; i < laws.size(); i++) {
            if (y + rowH > listBottom) break;
            if (mx >= 8 && mx <= listRight && my >= y && my < y + rowH) {
                selectedIndex = i;
                init();
                return true;
            }
            y += rowH;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private static class PresetTemplate {
        final String displayName;
        final ShenEventType eventType;
        final ShenTargetType targetType;
        final ShenEffectType effectType;
        final Map<String, String> params;

        PresetTemplate(String displayName, ShenEventType eventType, ShenTargetType targetType,
                       ShenEffectType effectType, Map<String, String> params) {
            this.displayName = displayName;
            this.eventType = eventType;
            this.targetType = targetType;
            this.effectType = effectType;
            this.params = params;
        }
    }

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            if (i + 1 < kv.length) m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
