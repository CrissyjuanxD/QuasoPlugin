package studios.viciont.quasogeyser;

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Run with Geyser 1256 and Mockito 5.20 on the classpath; no network or client required. */
public final class WorldTranslationTest {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        QuasoWardenCave extension = new QuasoWardenCave();
        BlockMappings vanilla = BlockMappings.builder().javaToBedrockBlocks(new GeyserBedrockBlock[1]).build();
        BlockMappings warden = BlockMappings.builder().javaToBedrockBlocks(new GeyserBedrockBlock[1]).build();
        map(extension, "wardenMappings").put(vanilla, warden);
        map(extension, "originals").put(warden, vanilla);
        AtomicReference<BlockMappings> playerA = new AtomicReference<>(vanilla);
        AtomicReference<BlockMappings> playerB = new AtomicReference<>(vanilla);
        GeyserSession a = session(playerA), b = session(playerB);
        AtomicReference<Key> destination = new AtomicReference<>(Key.key("minecraft:wardencave"));
        AtomicReference<BlockMappings> expected = new AtomicReference<>(warden);
        AtomicInteger forwarded = new AtomicInteger();
        PacketTranslator<ClientboundRespawnPacket> delegate = new PacketTranslator<>() {
            @Override public void translate(GeyserSession session, ClientboundRespawnPacket packet) {
                check(session.getBlockMappings() == expected.get(), "El mapping debe cambiar ANTES del respawn original");
                forwarded.incrementAndGet();
            }
        };
        var constructor = Class.forName(QuasoWardenCave.class.getName() + "$WorldTranslator")
                .getDeclaredConstructor(QuasoWardenCave.class, PacketTranslator.class, Function.class);
        constructor.setAccessible(true);
        var translator = (PacketTranslator<ClientboundRespawnPacket>) constructor.newInstance(extension, delegate,
                (Function<ClientboundRespawnPacket, Key>) packet -> destination.get());
        ClientboundRespawnPacket packet = mock(ClientboundRespawnPacket.class);
        translator.translate(a, packet);
        check(playerA.get() == warden && playerB.get() == vanilla, "Entrar no puede afectar otra sesión");
        translator.translate(a, packet);
        check(playerA.get() == warden, "Morir/reaparecer dentro debe mantener el mapping");
        destination.set(Key.key("minecraft:overworld")); expected.set(vanilla);
        translator.translate(a, packet);
        check(playerA.get() == vanilla, "Salir debe restaurar el mapping exacto, no otro clon");
        destination.set(Key.key("minecraft:wardencave_copia"));
        translator.translate(b, packet);
        check(playerB.get() == vanilla, "No debe activarse por coincidencia parcial del nombre");
        destination.set(Key.key("minecraft:wardencave")); expected.set(warden);
        translator.translate(b, packet);
        check(playerA.get() == vanilla && playerB.get() == warden, "Las sesiones deben seguir aisladas");
        check(forwarded.get() == 5, "Cada paquete original debe delegarse exactamente una vez");
        check(QuasoWardenCave.appearance("minecraft:cave_vines[age=25,berries=true]").equals("baya_luminosa"), "Bayas maduras");
        check(QuasoWardenCave.appearance("minecraft:cave_vines[age=25,berries=false]") == null, "Enredadera cosechada vanilla");
        check(QuasoWardenCave.appearance("minecraft:cave_vines_plant[berries=true]").equals("baya_luminosa"), "Tramo de enredadera");
        check(QuasoWardenCave.appearance("minecraft:ochre_froglight[axis=y]") == null, "Froglight ajeno vanilla");
        check(QuasoWardenCave.appearance("minecraft:stone") == null, "Bloques ajenos vanilla");
        System.out.println("OK: entrada, reaparición, salida, nombre exacto, dos sesiones aisladas, orden/delegación y bayas cosechadas.");
    }

    private static GeyserSession session(AtomicReference<BlockMappings> current) {
        GeyserSession session = mock(GeyserSession.class);
        when(session.getBlockMappings()).thenAnswer(call -> current.get());
        doAnswer(call -> { current.set(call.getArgument(0)); return null; }).when(session).setBlockMappings(any());
        return session;
    }

    @SuppressWarnings("unchecked")
    private static Map<BlockMappings, BlockMappings> map(QuasoWardenCave extension, String name) throws Exception {
        Field field = QuasoWardenCave.class.getDeclaredField(name); field.setAccessible(true);
        return (Map<BlockMappings, BlockMappings>) field.get(extension);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
