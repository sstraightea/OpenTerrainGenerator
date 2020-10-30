package com.pg85.otg.generator.biome.layers.type;

import com.pg85.otg.generator.biome.layers.util.LayerFactory;
import com.pg85.otg.generator.biome.layers.util.LayerRandomnessSource;
import com.pg85.otg.generator.biome.layers.util.LayerSampleContext;
import com.pg85.otg.generator.biome.layers.util.LayerSampler;

/**
 * The type for layers that create a new layer generation stack.
 */
public interface InitLayer
{
   default <R extends LayerSampler> LayerFactory<R> create(LayerSampleContext<R> context)
   {
      return () -> {
         return context.createSampler((x, z) -> {
            context.initSeed((long)x, (long)z);
            return this.sample(context, x, z);
         });
      };
   }

   int sample(LayerSampleContext<?> context, int x, int y);
}
