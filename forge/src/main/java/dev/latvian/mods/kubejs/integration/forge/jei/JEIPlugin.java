package dev.latvian.mods.kubejs.integration.forge.jei;

import dev.architectury.platform.Platform;
import dev.latvian.mods.kubejs.BuiltinKubeJSPlugin;
import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.item.ItemStackJS;
import dev.latvian.mods.kubejs.item.ingredient.IngredientJS;
import dev.latvian.mods.kubejs.script.ScriptType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.plugins.jei.info.IngredientInfoRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author LatvianModder
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
	public static final ResourceLocation ID = new ResourceLocation(KubeJS.MOD_ID, "jei");
	public IJeiRuntime runtime;

	private final boolean isREIWithoutWorkaround;

	public JEIPlugin() {
		if (Platform.isModLoaded("roughlyenoughitems") && !Platform.isModLoaded("rei_internals_workaround")) {
			isREIWithoutWorkaround = true;
			KubeJS.LOGGER.warn("""
					----------------------------------------------------------------
					KubeJS has detected that you are using Roughly Enough Items
					without the JEI Internals Workaround add-on. This *will* cause issues with
					certain parts of our JEI integration, like the information recipe category.
									
					As a safety measure, we have disabled all functionality of our JEI plugin.
					To fix this, you can either use the Internals Workaround, or just use
					our REI integration, which now works on Forge as well!
					----------------------------------------------------------------
					""".trim()
			);
		} else {
			isREIWithoutWorkaround = false;
		}
	}

	@Override
	public ResourceLocation getPluginUid() {
		return ID;
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime r) {
		if (isREIWithoutWorkaround) {
			return;
		}

		runtime = r;
		BuiltinKubeJSPlugin.GLOBAL.put("jeiRuntime", runtime);

		new HideJEIEventJS<>(runtime, VanillaTypes.ITEM, object -> IngredientJS.of(object)::testVanilla, stack -> !stack.isEmpty()).post(ScriptType.CLIENT, JEIIntegration.JEI_HIDE_ITEMS);

		new HideJEIEventJS<>(runtime, VanillaTypes.FLUID, object -> {
			var fs = FluidStackJS.of(object);
			return fluidStack -> fluidStack.getFluid().isSame(fs.getFluid()) && Objects.equals(fluidStack.getTag(), fs.getNbt());
		}, stack -> !stack.isEmpty()).post(ScriptType.CLIENT, JEIIntegration.JEI_HIDE_FLUIDS);

		new HideCustomJEIEventJS(runtime).post(ScriptType.CLIENT, JEIIntegration.JEI_HIDE_CUSTOM);

		new RemoveJEICategoriesEvent(runtime).post(ScriptType.CLIENT, JEIIntegration.JEI_REMOVE_CATEGORIES);
		new RemoveJEIRecipesEvent(runtime).post(ScriptType.CLIENT, JEIIntegration.JEI_REMOVE_RECIPES);

		new AddJEIEventJS<>(runtime, VanillaTypes.ITEM, object -> ItemStackJS.of(object).getItemStack(), stack -> !stack.isEmpty()).post(ScriptType.CLIENT, JEIIntegration.JEI_ADD_ITEMS);
		new AddJEIEventJS<>(runtime, VanillaTypes.FLUID, object -> fromArchitectury(FluidStackJS.of(object).getFluidStack()), stack -> !stack.isEmpty()).post(ScriptType.CLIENT, JEIIntegration.JEI_ADD_FLUIDS);
	}

	private FluidStack fromArchitectury(dev.architectury.fluid.FluidStack stack) {
		return new FluidStack(stack.getFluid(), (int) stack.getAmount(), stack.getTag());
	}

	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration) {
		if (isREIWithoutWorkaround) {
			return;
		}
		new JEISubtypesEventJS(registration).post(ScriptType.CLIENT, JEIIntegration.JEI_SUBTYPES);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		if (isREIWithoutWorkaround) {
			return;
		}
		List<IngredientInfoRecipe<?>> list = new ArrayList<>();
		new InformationJEIEventJS(registration.getIngredientManager(), list).post(ScriptType.CLIENT, JEIIntegration.JEI_INFORMATION);
		registration.addRecipes(list, VanillaRecipeCategoryUid.INFORMATION);
	}
}