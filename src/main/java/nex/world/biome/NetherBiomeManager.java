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

package nex.world.biome;

import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.biome.Biome;
import nex.NetherEx;
import nex.init.NetherExRegistries;
import nex.util.FileUtil;
import nex.world.gen.GenerationStage;
import nex.world.gen.feature.BiomeFeature;
import nex.world.gen.layer.GenLayerNetherEx;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class NetherBiomeManager
{
    private static final Logger LOGGER = LogManager.getLogger("NetherEx|NetherBiomeManager");

    public static void postInit(File directory)
    {
        copyBiomeConfigsToConfigDirectory(directory);
        parseBiomeConfigs(directory);
    }

    private static void copyBiomeConfigsToConfigDirectory(File directory)
    {
        try
        {
            if(!directory.exists())
            {
                directory.mkdir();
            }

            LOGGER.info("Copying the Biome Config Directory to the config folder.");

            if(NetherEx.IS_DEV_ENV)
            {
                FileUtils.copyDirectory(new File(NetherEx.class.getResource("/assets/nex/biome_configs").getFile()), directory);
            }
            else
            {
                FileUtil.extractFromJar("/assets/nex/biome_configs", directory.getPath());
            }
        }
        catch(IOException e)
        {
            LOGGER.fatal("The attempt to copy the Biome Config Directory to the config folder was unsuccessful.");
            LOGGER.fatal(e);
        }
    }

    private static void parseBiomeConfigs(File directory)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Path directoryPath = directory.toPath();

        try
        {
            Iterator<Path> pathIter = Files.walk(directoryPath).iterator();

            while(pathIter.hasNext())
            {
                Path configPath = pathIter.next();

                if(FilenameUtils.getExtension(configPath.toString()).equals("json"))
                {
                    BufferedReader reader = Files.newBufferedReader(configPath);
                    parseBiomeConfig(JsonUtils.fromJson(gson, reader, JsonObject.class));
                    IOUtils.closeQuietly(reader);
                }
                else if(!configPath.toFile().isDirectory())
                {
                    LOGGER.warn("Skipping file located at, " + configPath.toString() + ", as it is not a json file.");
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void parseBiomeConfig(JsonObject config)
    {
        NetherBiome netherBiome = deserializeBiome(config);
        NetherBiomeClimate netherBiomeClimate = NetherBiomeClimate.getFromString(JsonUtils.getString(config, "climate"));

        if(netherBiome != null)
        {
            netherBiomeClimate.addBiome(netherBiome);
            LOGGER.info("Added the " + netherBiome.getBiome().getRegistryName().getResourcePath() + " biome, from " + netherBiome.getBiome().getRegistryName().getResourceDomain() + ", to the Nether.");

            JsonArray biomeFeatureConfigs = JsonUtils.getJsonArray(config, "biomeFeatures", new JsonArray());

            if(biomeFeatureConfigs.size() > 0)
            {
                for(JsonElement biomeFeatureConfig : biomeFeatureConfigs)
                {
                    BiomeFeature biomeFeature = deserializeFeature(biomeFeatureConfig.getAsJsonObject());
                    GenerationStage generationStage = GenerationStage.getFromString(JsonUtils.getString(biomeFeatureConfig.getAsJsonObject(), "stage"));

                    if(biomeFeature != null)
                    {
                        generationStage.addBiomeFeature(netherBiome.getBiome(), biomeFeature);
                    }
                }
            }
        }
    }

    private static NetherBiome deserializeBiome(JsonObject config)
    {
        NetherBiome netherBiome = NetherExRegistries.getNetherBiome(new ResourceLocation(JsonUtils.getString(config, "biomeType", "")));

        if(netherBiome != null)
        {
            return netherBiome.deserialize(config);
        }

        return null;
    }

    private static BiomeFeature deserializeFeature(JsonObject config)
    {
        BiomeFeature feature = NetherExRegistries.getBiomeFeature(new ResourceLocation(JsonUtils.getString(config, "featureType", "")));

        if(feature != null)
        {
            return feature.deserialize(config);
        }

        return null;
    }

    public static Biome getRandomBiome(GenLayerNetherEx layer)
    {
        List<NetherBiomeEntry> biomeEntryList = Lists.newArrayList();

        for(NetherBiomeClimate biomeClimate : NetherBiomeClimate.values())
        {
            biomeEntryList.addAll(biomeClimate.getBiomeEntries());
        }

        return WeightedRandom.getRandomItem(biomeEntryList, layer.nextInt(WeightedRandom.getTotalWeight(biomeEntryList))).getBiome();
    }

    public static List<BiomeFeature> getBiomeFeatures(Biome biome, GenerationStage generationStage)
    {
        Map<Biome, List<BiomeFeature>> biomeFeatureMap = generationStage.getBiomeFeatureMap();

        if(biomeFeatureMap.containsKey(biome))
        {
            return biomeFeatureMap.get(biome);
        }

        return Lists.newArrayList();
    }

    public static IBlockState getBiomeFloorTopBlock(Biome biome)
    {
        Map<Biome, NetherBiomeEntry> biomeEntryMap = NetherBiomeClimate.getFromBiome(biome).getBiomeEntryMap();

        if(biomeEntryMap.containsKey(biome))
        {
            return biomeEntryMap.get(biome).getFloorTopBlock();
        }

        return biome.topBlock;
    }

    public static IBlockState getBiomeFloorFillerBlock(Biome biome)
    {
        Map<Biome, NetherBiomeEntry> biomeEntryMap = NetherBiomeClimate.getFromBiome(biome).getBiomeEntryMap();

        if(biomeEntryMap.containsKey(biome))
        {
            return biomeEntryMap.get(biome).getFloorFillerBlock();
        }

        return biome.fillerBlock;
    }

    public static IBlockState getBiomeWallBlock(Biome biome)
    {
        Map<Biome, NetherBiomeEntry> biomeEntryMap = NetherBiomeClimate.getFromBiome(biome).getBiomeEntryMap();

        if(biomeEntryMap.containsKey(biome))
        {
            return biomeEntryMap.get(biome).getWallBlock();
        }

        return biome.fillerBlock;
    }

    public static IBlockState getBiomeRoofBottomBlock(Biome biome)
    {
        Map<Biome, NetherBiomeEntry> biomeEntryMap = NetherBiomeClimate.getFromBiome(biome).getBiomeEntryMap();

        if(biomeEntryMap.containsKey(biome))
        {
            return biomeEntryMap.get(biome).getRoofBottomBlock();
        }

        return biome.fillerBlock;
    }

    public static IBlockState getBiomeRoofFillerBlock(Biome biome)
    {
        Map<Biome, NetherBiomeEntry> biomeEntryMap = NetherBiomeClimate.getFromBiome(biome).getBiomeEntryMap();

        if(biomeEntryMap.containsKey(biome))
        {
            return biomeEntryMap.get(biome).getRoofFillerBlock();
        }

        return biome.fillerBlock;
    }

    public static IBlockState getBiomeOceanBlock(Biome biome)
    {
        Map<Biome, NetherBiomeEntry> biomeEntryMap = NetherBiomeClimate.getFromBiome(biome).getBiomeEntryMap();

        if(biomeEntryMap.containsKey(biome))
        {
            return biomeEntryMap.get(biome).getOceanBlock();
        }

        return Blocks.LAVA.getDefaultState();
    }
}
