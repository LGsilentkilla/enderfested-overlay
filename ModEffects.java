package com.luigi.enderinfestation;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EnderInfestation.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> ENDER_INFESTATION =
            EFFECTS.register("ender_infestation",
                    () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9B45FF) {});

    private ModEffects() {}
}
