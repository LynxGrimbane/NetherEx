/*
 * NetherEx
 * Copyright (c) 2016-2017 by LogicTechCorp
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nex.world.gen.layer;

import lex.config.IConfig;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.BiomeManager;
import nex.world.biome.NetherExBiomeManager;

import java.util.ArrayList;
import java.util.List;

public class GenLayerNetherBiome extends GenLayerNetherEx
{
    public GenLayerNetherBiome(long seed)
    {
        super(seed);
    }

    @Override
    public int[] getInts(int areaX, int areaZ, int areaWidth, int areaHeight)
    {
        int[] outputs = IntCache.getIntCache(areaWidth * areaHeight);

        for(int z = 0; z < areaHeight; z++)
        {
            for(int x = 0; x < areaWidth; x++)
            {
                initChunkSeed(x + areaX, z + areaZ);
                outputs[x + z * areaWidth] = Biome.getIdForBiome(getRandomBiome());
            }
        }

        return outputs;
    }

    public Biome getRandomBiome()
    {
        List<BiomeManager.BiomeEntry> biomeEntryList = new ArrayList<>();

        for(Biome biome : NetherExBiomeManager.getBiomes())
        {
            IConfig config = NetherExBiomeManager.getBiomeConfig(biome);

            if(config != null)
            {
                biomeEntryList.add(new BiomeManager.BiomeEntry(biome, config.getInt("weight", 10)));
            }
        }

        return WeightedRandom.getRandomItem(biomeEntryList, nextInt(WeightedRandom.getTotalWeight(biomeEntryList))).biome;
    }
}
