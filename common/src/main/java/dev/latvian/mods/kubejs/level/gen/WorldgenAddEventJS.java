package dev.latvian.mods.kubejs.level.gen;

import com.google.common.collect.Iterables;
import com.mojang.serialization.JsonOps;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.latvian.mods.kubejs.KubeJSRegistries;
import dev.latvian.mods.kubejs.event.StartupEventJS;
import dev.latvian.mods.kubejs.level.gen.filter.biome.BiomeFilter;
import dev.latvian.mods.kubejs.level.gen.properties.AddLakeProperties;
import dev.latvian.mods.kubejs.level.gen.properties.AddOreProperties;
import dev.latvian.mods.kubejs.level.gen.properties.AddSpawnProperties;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static dev.latvian.mods.kubejs.util.JsonUtilsJS.getJsonHashBytes;

/**
 * FIXME: Move to {@link dev.architectury.registry.level.biome.BiomeModifications} once it's ready.
 *
 * @author LatvianModder
 */
public class WorldgenAddEventJS extends StartupEventJS {

	private static final Pattern SPAWN_PATTERN = Pattern.compile("(\\w+:\\w+)\\*\\((\\d+)-(\\d+)\\):(\\d+)");

	private static MessageDigest messageDigest;

	private void addFeature(ResourceLocation id, BiomeFilter filter, GenerationStep.Decoration decoration,
							ConfiguredFeature<?, ?> feature, List<PlacementModifier> modifiers) {
		var placedFeature = feature.placed(modifiers);

		if (id == null) {
			id = new ResourceLocation("kubejs:features/" + getUniqueId(placedFeature));
		}

		// TODO: we should probably not be registering to BuiltinRegistries directly,
		//  but rather to the synced registry directly by using the RegistryAccess.
		BuiltinRegistries.register(BuiltinRegistries.CONFIGURED_FEATURE, id, feature);
		BuiltinRegistries.register(BuiltinRegistries.PLACED_FEATURE, id, placedFeature);

		BiomeModifications.postProcessProperties(filter, (ctx, properties) -> {
			properties.getGenerationProperties().addFeature(decoration, placedFeature);
		});
	}

	private void addEntitySpawn(BiomeFilter filter, MobCategory category, MobSpawnSettings.SpawnerData spawnerData) {
		BiomeModifications.postProcessProperties(filter, (ctx, properties) -> {
			properties.getSpawnProperties().addSpawn(category, spawnerData);
		});
	}

	public void addOre(Consumer<AddOreProperties> p) {
		var properties = new AddOreProperties();
		p.accept(properties);

		if (properties.targets.isEmpty()) {
			return;
		}

		ConfiguredFeature<OreConfiguration, ?> oreFeature =
				// TODO: should we just turn noSurface into a float like vanilla has?
				Feature.ORE.configured(new OreConfiguration(properties.targets, properties.size, properties.noSurface ? 1.0F : 0.0F));

		var modifiers = new ArrayList<PlacementModifier>();

		if (properties.count.getMaxValue() > 1) {
			modifiers.add(CountPlacement.of(properties.count));
		}

		if (properties.chance > 0) {
			modifiers.add(RarityFilter.onAverageOnceEvery(properties.chance));
		}

		if (properties.squared) {
			modifiers.add(InSquarePlacement.spread());
		}

		modifiers.add(properties.height);

		addFeature(properties.id, properties.biomes, properties.worldgenLayer, oreFeature, modifiers);
	}

	public static String getUniqueId(PlacedFeature feature) {
		if (messageDigest == null) {
			try {
				messageDigest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException nsae) {
				throw new InternalError("MD5 not supported", nsae);
			}
		}

		var json = PlacedFeature.DIRECT_CODEC.encodeStart(JsonOps.COMPRESSED, feature)
				.getOrThrow(false, str -> {
					throw new RuntimeException("Could not encode feature to JSON: " + str);
				});

		if (messageDigest == null) {
			return new BigInteger(Hex.encodeHexString(getJsonHashBytes(json)), 16).toString(36);
		} else {
			messageDigest.reset();
			return new BigInteger(Hex.encodeHexString(messageDigest.digest(getJsonHashBytes(json))), 16).toString(36);
		}
	}

	public void addSpawn(Consumer<AddSpawnProperties> p) {
		var properties = new AddSpawnProperties();
		p.accept(properties);

		if (properties._entity == null || properties._category == null) {
			return;
		}

		addEntitySpawn(properties.biomes, properties._category, new MobSpawnSettings.SpawnerData(properties._entity, properties.weight, properties.minCount, properties.maxCount));
	}

	public void addSpawn(BiomeFilter filter, MobCategory category, String spawn) {
		var matcher = SPAWN_PATTERN.matcher(spawn);

		if (matcher.matches()) {
			try {
				var entity = Objects.requireNonNull(KubeJSRegistries.entityTypes().get(new ResourceLocation(matcher.group(1))));
				var weight = Integer.parseInt(matcher.group(4));
				var min = Integer.parseInt(matcher.group(2));
				var max = Integer.parseInt(matcher.group(3));
				addEntitySpawn(filter, category, new MobSpawnSettings.SpawnerData(entity, weight, min, max));
			} catch (Exception ex) {
				ConsoleJS.STARTUP.info("Failed to add spawn: " + ex);
			}
		} else {
			ConsoleJS.STARTUP.info("Invalid spawn syntax! Must be mod:entity_type*(minCount-maxCount):weight");
		}

		//minecraft:ghast*(4-4):50
	}

	public void addSpawn(MobCategory category, String spawn) {
		addSpawn(BiomeFilter.ALWAYS_TRUE, category, spawn);
	}

	public void addLake(Consumer<AddLakeProperties> p) {
		var properties = new AddLakeProperties();
		p.accept(properties);

		var fluid = Iterables.getFirst(properties.fluid.getBlockStates(), Blocks.AIR.defaultBlockState());
		if (fluid == null || fluid.isAir()) {
			return;
		}

		var barrier = Iterables.getFirst(properties.barrier.getBlockStates(), Blocks.AIR.defaultBlockState());
		if (barrier == null || barrier.isAir()) {
			return;
		}

		addFeature(properties.id, properties.biomes, properties.worldgenLayer,
				Feature.LAKE.configured(new LakeFeature.Configuration(BlockStateProvider.simple(fluid), BlockStateProvider.simple(barrier))),
				properties.chance > 0 ? Collections.singletonList(RarityFilter.onAverageOnceEvery(properties.chance)) : Collections.emptyList());
	}

	/*
	public void addLake(Consumer<AddLakeProperties> p) {
		AddLakeProperties properties = new AddLakeProperties();
		p.accept(properties);

		if (properties._block == Blocks.AIR.defaultBlockState()) {
			return;
		}

		if (!verifyBiomes(properties.biomes)) {
			return;
		}

		addFeature(properties._worldgenLayer, Feature.LAKE.configured(new BlockStateConfiguration(properties._block)).decorated((FeatureDecorator.WATER_LAKE).configured(new ChanceDecoratorConfiguration(properties.chance))));
	}
	*/
}