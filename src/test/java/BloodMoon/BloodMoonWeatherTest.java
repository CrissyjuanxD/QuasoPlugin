package BloodMoon;

import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.bukkit.Server;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BloodMoonWeatherTest {
    private BloodMoonWeather weather;
    private BloodMoonWeather.Packets packets;
    private BloodMoonSky sky;
    private Player player;
    private World world;
    private EmbeddedChannel channel;
    private Logger logger;

    @BeforeAll static void paper() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() throws Exception {
        BloodMoon manager = mock(BloodMoon.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(manager.getPlugin()).thenReturn(plugin);
        when(plugin.getServer()).thenReturn(mock(Server.class));
        logger = mock(Logger.class);
        when(plugin.getLogger()).thenReturn(logger);
        world = mock(World.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.isOnline()).thenReturn(true);
        ConfigReader config = mock(ConfigReader.class);
        when(config.GetThunderingConfig()).thenReturn(true);
        when(manager.getConfigReader(world)).thenReturn(config);
        sky = mock(BloodMoonSky.class);
        when(sky.strength(world)).thenReturn(1f);
        packets = mock(BloodMoonWeather.Packets.class);
        channel = new EmbeddedChannel();
        channel.pipeline().addLast("packet_handler", new ChannelOutboundHandlerAdapter());
        when(packets.channel(player)).thenReturn(channel);
        weather = new BloodMoonWeather(manager, sky, packets);
    }
    @AfterEach void cleanup() { weather.shutdown(); channel.finishAndReleaseAll(); }
    private BloodMoonWeather.Filter filter() { return (BloodMoonWeather.Filter) channel.pipeline().get("quaso_bloodmoon_weather"); }

    @Test void stormVisualsAreScaledWithoutChangingWorldWeatherOrSpammingStableUpdates() throws Exception {
        weather.sync(player, false);
        assertEquals(0.6f, filter().factor, 0.0001f);
        for (int i = 0; i < 50; i++) weather.sync(player, false);
        verify(packets).channel(player);
        verify(packets).refresh(player);
        verify(world, never()).setStorm(anyBoolean());
        verify(world, never()).setThundering(anyBoolean());
        verify(player, never()).setPlayerWeather(any());
    }
    @Test void bothFadesFollowTheSkyStrengthAndReleaseTheFilterAtZero() throws Exception {
        when(sky.strength(world)).thenReturn(0.5f);
        weather.sync(player, false);
        assertEquals(0.8f, filter().factor, 0.0001f);
        when(sky.strength(world)).thenReturn(1f);
        weather.sync(player, false);
        assertEquals(0.6f, filter().factor, 0.0001f);
        when(sky.strength(world)).thenReturn(0.5f);
        weather.sync(player, false);
        assertEquals(0.8f, filter().factor, 0.0001f);
        when(sky.strength(world)).thenReturn(0f);
        weather.sync(player, false);
        assertNull(filter());
        verify(packets, times(4)).refresh(player);
    }
    @Test void changingWorldRestoresNormalWeatherWithoutRemovingOtherHandlers() throws Exception {
        weather.sync(player, false);
        when(player.getWorld()).thenReturn(mock(World.class));
        weather.sync(player, true);
        assertNull(filter());
        assertNotNull(channel.pipeline().get("packet_handler"));
        verify(packets, times(2)).refresh(player);
    }
    @Test void joinAndRespawnResendVisualWeatherEvenIfIntensityHasNotChanged() throws Exception {
        weather.sync(player, true);
        weather.sync(player, true);
        verify(packets).channel(player);
        verify(packets, times(2)).refresh(player);
    }
    @Test void personalWeatherFromAnotherPluginIsNotOverwritten() throws Exception {
        weather.sync(player, false);
        when(player.getPlayerWeather()).thenReturn(WeatherType.CLEAR);
        weather.sync(player, false);
        assertNull(filter());
        verify(packets).refresh(player);
        verify(player, never()).resetPlayerWeather();
    }
    @Test void shutdownRestoresWeatherAndCannotReinstallTheFilter() throws Exception {
        weather.sync(player, false);
        weather.shutdown();
        weather.sync(player, true);
        assertNull(filter());
        verify(packets, times(2)).refresh(player);
    }
    @Test void aPacketConversionFailurePreservesTheOriginalPacketAndConnection() throws Exception {
        weather.sync(player, false);
        Object packet = new Object();
        when(packets.scale(packet, 0.6f)).thenThrow(new ReflectiveOperationException("test"));
        assertTrue(channel.writeOutbound(packet));
        assertSame(packet, channel.readOutbound());
        assertTrue(channel.isOpen());
        verify(logger).warning(contains("clima normal"));
    }
    @Test void anUnsupportedServerFallsBackWithoutLeavingAnAttachedFilter() throws Exception {
        doThrow(new ReflectiveOperationException("test")).when(packets).refresh(player);
        weather.sync(player, false);
        weather.sync(player, false);
        assertNull(filter());
        verify(logger).warning(contains("clima normal"));
        verify(packets).channel(player);
    }
}
