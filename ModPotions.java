package com.luigi.enderinfestation;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(Registries.POTION, EnderInfestation.MOD_ID);

    public static final DeferredHolder<Potion, Potion> ENDER_INFESTATION =
            POTIONS.register("ender_infestation",
                    () -> new Potion(new MobEffectInstance(
                            ModEffects.ENDER_INFESTATION,
                            3600,
                            0
                    )));

    private ModPotions() {
    }
}
