package dev.vegui.hfa.metadata;

public record HFAPluginMetadata(
        String pluginId,
        boolean clientRequired,
        HFAClientMode effectiveClientMode
) {
    public static HFAPluginMetadata serverOnly(String pluginId) {
        return new HFAPluginMetadata(pluginId, false, HFAClientMode.SERVER_ONLY);
    }

    public static HFAPluginMetadata required(String pluginId, boolean clientRequired) {
        return new HFAPluginMetadata(pluginId, clientRequired, HFAClientMode.REQUIRED);
    }
}
