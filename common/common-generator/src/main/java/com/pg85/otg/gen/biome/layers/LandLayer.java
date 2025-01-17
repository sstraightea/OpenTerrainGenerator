package com.pg85.otg.gen.biome.layers;

import static com.pg85.otg.gen.biome.layers.BiomeLayers.LAND_BIT;

import com.pg85.otg.gen.biome.layers.type.ParentedLayer;
import com.pg85.otg.gen.biome.layers.util.LayerSampleContext;
import com.pg85.otg.interfaces.ILayerSampler;

/**
 * Sets land based on the provided rarity.
 */
class LandLayer implements ParentedLayer
{
	private final int rarity;
	private final boolean forceLandAtSpawn;
	private final boolean spawnLand;

	LandLayer(int landRarity, boolean forceLandAtSpawn)
	{
		// Scale rarity from the world config
		this.rarity = 101 - landRarity;
		this.forceLandAtSpawn = forceLandAtSpawn;
		this.spawnLand = landRarity != 0;
	}

	@Override
	public int sample(LayerSampleContext<?> context, ILayerSampler parent, int x, int z)
	{
		int sample = parent.sample(x, z);

		// Set land based on the rarity
		if (context.nextInt(this.rarity) == 0 && spawnLand)
		{
			return sample | LAND_BIT;
		} else {
			// If we're on the center sample return land to try and make sure that the player doesn't spawn in the ocean.
			if (x == 0 && z == 0 && forceLandAtSpawn)
			{
				return sample | LAND_BIT;
			}
		}

		return sample;
	}
}
