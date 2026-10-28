
package com.luigi.enderinfestation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(EnderInfestation.MOD_ID)
public final class EnderInfestation {
    public static final String MOD_ID = "ender_infestation";

    public EnderInfestation(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(
                ModConfig.Type.COMMON,
                EnderInfestationConfig.SPEC
        );

        ModBlocks.BLOCKS.register(modBus);
        ModBlocks.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModPotions.POTIONS.register(modBus);

        modBus.addListener(this::addCreativeContents);
    }

    private static ItemStack createPotionStack(Item item) {
        ItemStack stack = new ItemStack(item);

        stack.set(
                DataComponents.POTION_CONTENTS,
                new PotionContents(ModPotions.ENDER_INFESTATION)
        );

        return stack;
    }

    private void addCreativeContents(
            BuildCreativeModeTabContentsEvent event
    ) {
        // Add Enderfested blocks to the Building Blocks tab.
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) {
            ModBlocks.ITEMS.getEntries().forEach(
                    holder -> event.accept(holder.get())
            );
        }

        // Add Ender Infestation potion variants to Ingredients.
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(createPotionStack(Items.POTION));
            event.accept(createPotionStack(Items.SPLASH_POTION));
            event.accept(createPotionStack(Items.LINGERING_POTION));
        }
    }
}
