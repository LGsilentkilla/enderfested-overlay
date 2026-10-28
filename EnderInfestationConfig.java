package com.luigi.enderinfestation;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common configuration for rare, localized instability explosions. */
public final class EnderInfestationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue EXPLOSIONS_ENABLED;
    public static final ModConfigSpec.DoubleValue EXPLOSION_CHANCE;
    public static final ModConfigSpec.DoubleValue EXPLOSION_STRENGTH;
    public static final ModConfigSpec.BooleanValue EXPLOSIONS_BREAK_BLOCKS;

    static {
        BUILDER.push("unstable_infestation");
        EXPLOSIONS_ENABLED = BUILDER
                .comment("Whether Enderfested conversions can trigger rare instability explosions.")
                .define("explosionsEnabled", true);
        EXPLOSION_CHANCE = BUILDER
                .comment("Chance per successful block conversion to trigger an explosion (0.005 = 0.5%).")
                .defineInRange("explosionChance", 0.005D, 0.0D, 1.0D);
        EXPLOSION_STRENGTH = BUILDER
                .comment("Explosion strength. 3.2 is slightly stronger than a standard creeper blast; higher values are more destructive.")
                .defineInRange("explosionStrength", 3.2D, 0.1D, 4.0D);
        EXPLOSIONS_BREAK_BLOCKS = BUILDER
                .comment("Whether instability explosions can damage blocks. Enabled by default for the Unstable Mixture effect; disable to protect builds.")
                .define("explosionsBreakBlocks", true);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
    private EnderInfestationConfig() {}
}
