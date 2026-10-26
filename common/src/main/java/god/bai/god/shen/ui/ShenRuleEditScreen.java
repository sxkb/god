package god.bai.god.shen.ui;

import god.bai.god.shen.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShenRuleEditScreen extends Screen {
    private final Screen parent;
    private final ShenLaw law;
    private final ShenRule editingRule;
    private int editingRuleIndex = -1;
    private int selectedSlot = 0;
    private boolean eventMultiMode = false;
    private boolean conditionMultiMode = false;
    private boolean targetMultiMode = false;
    private boolean multiEffectMode = false;
    private int selectedEventIndex = 0;
    private int selectedConditionIndex = 0;
    private int selectedTargetIndex = 0;
    private int selectedEffectIndex = 0;
    private EditBox customInput;
    private EditBox nameInput;

    private String dropdownCustomKey = "";
    /** 布局缓存：render() 复用 init() 的计算结果 */
    private int layout_paramStartY = 0;
    private int layout_selectorStartX = 0;
    private int scrollOffset = 0;
    private int maxScroll = 0;
    // 延迟重建标记：避免在 Minecraft 的点击事件处理中直接修改 children 列表
    private boolean dirty = false;

    public ShenRuleEditScreen(Screen parent, ShenLaw law) {
        super(Component.literal("编辑法则: " + law.name));
        this.parent = parent;
        this.law = law;
        this.editingRule = new ShenRule(ShenEventType.PLAYER_RIGHT_CLICK,
                ShenTargetType.EVENT_SOURCE, ShenEffectType.DEAL_DAMAGE);
    }

    private void rebuild() {
        this.dirty = true;
    }

    private void doRebuild() {
        this.dirty = false;
        this.clearWidgets();
        this.maxScroll = 0;
        this.init();
        // 在init之后裁剪scrollOffset（防止内容变少后scroll超出）
        if (scrollOffset > maxScroll) scrollOffset = Math.max(0, maxScroll);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (verticalAmount != 0) {
            int oldOffset = scrollOffset;
            scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * 20));
            if (scrollOffset != oldOffset) {
                this.dirty = false;
                this.clearWidgets();
                this.init();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

    @Override
    protected void init() {
        super.init();

        int sw = this.width;
        int sh = this.height;

        int ruleListW = Math.min(180, Math.max(60, sw / 5));
        int selectorStartX = ruleListW + 15;
        layout_selectorStartX = selectorStartX;

        int y = 45;
        int listBottom = sh - 80;
        for (int i = 0; i < law.rules.size(); i++) {
            if (y + 18 > listBottom) break;
            ShenRule rule = law.rules.get(i);
            String displayName;
            if (!rule.name.isEmpty()) {
                displayName = "§e✏ " + rule.name;
            } else {
                String shortSummary = rule.getSummary();
                int maxLen = Math.max(8, (ruleListW - 20) / 5);
                if (shortSummary.length() > maxLen) {
                    shortSummary = shortSummary.substring(0, maxLen - 2) + "..";
                }
                displayName = "§7[" + (i+1) + "] " + shortSummary;
            }
            final int idx = i;
            boolean isEditing = (idx == editingRuleIndex);
            this.addRenderableWidget(Button.builder(
                    Component.literal((isEditing ? "§a▶ " : "  ") + displayName),
                    button -> {
                        syncCurrentParams();
                        this.editingRule.copyFrom(law.rules.get(idx));
                        this.editingRuleIndex = idx;
                        this.rebuild();
                    }
            ).bounds(8, y, ruleListW, 16).build());
            y += 18;
        }

        int chainEndY = y;
        int nameBtnW = ruleListW - 16;
        if (selectedSlot == 0 && eventMultiMode && editingRule.eventTypes.size() > 1) {
            for (int i = 0; i < editingRule.eventTypes.size(); i++) {
                if (y + 14 > listBottom - 35) break;
                boolean isSelected = (i == selectedEventIndex);
                final int fi = i;
                ShenEventType et = editingRule.eventTypes.get(i);
                this.addRenderableWidget(Button.builder(
                        Component.literal((isSelected ? "§a▶ " : "  ") + "§7[" + i + "]§r " + et.getDisplayName()),
                        b -> { selectedEventIndex = fi; rebuild(); }
                ).bounds(8, y, nameBtnW, 14).build());
                this.addRenderableWidget(Button.builder(
                        Component.literal("§c✗"),
                        b -> { editingRule.eventTypes.remove(fi); if (selectedEventIndex >= editingRule.eventTypes.size()) selectedEventIndex = editingRule.eventTypes.size() - 1; rebuild(); }
                ).bounds(8 + nameBtnW, y, 14, 14).build());
                y += 15;
            }
            y += 3;
        } else if (selectedSlot == 1 && conditionMultiMode && editingRule.conditions.size() > 1) {
            for (int i = 0; i < editingRule.conditions.size(); i++) {
                if (y + 14 > listBottom - 35) break;
                boolean isSelected = (i == selectedConditionIndex);
                final int fi = i;
                ShenRule.ShenCondition cond = editingRule.conditions.get(i);
                this.addRenderableWidget(Button.builder(
                        Component.literal((isSelected ? "§a▶ " : "  ") + "§7[" + i + "]§r " + cond.type.getDisplayName()),
                        b -> {
                            if (fi != selectedConditionIndex) loadConditionParams(fi);
                            rebuild();
                        }
                ).bounds(8, y, nameBtnW, 14).build());
                this.addRenderableWidget(Button.builder(
                        Component.literal("§c✗"),
                        b -> { editingRule.conditions.remove(fi); if (selectedConditionIndex >= editingRule.conditions.size()) selectedConditionIndex = editingRule.conditions.size() - 1; rebuild(); }
                ).bounds(8 + nameBtnW, y, 14, 14).build());
                y += 15;
            }
            y += 3;
        } else if (selectedSlot == 2 && targetMultiMode && editingRule.targetTypes.size() > 1) {
            for (int i = 0; i < editingRule.targetTypes.size(); i++) {
                if (y + 14 > listBottom - 35) break;
                boolean isSelected = (i == selectedTargetIndex);
                final int fi = i;
                ShenTargetType tt = editingRule.targetTypes.get(i);
                this.addRenderableWidget(Button.builder(
                        Component.literal((isSelected ? "§a▶ " : "  ") + "§7[" + i + "]§r " + tt.getDisplayName()),
                        b -> { selectedTargetIndex = fi; rebuild(); }
                ).bounds(8, y, nameBtnW, 14).build());
                this.addRenderableWidget(Button.builder(
                        Component.literal("§c✗"),
                        b -> { editingRule.targetTypes.remove(fi); if (selectedTargetIndex >= editingRule.targetTypes.size()) selectedTargetIndex = editingRule.targetTypes.size() - 1; rebuild(); }
                ).bounds(8 + nameBtnW, y, 14, 14).build());
                y += 15;
            }
            y += 3;
        } else if (selectedSlot == 3 && multiEffectMode && editingRule.effects.size() > 1) {
            for (int i = 0; i < editingRule.effects.size(); i++) {
                if (y + 14 > listBottom - 35) break;
                ShenRule.ShenEffectEntry entry = editingRule.effects.get(i);
                String delayLabel = "";
                if (entry.delayTicks > 0) {
                    delayLabel = " §8(" + (entry.delayTicks / 20.0) + "s)";
                }
                boolean isSelected = (i == selectedEffectIndex);
                final int fi = i;
                this.addRenderableWidget(Button.builder(
                        Component.literal((isSelected ? "§a▶ " : "  ") + "§7[" + i + "]§r " + entry.effectType.getDisplayName() + delayLabel),
                        b -> {
                            if (fi != selectedEffectIndex) loadEffectParams(fi);
                            rebuild();
                        }
                ).bounds(8, y, nameBtnW, 14).build());
                this.addRenderableWidget(Button.builder(
                        Component.literal("§c✗"),
                        b -> { editingRule.effects.remove(fi); if (selectedEffectIndex >= editingRule.effects.size()) selectedEffectIndex = editingRule.effects.size() - 1; rebuild(); }
                ).bounds(8 + nameBtnW, y, 14, 14).build());
                y += 15;
            }
            y += 3;
        }
        chainEndY = y;

        int topSlotY = 26;
        int cols = 4;
        int gap = 3;
        int topSlotW = Math.max(30, (sw - selectorStartX - 10 - gap * 3) / cols);

        String eventLabel, condLabel, targetLabel, effectLabel;
        if (sw < 500) {
            String eventModeIcon = eventMultiMode ? "§e⛓" : "§7⚡";
            eventLabel = "§b事:" + editingRule.eventType.getDisplayName().substring(0, Math.min(2, editingRule.eventType.getDisplayName().length())) + eventModeIcon;
            condLabel = "§e条:" + (editingRule.conditions.isEmpty() ? "无" : editingRule.conditions.size() + "") + (conditionMultiMode ? "§e⛓" : "§7⚡");
            targetLabel = "§a目:" + editingRule.targetType.getDisplayName().substring(0, Math.min(2, editingRule.targetType.getDisplayName().length())) + (targetMultiMode ? "§e⛓" : "§7⚡");
            effectLabel = "§d效:" + (editingRule.effects.isEmpty() ? "无" : editingRule.effects.size() + "") + (multiEffectMode ? "§e⛓" : "§7⚡");
        } else {
            String eventModeIcon = eventMultiMode ? "§e⛓多" : "§7⚡单";
            eventLabel = "§b[事件] " + (editingRule.eventTypes.size() > 1 ? editingRule.eventTypes.size() + "个" : editingRule.eventType.getDisplayName()) + " " + eventModeIcon;
            condLabel = "§e[条件] " + (editingRule.conditions.isEmpty() ? "无" : editingRule.conditions.size() + "个") + (conditionMultiMode ? " §e⛓多" : " §7⚡单");
            targetLabel = "§a[目标] " + (editingRule.targetTypes.size() > 1 ? editingRule.targetTypes.size() + "个" : editingRule.targetType.getDisplayName()) + (targetMultiMode ? " §e⛓多" : " §7⚡单");
            effectLabel = "§d[效果] " + (editingRule.effects.isEmpty() ? "无" : editingRule.effects.size() + "个") + (multiEffectMode ? " §e⛓多" : " §7⚡单");
        }

        this.addRenderableWidget(Button.builder(
                Component.literal(eventLabel),
                button -> {
                    if (selectedSlot == 0) {
                        eventMultiMode = !eventMultiMode;
                        if (!eventMultiMode && editingRule.eventTypes.size() > 1) {
                            ShenEventType first = editingRule.eventTypes.get(0);
                            editingRule.eventTypes.clear();
                            editingRule.eventTypes.add(first);
                            editingRule.eventType = first;
                        }
                        selectedEventIndex = 0;
                    } else {
                        syncCurrentParams();
                        selectedSlot = 0;
                    }
                    rebuild();
                }
        ).bounds(selectorStartX, topSlotY, topSlotW, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(condLabel),
                button -> {
                    if (selectedSlot == 1) {
                        conditionMultiMode = !conditionMultiMode;
                        if (!conditionMultiMode && editingRule.conditions.size() > 1) {
                            ShenRule.ShenCondition first = editingRule.conditions.get(0);
                            editingRule.conditions.clear();
                            editingRule.conditions.add(first);
                        }
                        selectedConditionIndex = 0;
                    } else {
                        syncCurrentParams();
                        selectedSlot = 1;
                    }
                    rebuild();
                }
        ).bounds(selectorStartX + (topSlotW + gap), topSlotY, topSlotW, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(targetLabel),
                button -> {
                    if (selectedSlot == 2) {
                        targetMultiMode = !targetMultiMode;
                        if (!targetMultiMode && editingRule.targetTypes.size() > 1) {
                            ShenTargetType first = editingRule.targetTypes.get(0);
                            editingRule.targetTypes.clear();
                            editingRule.targetTypes.add(first);
                            editingRule.targetType = first;
                        }
                        selectedTargetIndex = 0;
                    } else {
                        syncCurrentParams();
                        selectedSlot = 2;
                    }
                    rebuild();
                }
        ).bounds(selectorStartX + (topSlotW + gap) * 2, topSlotY, topSlotW, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(effectLabel),
                button -> {
                    if (selectedSlot == 3) {
                        multiEffectMode = !multiEffectMode;
                        if (!multiEffectMode && editingRule.effects.size() > 1) {
                            ShenEffectType firstType = editingRule.effects.get(0).effectType;
                            editingRule.effects.clear();
                            editingRule.effects.add(new ShenRule.ShenEffectEntry(firstType));
                            editingRule.effectType = firstType;
                        }
                        selectedEffectIndex = 0;
                    } else {
                        syncCurrentParams();
                        selectedSlot = 3;
                    }
                    rebuild();
                }
        ).bounds(selectorStartX + (topSlotW + gap) * 3, topSlotY, topSlotW, 18).build());

        int nameInputY = 48;
        if (sw > 400) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("§6[名称]"),
                    b -> {}
            ).bounds(selectorStartX, nameInputY, 40, 16).build());
        }

        int nameFieldW = Math.min(200, Math.max(60, sw - selectorStartX - (sw > 400 ? 50 : 10)));
        if (this.nameInput == null) {
            this.nameInput = new EditBox(this.font,
                    selectorStartX + (sw > 400 ? 42 : 0), nameInputY, nameFieldW, 16, Component.literal("规则名称（留空自动生成）"));
            this.nameInput.setMaxLength(32);
            this.nameInput.setValue(editingRule.name);
            this.nameInput.setResponder(text -> editingRule.name = text);
        } else {
            String currentText = this.nameInput.getValue();
            this.nameInput = new EditBox(this.font,
                    selectorStartX + (sw > 400 ? 42 : 0), nameInputY, nameFieldW, 16, Component.literal("规则名称（留空自动生成）"));
            this.nameInput.setMaxLength(32);
            this.nameInput.setValue(currentText);
            this.nameInput.setResponder(text -> editingRule.name = text);
        }
        this.addRenderableWidget(this.nameInput);

        int scrollViewTop = (sw < 500) ? 65 : 70;
        int scrollViewBottom = sh - 50;

        int virtualTotalHeight = calcVirtualCategoryHeight(scrollViewTop, sw);
        int viewportHeight = scrollViewBottom - scrollViewTop;
        maxScroll = Math.max(0, virtualTotalHeight - viewportHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int selectorEndY = renderSelector(selectorStartX, scrollViewTop, sw, sh, scrollOffset, scrollViewTop, scrollViewBottom);

        java.util.List<ShenParamHelper.ParamDef> paramDefs = getCurrentParamDefs();
        if (!paramDefs.isEmpty()) {
            String summary = getCurrentSummaryText();
            int paramBtnWidth = ruleListW;
            int paramBtnX = 8;
            int paramBtnY = Math.min(y + 5, listBottom - 30);
            if (paramBtnY > 50 && paramBtnY + 24 < sh - 35) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("§6⚙ 配置参数 (" + paramDefs.size() + "项)"),
                        button -> {
                            syncCurrentParams();
                            Map<String, String> paramsCopy = new HashMap<>(getCurrentParams());
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new god.bai.god.shen.ui.ShenParamConfigScreen(
                                        this,
                                        summary,
                                        paramDefs,
                                        paramsCopy,
                                        () -> applyParams(paramsCopy)
                                ));
                            }
                        }
                ).bounds(paramBtnX, paramBtnY, paramBtnWidth, 20).build());
            }
        }

        int bottomY = sh - 28;
        int bottomBtnW = Math.min(100, (sw - 30) / 3);
        int saveBtnX = sw / 2 - bottomBtnW / 2;

        String saveLabel = (editingRuleIndex >= 0 && editingRuleIndex < law.rules.size())
                ? "§a✓ 更新规则" : "§a+ 添加规则";
        this.addRenderableWidget(Button.builder(Component.literal(saveLabel), button -> {
            syncConditionParams();
            syncEffectDelay();
            int savedScroll = scrollOffset;
            if (editingRuleIndex >= 0 && editingRuleIndex < law.rules.size()) {
                law.rules.set(editingRuleIndex, editingRule);
            } else {
                if (!law.contains(editingRule)) law.addRule(editingRule);
                editingRuleIndex = law.rules.indexOf(editingRule);
            }
            god.bai.god.shen.ShenNetwork.sendSaveLaw(law);
            rebuild();
            scrollOffset = savedScroll; // 恢复滚动位置，避免跳回顶部
        }).bounds(saveBtnX, bottomY, bottomBtnW, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c删除选中"), button -> {
            if (editingRuleIndex >= 0 && editingRuleIndex < law.rules.size()) {
                law.rules.remove(editingRuleIndex);
                god.bai.god.shen.ShenNetwork.sendSaveLaw(law);
                editingRuleIndex = -1;
                this.rebuild();
            } else if (law.contains(editingRule)) {
                law.rules.remove(editingRule);
                god.bai.god.shen.ShenNetwork.sendSaveLaw(law);
                editingRuleIndex = -1;
                this.rebuild();
            }
        }).bounds(saveBtnX - bottomBtnW - 5, bottomY, bottomBtnW, 18).build());

        this.addRenderableWidget(Button.builder(Component.literal("§c返回"), button -> {
            syncCurrentParams();
            if (editingRuleIndex >= 0 && editingRuleIndex < law.rules.size()) {
                law.rules.set(editingRuleIndex, editingRule);
            } else {
                if (!law.contains(editingRule)) law.addRule(editingRule);
                editingRuleIndex = law.rules.indexOf(editingRule);
            }
            god.bai.god.shen.ShenNetwork.sendSaveLaw(law);
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).bounds(saveBtnX + bottomBtnW + 5, bottomY, bottomBtnW, 18).build());
    }

    private int calcVirtualCategoryHeight(int startY, int sw) {
        int cols = Math.max(2, (sw - layout_selectorStartX - 10) / (Math.min(130, Math.max(50, (sw - layout_selectorStartX - 15) / 2)) + 5));
        int buttonWidth = Math.min(130, Math.max(50, (sw - layout_selectorStartX - 15) / 2));
        int buttonHeight = 18;
        int x = layout_selectorStartX;

        Enum<?>[] allValues;
        Function<Enum<?>, String> categoryFn;
        switch (selectedSlot) {
            case 0:
                allValues = ShenEventType.values();
                categoryFn = v -> ((ShenEventType) v).getCategory();
                break;
            case 1:
                allValues = ShenConditionType.values();
                categoryFn = v -> ((ShenConditionType) v).getCategory();
                break;
            case 2:
                allValues = ShenTargetType.values();
                categoryFn = v -> ((ShenTargetType) v).getCategory();
                break;
            case 3:
                allValues = ShenEffectType.values();
                categoryFn = v -> ((ShenEffectType) v).getCategory();
                break;
            default:
                return 0;
        }

        Map<String, List<Enum<?>>> grouped = new LinkedHashMap<>();
        for (Enum<?> val : allValues) {
            String cat = categoryFn.apply(val);
            grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(val);
        }

        int virtualY = 0;
        for (Map.Entry<String, List<Enum<?>>> entry : grouped.entrySet()) {
            List<Enum<?>> items = entry.getValue();
            virtualY += buttonHeight;
            int rows = (int) Math.ceil((double) items.size() / cols);
            virtualY += rows * (buttonHeight + 2);
            virtualY += 4;
        }
        return virtualY;
    }

    private int renderSelector(int startX, int startY, int sw, int sh, int scrollOffset,
                               int viewTop, int viewBottom) {
        int buttonWidth = Math.min(130, Math.max(50, (sw - startX - 15) / 2));
        int buttonHeight = 18;
        int availableWidth = sw - startX - 10;
        int cols = Math.max(2, availableWidth / (buttonWidth + 5));

        switch (selectedSlot) {
            case 0:
                return renderCategoryGroup(startX, startY, cols, sw, sh, scrollOffset, viewTop, viewBottom,
                        ShenEventType.values(),
                        v -> ((ShenEventType) v).getCategory(),
                        v -> ((ShenEventType) v).getDisplayName(),
                        v -> ((ShenEventType) v).toText(),
                        type -> {
                            ShenEventType clicked = (ShenEventType) type;
                            if (eventMultiMode) {
                                if (editingRule.eventTypes.isEmpty() ||
                                        !editingRule.eventTypes.get(editingRule.eventTypes.size() - 1).equals(clicked)) {
                                    editingRule.eventTypes.add(clicked);
                                    selectedEventIndex = editingRule.eventTypes.size() - 1;
                                }
                            } else {
                                editingRule.eventTypes.clear();
                                editingRule.eventTypes.add(clicked);
                                selectedEventIndex = 0;
                            }
                            editingRule.eventType = clicked;
                            rebuild();
                        });
            case 1:
                return renderCategoryGroup(startX, startY, cols, sw, sh, scrollOffset, viewTop, viewBottom,
                        ShenConditionType.values(),
                        v -> ((ShenConditionType) v).getCategory(),
                        v -> ((ShenConditionType) v).getDisplayName(),
                        v -> ((ShenConditionType) v).toText(),
                        type -> {
                            ShenConditionType clicked = (ShenConditionType) type;
                            if (conditionMultiMode) {
                                editingRule.conditions.add(new ShenRule.ShenCondition(clicked, "==", "0"));
                                selectedConditionIndex = editingRule.conditions.size() - 1;
                            } else {
                                syncConditionParams();
                                editingRule.conditions.clear();
                                editingRule.conditions.add(new ShenRule.ShenCondition(clicked, "==", "0"));
                                selectedConditionIndex = 0;
                            }
                            rebuild();
                        });
            case 2:
                return renderCategoryGroup(startX, startY, cols, sw, sh, scrollOffset, viewTop, viewBottom,
                        ShenTargetType.values(),
                        v -> ((ShenTargetType) v).getCategory(),
                        v -> ((ShenTargetType) v).getDisplayName(),
                        v -> ((ShenTargetType) v).toText(),
                        type -> {
                            ShenTargetType clicked = (ShenTargetType) type;
                            if (targetMultiMode) {
                                if (editingRule.targetTypes.isEmpty() ||
                                        !editingRule.targetTypes.get(editingRule.targetTypes.size() - 1).equals(clicked)) {
                                    editingRule.targetTypes.add(clicked);
                                    selectedTargetIndex = editingRule.targetTypes.size() - 1;
                                }
                            } else {
                                editingRule.targetTypes.clear();
                                editingRule.targetTypes.add(clicked);
                                selectedTargetIndex = 0;
                            }
                            editingRule.targetType = clicked;
                            rebuild();
                        });
            case 3:
                return renderCategoryGroup(startX, startY, cols, sw, sh, scrollOffset, viewTop, viewBottom,
                        ShenEffectType.values(),
                        v -> ((ShenEffectType) v).getCategory(),
                        v -> ((ShenEffectType) v).getDisplayName(),
                        v -> ((ShenEffectType) v).toText(),
                        type -> {
                            ShenEffectType clickedType = (ShenEffectType) type;
                            if (multiEffectMode) {
                                if (editingRule.effects.isEmpty() ||
                                        !editingRule.effects.get(editingRule.effects.size() - 1).effectType.equals(clickedType)) {
                                    editingRule.effects.add(new ShenRule.ShenEffectEntry(clickedType));
                                    selectedEffectIndex = editingRule.effects.size() - 1;
                                }
                            } else {
                                editingRule.effects.clear();
                                editingRule.effects.add(new ShenRule.ShenEffectEntry(clickedType));
                                selectedEffectIndex = 0;
                            }
                            editingRule.effectType = clickedType;
                            rebuild();
                        });
            default:
                return startY;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> int renderCategoryGroup(int startX, int startY, int cols, int sw, int sh,
                                                        int scrollOffset, int viewTop, int viewBottom,
                                                        T[] allValues, Function<T, String> categoryFn,
                                                        Function<T, String> displayFn,
                                                        Function<T, Component> toTextFn,
                                                        Consumer<T> onClick) {
        int buttonWidth = Math.min(130, Math.max(50, (sw - startX - 15) / cols - 3));
        int buttonHeight = 18;

        Map<String, List<T>> grouped = new LinkedHashMap<>();
        for (T val : allValues) {
            String cat = categoryFn.apply(val);
            grouped.computeIfAbsent(cat, k -> new ArrayList<>()).add(val);
        }

        T currentSelected = null;
        if (allValues.length > 0) {
            if (selectedSlot == 0) currentSelected = (T) editingRule.eventType;
            else if (selectedSlot == 2) currentSelected = (T) editingRule.targetType;
            else if (selectedSlot == 3) currentSelected = editingRule.effects.isEmpty() ? null : (T) editingRule.effects.get(0).effectType;
        }

        int virtualY = 0;
        int lastRenderedEndY = viewTop;

        for (Map.Entry<String, List<T>> entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<T> items = entry.getValue();

            int catTitleHeight = buttonHeight;
            int itemRows = (int) Math.ceil((double) items.size() / cols);
            int itemsHeight = itemRows * (buttonHeight + 2);
            int categoryHeight = catTitleHeight + itemsHeight + 4;

            int renderedY = viewTop - scrollOffset + virtualY;

            boolean catVisible = (renderedY + categoryHeight > viewTop && renderedY < viewBottom);

            if (catVisible) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("§6§l" + category),
                        b -> {}
                ).bounds(startX, renderedY, buttonWidth * Math.min(cols, items.size()) + 5, buttonHeight - 2).build());

                int itemY = renderedY + buttonHeight;
                int col = 0;
                for (int i = 0; i < items.size(); i++) {
                    T item = items.get(i);
                    boolean isSelected = currentSelected != null && item.equals(currentSelected);
                    String display = displayFn.apply(item);
                    if (sw < 300 && display.length() > 8) {
                        display = display.substring(0, 6) + "..";
                    }
                    String prefix = isSelected ? "§a▶ " : "  ";

                    int btnY = itemY + (i / cols) * (buttonHeight + 2);
                    if (btnY + buttonHeight > viewTop && btnY < viewBottom) {
                        this.addRenderableWidget(Button.builder(
                                Component.literal(prefix + display),
                                b -> onClick.accept(item)
                        ).bounds(startX + col * (buttonWidth + 3), btnY, buttonWidth, buttonHeight).build());
                    }

                    col++;
                    if (col >= cols) {
                        col = 0;
                    }
                }
                lastRenderedEndY = renderedY + categoryHeight;
            }

            virtualY += categoryHeight;
        }

        return Math.max(lastRenderedEndY, viewTop);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // 延迟重建：避免在 Minecraft 事件处理中直接修改 children 列表
        if (dirty) {
            doRebuild();
            // 不 return，继续渲染以避免画面闪烁
        }

        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int sw = this.width;
        int sh = this.height;

        context.drawCenteredString(this.font, this.title, sw / 2, 10, 0xFFFFFF);

        if (!law.rules.isEmpty()) {
            context.drawString(this.font,
                    Component.literal("§e已有规则:"),
                    10, 32, 0xAAAAAA);
        }

        if (maxScroll > 0) {
            String scrollHint = scrollOffset > 0 ? "§7▲ 滚轮上下滚动 ▼" : "§7▼ 滚轮向下滚动 ▼";
            context.drawCenteredString(this.font,
                    Component.literal(scrollHint),
                    sw / 2, sh - 10, 0x666666);
        }
    }

    private List<ShenParamHelper.ParamDef> getCurrentParamDefs() {
        if (selectedSlot == 1) {
            return getConditionParamDefs();
        } else if (selectedSlot == 2) {
            return ShenParamHelper.getTargetParams(editingRule.targetType);
        } else if (selectedSlot == 3) {
            if (!editingRule.effects.isEmpty()) {
                int idx = Math.min(selectedEffectIndex, editingRule.effects.size() - 1);
                @SuppressWarnings("unchecked")
                List<ShenParamHelper.ParamDef> params = ShenParamHelper.getEffectParams(editingRule.effects.get(idx).effectType);
                params.add(0, new ShenParamHelper.ParamDef("_effect_delay", "延迟(秒)",
                        ShenParamHelper.WidgetType.NUMBER, String.valueOf(editingRule.effects.get(idx).delayTicks / 20.0), 0, 300));
                return params;
            }
            return List.of();
        }
        return List.of();
    }

    private List<ShenParamHelper.ParamDef> getConditionParamDefs() {
        if (editingRule.conditions.isEmpty()) return List.of();
        int condIdx = Math.min(selectedConditionIndex, editingRule.conditions.size() - 1);
        ShenRule.ShenCondition cond = editingRule.conditions.get(condIdx);
        List<ShenParamHelper.ParamDef> params = new ArrayList<>();

        if (editingRule.conditions.size() > 1) {
            params.add(new ShenParamHelper.ParamDef("_cond_logic", "多条件逻辑",
                    ShenParamHelper.WidgetType.BOOLEAN, editingRule.conditionLogic ? "true" : "false"));
        }

        params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较",
                ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                {"等于 ==", "=="}, {"大于 >", ">"}, {"小于 <", "<"},
                {"大于等于 >=", ">="}, {"小于等于 <=", "<="},
        }));

        switch (cond.type) {
            case ITEM_IN_HAND:
            case ITEM_IN_INVENTORY:
                params.add(new ShenParamHelper.ParamDef("_cond_value", "物品",
                        cond.value.equals("0") ? "minecraft:diamond" : cond.value,
                        ShenParamHelper.RegistryType.ITEM));
                break;
            case HAS_EFFECT:
                params.add(new ShenParamHelper.ParamDef("_cond_value", "药水效果",
                        cond.value.equals("0") ? "minecraft:strength" : cond.value,
                        ShenParamHelper.RegistryType.STATUS_EFFECT));
                break;
            case ITEM_HAS_ENCHANTMENT:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "附魔类型",
                        cond.value.equals("0") ? "minecraft:sharpness" : cond.value,
                        ShenParamHelper.RegistryType.ENCHANTMENT));
                break;
            case NBT_COMPARE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_nbtKey", "NBT路径",
                        cond.params.getOrDefault("nbtKey", "Health"),
                        ShenParamHelper.RegistryType.NBT_VARIABLE));
                params.add(new ShenParamHelper.ParamDef("_cond_value", "期望值",
                        ShenParamHelper.WidgetType.TEXT, cond.value));
                break;
            case ENTITY_TYPE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "实体类型",
                        cond.value.equals("0") ? "minecraft:zombie" : cond.value,
                        ShenParamHelper.RegistryType.ENTITY_TYPE));
                break;
            case ENTITY_EXISTS:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "实体ID",
                        ShenParamHelper.WidgetType.TEXT, cond.value));
                break;
            case BLOCK_BELOW:
            case BLOCK_ABOVE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "方块",
                        cond.value.equals("0") ? "minecraft:stone" : cond.value,
                        ShenParamHelper.RegistryType.BLOCK));
                break;
            case PERMISSION_LEVEL:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "权限等级",
                        ShenParamHelper.WidgetType.NUMBER, cond.value, 0, 4));
                break;
            case ATTRIBUTE_VALUE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "属性ID",
                        cond.value, ShenParamHelper.RegistryType.ATTRIBUTE));
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "比较值",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "0"), 0, 100000));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {">", ">"}, {"<", "<"}, {"==", "=="},
                }));
                break;
            case IS_WEARING_ARMOR:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "盔甲部位",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"任意", "any"}, {"头盔", "head"}, {"胸甲", "chest"}, {"护腿", "legs"}, {"靴子", "feet"},
                }));
                break;
            case IS_WEARING_ITEM:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "物品ID",
                        "minecraft:diamond_chestplate", ShenParamHelper.RegistryType.ITEM));
                break;
            case ITEM_NAME:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_nameKeyword", "名称关键词",
                        ShenParamHelper.WidgetType.TEXT, cond.params.getOrDefault("nameKeyword", cond.value)));
                params.add(new ShenParamHelper.ParamDef("_cond_nameMatchMode", "匹配模式",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.params.getOrDefault("nameMatchMode", "contains"), new String[][]{
                        {"包含关键词", "contains"}, {"完全一致", "exact"},
                }));
                break;
            case MOON_PHASE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "月相",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"满月", "0"}, {"亏凸月", "1"}, {"下弦月", "2"}, {"残月", "3"},
                        {"新月", "4"}, {"蛾眉月", "5"}, {"上弦月", "6"}, {"盈凸月", "7"},
                }));
                break;
            case DIFFICULTY:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "难度",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"和平", "peaceful"}, {"简单", "easy"}, {"普通", "normal"}, {"困难", "hard"},
                }));
                break;
            case IN_STRUCTURE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "结构",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.value.isEmpty() ? "minecraft:village_plains" : cond.value,
                        ShenParamHelper.buildStructureOptions()));
                break;
            case STRUCTURE_DISTANCE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "结构",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.value.isEmpty() ? "minecraft:village_plains" : cond.value,
                        ShenParamHelper.buildStructureOptions()));
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "距离(格)",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "100"), 0, 10000, true));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {"<=", "<="}, {">=", ">="}, {"<", "<"}, {">", ">"},
                }));
                params.add(new ShenParamHelper.ParamDef("_cond_maxDistance", "搜索范围",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("maxDistance", "200"), 16, 10000));
                break;
            case MOD_ADAPTER_CONDITION:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "适配器条件",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.value.isEmpty() ? "" : cond.value,
                        god.bai.god.shen.compat.ModAdapterManager.buildAdapterOptions("condition")));
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "比较值",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "0"), -100000, 100000));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {">", ">"}, {"<", "<"}, {"==", "=="}, {"!=", "!="},
                }));
                break;
            case SPECIFIC_PLAYER:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_playerName", "在线玩家",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.params.getOrDefault("playerName", ""),
                        ShenParamHelper.buildPlayerOptions()));
                params.add(new ShenParamHelper.ParamDef("_cond_playerNameInput", "玩家名（手动输入）",
                        ShenParamHelper.WidgetType.TEXT, cond.params.getOrDefault("playerName", "")));
                params.add(new ShenParamHelper.ParamDef("_cond_playerUuid", "玩家UUID（手动输入）",
                        ShenParamHelper.WidgetType.TEXT, cond.params.getOrDefault("playerUuid", "")));
                break;

            case SMFS_HAS_GHOST_TYPE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "鬼类型",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.value.isEmpty() ? "smfs:ghost_master" : cond.value,
                        god.bai.god.shen.compat.smfs.SmfsAdapter.buildGhostTypeOptions()));
                break;
            case SMFS_SLOT_OCCUPIED:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_slotIndex", "槽位索引",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("slotIndex", "0"),
                        0, god.bai.god.shen.compat.smfs.SmfsIntegration.getTotalSlotCount() - 1));
                params.add(new ShenParamHelper.ParamDef("_cond_value", "是否占用",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"占用", "true"}, {"空置", "false"},
                }));
                break;
            case SMFS_HAS_MAIN_GHOST:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "是否有主鬼",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"是", "true"}, {"否", "false"},
                }));
                break;
            case SMFS_SPIRIT_ATTR_COMPARE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_spiritAttr", "灵异属性",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.params.getOrDefault("spiritAttr", "spiritResistance"),
                        new String[][]{
                                {"灵异抗性", "spiritResistance"},
                                {"灵异伤害", "spiritDamage"},
                                {"当前灵力", "currentSpirit"},
                                {"最大灵力", "maxSpirit"},
                                {"理智值", "sanity"},
                                {"复苏因子", "revivalFactor"},
                        }));
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "比较值",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "0"),
                        0, 100000));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {">", ">"}, {"<", "<"}, {"==", "=="}, {"!=", "!="},
                }));
                break;
            case SMFS_GHOST_POSSESSION:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "复苏程度(%)",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "50"),
                        0, 100));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {">", ">"}, {"<", "<"}, {"==", "=="},
                }));
                break;
            case SMFS_MAIN_GHOST_TYPE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "主鬼类型",
                        ShenParamHelper.WidgetType.DROPDOWN,
                        cond.value.isEmpty() ? "smfs:ghost_master" : cond.value,
                        god.bai.god.shen.compat.smfs.SmfsAdapter.buildGhostTypeOptions()));
                break;

            case SMFS_HAS_GHOST_DOMAIN:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "鬼域状态",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"开启", "true"}, {"关闭", "false"},
                }));
                break;
            case SMFS_GHOST_DOMAIN_TYPE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "鬼域颜色",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"红色", "0"}, {"绿色", "1"}, {"蓝色", "2"}, {"灰色", "3"},
                        {"金色", "4"}, {"紫色", "5"}, {"黑色", "6"}, {"青色", "7"},
                }));
                break;
            case SMFS_GHOST_DOMAIN_LEVEL:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "比较等级(1~10)",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "1"),
                        1, 10));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {"==", "=="}, {">", ">"}, {"<", "<"}, {"!=", "!="},
                }));
                break;

            case SMFS_HAS_GHOST_EYE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_value", "鬼状态",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.value, new String[][]{
                        {"开启", "true"}, {"关闭", "false"},
                }));
                break;
            case SMFS_GHOST_EYE_LEVEL:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "比较等级(1~10)",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "1"),
                        1, 10));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {"==", "=="}, {">", ">"}, {"<", "<"}, {"!=", "!="},
                }));
                break;

            case SMFS_SLOT_LEVEL_COMPARE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_slotIndex", "槽位索引",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("slotIndex", "0"),
                        0, god.bai.god.shen.compat.smfs.SmfsIntegration.getTotalSlotCount() - 1));
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "比较等级",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "1"),
                        0, 100));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {"==", "=="}, {">", ">"}, {"<", "<"},
                }));
                break;
            case SMFS_SLOT_COUNT_COMPARE:
                params.clear();
                params.add(new ShenParamHelper.ParamDef("_cond_compareValue", "已驾驭数量",
                        ShenParamHelper.WidgetType.NUMBER, cond.params.getOrDefault("compareValue", "1"),
                        0, god.bai.god.shen.compat.smfs.SmfsIntegration.getTotalSlotCount()));
                params.add(new ShenParamHelper.ParamDef("_cond_operator", "比较符",
                        ShenParamHelper.WidgetType.DROPDOWN, cond.operator, new String[][]{
                        {">=", ">="}, {"<=", "<="}, {"==", "=="}, {">", ">"}, {"<", "<"}, {"!=", "!="},
                }));
                break;

            case IS_ELYTRA_FLYING:
            case IS_GLIDING:
            case IS_SWIMMING:
            case IS_RIDING:
                params.clear();
                break;
            default:
                params.add(new ShenParamHelper.ParamDef("_cond_value", "值",
                        ShenParamHelper.WidgetType.TEXT, cond.value));
                break;
        }
        return params;
    }

    private String getCurrentSummaryText() {
        switch (selectedSlot) {
            case 0: return "事件: " + (editingRule.eventTypes.size() > selectedEventIndex ?
                    editingRule.eventTypes.get(selectedEventIndex).getDisplayName() : editingRule.eventType.getDisplayName());
            case 1: {
                int idx = Math.min(selectedConditionIndex, editingRule.conditions.size() - 1);
                return "条件[" + idx + "]: " + (editingRule.conditions.isEmpty() ? "无" :
                        editingRule.conditions.get(idx).type.getDisplayName());
            }
            case 2: return "目标: " + (editingRule.targetTypes.size() > selectedTargetIndex ?
                    editingRule.targetTypes.get(selectedTargetIndex).getDisplayName() : editingRule.targetType.getDisplayName());
            case 3: {
                int idx = Math.min(selectedEffectIndex, editingRule.effects.size() - 1);
                String effectName = editingRule.effects.isEmpty() ? "无" :
                        editingRule.effects.get(idx).effectType.getDisplayName();
                return "效果[" + idx + "]: " + effectName;
            }
            default: return "参数";
        }
    }

    private Map<String, String> getCurrentParams() {
        if (selectedSlot == 3) {
            if (!editingRule.effects.isEmpty()) {
                int idx = Math.min(selectedEffectIndex, editingRule.effects.size() - 1);
                return editingRule.effects.get(idx).params;
            }
        }
        return editingRule.params;
    }

    private void syncCurrentParams() {
        syncConditionParams();
        syncEffectDelay();
    }

    private void applyParams(Map<String, String> newParams) {
        if (selectedSlot == 3) {
            if (!editingRule.effects.isEmpty()) {
                int idx = Math.min(selectedEffectIndex, editingRule.effects.size() - 1);
                editingRule.effects.get(idx).params.clear();
                editingRule.effects.get(idx).params.putAll(newParams);
            }
        } else {
            editingRule.params.clear();
            editingRule.params.putAll(newParams);
        }
        syncConditionParams();
        syncEffectDelay();
    }

    private void syncConditionParams() {
        if (editingRule.conditions.isEmpty()) return;
        int ci = Math.min(selectedConditionIndex, editingRule.conditions.size() - 1);
        ShenRule.ShenCondition cond = editingRule.conditions.get(ci);
        if (editingRule.params.containsKey("_cond_operator")) {
            cond.operator = editingRule.params.get("_cond_operator");
        }
        if (editingRule.params.containsKey("_cond_value")) {
            cond.value = editingRule.params.get("_cond_value");
        }
        if (editingRule.params.containsKey("_cond_nbtKey")) {
            cond.params.put("nbtKey", editingRule.params.get("_cond_nbtKey"));
        }
        if (editingRule.params.containsKey("_cond_varKey")) {
            cond.params.put("varKey", editingRule.params.get("_cond_varKey"));
        }
        if (editingRule.params.containsKey("_cond_scope")) {
            cond.params.put("scope", editingRule.params.get("_cond_scope"));
        }
        if (editingRule.params.containsKey("_cond_compareValue")) {
            cond.params.put("compareValue", editingRule.params.get("_cond_compareValue"));
        }
        if (editingRule.params.containsKey("_cond_maxDistance")) {
            cond.params.put("maxDistance", editingRule.params.get("_cond_maxDistance"));
        }
        if (editingRule.params.containsKey("_cond_logic")) {
            editingRule.conditionLogic = "true".equals(editingRule.params.get("_cond_logic"));
        }
        if (editingRule.params.containsKey("_cond_nameKeyword")) {
            cond.params.put("nameKeyword", editingRule.params.get("_cond_nameKeyword"));
        }
        if (editingRule.params.containsKey("_cond_nameMatchMode")) {
            cond.params.put("nameMatchMode", editingRule.params.get("_cond_nameMatchMode"));
        }
        if (editingRule.params.containsKey("_cond_playerName")) {
            String val = editingRule.params.get("_cond_playerName");
            if (val != null && !val.isEmpty()) cond.params.put("playerName", val);
        }
        if (editingRule.params.containsKey("_cond_playerUuid")) {
            String val = editingRule.params.get("_cond_playerUuid");
            if (val != null && !val.isEmpty()) cond.params.put("playerUuid", val);
        }
        if (editingRule.params.containsKey("_cond_playerNameInput")) {
            String val = editingRule.params.get("_cond_playerNameInput");
            if (val != null && !val.isEmpty()) cond.params.put("playerName", val);
        }

        if (editingRule.params.containsKey("_cond_slotIndex")) {
            cond.params.put("slotIndex", editingRule.params.get("_cond_slotIndex"));
        }
        if (editingRule.params.containsKey("_cond_spiritAttr")) {
            cond.params.put("spiritAttr", editingRule.params.get("_cond_spiritAttr"));
        }

        editingRule.params.remove("_cond_operator");
        editingRule.params.remove("_cond_value");
        editingRule.params.remove("_cond_nbtKey");
        editingRule.params.remove("_cond_varKey");
        editingRule.params.remove("_cond_scope");
        editingRule.params.remove("_cond_compareValue");
        editingRule.params.remove("_cond_logic");
        editingRule.params.remove("_cond_nameKeyword");
        editingRule.params.remove("_cond_nameMatchMode");
        editingRule.params.remove("_cond_playerName");
        editingRule.params.remove("_cond_playerUuid");
        editingRule.params.remove("_cond_playerNameInput");
        editingRule.params.remove("_cond_slotIndex");
        editingRule.params.remove("_cond_spiritAttr");
    }

    private void syncEffectDelay() {
        if (editingRule.effects.isEmpty()) return;
        int idx = Math.min(selectedEffectIndex, editingRule.effects.size() - 1);
        ShenRule.ShenEffectEntry entry = editingRule.effects.get(idx);
        if (entry.params.containsKey("_effect_delay")) {
            try {
                entry.delayTicks = (int) Math.round(Double.parseDouble(entry.params.get("_effect_delay")) * 20.0);
            } catch (NumberFormatException ignored) {}
            entry.params.remove("_effect_delay");
        }
        editingRule.effectType = entry.effectType;
    }

    private void loadEffectParams(int index) {
        if (index < 0 || index >= editingRule.effects.size()) return;
        syncEffectDelay();
        selectedEffectIndex = index;
    }

    private void loadConditionParams(int index) {
        if (index < 0 || index >= editingRule.conditions.size()) return;
        syncConditionParams();
        selectedConditionIndex = index;
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
}