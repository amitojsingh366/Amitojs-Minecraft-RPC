package net.amitoj.minecraftRPC;


import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import club.minnced.discord.rpc.*;


import java.util.Timer;
import java.util.TimerTask;

import static net.minecraft.util.Hand.MAIN_HAND;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("amrpc")
public class MinecraftRPCMod {
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    DiscordRPC lib = DiscordRPC.INSTANCE;
    String applicationId = "765845744027435019";
    String steamId = "";
    DiscordEventHandlers handlers = new DiscordEventHandlers();
    Long start_time = System.currentTimeMillis() / 1000;
    Minecraft mc = Minecraft.getInstance();

    Integer times = 0;
    Timer t = new Timer();

    //GameRegistry gr = new GameRegistry();
    public MinecraftRPCMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRenderGui);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loggedInEvent);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::loggedInEvent);
        MinecraftForge.EVENT_BUS.addListener(this::loggedOutEvent);


        handlers.ready = (user) -> System.out.println("Ready!");
        lib.Discord_Initialize(applicationId, handlers, true, steamId);

        basicPresence();

        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                lib.Discord_RunCallbacks();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
            }
        }, "RPC-Callback-Handler").start();

    }


    public void loggedOutEvent(ClientPlayerNetworkEvent.LoggedOutEvent e) {

        t.cancel();
        t = new Timer();
        times = 0;

        basicPresence();
    }


    public void loggedInEvent(ClientPlayerNetworkEvent.LoggedInEvent e) {
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                updatePresence();

            }
        }, 1000, 5000);

    }


    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().gameSettings);

    }


    private void basicPresence() {

        DiscordRichPresence presence = new DiscordRichPresence();
        presence.startTimestamp = start_time; // epoch second
        presence.details = "In The Main Menu";
        presence.largeImageKey = "icon_720";
        presence.largeImageText = "Amitoj's Minecraft RPC";
        presence.instance = 1;
        lib.Discord_UpdatePresence(presence);

    }

    private void updatePresence() {
        times++;
        Boolean issinglePlayer = mc.isSingleplayer();
        RegistryKey<World> dimKey = mc.player.world.getDimensionKey();
        DiscordRichPresence presence = new DiscordRichPresence();
        ItemStack held_item = mc.player.getHeldItem(MAIN_HAND);
        String item_name = held_item.getDisplayName().getString();
        if(!item_name.equals("Air")){
            presence.details = "Holding " + item_name;
        }
        presence.startTimestamp = start_time; // epoch second
        presence.largeImageKey = "icon_720";
        presence.largeImageText = "Amitoj's Minecraft RPC";
        presence.instance = 1;
        if (!issinglePlayer) {
            String serverip = mc.getCurrentServerData().serverIP.toUpperCase();
            presence.state = "Multiplayer - " + serverip;
            presence.partyId = mc.getCurrentServerData().serverIP;
            presence.matchSecret = mc.getCurrentServerData().serverIP;
            presence.joinSecret = mc.getCurrentServerData().serverIP;
            presence.spectateSecret = mc.getCurrentServerData().serverIP;
        } else {
            presence.state = "Singleplayer";
            presence.partySize = 1;
            presence.partyMax = 1;
            presence.partyId =  mc.world.getWorldInfo().toString();
            presence.matchSecret = mc.world.getWorldInfo().toString();
            presence.joinSecret = mc.world.getWorldInfo().toString();
            presence.spectateSecret = mc.world.getWorldInfo().toString();
        }
        if (dimKey == World.OVERWORLD) {
            presence.smallImageKey = "zombie_face";
            presence.smallImageText = "In The Overworld";
        } else if (dimKey == World.THE_NETHER) {
            presence.smallImageKey = "ghast_face";
            presence.smallImageText = "In The Nether";
        } else if (dimKey == World.THE_END) {
            presence.smallImageKey = "enderman_face";
            presence.smallImageText = "In The End";
        }
        lib.Discord_UpdatePresence(presence);

    }


}
