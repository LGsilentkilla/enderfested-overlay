
package com.luigi.enderinfestation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.tags.DamageTypeTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = EnderInfestation.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class InfestationEvents {
    private static final float PURPUR_CHANCE = 0.10F;
    private static final float CHORUS_START_CHANCE = 0.04F;
    private static final int PLAYER_SPREAD_INTERVAL_TICKS = 60;
    private static final float PLAYER_SPREAD_CHANCE = 0.35F;
    private static final float PLAYER_POISON_INSTEAD_CHANCE = 0.25F;
    private static final int PLAYER_POISON_DURATION_TICKS = 600;

    private static final Map<UUID, Integer> NEXT_PLAYER_SPREAD_TICK =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final Set<UUID> PLAYER_OUTCOME_ROLLED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final List<PendingInstabilityBlast> PENDING_BLASTS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private record PendingInstabilityBlast(
            ServerLevel level, double x, double y, double z, long expiresAt) {}

    private static final Map<UUID, CloudSpreadState> CLOUD_SPREAD_STATES =
            new HashMap<>();

    private static final class CloudSpreadState {
        private final ServerLevel level;
        private final Set<BlockPos> frontier = new LinkedHashSet<>();
        private final Set<BlockPos> knownInfected = new HashSet<>();
        private long lastSeenGameTime;

        private CloudSpreadState(ServerLevel level, long gameTime) {
            this.level = level;
            this.lastSeenGameTime = gameTime;
        }

        private void remember(BlockPos pos) {
            BlockPos immutable = pos.immutable();
            if (knownInfected.add(immutable)) {
                frontier.add(immutable);
            }
        }
    }

    private InfestationEvents() {}

    @SubscribeEvent
    public static void onLivingDamaged(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (!entity.hasEffect(ModEffects.ENDER_INFESTATION)) return;

        if (entity instanceof Player player && player.isAlive()
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && survivedInstabilityBlast(player, level)) {
            awardUnstableMixture(player);
        }

        if (level.random.nextFloat() >= 0.10F) return;

        Endermite mite = EntityType.ENDERMITE.create(level);
        if (mite == null) return;

        mite.moveTo(
                entity.getX() + (level.random.nextDouble() - 0.5D) * 0.8D,
                entity.getY() + 0.1D,
                entity.getZ() + (level.random.nextDouble() - 0.5D) * 0.8D,
                level.random.nextFloat() * 360.0F,
                0.0F);

        level.addFreshEntity(mite);
        level.sendParticles(
                ParticleTypes.PORTAL,
                entity.getX(),
                entity.getY() + entity.getBbHeight() * 0.5D,
                entity.getZ(),
                6, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!player.hasEffect(ModEffects.ENDER_INFESTATION)) return;

        BlockPos pos = event.getPos();
        Endermite mite = EntityType.ENDERMITE.create(level);

        if (mite != null) {
            mite.moveTo(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.15D,
                    pos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F);
            level.addFreshEntity(mite);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().level() instanceof ServerLevel tickingLevel) {
            long now = tickingLevel.getGameTime();
            CLOUD_SPREAD_STATES.entrySet().removeIf(entry ->
                    entry.getValue().level == tickingLevel
                            && entry.getValue().lastSeenGameTime < now - 2L);
        }

        if (event.getEntity() instanceof Player player) {
            handlePlayerInfestation(player);
            return;
        }

        if (!(event.getEntity() instanceof AreaEffectCloud cloud)) return;
        if (!(cloud.level() instanceof ServerLevel level)) return;

        if (!(cloud.getParticle() instanceof ColorParticleOption color)) return;
        if (color.getType() != ParticleTypes.ENTITY_EFFECT) return;

        if (Math.abs(color.getRed() - 155.0F / 255.0F) > 0.01F
                || Math.abs(color.getGreen() - 69.0F / 255.0F) > 0.01F
                || Math.abs(color.getBlue() - 1.0f) > 0.01F) {
            return;
        }

        if (cloud.isRemoved()) {
            CLOUD_SPREAD_STATES.remove(cloud.getUUID());
            return;
        }

        long gameTime = level.getGameTime();
        CloudSpreadState spread = CLOUD_SPREAD_STATES.computeIfAbsent(
                cloud.getUUID(),
                id -> new CloudSpreadState(level, gameTime));
        spread.lastSeenGameTime = gameTime;

        if (cloud.tickCount % 10 == 0) {
            spawnAmbientParticles(
                    level,
                    cloud.blockPosition(),
                    Math.max(2, (int) Math.ceil(cloud.getRadius())));
        }

        double radius = Math.max(0.0D, cloud.getRadius());
        if (radius <= 0.0D) return;

        trySpawnChorusPlant(level, cloud, radius);

        BlockPos center = cloud.blockPosition();
        int range = (int) Math.ceil(radius);
        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    double dx = pos.getX() + 0.5D - cloud.getX();
                    double dz = pos.getZ() + 0.5D - cloud.getZ();

                    if (dx * dx + dz * dz > radius * radius) continue;

                    BlockState state = level.getBlockState(pos);
                    if (isInfected(state)) {
                        spread.remember(pos);
                    } else if (isCorruptible(state)) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }

        // Aggressive spreading: inspect multiple infected blocks every tick.
        final int maxFrontierChecksPerTick = 24;
        final int maxConversionsPerTick = 3;
        int convertedThisTick = 0;
        int checkedThisTick = 0;

        Set<BlockPos> uniqueCandidates = new LinkedHashSet<>(candidates);

        while (!spread.frontier.isEmpty() && checkedThisTick < maxFrontierChecksPerTick) {

            BlockPos source = spread.frontier.iterator().next();
            spread.frontier.remove(source);
            checkedThisTick++;

            if (!isInfected(level.getBlockState(source))) continue;

            boolean hasEligibleNeighbor = false;

            for (net.minecraft.core.Direction direction
                    : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = source.relative(direction);
                BlockState neighborState = level.getBlockState(neighbor);

                if (isInfected(neighborState)) {
                    spread.remember(neighbor);
                } else if (isCorruptible(neighborState)) {
                    hasEligibleNeighbor = true;
                    uniqueCandidates.add(neighbor.immutable());
                }
            }

            if (hasEligibleNeighbor) {
                spread.frontier.add(source);
            }
        }

        while (convertedThisTick < maxConversionsPerTick
                && !uniqueCandidates.isEmpty()) {

            BlockPos chosen = uniqueCandidates.iterator().next();
            uniqueCandidates.remove(chosen);

            if (!isCorruptible(level.getBlockState(chosen))) continue;

            convertBlock(level, chosen);

            if (isInfected(level.getBlockState(chosen))) {
                spread.remember(chosen);
                convertedThisTick++;
            }
        }
    }

    private static void handlePlayerInfestation(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        UUID playerId = player.getUUID();

        if (!player.hasEffect(ModEffects.ENDER_INFESTATION)) {
            NEXT_PLAYER_SPREAD_TICK.remove(playerId);
            PLAYER_OUTCOME_ROLLED.remove(playerId);
            return;
        }

        if (PLAYER_OUTCOME_ROLLED.add(playerId)
                && level.random.nextFloat() < PLAYER_POISON_INSTEAD_CHANCE) {
            player.removeEffect(ModEffects.ENDER_INFESTATION);
            player.addEffect(new MobEffectInstance(
                    MobEffects.POISON, PLAYER_POISON_DURATION_TICKS, 0));

            NEXT_PLAYER_SPREAD_TICK.remove(playerId);
            PLAYER_OUTCOME_ROLLED.remove(playerId);

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    8, 0.35D, 0.4D, 0.35D, 0.01D);
            return;
        }

        int nextTick = NEXT_PLAYER_SPREAD_TICK.getOrDefault(
                playerId, player.tickCount);

        if (player.tickCount < nextTick) return;

        NEXT_PLAYER_SPREAD_TICK.put(
                playerId, player.tickCount + PLAYER_SPREAD_INTERVAL_TICKS);

        if (level.random.nextFloat() >= PLAYER_SPREAD_CHANCE) return;

        BlockPos center = player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (pos.distSqr(center) > 3.0D) continue;

                    if (isCorruptible(level.getBlockState(pos))) {
                        candidates.add(pos.immutable());
                    }
                }
            }
        }

        if (!candidates.isEmpty()) {
            BlockPos chosen = candidates.get(
                    level.random.nextInt(candidates.size()));
            convertBlock(level, chosen);
        }
    }

    private static void trySpawnChorusPlant(
            ServerLevel level, AreaEffectCloud cloud, double radius) {
        if (level.random.nextFloat() >= CHORUS_START_CHANCE) return;

        BlockPos center = cloud.blockPosition();
        int range = (int) Math.ceil(radius);
        List<BlockPos> validBases = new ArrayList<>();

        for (int x = -range; x <= range; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos base = center.offset(x, y, z);

                    double dx = base.getX() + 0.5D - cloud.getX();
                    double dz = base.getZ() + 0.5D - cloud.getZ();

                    if (dx * dx + dz * dz > radius * radius) continue;
                    if (!level.getBlockState(base).is(Blocks.END_STONE)) continue;

                    BlockPos plantPos = base.above();
                    BlockPos flowerPos = plantPos.above();

                    if (level.getBlockState(plantPos).isAir()
                            && level.getBlockState(flowerPos).isAir()) {
                        validBases.add(base.immutable());
                    }
                }
            }
        }

        if (validBases.isEmpty()) return;

        BlockPos base = validBases.get(level.random.nextInt(validBases.size()));
        BlockPos plantPos = base.above();
        BlockPos flowerPos = plantPos.above();

        level.setBlock(plantPos, Blocks.CHORUS_PLANT.defaultBlockState(), 3);
        level.setBlock(flowerPos, Blocks.CHORUS_FLOWER.defaultBlockState(), 3);

        level.sendParticles(
                ParticleTypes.PORTAL,
                plantPos.getX() + 0.5D,
                plantPos.getY() + 0.8D,
                plantPos.getZ() + 0.5D,
                8, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    private static void convertBlock(ServerLevel level, BlockPos pos) {
        BlockState original = level.getBlockState(pos);

        if (original.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.END_STONE.defaultBlockState(), 3);
            spawnConversionParticles(level, pos);
            maybeTriggerInstabilityExplosion(level, pos);
            return;
        }

        if (level.random.nextFloat() < PURPUR_CHANCE) {
            level.setBlock(pos, Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
            spawnConversionParticles(level, pos);
            maybeTriggerInstabilityExplosion(level, pos);
            return;
        }

        BlockState replacement = null;

        if (original.is(Blocks.STONE)) replacement = ModBlocks.ENDERFESTED_STONE.get().defaultBlockState();
        if (original.is(Blocks.COBBLESTONE)) replacement = ModBlocks.ENDERFESTED_COBBLESTONE.get().defaultBlockState();
        if (original.is(Blocks.DEEPSLATE)) replacement = ModBlocks.ENDERFESTED_DEEPSLATE.get().defaultBlockState();
        if (original.is(Blocks.COBBLED_DEEPSLATE)) replacement = ModBlocks.ENDERFESTED_COBBLED_DEEPSLATE.get().defaultBlockState();
        if (original.is(Blocks.DIRT)) replacement = ModBlocks.ENDERFESTED_DIRT.get().defaultBlockState();
        if (original.is(Blocks.COARSE_DIRT)) replacement = ModBlocks.ENDERFESTED_COARSE_DIRT.get().defaultBlockState();
        if (original.is(Blocks.GRAVEL)) replacement = ModBlocks.ENDERFESTED_GRAVEL.get().defaultBlockState();
        if (original.is(Blocks.SAND)) replacement = ModBlocks.ENDERFESTED_SAND.get().defaultBlockState();
        if (original.is(Blocks.SANDSTONE)) replacement = ModBlocks.ENDERFESTED_SANDSTONE.get().defaultBlockState();
        if (original.is(Blocks.RED_SANDSTONE)) replacement = ModBlocks.ENDERFESTED_RED_SANDSTONE.get().defaultBlockState();
        if (original.is(Blocks.OAK_LOG)) replacement = ModBlocks.ENDERFESTED_OAK_LOG.get().defaultBlockState();
        if (original.is(Blocks.OAK_LEAVES)) replacement = ModBlocks.ENDERFESTED_OAK_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.SPRUCE_LOG)) replacement = ModBlocks.ENDERFESTED_SPRUCE_LOG.get().defaultBlockState();
        if (original.is(Blocks.SPRUCE_LEAVES)) replacement = ModBlocks.ENDERFESTED_SPRUCE_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.BIRCH_LOG)) replacement = ModBlocks.ENDERFESTED_BIRCH_LOG.get().defaultBlockState();
        if (original.is(Blocks.BIRCH_LEAVES)) replacement = ModBlocks.ENDERFESTED_BIRCH_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.JUNGLE_LOG)) replacement = ModBlocks.ENDERFESTED_JUNGLE_LOG.get().defaultBlockState();
        if (original.is(Blocks.JUNGLE_LEAVES)) replacement = ModBlocks.ENDERFESTED_JUNGLE_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.ACACIA_LOG)) replacement = ModBlocks.ENDERFESTED_ACACIA_LOG.get().defaultBlockState();
        if (original.is(Blocks.ACACIA_LEAVES)) replacement = ModBlocks.ENDERFESTED_ACACIA_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.DARK_OAK_LOG)) replacement = ModBlocks.ENDERFESTED_DARK_OAK_LOG.get().defaultBlockState();
        if (original.is(Blocks.DARK_OAK_LEAVES)) replacement = ModBlocks.ENDERFESTED_DARK_OAK_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.MANGROVE_LOG)) replacement = ModBlocks.ENDERFESTED_MANGROVE_LOG.get().defaultBlockState();
        if (original.is(Blocks.MANGROVE_LEAVES)) replacement = ModBlocks.ENDERFESTED_MANGROVE_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.CHERRY_LOG)) replacement = ModBlocks.ENDERFESTED_CHERRY_LOG.get().defaultBlockState();
        if (original.is(Blocks.CHERRY_LEAVES)) replacement = ModBlocks.ENDERFESTED_CHERRY_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.AZALEA_LEAVES)) replacement = ModBlocks.ENDERFESTED_AZALEA_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.FLOWERING_AZALEA_LEAVES)) replacement = ModBlocks.ENDERFESTED_FLOWERING_AZALEA_LEAVES.get().defaultBlockState();
        if (original.is(Blocks.CRIMSON_STEM)) replacement = ModBlocks.ENDERFESTED_CRIMSON_STEM.get().defaultBlockState();
        if (original.is(Blocks.WARPED_STEM)) replacement = ModBlocks.ENDERFESTED_WARPED_STEM.get().defaultBlockState();

        if (replacement == null) return;

        if (original.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)
                && replacement.hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS)) {
            replacement = replacement.setValue(
                    net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                    original.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS));
        }

        if (original.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)
                && replacement.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)) {
            replacement = replacement.setValue(
                    net.minecraft.world.level.block.LeavesBlock.DISTANCE,
                    original.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE));
        }

        if (original.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)
                && replacement.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)) {
            replacement = replacement.setValue(
                    net.minecraft.world.level.block.LeavesBlock.PERSISTENT,
                    original.getValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT));
        }

        level.setBlock(pos, replacement, 3);
        spawnConversionParticles(level, pos);
        maybeTriggerInstabilityExplosion(level, pos);
    }

    private static void maybeTriggerInstabilityExplosion(
            ServerLevel level, BlockPos convertedPos) {
        if (!EnderInfestationConfig.EXPLOSIONS_ENABLED.get()) return;
        if (level.random.nextDouble() >= EnderInfestationConfig.EXPLOSION_CHANCE.get()) return;

        double x = convertedPos.getX() + 0.5D
                + (level.random.nextDouble() - 0.5D) * 1.5D;
        double y = convertedPos.getY() + 0.5D
                + level.random.nextDouble() * 0.75D;
        double z = convertedPos.getZ() + 0.5D
                + (level.random.nextDouble() - 0.5D) * 1.5D;

        float strength = EnderInfestationConfig.EXPLOSION_STRENGTH.get().floatValue();

        net.minecraft.world.level.Level.ExplosionInteraction interaction =
                EnderInfestationConfig.EXPLOSIONS_BREAK_BLOCKS.get()
                        ? net.minecraft.world.level.Level.ExplosionInteraction.BLOCK
                        : net.minecraft.world.level.Level.ExplosionInteraction.NONE;

        level.sendParticles(
                ParticleTypes.PORTAL, x, y, z,
                18, 0.45D, 0.35D, 0.45D, 0.03D);

        PENDING_BLASTS.add(new PendingInstabilityBlast(
                level, x, y, z, level.getGameTime() + 2L));

        level.explode(null, x, y, z, strength, false, interaction);

        PENDING_BLASTS.removeIf(blast ->
                blast.level() == level && blast.expiresAt() < level.getGameTime());
    }

    private static boolean survivedInstabilityBlast(
            Player player, ServerLevel level) {
        double maxDistanceSqr = 16.0D;
        long now = level.getGameTime();

        for (PendingInstabilityBlast blast : PENDING_BLASTS) {
            if (blast.expiresAt() < now) {
                PENDING_BLASTS.remove(blast);
                continue;
            }

            if (blast.level() != level) continue;

            double dx = player.getX() - blast.x();
            double dy = player.getY() + player.getBbHeight() * 0.5D - blast.y();
            double dz = player.getZ() - blast.z();

            if (dx * dx + dy * dy + dz * dz <= maxDistanceSqr) {
                PENDING_BLASTS.remove(blast);
                return player.isAlive() && player.getHealth() > 0.0F;
            }
        }

        return false;
    }

    private static void awardUnstableMixture(Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                EnderInfestation.MOD_ID, "unstable_mixture");

        AdvancementHolder advancement = serverPlayer.server.getAdvancements().get(id);

        if (advancement != null) {
            serverPlayer.getAdvancements().award(
                    advancement, "survive_instability_explosion");
        }
    }

    private static void spawnConversionParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(
                ParticleTypes.PORTAL,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                8, 0.28D, 0.28D, 0.28D, 0.015D);
    }

    private static void spawnAmbientParticles(
            ServerLevel level, BlockPos center, int range) {
        for (int attempt = 0; attempt < 4; attempt++) {
            BlockPos candidate = center.offset(
                    level.random.nextInt(range * 2 + 1) - range,
                    level.random.nextInt(5) - 2,
                    level.random.nextInt(range * 2 + 1) - range);

            if (!isInfected(level.getBlockState(candidate))) continue;
            if (level.random.nextFloat() > 0.35F) continue;

            level.sendParticles(
                    ParticleTypes.PORTAL,
                    candidate.getX() + 0.5D,
                    candidate.getY() + 0.7D,
                    candidate.getZ() + 0.5D,
                    2, 0.18D, 0.22D, 0.18D, 0.004D);
        }
    }

    private static boolean isInfected(BlockState state) {
        return state.is(ModBlocks.ENDERFESTED_STONE.get())
                || state.is(ModBlocks.ENDERFESTED_COBBLESTONE.get())
                || state.is(ModBlocks.ENDERFESTED_DEEPSLATE.get())
                || state.is(ModBlocks.ENDERFESTED_COBBLED_DEEPSLATE.get())
                || state.is(ModBlocks.ENDERFESTED_DIRT.get())
                || state.is(ModBlocks.ENDERFESTED_COARSE_DIRT.get())
                || state.is(ModBlocks.ENDERFESTED_GRAVEL.get())
                || state.is(ModBlocks.ENDERFESTED_SAND.get())
                || state.is(ModBlocks.ENDERFESTED_SANDSTONE.get())
                || state.is(ModBlocks.ENDERFESTED_RED_SANDSTONE.get())
                || state.is(ModBlocks.ENDERFESTED_OAK_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_OAK_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_SPRUCE_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_SPRUCE_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_BIRCH_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_BIRCH_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_JUNGLE_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_JUNGLE_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_ACACIA_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_ACACIA_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_DARK_OAK_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_DARK_OAK_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_MANGROVE_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_MANGROVE_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_CHERRY_LOG.get())
                || state.is(ModBlocks.ENDERFESTED_CHERRY_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_AZALEA_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_FLOWERING_AZALEA_LEAVES.get())
                || state.is(ModBlocks.ENDERFESTED_CRIMSON_STEM.get())
                || state.is(ModBlocks.ENDERFESTED_WARPED_STEM.get());
    }

    private static boolean isCorruptible(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.COBBLED_DEEPSLATE)
                || state.is(Blocks.DIRT)
                || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.SAND)
                || state.is(Blocks.SANDSTONE)
                || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.OAK_LOG)
                || state.is(Blocks.OAK_LEAVES)
                || state.is(Blocks.SPRUCE_LOG)
                || state.is(Blocks.SPRUCE_LEAVES)
                || state.is(Blocks.BIRCH_LOG)
                || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.JUNGLE_LOG)
                || state.is(Blocks.JUNGLE_LEAVES)
                || state.is(Blocks.ACACIA_LOG)
                || state.is(Blocks.ACACIA_LEAVES)
                || state.is(Blocks.DARK_OAK_LOG)
                || state.is(Blocks.DARK_OAK_LEAVES)
                || state.is(Blocks.MANGROVE_LOG)
                || state.is(Blocks.MANGROVE_LEAVES)
                || state.is(Blocks.CHERRY_LOG)
                || state.is(Blocks.CHERRY_LEAVES)
                || state.is(Blocks.AZALEA_LEAVES)
                || state.is(Blocks.FLOWERING_AZALEA_LEAVES)
                || state.is(Blocks.CRIMSON_STEM)
                || state.is(Blocks.WARPED_STEM);
    }
}
