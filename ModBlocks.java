package com.luigi.enderinfestation;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Enderfested variants of terrain and vegetation corrupted by the lingering cloud. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, EnderInfestation.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, EnderInfestation.MOD_ID);

    public static final DeferredHolder<Block, Block> ENDERFESTED_STONE = cube_block("enderfested_stone", Blocks.STONE);
    public static final DeferredHolder<Block, Block> ENDERFESTED_COBBLESTONE = cube_block("enderfested_cobblestone", Blocks.COBBLESTONE);
    public static final DeferredHolder<Block, Block> ENDERFESTED_DEEPSLATE = cube_block("enderfested_deepslate", Blocks.DEEPSLATE);
    public static final DeferredHolder<Block, Block> ENDERFESTED_COBBLED_DEEPSLATE = cube_block("enderfested_cobbled_deepslate", Blocks.COBBLED_DEEPSLATE);
    public static final DeferredHolder<Block, Block> ENDERFESTED_DIRT = cube_block("enderfested_dirt", Blocks.DIRT);
    public static final DeferredHolder<Block, Block> ENDERFESTED_COARSE_DIRT = cube_block("enderfested_coarse_dirt", Blocks.COARSE_DIRT);
    public static final DeferredHolder<Block, Block> ENDERFESTED_GRAVEL = cube_block("enderfested_gravel", Blocks.GRAVEL);
    public static final DeferredHolder<Block, Block> ENDERFESTED_SAND = cube_block("enderfested_sand", Blocks.SAND);
    public static final DeferredHolder<Block, Block> ENDERFESTED_SANDSTONE = cube_block("enderfested_sandstone", Blocks.SANDSTONE);
    public static final DeferredHolder<Block, Block> ENDERFESTED_RED_SANDSTONE = cube_block("enderfested_red_sandstone", Blocks.RED_SANDSTONE);
    public static final DeferredHolder<Block, Block> ENDERFESTED_OAK_LOG = log_block("enderfested_oak_log", Blocks.OAK_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_OAK_LEAVES = leaves_block("enderfested_oak_leaves", Blocks.OAK_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_SPRUCE_LOG = log_block("enderfested_spruce_log", Blocks.SPRUCE_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_SPRUCE_LEAVES = leaves_block("enderfested_spruce_leaves", Blocks.SPRUCE_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_BIRCH_LOG = log_block("enderfested_birch_log", Blocks.BIRCH_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_BIRCH_LEAVES = leaves_block("enderfested_birch_leaves", Blocks.BIRCH_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_JUNGLE_LOG = log_block("enderfested_jungle_log", Blocks.JUNGLE_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_JUNGLE_LEAVES = leaves_block("enderfested_jungle_leaves", Blocks.JUNGLE_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_ACACIA_LOG = log_block("enderfested_acacia_log", Blocks.ACACIA_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_ACACIA_LEAVES = leaves_block("enderfested_acacia_leaves", Blocks.ACACIA_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_DARK_OAK_LOG = log_block("enderfested_dark_oak_log", Blocks.DARK_OAK_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_DARK_OAK_LEAVES = leaves_block("enderfested_dark_oak_leaves", Blocks.DARK_OAK_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_MANGROVE_LOG = log_block("enderfested_mangrove_log", Blocks.MANGROVE_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_MANGROVE_LEAVES = leaves_block("enderfested_mangrove_leaves", Blocks.MANGROVE_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_CHERRY_LOG = log_block("enderfested_cherry_log", Blocks.CHERRY_LOG);
    public static final DeferredHolder<Block, Block> ENDERFESTED_CHERRY_LEAVES = leaves_block("enderfested_cherry_leaves", Blocks.CHERRY_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_AZALEA_LEAVES = leaves_block("enderfested_azalea_leaves", Blocks.AZALEA_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_FLOWERING_AZALEA_LEAVES = leaves_block("enderfested_flowering_azalea_leaves", Blocks.FLOWERING_AZALEA_LEAVES);
    public static final DeferredHolder<Block, Block> ENDERFESTED_CRIMSON_STEM = log_block("enderfested_crimson_stem", Blocks.CRIMSON_STEM);
    public static final DeferredHolder<Block, Block> ENDERFESTED_WARPED_STEM = log_block("enderfested_warped_stem", Blocks.WARPED_STEM);

    private static DeferredHolder<Block, Block> cube_block(String name, Block original) {
        DeferredHolder<Block, Block> holder = BLOCKS.register(name,
                () -> new Block(BlockBehaviour.Properties.ofFullCopy(original)));
        ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    private static DeferredHolder<Block, Block> log_block(String name, Block original) {
        DeferredHolder<Block, Block> holder = BLOCKS.register(name,
                () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(original)));
        ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    private static DeferredHolder<Block, Block> leaves_block(String name, Block original) {
        DeferredHolder<Block, Block> holder = BLOCKS.register(name,
                () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(original)));
        ITEMS.register(name, () -> new BlockItem(holder.get(), new Item.Properties()));
        return holder;
    }

    private ModBlocks() {}
}
