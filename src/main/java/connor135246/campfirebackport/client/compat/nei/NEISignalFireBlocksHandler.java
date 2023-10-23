package connor135246.campfirebackport.client.compat.nei;

import static connor135246.campfirebackport.client.compat.nei.NEIGenericRecipeHandler.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import connor135246.campfirebackport.common.items.ItemBlockCampfire;
import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.Reference;
import connor135246.campfirebackport.util.SingleBlockAccess;
import connor135246.campfirebackport.util.StringParsers;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.oredict.OreDictionary;

public class NEISignalFireBlocksHandler extends TemplateRecipeHandler
{

    public static final String recipeID = Reference.NEI_SIGNALBLOCKS_ID;

    public static final RecipeTransferRect transfer = new RecipeTransferRect(new Rectangle(62, 31, 40, 32), recipeID);
    public static final Rectangle blockRect = new Rectangle(62, 64, 40, 32);
    public static final Rectangle inputRect = new Rectangle(74, 99, 20, 20);

    public class CachedSignalFireBlocksRecipe extends CachedRecipe
    {
        final long offset = System.currentTimeMillis();

        public Block[] blocks;
        public int[] metas;

        public List<PositionedStack> posStack = new ArrayList<PositionedStack>();

        /** single-line tooltip for ore recipes */
        public String tooltip = "";

        /** hides the inventory slot if the block doesn't have an item form */
        public boolean hideSlot = false;

        /** block currently being rendered */
        public Block blockToRender;
        /** metadata of block currently being rendered */
        public int metaToRender;
        /** true if the block being rendered did, in fact, render */
        public boolean didRender = false;

        /**
         * For a block with any meta.
         */
        public CachedSignalFireBlocksRecipe(Block block)
        {
            List<ItemStack> stacks = new ArrayList<ItemStack>();

            Item item = block.getItem(Minecraft.getMinecraft().theWorld, 0, 0, 0);
            if (item != null)
            {
                block.getSubBlocks(item, CreativeTabs.tabAllSearch, stacks);

                this.blocks = new Block[stacks.size()];
                Arrays.fill(blocks, block);
                this.metas = new int[stacks.size()];
                for (int i = 0; i < stacks.size(); i++)
                {
                    ItemStack stack = stacks.get(i);
                    this.metas[i] = stack.getItem().getMetadata(stack.getItemDamage());
                }

                this.posStack.add(new PositionedStack(stacks, 75, 100, false));
            }
            else
            {
                stacks.add(new ItemStack(Blocks.air));
                this.hideSlot = true;

                this.blocks = new Block[] { block };
                this.metas = new int[] { 0 };
            }

            setBlockToRender(0);
        }

        /**
         * For a block with a non-wildcard meta.
         */
        public CachedSignalFireBlocksRecipe(Block block, int meta)
        {
            this.blocks = new Block[] { block };
            this.metas = new int[] { meta };

            Item item = block.getItem(Minecraft.getMinecraft().theWorld, 0, 0, 0);
            if (item != null)
                this.posStack.add(new PositionedStack(new ItemStack(item, 1, meta), 75, 100, false));
            else
            {
                this.posStack.add(new PositionedStack(new ItemStack(Blocks.air), 75, 100, false));
                this.hideSlot = true;
            }

            setBlockToRender(0);
        }

        /**
         * For oredict entries. stacks must not contain non-ItemBlock items!
         */
        public CachedSignalFireBlocksRecipe(List<ItemStack> stacks, String tooltip)
        {
            List<ItemStack> stacksFinal = new ArrayList<ItemStack>();
            List<Block> blocksList = new ArrayList<Block>();
            List<Integer> metasList = new ArrayList<Integer>();
            for (int i = 0; i < stacks.size(); i++)
            {
                ItemStack stack = stacks.get(i);
                Block block = Block.getBlockFromItem(stack.getItem());
                if (stack.getItemDamage() == OreDictionary.WILDCARD_VALUE)
                {
                    List<ItemStack> metaStacks = new ArrayList<ItemStack>();
                    block.getSubBlocks(stack.getItem(), CreativeTabs.tabAllSearch, metaStacks);
                    for (ItemStack metaStack : metaStacks)
                    {
                        blocksList.add(block);
                        metasList.add(metaStack.getItem().getMetadata(metaStack.getItemDamage()));
                        stacksFinal.add(metaStack);
                    }
                }
                else
                {
                    blocksList.add(block);
                    metasList.add(stack.getItem().getMetadata(stack.getItemDamage()));
                    stacksFinal.add(stack);
                }
            }

            this.blocks = blocksList.toArray(new Block[blocksList.size()]);
            this.metas = new int[metasList.size()];
            for (int i = 0; i < metasList.size(); i++)
                this.metas[i] = metasList.get(i);

            setBlockToRender(0);

            this.posStack.add(new PositionedStack(stacksFinal, 75, 100, false));

            this.tooltip = tooltip;
        }

        @Override
        public PositionedStack getResult()
        {
            // no result
            return null;
        }

        @Override
        public List<PositionedStack> getIngredients()
        {
            return getCycledIngredients(cycleticks / 20, posStack);
        }

        @Override
        public void randomRenderPermutation(PositionedStack stack, long cycle)
        {
            Random rand = new Random(cycle + this.offset);
            int permutation = Math.abs(rand.nextInt());
            stack.setPermutationToRender(permutation % stack.items.length);
            setBlockToRender(permutation);
        }

        public void setBlockToRender(int index)
        {
            if (blocks.length > 0)
                blockToRender = blocks[index % blocks.length];
            else
                blockToRender = Blocks.air;

            if (metas.length > 0)
                metaToRender = Math.abs(metas[index % metas.length]) % 16;
            else
                metaToRender = 0;
        }

        // NEI-GTNH compatibility
        public void jeiStyledRenderPermutation(List<PositionedStack> stacks, long cycle)
        {
            for (PositionedStack stack : stacks)
            {
                stack.setPermutationToRender((int) (cycle % stack.items.length));
                setBlockToRender((int) cycle);
            }
        }

    }

    @Override
    public int recipiesPerPage()
    {
        return 1;
    }

    @Override
    public String getRecipeName()
    {
        return StringParsers.translateNEI("signal_fire_blocks");
    }

    @Override
    public void loadTransferRects()
    {
        transferRects.add(transfer);
    }

    public String getRecipeID()
    {
        return recipeID;
    }

    // NEI-GTNH compatibility
    public String getHandlerId()
    {
        return getRecipeID();
    }

    @Override
    public String getGuiTexture()
    {
        return basicBackground;
    }

    @Override
    public void loadCraftingRecipes(String outputId, Object... results)
    {
        if (outputId.equals(getRecipeID()))
            loadAllRecipes();
        else
            super.loadCraftingRecipes(outputId, results);
    }

    @Override
    public void loadUsageRecipes(String inputId, Object... ingredients)
    {
        if (inputId.equals(getRecipeID()))
            loadAllRecipes();
        else
            super.loadUsageRecipes(inputId, ingredients);
    }

    @Override
    public void loadUsageRecipes(ItemStack ingredient)
    {
        if (ingredient.getItem() instanceof ItemBlockCampfire)
        {
            loadAllRecipes();
            return;
        }

        Block ingredientBlock = Block.getBlockFromItem(ingredient.getItem());
        if (ingredientBlock != Blocks.air)
        {
            if (!CampfireBackportConfig.signalFireBlocks.isEmpty())
            {
                Set<Integer> metas = CampfireBackportConfig.signalFireBlocks.get(ingredientBlock);
                if (metas != null)
                {
                    if (metas.contains(OreDictionary.WILDCARD_VALUE))
                        arecipes.add(new CachedSignalFireBlocksRecipe(ingredientBlock));
                    else if (metas.contains(ingredient.getItemDamage()))
                        arecipes.add(new CachedSignalFireBlocksRecipe(ingredientBlock, ingredient.getItemDamage()));
                }
            }

            if (!CampfireBackportConfig.signalFireOres.isEmpty())
            {
                for (int id : OreDictionary.getOreIDs(ingredient))
                {
                    String ore = OreDictionary.getOreName(id);
                    if (CampfireBackportConfig.signalFireOres.contains(ore))
                        loadValidOreRecipe(ore);
                }
            }
        }
    }

    public void loadAllRecipes()
    {
        for (Entry<Block, Set<Integer>> blockEntry : CampfireBackportConfig.signalFireBlocks.entrySet())
        {
            if (blockEntry.getValue().contains(OreDictionary.WILDCARD_VALUE))
                arecipes.add(new CachedSignalFireBlocksRecipe(blockEntry.getKey()));
            else
            {
                for (Integer meta : blockEntry.getValue())
                {
                    if (meta != null)
                        arecipes.add(new CachedSignalFireBlocksRecipe(blockEntry.getKey(), meta));
                }
            }
        }

        for (String ore : CampfireBackportConfig.signalFireOres)
            loadValidOreRecipe(ore);
    }

    /**
     * Adds an oredict recipe to the list of recipes, but first makes sure it's only sending over ItemBlocks.
     */
    public void loadValidOreRecipe(String ore)
    {
        List<ItemStack> stacks = new ArrayList<ItemStack>(OreDictionary.getOres(ore, false));
        ListIterator<ItemStack> iterator = stacks.listIterator();
        while (iterator.hasNext())
        {
            ItemStack stack = iterator.next();
            if (Block.getBlockFromItem(stack.getItem()) == Blocks.air)
                iterator.remove();
        }

        if (!stacks.isEmpty())
            arecipes.add(new CachedSignalFireBlocksRecipe(stacks, EnumChatFormatting.GOLD + StringParsers.translateNEI("ore_input", ore)));
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> tooltip, int recipe)
    {
        CachedSignalFireBlocksRecipe cachedSblock = (CachedSignalFireBlocksRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedSblock != null)
        {
            Point relMouse = getRelMouse(gui, recipe);

            boolean onBlock = blockRect.contains(relMouse);

            if (onBlock)
            {
                tooltip.add(EnumChatFormatting.GOLD + "<" + GameRegistry.findUniqueIdentifierFor(cachedSblock.blockToRender)
                        + (cachedSblock.metaToRender != 0 ? ":" + cachedSblock.metaToRender : "") + ">");
            }

            if (onBlock || (!tooltip.isEmpty() && inputRect.contains(relMouse)))
            {
                String tip = cachedSblock.tooltip;
                if (!tip.isEmpty())
                {
                    tooltip.add("");
                    tooltip.add(tip);
                }
            }

            if (onBlock && !cachedSblock.didRender)
            {
                tooltip.add("");
                tooltip.add(EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.ITALIC + StringParsers.translateNEI("no_block_render"));
            }
        }

        return tooltip;
    }

    @Override
    public void drawBackground(int recipe)
    {
        GL11.glPushMatrix();
        GL11.glColor4f(1, 1, 1, 1);

        CachedSignalFireBlocksRecipe cachedSblock = (CachedSignalFireBlocksRecipe) this.arecipes.get(recipe % arecipes.size());

        if (cachedSblock != null)
        {
            if (!cachedSblock.hideSlot)
                drawSlot(cachedSblock.posStack.get(0).relx, cachedSblock.posStack.get(0).rely);

            GL11.glTranslatef(0, 0, 300);

            String desc1 = StringParsers.translateNEI("signal_fire_desc1");
            fonty().drawString(desc1, 82 - fonty().getStringWidth(desc1) / 2, 5, 0x777777);
            String desc2 = StringParsers.translateNEI("signal_fire_desc2");
            fonty().drawString(desc2, 82 - fonty().getStringWidth(desc2) / 2, 7 + fonty().FONT_HEIGHT, 0x777777);

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(103.4F, 80, 10);
            GL11.glRotatef(-30, 1, 0, 0);
            GL11.glRotatef(180, 0, 1, 0);
            GL11.glRotatef(45, 0, 1, 0);
            GL11.glScalef(30, -30, 30);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            RenderBlocks renderer = getRenderBlocks();
            Tessellator tess = Tessellator.instance;

            IBlockAccess access = renderer.blockAccess;
            int meta = cachedSblock.metaToRender;
            if (cachedSblock.blockToRender == Blocks.dispenser || cachedSblock.blockToRender == Blocks.dropper || cachedSblock.blockToRender == Blocks.furnace
                    || cachedSblock.blockToRender == Blocks.lit_furnace)
            {
                meta = 2;
            }
            renderer.blockAccess = SingleBlockAccess.getSingleBlockAccess(cachedSblock.blockToRender, meta);

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            boolean blend = GL11.glGetBoolean(GL11.GL_BLEND);
            if (!blend)
                GL11.glEnable(GL11.GL_BLEND);
            boolean depth = GL11.glGetBoolean(GL11.GL_DEPTH_TEST);
            if (!depth)
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            tess.startDrawingQuads();
            try
            {
                cachedSblock.didRender = renderer.renderBlockByRenderType(cachedSblock.blockToRender, 0, 0, 0);
            }
            catch (Exception excep)
            {
                ;
            }
            if (!cachedSblock.didRender)
            {
                renderer.blockAccess = SingleBlockAccess.getSingleBlockAccess(Blocks.stone);
                renderer.setOverrideBlockTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("missingno"));
                renderer.renderBlockByRenderType(Blocks.stone, 0, 0, 0);
                renderer.clearOverrideBlockTexture();
            }
            tess.draw();
            if (!blend)
                GL11.glDisable(GL11.GL_BLEND);
            if (!depth)
                GL11.glDisable(GL11.GL_DEPTH_TEST);

            renderer.blockAccess = access;

            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glColor4f(1, 1, 1, 1);

            GL11.glTranslatef(61, 54, 100);

            GuiDraw.changeTexture(TextureMap.locationBlocksTexture);

            renderCampfire(EnumCampfireType.BOTH, true, 2);
        }

        GL11.glPopMatrix();
    }

}
