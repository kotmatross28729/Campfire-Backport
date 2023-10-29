package connor135246.campfirebackport.common.blocks;

import java.util.List;

import com.google.common.collect.Lists;

import connor135246.campfirebackport.common.compat.CampfireBackportCompat;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

public class CampfireBackportBlocks
{

    public static final Block campfire = createCampfireBlock(true, EnumCampfireType.regIndex, "campfire", 1.0F);
    public static final Block campfire_base = createCampfireBlock(false, EnumCampfireType.regIndex, "campfire_base", 0.0F);
    public static final Block soul_campfire = createCampfireBlock(true, EnumCampfireType.soulIndex, "soul_campfire", 0.67F);
    public static final Block soul_campfire_base = createCampfireBlock(false, EnumCampfireType.soulIndex, "soul_campfire_base", 0.0F);
    public static final Block foxfire_campfire = createCampfireBlock(true, EnumCampfireType.foxfireIndex, "foxfire_campfire", 1.0F);
    public static final Block foxfire_campfire_base = createCampfireBlock(false, EnumCampfireType.foxfireIndex, "foxfire_campfire_base", 0.0F);
    public static final Block shadow_campfire = createCampfireBlock(true, EnumCampfireType.shadowIndex, "shadow_campfire", 0.67F);
    public static final Block shadow_campfire_base = createCampfireBlock(false, EnumCampfireType.shadowIndex, "shadow_campfire_base", 0.0F);

    /** a list containing all the campfires */
    public static final List<Block> LIST_OF_CAMPFIRES = Lists.newArrayList(campfire, soul_campfire, foxfire_campfire, shadow_campfire,
            campfire_base, soul_campfire_base, foxfire_campfire_base, shadow_campfire_base);

    /**
     * registers campfire blocks
     */
    public static void preInit()
    {
        LIST_OF_CAMPFIRES.forEach(cblock -> {
            GameRegistry.registerBlock(cblock, ItemBlockCampfire.class, cblock.getUnlocalizedName().substring(5));
        });
    }

    /**
     * makes the campfire block, or the botania campfire block if botania is installed
     */
    private static Block createCampfireBlock(boolean lit, int typeIndex, String name, float lightLevel)
    {
        Block block = null;

        if (CampfireBackportCompat.isBotaniaLoaded)
        {
            try
            {
                block = (Block) Class.forName(Reference.MOD_PACKAGE + ".common.compat.botania.BlockCampfireBotania").getConstructor(boolean.class, int.class)
                        .newInstance(lit, typeIndex);
            }
            catch (Exception excep)
            {
                CampfireBackportCompat.logError("Botania");
                block = null;
            }
        }

        if (block == null)
            block = new BlockCampfire(lit, typeIndex);

        return block.setLightLevel(lightLevel).setBlockName(name).setBlockTextureName(Reference.MODID + ":" + name);
    }

    //

    /**
     * @param lit
     *            - true if you want a lit campfire, false if you want an unlit campfire
     * @param typeIndex
     *            - campfire type index
     * @return the corresponding campfire block
     */
    public static Block getBlockFromLitAndType(boolean lit, int typeIndex)
    {
        return LIST_OF_CAMPFIRES.get((lit ? 0 : 4) + (EnumCampfireType.isValidIndex(typeIndex) ? typeIndex : 0));
    }

    /**
     * @param lit
     *            - true if you want a lit campfire, false if you want an unlit campfire
     * @param type
     *            - "EnumCampfireType.REG_ONLY" for regular campfire, "EnumCampfireType.SOUL_ONLY" for soul campfire (if neither, defaults to regular)
     * @return the corresponding campfire block
     */
    public static Block getBlockFromLitAndType(boolean lit, EnumCampfireType type)
    {
        return LIST_OF_CAMPFIRES.get((lit ? 0 : 4) + EnumCampfireType.index(type));
    }

    /**
     * @return true if and only if the given block is a lit campfire
     */
    public static boolean isLitCampfire(Block block)
    {
        return block instanceof BlockCampfire ? ((BlockCampfire) block).isLit() : false;
    }

    /**
     * @return true if and only if the given block is an unlit campfire
     */
    public static boolean isUnlitCampfire(Block block)
    {
        return block instanceof BlockCampfire ? !((BlockCampfire) block).isLit() : false;
    }

}
