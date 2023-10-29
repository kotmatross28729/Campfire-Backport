package connor135246.campfirebackport.common.recipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import connor135246.campfirebackport.config.CampfireBackportConfig;
import connor135246.campfirebackport.config.ConfigReference;
import connor135246.campfirebackport.util.EnumCampfireType;
import connor135246.campfirebackport.util.StringParsers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

public class CampfireRecipe extends GenericRecipe implements Comparable<CampfireRecipe>
{

    public static final String SIGNAL = "signal", NOTSIGNAL = "notsignal", ANY = "any";

    /** a second output. this one can have a chance associated with it. */
    protected final ItemStack byproduct;
    /** {@link #byproduct} */
    protected final double byproductChance;
    /** the time until the recipe is complete. all inputs must have reached this time. */
    protected final int cookingTime;
    /** 1 if this recipe requires a signal fire, -1 if it requires not a signal fire. 0 means it doesn't matter. */
    protected final byte signalFire;

    /** the master list of recipes! */
    private static List<CampfireRecipe> masterRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the regular campfire. copied from the master list. */
    private static List<CampfireRecipe> regRecipeList = new ArrayList<CampfireRecipe>();
    /** the list of recipes that work in the soul campfire. copied from the master list. */
    private static List<CampfireRecipe> soulRecipeList = new ArrayList<CampfireRecipe>();

    /** the list of recipes created with {@link connor135246.campfirebackport.config.CampfireBackportConfig#autoRecipe Auto Recipe Discovery}. */
    private static List<CampfireRecipe> furnaceRecipeList = new ArrayList<CampfireRecipe>();

    /** the list of recipes created with CraftTweaker. */
    private static List<CampfireRecipe> crafttweakerRecipeList = new ArrayList<CampfireRecipe>();

    /**
     * Converts a string into a CampfireRecipe.
     * 
     * @param segment
     *            - the segments of the recipe in proper format (see config explanation)
     * @param types
     *            - the types of campfire this is being added to
     * @param cookingTime
     *            - the cooking time of the recipe, or null if it should use the default time
     */
    public static CampfireRecipe createCustomRecipe(String[] segment, EnumCampfireType types, @Nullable Integer cookingTime)
    {
        try
        {
            // input

            String[] inputs = segment[0].split("&");

            ArrayList<CustomInput> tempInputsList = new ArrayList<CustomInput>(4);

            inputLoop: for (String input : inputs)
            {
                Object[] anInput = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(input, true);
                int stackSize = (Integer) anInput[1];
                anInput[1] = 1;
                for (int i = 0; i < stackSize; ++i)
                {
                    if (tempInputsList.size() < 4)
                        tempInputsList.add(CustomInput.createFromParsed(anInput, false, -1));
                    else
                        break inputLoop;
                }
            }

            if (tempInputsList.size() == 0)
                throw new Exception();

            // output

            Object[] output = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[1], false);

            ItemStack outputStack = new ItemStack((Item) output[0], MathHelper.clamp_int((Integer) output[1], 1, 64), (Integer) output[2]);

            if (!((NBTTagCompound) output[3]).hasNoTags())
            {
                outputStack.setTagCompound((NBTTagCompound) output[3]);
                outputStack.getTagCompound().removeTag(StringParsers.KEY_GCIDataType);
            }

            // signal fire setting

            byte signalFire = 0;

            if (segment.length > 3 && !segment[3].equals(ANY))
                signalFire = (byte) (segment[3].equals(SIGNAL) ? 1 : -1);

            // byproduct

            ItemStack byproductStack = null;

            if (segment.length > 4)
            {
                Object[] byproduct = StringParsers.parseItemOrOreOrToolOrClassWithNBTOrDataWithSize(segment[4], false);

                byproductStack = new ItemStack((Item) byproduct[0], MathHelper.clamp_int((Integer) byproduct[1], 1, 64), (Integer) byproduct[2]);

                if (!((NBTTagCompound) byproduct[3]).hasNoTags())
                {
                    byproductStack.setTagCompound((NBTTagCompound) byproduct[3]);
                    byproductStack.getTagCompound().removeTag(StringParsers.KEY_GCIDataType);
                }
            }

            double byproductChance = 1.0;

            if (segment.length > 5)
                byproductChance = Double.parseDouble(segment[5]);

            // done!

            return new CampfireRecipe(types, tempInputsList.toArray(new CustomInput[0]), new ItemStack[] { outputStack }, cookingTime, signalFire,
                    byproductStack, byproductChance, 0);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    /**
     * for creating recipes from Auto Recipe Discovery. {@link #sortOrder} is increased. everything else is default.
     */
    public static CampfireRecipe createAutoDiscoveryRecipe(ItemStack input, ItemStack output, EnumCampfireType types)
    {
        try
        {
            return new CampfireRecipe(types, new CustomInput[] { CustomInput.createAuto(ItemStack.copyItemStack(input)) }, new ItemStack[] { output }, null,
                    null, null, null, 50);
        }
        catch (Exception excep)
        {
            return null;
        }
    }

    public CampfireRecipe(EnumCampfireType types, CustomInput[] inputs, ItemStack[] outputs, @Nullable Integer cookingTime, @Nullable Byte signalFire,
            @Nullable ItemStack byproduct, @Nullable Double byproductChance, int sortOrder)
    {
        super(types, inputs, outputs, sortOrder);

        this.cookingTime = cookingTime == null ? (CampfireBackportConfig.defaultCookingTimes[EnumCampfireType.index(types)]) : Math.max(1, cookingTime);
        this.signalFire = (byte) (signalFire == null ? 0 : MathHelper.clamp_int(signalFire, -1, 1));
        this.byproduct = ItemStack.copyItemStack(byproduct);
        this.byproductChance = byproductChance == null ? 1.0 : MathHelper.clamp_double(byproductChance, 0.0, 1.0);
    }

    /**
     * Tries to make a CampfireRecipe based on user input for the given campfire types and add it to the recipe lists.<br>
     * See {@link #createCustomRecipe}.
     * 
     * @param recipe
     *            - the user-input string that represents a recipe (see config explanation)
     * @param types
     *            - the types of campfires this recipe should apply to
     * @return true if the recipe was added successfully, false if it wasn't
     */
    public static boolean addToRecipeLists(String recipe, EnumCampfireType types)
    {
        String[] segment = recipe.split("/");

        Integer cookingTime = null;

        if (segment.length > 2 && !segment[2].equals("*"))
            cookingTime = Integer.valueOf(segment[2]);

        CampfireRecipe crecipe = createCustomRecipe(segment, types, cookingTime);

        boolean added = false;

        for (CampfireRecipe splitCrecipe : splitRecipeIfNecessary(crecipe, cookingTime))
            added = addToRecipeLists(splitCrecipe) || added;

        if (!added)
            ConfigReference.logError("invalid_recipe", recipe);

        return added;
    }

    /**
     * Adds the given CampfireRecipe to the recipe lists, if it's not null.
     * 
     * @return true if the recipe isn't null, false otherwise
     */
    public static boolean addToRecipeLists(CampfireRecipe crecipe)
    {
        if (crecipe != null)
        {
            masterRecipeList.add(crecipe);

            if (crecipe.getTypes().acceptsRegular())
                regRecipeList.add(crecipe);
            if (crecipe.getTypes().acceptsSoul())
                soulRecipeList.add(crecipe);

            return true;
        }
        else
            return false;
    }

    /**
     * Removes the given CampfireRecipe from the recipe lists, if it's not null.
     */
    public static void removeFromRecipeLists(CampfireRecipe crecipe)
    {
        if (crecipe != null)
        {
            masterRecipeList.remove(crecipe);

            if (crecipe.getTypes().acceptsRegular())
                regRecipeList.remove(crecipe);
            if (crecipe.getTypes().acceptsSoul())
                soulRecipeList.remove(crecipe);
        }
    }

    /**
     * Sorts the three main recipe lists.
     */
    public static void sortRecipeLists()
    {
        Collections.sort(masterRecipeList);
        Collections.sort(regRecipeList);
        Collections.sort(soulRecipeList);
    }

    /**
     * Clears the three main recipe lists.
     */
    public static void clearRecipeLists()
    {
        masterRecipeList.clear();
        regRecipeList.clear();
        soulRecipeList.clear();
    }

    public static List<CampfireRecipe> getMasterList()
    {
        return masterRecipeList;
    }

    public static List<CampfireRecipe> getRecipeList(int typeIndex)
    {
        return EnumCampfireType.isSoulLike(typeIndex) ? soulRecipeList : regRecipeList;
    }

    public static List<CampfireRecipe> getFurnaceList()
    {
        return furnaceRecipeList;
    }

    public static List<CampfireRecipe> getCraftTweakerList()
    {
        return crafttweakerRecipeList;
    }

    /**
     * If the given campfire recipe is using the default cooking time, is for both campfires, and the default cooking time for regular recipes is different than the one for soul
     * recipes, it has to be separated into two different recipes. <br>
     * This will do so and return the two new campfire recipes in an array. <br>
     * Otherwise, just returns the given campfire recipe in an array.
     */
    public static CampfireRecipe[] splitRecipeIfNecessary(CampfireRecipe crecipe, @Nullable Integer cookingTime)
    {
        if (crecipe != null && shouldSplitRecipe(crecipe.getTypes(), cookingTime))
        {
            CampfireRecipe crecipeReg = new CampfireRecipe(EnumCampfireType.REG_ONLY, crecipe.getInputs(), crecipe.getOutputs(),
                    CampfireBackportConfig.defaultCookingTimes[0], crecipe.getSignalFire(), crecipe.getByproduct(), crecipe.getByproductChance(),
                    crecipe.getSortOrder());
            CampfireRecipe crecipeSoul = new CampfireRecipe(EnumCampfireType.SOUL_ONLY, crecipe.getInputs(), crecipe.getOutputs(),
                    CampfireBackportConfig.defaultCookingTimes[1], crecipe.getSignalFire(), crecipe.getByproduct(), crecipe.getByproductChance(),
                    crecipe.getSortOrder());

            return new CampfireRecipe[] { crecipeReg, crecipeSoul };
        }
        else
            return new CampfireRecipe[] { crecipe };
    }

    public static boolean shouldSplitRecipe(EnumCampfireType types, @Nullable Integer cookingTime)
    {
        return cookingTime == null && types == EnumCampfireType.BOTH
                && CampfireBackportConfig.defaultCookingTimes[0] != CampfireBackportConfig.defaultCookingTimes[1];
    }

    /**
     * Finds a CampfireRecipe in the given campfire type that applies to the given ItemStack, isn't contained in the given list, and has at most the given number of inputs.
     * 
     * @param stack
     * @param type
     * @param signalFire
     *            - is the campfire calling this a signal fire?
     * @param skips
     *            - the CampfireRecipes to skip over checking
     * @param maxInputs
     *            - the maximum number of inputs the CampfireRecipe should have
     * @return a matching CampfireRecipe, or null if none was found
     */
    public static CampfireRecipe findRecipe(ItemStack stack, int typeIndex, boolean signalFire, List<CampfireRecipe> skips, int maxInputs)
    {
        if (stack != null)
        {
            for (CampfireRecipe crecipe : getRecipeList(typeIndex))
            {
                if (crecipe.getInputs().length <= maxInputs && crecipe.matches(stack, signalFire) && !skips.contains(crecipe))
                    return crecipe;
            }
        }
        return null;
    }

    private static final List<CampfireRecipe> EMPTY_CAMPFIRE_LIST = new ArrayList<CampfireRecipe>(0);

    /**
     * sends to {@link #findRecipe(ItemStack, int, boolean, List, int)} with an empty List<CampfireRecipe> and maxInputs = 4
     */
    public static CampfireRecipe findRecipe(ItemStack stack, int typeIndex, boolean signalFire)
    {
        return findRecipe(stack, typeIndex, signalFire, EMPTY_CAMPFIRE_LIST, 4);
    }

    /**
     * Finds the lowest cooking time of all Campfire Recipes that match the stack and campfire. If none match, returns Integer.MAX_VALUE.
     */
    public static int findLowestCookingTime(ItemStack stack, int typeIndex, boolean signalFire)
    {
        int lowestCookingTime = Integer.MAX_VALUE;
        if (stack != null)
        {
            for (CampfireRecipe crecipe : getRecipeList(typeIndex))
            {
                if (crecipe.getCookingTime() < lowestCookingTime && crecipe.matches(stack, signalFire))
                    lowestCookingTime = crecipe.getCookingTime();
            }
        }
        return lowestCookingTime;
    }

    /**
     * Finds the lowest cooking time of all Campfire Recipes that match the stack and campfire. If none match, returns the default cooking time for the campfire type.
     */
    public static int findLowestCookingTimeOrDefault(ItemStack stack, int typeIndex, boolean signalFire)
    {
        int lowestCookingTime = findLowestCookingTime(stack, typeIndex, signalFire);
        return lowestCookingTime == Integer.MAX_VALUE ? CampfireBackportConfig.defaultCookingTimes[EnumCampfireType.index(typeIndex)] : lowestCookingTime;
    }

    /**
     * Checks if the given ItemStack and signalFire state match this CampfireRecipe.
     */
    public boolean matches(ItemStack stack, boolean signalFire)
    {
        if (stack != null && (doesSignalFireMatter() ? (requiresSignalFire() == signalFire) : true))
        {
            for (CustomInput cinput : getInputs())
            {
                if (cinput.matches(stack))
                    return true;
            }
        }
        return false;
    }

    /**
     * Meant for comparing two CampfireRecipes, the first of which was created from {@link #createAutoDiscoveryRecipe}. The order they're given matters!
     * 
     * @return true if the CampfireRecipes are the same, false if they aren't
     */
    public static boolean doStackRecipesMatch(CampfireRecipe crecipeAuto, CampfireRecipe crecipeCustom)
    {
        if (crecipeCustom.getInputs().length == 1)
        {
            CustomInput cinputCustom = crecipeCustom.getInputs()[0];
            CustomInput cinputAuto = crecipeAuto.getInputs()[0];

            if (cinputAuto instanceof CustomItemStack && cinputCustom instanceof CustomItemStack && !cinputCustom.hasExtraData())
                return ((CustomItemStack) cinputCustom).matchesStack(((CustomItemStack) cinputAuto).getInput());
        }
        return false;
    }

    @Override
    protected ItemStack use(CustomInput cinput, ItemStack stack, EntityPlayer player)
    {
        if (stack != null && stack.stackSize > 0)
            stack.stackSize--;

        return stack;
    }

    // toString
    /**
     * for easy readin
     */
    @Override
    public String toString()
    {
        StringBuilder recipeToString = new StringBuilder(48);

        for (CustomInput cinput : inputs)
            recipeToString.append(cinput.toString() + ", ");

        return recipeToString.substring(0, recipeToString.length() - 2) + " -> " + stackToString(getOutput())
                + (hasByproduct() ? ((getByproductChance() < 100 ? " with a " + getByproductChance() * 100 + "% chance of " : " and ")
                        + stackToString(getByproduct())) : "")
                + ", after " + getCookingTime() + " Ticks on "
                + (getTypes() == EnumCampfireType.BOTH ? "all" : (getTypes().acceptsRegular() ? "Regular" : "Soul")) + " campfires"
                + (doesSignalFireMatter() ? " (" + (requiresSignalFire() ? "must" : "must not") + " be a signal fire)" : "");
    }

    // Getters

    /**
     * CampfireRecipes have only one output, so we have a shortcut for {@link #getOutputs()}.
     */
    public ItemStack getOutput()
    {
        return getOutputs()[0];
    }

    public int getCookingTime()
    {
        return cookingTime;
    }

    public byte getSignalFire()
    {
        return signalFire;
    }

    public boolean doesSignalFireMatter()
    {
        return signalFire != 0;
    }

    public boolean requiresSignalFire()
    {
        return signalFire == 1;
    }

    public boolean hasByproduct()
    {
        return byproduct != null;
    }

    public ItemStack getByproduct()
    {
        return byproduct;
    }

    public double getByproductChance()
    {
        return byproductChance;
    }

    // Sorting
    @Override
    public int compareTo(CampfireRecipe crecipe)
    {
        return super.compareTo(crecipe);
    }

}
