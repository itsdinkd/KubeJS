package dev.latvian.kubejs.integration.aurora;

import dev.latvian.kubejs.script.ScriptModData;
import dev.latvian.mods.aurora.page.HTTPWebPage;
import dev.latvian.mods.aurora.tag.Tag;
import net.minecraftforge.fml.common.FMLCommonHandler;

/**
 * @author LatvianModder
 */
public class KubeJSClassErrorPage extends HTTPWebPage
{
	private final String className;

	public KubeJSClassErrorPage(String c)
	{
		className = c;
	}

	@Override
	public String getTitle()
	{
		return "KubeJS Documentation";
	}

	@Override
	public String getDescription()
	{
		return className;
	}

	@Override
	public String getIcon()
	{
		return "https://kubejs.latvian.dev/logo_48.png";
	}

	@Override
	public String getStylesheet()
	{
		return "https://kubejs.latvian.dev/style.css";
	}

	@Override
	public void body(Tag body)
	{
		body.img("https://kubejs.latvian.dev/logo_title.png").style("height", "7em");
		body.br();
		body.h1("").a("KubeJS Documentation", "/");

		body.h1("Error!");
		Tag t = body.p();
		t.text("Class ");
		t.span(className, "type");
		t.text(" not found!");

		body.br();
		body.p().paired("i", "Hosted from '" + FMLCommonHandler.instance().getMinecraftServerInstance().getMOTD() + "'");
		body.p().paired("i", "Mod version: " + ScriptModData.getInstance().getModVersion());
		body.p().paired("i", "Mod loader: " + ScriptModData.getInstance().getType());
		body.p().paired("i", "Minecraft version: " + ScriptModData.getInstance().getMcVersion());
		body.p().paired("i").a("Visit kubejs.latvian.dev for more info about the mod", "https://kubejs.latvian.dev");
	}
}