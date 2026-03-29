package github.saukiya.sxattribute.data.attribute;

import com.dakuo.novaattribute.core.attribute.AttributeData;

import java.util.List;

/**
 * SX2 兼容 - 属性数据容器
 * 内部维护 double[][] 供 SX2 附属使用，同时可与 NovaAttribute 的 AttributeData 互转
 */
public class SXAttributeData {

    private double combatPower = 0D;
    private double[][] values;

    public SXAttributeData() {
        List<SubAttribute> attrs = SubAttribute.getAttributes();
        values = new double[Math.max(attrs.size(), 1)][];
        for (SubAttribute attr : attrs) {
            if (attr.getPriority() >= 0 && attr.getPriority() < values.length) {
                values[attr.getPriority()] = new double[attr.getLength()];
            }
        }
        // 填充空行
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) values[i] = new double[0];
        }
    }

    public double getCombatPower() { return combatPower; }
    public double[][] getValues() { return values; }

    public double[] getValues(String attributeName) {
        return getValues(SubAttribute.getSubAttribute(attributeName));
    }

    public double[] getValues(SubAttribute attribute) {
        if (attribute != null && attribute.getPriority() >= 0 && attribute.getPriority() < values.length) {
            return values[attribute.getPriority()];
        }
        return new double[12];
    }

    public boolean isValid() {
        for (double[] row : values) {
            for (double v : row) {
                if (v != 0) return true;
            }
        }
        return false;
    }

    public boolean isValid(SubAttribute attribute) {
        double[] vals = getValues(attribute);
        for (double v : vals) {
            if (v != 0) return true;
        }
        return false;
    }

    public SXAttributeData add(SXAttributeData other) {
        if (other != null && other.isValid()) {
            for (int i = 0; i < Math.min(values.length, other.values.length); i++) {
                for (int j = 0; j < Math.min(values[i].length, other.values[i].length); j++) {
                    values[i][j] += other.values[i][j];
                }
            }
        }
        return this;
    }

    public SXAttributeData take(SXAttributeData other) {
        if (other != null && other.isValid()) {
            for (int i = 0; i < Math.min(values.length, other.values.length); i++) {
                for (int j = 0; j < Math.min(values[i].length, other.values[i].length); j++) {
                    values[i][j] -= other.values[i][j];
                }
            }
        }
        return this;
    }

    public double calculationCombatPower() {
        combatPower = 0D;
        for (SubAttribute attr : SubAttribute.getAttributes()) {
            if (attr.getPriority() >= 0 && attr.getPriority() < values.length) {
                combatPower += attr.calculationCombatPower(values[attr.getPriority()]);
            }
        }
        return combatPower;
    }

    public void correct() {
        for (SubAttribute attr : SubAttribute.getAttributes()) {
            if (attr.getPriority() >= 0 && attr.getPriority() < values.length) {
                attr.correct(values[attr.getPriority()]);
            }
        }
    }

    /**
     * 从 NovaAttribute 的 AttributeData 转换
     */
    public static SXAttributeData fromNovaData(AttributeData novaData) {
        SXAttributeData data = new SXAttributeData();
        for (SubAttribute attr : SubAttribute.getAttributes()) {
            if (attr.getPriority() < 0 || attr.getPriority() >= data.values.length) continue;
            java.util.List<Double> novaValues = novaData.get(attr.getName());
            if (novaValues != null) {
                double[] row = data.values[attr.getPriority()];
                for (int i = 0; i < Math.min(row.length, novaValues.size()); i++) {
                    row[i] = novaValues.get(i);
                }
            }
        }
        return data;
    }

    /**
     * 转换为 NovaAttribute 的 AttributeData
     */
    public AttributeData toNovaData() {
        AttributeData novaData = new AttributeData();
        for (SubAttribute attr : SubAttribute.getAttributes()) {
            if (attr.getPriority() < 0 || attr.getPriority() >= values.length) continue;
            double[] row = values[attr.getPriority()];
            boolean hasValue = false;
            for (double v : row) { if (v != 0) { hasValue = true; break; } }
            if (hasValue) {
                double[] copy = new double[row.length];
                System.arraycopy(row, 0, copy, 0, row.length);
                novaData.set(attr.getName(), copy);
            }
        }
        return novaData;
    }
}
