package com.graftlink.api.registry;

import minicraft.item.Recipe;
import java.util.ArrayList;
import java.util.List;

public class RecipeRegistry {
    private static final List<Recipe> WORKBENCH_RECIPES = new ArrayList<>();
    private static final List<Recipe> INVENTORY_RECIPES = new ArrayList<>();

    public static void registerWorkbenchRecipe(Recipe recipe) {
        WORKBENCH_RECIPES.add(recipe);
        System.out.println("[GraftLink API] Registered Workbench Recipe.");
    }

    public static void registerInventoryRecipe(Recipe recipe) {
        INVENTORY_RECIPES.add(recipe);
        System.out.println("[GraftLink API] Registered Inventory Crafting Recipe.");
    }

    public static List<Recipe> getWorkbenchRecipes() {
        return WORKBENCH_RECIPES;
    }

    public static List<Recipe> getInventoryRecipes() {
        return INVENTORY_RECIPES;
    }
}