package studios.viciont.quasogeyser;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.extension.ExtensionLogger;


/** Sólo presentación. No cambia inventarios, combate, colisiones ni datos del mundo Java. */
final class PresentationHandler extends ChannelOutboundHandlerAdapter {
    static final String NAME = "quaso_presentation";
    private final VisualText text;
    private final ExtensionLogger logger;
    private boolean warned;

    PresentationHandler(ExtensionLogger logger) throws Exception {
        this.logger = logger;
        text = new VisualText();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BedrockPacketWrapper wrapper && wrapper.getPacketBuffer() == null) {
            try {
                wrapper.setPacket(adapt(wrapper.getPacket()));
            } catch (RuntimeException | LinkageError error) {
                // Un cambio de protocolo no debe cortar la conexión del jugador.
                if (!warned) { warned = true; logger.warning("No se pudo adaptar un paquete visual; se conserva el original: " + error); }
            }
        }
        super.write(ctx, msg, promise);
    }

    private EntityDataMap metadata(EntityDataMap source) {
        EntityDataMap data = new EntityDataMap();
        data.putAll(source);
        if (source.containsKey(EntityDataTypes.NAME)) data.put(EntityDataTypes.NAME, text.ranks(source.get(EntityDataTypes.NAME).toString()));
        return data;
    }

    BedrockPacket adapt(BedrockPacket original) {
        if (original instanceof TextPacket source) {
            TextPacket p = source.clone();
            p.setMessage(text.ranks(p.getMessage()));
            p.setSourceName(text.ranks(p.getSourceName()));
            p.setFilteredMessage(text.ranks(p.getFilteredMessage()));
            if (p.getParameters() != null) p.setParameters(p.getParameters().stream().map(text::ranks).toList());
            return p;
        }
        if (original instanceof AddEntityPacket source) {
            AddEntityPacket p = source.clone();
            p.setMetadata(metadata(source.getMetadata()));
            return p;
        }
        if (original instanceof AddPlayerPacket source) {
            AddPlayerPacket p = source.clone();
            p.setUsername(text.ranks(p.getUsername()));
            p.setMetadata(metadata(source.getMetadata()));
            return p;
        }
        if (original instanceof SetEntityDataPacket source) {
            SetEntityDataPacket p = source.clone();
            p.setMetadata(metadata(source.getMetadata()));
            return p;
        }
        if (original instanceof SetScorePacket source) {
            SetScorePacket p = source.clone();
            p.setInfos(source.getInfos().stream().map(i -> i.getType() == ScoreInfo.ScorerType.FAKE
                    ? new ScoreInfo(i.getScoreboardId(), i.getObjectiveId(), i.getScore(), text.ranks(i.getName())) : i).toList());
            return p;
        }
        if (original instanceof PlayerListPacket source && source.getAction() == PlayerListPacket.Action.ADD) {
            // Entries también llevan skins; se cambian sólo nombres de la lista ya destinada a Bedrock.
            for (var entry : source.getEntries()) entry.setName(text.ranks(entry.getName()));
        }
        if (original instanceof BlockEntityDataPacket source && source.getData().containsKey("CustomName")) {
            String title = source.getData().getString("CustomName");
            String converted = VisualText.menuTitle(title);
            if (!title.equals(converted)) {
                BlockEntityDataPacket p = source.clone();
                p.setData(source.getData().toBuilder().putString("CustomName", converted).build());
                return p;
            }
        }
        return original;
    }
}
