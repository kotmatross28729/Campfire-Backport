package connor135246.campfirebackport.common.compat.handlers;

import connor135246.campfirebackport.common.blocks.BlockCampfire;
import connor135246.campfirebackport.common.tileentity.TileEntityCampfire;
import connor135246.campfirebackport.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.wiki.IWikiProvider;

public class BotaniaHandler
{
/**
    public static void load()
    {
        BotaniaAPI.registerModWiki(Reference.MODID, new CampfireWikiProvider());
    }

    public static class CampfireWikiProvider implements IWikiProvider
    {

        @Override
        public String getBlockName(World world, MovingObjectPosition mop)
        {
            int x = mop.blockX;
            int y = mop.blockY;
            int z = mop.blockZ;

            Block block = world.getBlock(x, y, z);
            if (block instanceof BlockCampfire)
            {
                TileEntity tile = world.getTileEntity(x, y, z);
                if (tile instanceof TileEntityCampfire)
                {
                    TileEntityCampfire ctile = (TileEntityCampfire) tile;
                    if (ctile.hasCustomInventoryName())
                        return "\"" + ctile.getInventoryName() + "\"";
                }

                ItemStack stack = new ItemStack(((BlockCampfire) block).getCampfireBlockItem());
                if (stack != null && stack.getItem() != null)
                    return stack.getDisplayName();
            }

            return null;
        }

        @Override
        public String getWikiName(World world, MovingObjectPosition mop)
        {
            return Reference.NAME + " Wiki";
        }

        @Override
        public String getWikiURL(World world, MovingObjectPosition mop)
        {
            return Reference.WIKI;
        }

    }
*/
}
