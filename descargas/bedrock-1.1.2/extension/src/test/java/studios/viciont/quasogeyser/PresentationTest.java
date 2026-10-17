package studios.viciont.quasogeyser;

import io.netty.channel.embedded.EmbeddedChannel;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.extension.ExtensionLogger;

import java.lang.reflect.Proxy;
import java.util.List;

/** Ejercita paquetes Bedrock reales y el canal Netty, sin servidor ni cuenta Microsoft. */
public final class PresentationTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        ExtensionLogger log = (ExtensionLogger) Proxy.newProxyInstance(ExtensionLogger.class.getClassLoader(),
                new Class[]{ExtensionLogger.class}, (proxy, method, values) -> null);
        PresentationHandler handler = new PresentationHandler(log);
        VisualText text = new VisualText();
        for (int code = 0xEB87; code <= 0xEB96; code++) {
            String converted = text.ranks("§a" + (char) code + " nombre");
            check(converted.indexOf((char) code) < 0 && converted.contains("§r§a nombre"), "Rango y color " + code);
            check(text.ranks(converted).equals(converted), "Conversión idempotente " + code);
        }
        check(text.ranks("§cHola sin rangos").equals("§cHola sin rangos"), "Chat normal intacto");
        check(VisualText.menuTitle("㈁§f㈂").equals("[quaso:misiones]"), "Título de misión");
        check(VisualText.menuTitle("Cofre de Juan").equals("Cofre de Juan"), "Cofre normal intacto");
        for (int code = 0x3202; code <= 0x3208; code++)
            check(VisualText.menuTitle("㈁§f"+(char) code).startsWith("[quaso:"), "7 títulos");
        AddEntityPacket spawn = new AddEntityPacket();
        spawn.setRuntimeEntityId(42); spawn.setUniqueEntityId(900); spawn.setIdentifier("minecraft:bee");
        spawn.getMetadata().put(EntityDataTypes.MARK_VARIANT, 1);
        spawn.getMetadata().put(EntityDataTypes.NAME, "Mini Abeja Floral");
        var translated = (AddEntityPacket) handler.adapt(spawn);
        check(translated.getMetadata().get(EntityDataTypes.MARK_VARIANT) == 1, "No se roba la variante del aguijón");
        SetEntityDataPacket update = new SetEntityDataPacket(); update.setRuntimeEntityId(42);
        update.getMetadata().put(EntityDataTypes.MARK_VARIANT, 0);
        check(((SetEntityDataPacket)handler.adapt(update)).getMetadata().get(EntityDataTypes.MARK_VARIANT) == 0,
            "Se conservan las actualizaciones nativas de la abeja");
        RemoveEntityPacket remove = new RemoveEntityPacket(); remove.setUniqueEntityId(900); handler.adapt(remove);
        BlockEntityDataPacket block = new BlockEntityDataPacket();
        block.setData(NbtMap.builder().putString("id","Chest").putString("CustomName","㈁§f㈆").putInt("x",24).build());
        var gui = (BlockEntityDataPacket) handler.adapt(block);
        check(gui.getData().getString("CustomName").equals("[quaso:habilidades]") && gui.getData().getInt("x")==24, "GUI sólo cambia título");
        SetScorePacket score = new SetScorePacket(); score.setAction(SetScorePacket.Action.SET);
        score.setInfos(List.of(new ScoreInfo(7,"QuasoBoard",11,"Usuario: \uEB87")));
        var changed = (SetScorePacket) handler.adapt(score);
        check(changed.getInfos().getFirst().getScore()==11 && changed.getInfos().getFirst().getScoreboardId()==7,
                "El orden numérico del sidebar se conserva");
        check(changed.getInfos().getFirst().getName().indexOf('\uEB87')<0,"Rango en sidebar");
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        var wrapper = BedrockPacketWrapper.create(0,0,0,block,null);
        check(channel.writeOutbound(wrapper),"Intercepta el envío real por Netty");
        BedrockPacketWrapper result = channel.readOutbound();
        check(((BlockEntityDataPacket) result.getPacket()).getData().getString("CustomName").equals("[quaso:habilidades]"),
                "Paquete que sale contiene el título corregido");
        result.release(); channel.finishAndReleaseAll();
        System.out.println("OK: " + checks + " comprobaciones de rangos, GUIs, mobs, restauración vanilla y canal de salida.");
    }
    private static void check(boolean ok, String reason) { checks++; if (!ok) throw new AssertionError(reason); }
}
