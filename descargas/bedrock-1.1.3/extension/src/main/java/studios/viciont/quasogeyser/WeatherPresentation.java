package studios.viciont.quasogeyser;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Reduce la opacidad visual de la tormenta sin cambiar el clima Java ni el estado de Geyser. */
final class WeatherPresentation {
    private final Map<GeyserSession, Integer> steps = new ConcurrentHashMap<>();
    private PacketTranslator<ClientboundGameEventPacket> previous;
    private PacketTranslator<ClientboundGameEventPacket> installed;
    static final float RAIN_AT_FULL_MOON = .30f;
    static final float THUNDER_AT_FULL_MOON = .08f;

    @SuppressWarnings("unchecked")
    void install() {
        previous = (PacketTranslator<ClientboundGameEventPacket>) Objects.requireNonNull(
                Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundGameEventPacket.class));
        installed = new PacketTranslator<>() {
            @Override public void translate(GeyserSession session, ClientboundGameEventPacket packet) {
                translateWeather(session, packet, previous);
            }
        };
        Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundGameEventPacket.class, installed);
    }

    void translateWeather(GeyserSession session, ClientboundGameEventPacket packet,
                          PacketTranslator<ClientboundGameEventPacket> delegate) {
        // Primero Geyser conserva el valor real y traduce todos los eventos de juego.
        delegate.translate(session, packet);
        if (!steps.containsKey(session)) return;
        switch (packet.getNotification()) {
            case START_RAINING, STOP_RAINING, RAIN_LEVEL_CHANGE, THUNDER_LEVEL_CHANGE -> resend(session);
            default -> { }
        }
    }

    /** Se llama en el event loop de esta sesión, junto al paso de niebla correspondiente. */
    void strength(GeyserSession session, int step) {
        step = Math.max(0, Math.min(20, step));
        Integer old = step == 0 ? steps.remove(session) : steps.put(session, step);
        if ((old == null ? 0 : old) != step) resend(session);
    }

    void retain(Set<? extends GeyserConnection> online) { steps.keySet().retainAll(online); }

    private void resend(GeyserSession session) {
        float fade = steps.getOrDefault(session, 0) / 20f;
        float rain = clamp(session.getRainStrength());
        float thunder = rain > 0 ? clamp(session.getThunderStrength()) : 0;
        send(session, LevelEvent.START_RAINING, LevelEvent.STOP_RAINING,
                rain * (1 - fade * (1 - RAIN_AT_FULL_MOON)));
        send(session, LevelEvent.START_THUNDERSTORM, LevelEvent.STOP_THUNDERSTORM,
                thunder * (1 - fade * (1 - THUNDER_AT_FULL_MOON)));
    }

    private static float clamp(float n) { return Float.isFinite(n) ? Math.max(0, Math.min(1, n)) : 0; }

    private static void send(GeyserSession session, LevelEvent on, LevelEvent off, float value) {
        LevelEventPacket packet = new LevelEventPacket();
        packet.setPosition(Vector3f.ZERO);
        packet.setType(value > 0 ? on : off);
        packet.setData(Math.round(value * 65535));
        session.sendUpstreamPacket(packet);
    }

    void stop() {
        if (installed != null && Registries.JAVA_PACKET_TRANSLATORS.get(ClientboundGameEventPacket.class) == installed)
            Registries.JAVA_PACKET_TRANSLATORS.register(ClientboundGameEventPacket.class, previous);
        for (GeyserSession session : Set.copyOf(steps.keySet()))
            session.executeInEventLoop(() -> strength(session, 0));
    }
}
