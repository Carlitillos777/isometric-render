package com.isometricrender;

import com.isometricrender.event.ClientEventHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = IsometricRenderMod.MOD_ID, name = "Isometric Render",
     version = "1.0.0", clientSideOnly = true)
public class IsometricRenderMod {

    public static final String MOD_ID = "isometricrender";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
    }
}
