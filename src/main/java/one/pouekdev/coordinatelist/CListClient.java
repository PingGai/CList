package one.pouekdev.coordinatelist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class CListClient implements ClientModInitializer {
    public static CListVariables variables = new CListVariables();
    KeyBinding open_waypoints_keybind;
    KeyBinding add_a_waypoint;
    KeyBinding show_hud;
    //public float distanceTo(int index, MinecraftClient client) {
    //    float f = (float)(client.getInstance().player.getX() - CListClient.getX(index));
    //    float g = (float)(client.getInstance().player.getY() - CListClient.getY(index));
    //    float h = (float)(client.getInstance().player.getZ() - CListClient.getZ(index));
    //    return MathHelper.sqrt(f * f + g * g + h * h);
    //}
    @Override
    public void onInitializeClient() {
        open_waypoints_keybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open waypoints menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "CList Keybinds"
        ));
        add_a_waypoint = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Add a waypoint in current position",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "CList Keybinds"
        ));
        show_hud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Show waypoints",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "CList Keybinds"
        ));
        WorldRenderEvents.AFTER_ENTITIES.register((ctx) -> {
            if (!variables.waypoints.isEmpty()) {
                for(int i = 0; i < variables.waypoints.size(); i++){
                    ParticleEffect particle = ParticleTypes.FLAME;
                    ParticleManager particleManager = MinecraftClient.getInstance().particleManager;
                    particleManager.addParticle(particle, getX(i), getY(i), getZ(i), 0.0, 0.0, 0.0);
                    // Here will go the code for rendering the actual waypoint
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (open_waypoints_keybind.wasPressed()) {
                client.setScreen(new CListWaypointScreen(Text.literal("Waypoints")));
            }
            while(add_a_waypoint.wasPressed()){
                PlayerEntity player = MinecraftClient.getInstance().player;
                addNewWaypoint("X: "+Math.round(player.getX())+" Y: "+Math.round(player.getY())+" Z: "+Math.round(player.getZ()));
            }
            while(show_hud.wasPressed()){
                client.setScreen(new CListWaypointHUD(Text.literal("hud"),MinecraftClient.getInstance()));
            }
            if (client.world == null) {
                variables.loaded_last_world = false;
                variables.waypoints.clear();
                variables.names.clear();
                variables.dimensions.clear();
                variables.worldName = null;
                variables.last_world = null;
            }
            else{
                variables.last_world = client.world;
                checkForWorldChanges(variables.last_world);
                checkIfSaveIsNeeded();
                if (client.isInSingleplayer()) {
                    variables.worldName = client.getServer().getSaveProperties().getLevelName();
                } else {
                    variables.worldName = client.getCurrentServerEntry().name;
                }
            }
        });
        variables.saved_since_last_update = true;
        variables.loaded_last_world = false;
    }
    public static void addNewWaypoint(String name){
        variables.waypoints.add(name);
        CList.LOGGER.info("New waypoint for dimension " + variables.last_world.getDimension().effects());
        variables.dimensions.add(String.valueOf(variables.last_world.getDimension().effects()));
        variables.names.add("New Waypoint");
        variables.saved_since_last_update = false;
    }
    public static void deleteWaypoint(int position){
        try {
            variables.waypoints.remove(position);
            variables.names.remove(position);
            variables.dimensions.remove(position);
            variables.saved_since_last_update = false;
        }
        catch (IndexOutOfBoundsException e){
            //CList.LOGGER.info("WTF");
        }
    }
    public static Text getDimension(int position){
        String s = variables.dimensions.get(position);
        s = s.replace("minecraft:","");
        s = s.replace("_"," ");
        s = StringUtils.capitalize(s);
        return Text.literal(s);
    }
    public static Text getDimension(String string){
        String s = string;
        s = s.replace("minecraft:","");
        s = s.replace("_"," ");
        s = StringUtils.capitalize(s);
        return Text.literal(s);
    }
    public static int getX(int position){
        String s = variables.waypoints.get(position);
        s = s.replace("X","");
        s = s.replace("Y","");
        s = s.replace("Z","");
        s = s.replace(" ","");
        String[] segments = s.split(":");
        return Integer.parseInt(segments[1]);
    }
    public static int getY(int position){
        String s = variables.waypoints.get(position);
        s = s.replace("X","");
        s = s.replace("Y","");
        s = s.replace("Z","");
        s = s.replace(" ","");
        String[] segments = s.split(":");
        return Integer.parseInt(segments[2]);
    }
    public static int getZ(int position){
        String s = variables.waypoints.get(position);
        s = s.replace("X","");
        s = s.replace("Y","");
        s = s.replace("Z","");
        s = s.replace(" ","");
        String[] segments = s.split(":");
        return Integer.parseInt(segments[3]);
    }
    public static void checkForWorldChanges(ClientWorld current_world){
        if(!variables.loaded_last_world && variables.worldName != null){
            CList.LOGGER.info("New world " + variables.worldName);
            variables.last_world = current_world;
            List<String> temp = CListData.loadListFromFile("clist_"+variables.worldName);
            List<String> names = CListData.loadListFromFile("clist_names_"+variables.worldName);
            List<String> dimensions = CListData.loadListFromFile("clist_dimensions_"+variables.worldName);
            if(temp != null && temp.size()>0){
                variables.waypoints = temp;
                variables.names = names;
                variables.dimensions = dimensions;
                CList.LOGGER.info("Loaded data for world " + variables.worldName);
            }
            else{
                CList.LOGGER.info("The file for " + variables.worldName + " doesn't exist");
            }
            variables.loaded_last_world = true;
        }
    }
    public static void checkIfSaveIsNeeded(){
        if(!variables.saved_since_last_update){
            CList.LOGGER.info("Saving data for world " + variables.worldName);
            CListData.saveListToFile("clist_"+variables.worldName, variables.waypoints);
            CListData.saveListToFile("clist_names_"+variables.worldName, variables.names);
            CListData.saveListToFile("clist_dimensions_"+variables.worldName, variables.dimensions);
            variables.saved_since_last_update = true;
        }
    }
}
