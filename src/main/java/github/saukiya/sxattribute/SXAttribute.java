package github.saukiya.sxattribute;

import github.saukiya.sxattribute.api.SXAPI;
import github.saukiya.sxattribute.data.attribute.SXAttributeManager;

import java.text.DecimalFormat;

/**
 * SX2 兼容 - 主类静态入口
 * SX2 附属通过 SXAttribute.getApi() 和 SXAttribute.getInst() 访问
 */
public class SXAttribute {

    private static final SXAPI api = new SXAPI();
    private static final SXAttributeManager attributeManager = new SXAttributeManager();
    private static final DecimalFormat df = new DecimalFormat("#.##");

    public static SXAPI getApi() {
        return api;
    }

    public static SXAttribute getInst() {
        return null; // SX2 附属很少直接用 getInst()，需要时返回 null 安全处理
    }

    public static SXAttributeManager getAttributeManager() {
        return attributeManager;
    }

    public static DecimalFormat getDf() {
        return df;
    }
}
