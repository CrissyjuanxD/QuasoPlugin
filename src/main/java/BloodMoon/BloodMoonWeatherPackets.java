package BloodMoon;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Puente aislado para los paquetes de clima de Paper 26.2, sin modificar el clima del mundo. */
final class BloodMoonWeatherPackets implements BloodMoonWeather.Packets {
    private final Class<?> packetType = Class.forName("net.minecraft.network.protocol.game.ClientboundGameEventPacket");
    private final Constructor<?> constructor = packetType.getConstructor(Class.forName(packetType.getName() + "$Type"), float.class);
    private final Method event = packetType.getMethod("getEvent");
    private final Method parameter = packetType.getMethod("getParam");
    private final Object rain = packetType.getField("RAIN_LEVEL_CHANGE").get(null);
    private final Object thunder = packetType.getField("THUNDER_LEVEL_CHANGE").get(null);
    private final Object start = packetType.getField("START_RAINING").get(null);
    private final Object stop = packetType.getField("STOP_RAINING").get(null);
    private final Method playerHandle = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer").getMethod("getHandle");
    private final Method worldHandle = Class.forName("org.bukkit.craftbukkit.CraftWorld").getMethod("getHandle");
    private final Field listener = Class.forName("net.minecraft.server.level.ServerPlayer").getField("connection");
    private final Field connection = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl").getField("connection");
    private final Field channel = Class.forName("net.minecraft.network.Connection").getField("channel");
    private final Method send = listener.getType().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"));
    private final Field rainLevel = Class.forName("net.minecraft.world.level.Level").getField("rainLevel");
    private final Field thunderLevel = Class.forName("net.minecraft.world.level.Level").getField("thunderLevel");

    BloodMoonWeatherPackets() throws ReflectiveOperationException {}

    @Override public Channel channel(Player player) throws ReflectiveOperationException {
        return (Channel) channel.get(connection.get(listener.get(playerHandle.invoke(player))));
    }

    @Override public Object scale(Object packet, float factor) throws ReflectiveOperationException {
        if (factor == 1 || !packetType.isInstance(packet)) return packet;
        Object type = event.invoke(packet);
        if (type == rain || type == thunder) {
            float value = (float) parameter.invoke(packet);
            return constructor.newInstance(type, Math.clamp(value, 0, 1) * factor);
        }
        // Estos dos eventos fijan 0 y 1 en ClientPacketListener de 26.2, respectivamente.
        // Convertirlos evita un fotograma de lluvia completa al cambiar el estado booleano.
        if (type == start || type == stop) return constructor.newInstance(rain, type == start ? 0f : factor);
        return packet;
    }

    @Override public void refresh(Player player) throws ReflectiveOperationException {
        Object level = worldHandle.invoke(player.getWorld());
        Object target = listener.get(playerHandle.invoke(player));
        send.invoke(target, constructor.newInstance(rain, rainLevel.getFloat(level)));
        send.invoke(target, constructor.newInstance(thunder, thunderLevel.getFloat(level)));
    }
}
