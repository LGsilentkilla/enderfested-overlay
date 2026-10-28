package com.luigi.enderinfestation;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

@EventBusSubscriber(modid = EnderInfestation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class BrewingEvents {

    private BrewingEvents() {
    }

    @SubscribeEvent
    public static void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        // Brew a Potion of Ender Infestation by adding Chorus Fruit to a Potion of Infestation.
        // NeoForge's addMix also supports the corresponding splash and lingering potion variants.
        event.getBuilder().addMix(
                Potions.INFESTED,
                Items.CHORUS_FRUIT,
                ModPotions.ENDER_INFESTATION
        );
    }
}
