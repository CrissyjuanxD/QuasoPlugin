package studios.viciont.quasogeyser;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.bedrock.camera.CameraData;
import org.geysermc.geyser.session.GeyserSession;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public final class AtmosphereTest {
    public interface QuasoApi { Object getBloodMoon(); }
    public static final class Moon {
        boolean active = true;
        World world;
        public boolean isActive(World w) { return active && w == world; }
    }
    public static void main(String[] args) throws Exception {
        var extension = mock(QuasoWardenCave.class);
        var api = mock(GeyserApi.class);
        when(extension.geyserApi()).thenReturn(api);
        var session = mock(GeyserSession.class);
        doReturn(List.of(session)).when(api).onlineConnections();
        UUID id = UUID.randomUUID(); when(session.javaUuid()).thenReturn(id);
        CameraData camera = mock(CameraData.class); when(session.camera()).thenReturn(camera);
        Set<String> stack = new LinkedHashSet<>(Set.of("otroplugin:niebla"));
        doAnswer(call -> { stack.add(call.getArgument(0,String.class)); return null; }).when(camera).sendFog(anyString());
        doAnswer(call -> { stack.remove(call.getArgument(0,String.class)); return null; }).when(camera).removeFog(anyString());
        doAnswer(call -> { call.getArgument(0,Runnable.class).run(); return null; }).when(session).executeInEventLoop(any());
        Player player = mock(Player.class);
        World overworld = mock(World.class), cave = mock(World.class);
        when(overworld.getUID()).thenReturn(UUID.randomUUID()); when(cave.getUID()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(overworld);
        Moon moon = new Moon(); moon.world = overworld;
        Plugin quaso = mock(Plugin.class,withSettings().extraInterfaces(QuasoApi.class));
        when(quaso.isEnabled()).thenReturn(true); when(((QuasoApi) quaso).getBloodMoon()).thenReturn(moon);
        PluginManager plugins = mock(PluginManager.class); when(plugins.getPlugin("QuasoPlugin")).thenReturn(quaso);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(() -> Bukkit.getPlayer(id)).thenReturn(player);
            Atmosphere atmosphere = new Atmosphere(extension);
            Method tick = Atmosphere.class.getDeclaredMethod("tick"); tick.setAccessible(true);
            for (int i=1;i<=20;i++) {
                tick.invoke(atmosphere);
                check(stack.equals(Set.of("otroplugin:niebla","quaso:bloodmoon_"+i)),"Un solo paso activo, fade de entrada");
            }
            tick.invoke(atmosphere);
            verify(camera,times(20)).sendFog(anyString()); // Sin paquetes repetidos al quedarse en rojo.
            moon.active=false;
            for (int i=19;i>=1;i--) { tick.invoke(atmosphere); check(stack.contains("quaso:bloodmoon_"+i),"Fade de salida"); }
            tick.invoke(atmosphere); check(stack.equals(Set.of("otroplugin:niebla")),"Retira sólo nuestra niebla");
            moon.active=true; tick.invoke(atmosphere);
            when(player.getWorld()).thenReturn(cave); tick.invoke(atmosphere);
            check(stack.equals(Set.of("otroplugin:niebla")),"Cambio de mundo limpia el rojo inmediatamente");
            when(player.getWorld()).thenReturn(overworld); tick.invoke(atmosphere);
            atmosphere.stop(); check(stack.equals(Set.of("otroplugin:niebla")),"Apagar la extensión limpia su estado");
        }
        System.out.println("OK: BloodMoon 10 s, salida gradual, sin paquetes repetidos, portal, apagado y niebla ajena intacta.");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
