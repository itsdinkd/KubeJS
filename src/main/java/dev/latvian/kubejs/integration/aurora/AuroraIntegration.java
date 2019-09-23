package dev.latvian.kubejs.integration.aurora;

import dev.latvian.kubejs.documentation.Documentation;
import dev.latvian.mods.aurora.AuroraHomePageEvent;
import dev.latvian.mods.aurora.AuroraPageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * @author LatvianModder
 */
public class AuroraIntegration
{
	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(AuroraIntegration.class);
	}

	@SubscribeEvent
	public static void onAuroraHomePageEvent(AuroraHomePageEvent event)
	{
		event.add("KubeJS Documentation", "kubejs");
	}

	@SubscribeEvent
	public static void onAuroraEvent(AuroraPageEvent event)
	{
		if (event.getUri().startsWith("kubejs"))
		{
			String s = event.getUri().substring(6);

			if (s.isEmpty())
			{
				event.setPage(new KubeJSHomePage(Documentation.get()));
			}
			else
			{
				try
				{
					Class c = Class.forName(s.substring(1));
					event.setPage(new KubeJSClassPage(Documentation.get(), c));
				}
				catch (Exception ex)
				{
					event.setPage(new KubeJSClassErrorPage(s.substring(1)));
				}
			}

			event.setCanceled(true);
		}
	}
}