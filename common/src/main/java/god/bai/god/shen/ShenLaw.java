package god.bai.god.shen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class ShenLaw {
    public String name = "";
    public String description = "";
    public List<ShenRule> rules = new ArrayList<>();
    public boolean enabled = true;

    public ShenLaw() {}

    public ShenLaw(String name) {
        this.name = name;
    }

    public void addRule(ShenRule rule) {
        rules.add(rule);
    }

    public void removeRule(int index) {
        if (index >= 0 && index < rules.size()) {
            rules.remove(index);
        }
    }

    public boolean contains(ShenRule rule) {
        return rules.contains(rule);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", name);
        nbt.putString("description", description);
        nbt.putBoolean("enabled", enabled);

        ListTag ruleList = new ListTag();
        for (ShenRule rule : rules) {
            ruleList.add(rule.toNbt());
        }
        nbt.put("rules", ruleList);

        return nbt;
    }

    public static ShenLaw fromNbt(CompoundTag nbt) {
        ShenLaw law = new ShenLaw();
        law.name = nbt.getString("name");
        law.description = nbt.getString("description");
        law.enabled = nbt.getBoolean("enabled");

        ListTag ruleList = nbt.getList("rules", Tag.TAG_COMPOUND);
        for (int i = 0; i < ruleList.size(); i++) {
            law.rules.add(ShenRule.fromNbt(ruleList.getCompound(i)));
        }

        return law;
    }
}
