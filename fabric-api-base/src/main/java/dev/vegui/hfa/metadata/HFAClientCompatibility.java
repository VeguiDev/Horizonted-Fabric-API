package dev.vegui.hfa.metadata;

import java.util.List;
import java.util.Objects;

public final class HFAClientCompatibility {
    private static volatile HFAClientMode effectiveClientMode = HFAClientMode.SERVER_ONLY;
    private static volatile List<String> reasons = List.of();

    private HFAClientCompatibility() {
    }

    public static HFAClientMode effectiveClientMode() {
        return effectiveClientMode;
    }

    public static boolean requiresClient() {
        return effectiveClientMode == HFAClientMode.REQUIRED;
    }

    public static List<String> reasons() {
        return reasons;
    }

    public static void setEffectiveClientMode(HFAClientMode mode, List<String> reasons) {
        HFAClientCompatibility.effectiveClientMode = Objects.requireNonNull(mode, "mode");
        HFAClientCompatibility.reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
    }
}
