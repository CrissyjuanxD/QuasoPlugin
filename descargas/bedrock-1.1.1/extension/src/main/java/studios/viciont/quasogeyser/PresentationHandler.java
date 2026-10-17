package studios.viciont.quasogeyser;

import com.google.gson.Gson;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.cloudburstmc.protocol.bedrock.data.ScoreInfo;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.extension.ExtensionLogger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Sólo presentación. No cambia inventarios, combate, colisiones ni datos del mundo Java. */
final class PresentationHandler extends ChannelOutboundHandlerAdapter {
    static final String NAME = "quaso_presentation";
    record Mob(String model, String entity, String[] names, int mark_variant) {}
    private final VisualText text;
    private final Mob[] mobs;
    private final ExtensionLogger logger;
    private final Map<Integer, State> clients = new HashMap<>();
    private boolean warned;

    PresentationHandler(ExtensionLogger logger) throws Exception {
        this.logger = logger;
        text = new VisualText();
        try (var in = new InputStreamReader(getClass().getResourceAsStream("/mobs.json"), StandardCharsets.UTF_8)) {
            mobs = new Gson().fromJson(in, Mob[].class);
        }
    }

    static final class Tracked {
        final String type;
        int originalMark, model;
        Tracked(String type) { this.type = type; }
    }
    static final class State {
        final Map<Long, Tracked> entities = new HashMap<>();
        final Map<Long, Long> uniqueToRuntime = new HashMap<>();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof BedrockPacketWrapper wrapper && wrapper.getPacketBuffer() == null) {
            try {
                var state = clients.computeIfAbsent((int) wrapper.getTargetSubClientId(), unused -> new State());
                wrapper.setPacket(adapt(wrapper.getPacket(), state));
            } catch (RuntimeException | LinkageError error) {
                // Un cambio de protocolo no debe cortar la conexión del jugador.
                if (!warned) { warned = true; logger.warning("No se pudo adaptar un paquete visual; se conserva el original: " + error); }
            }
        }
        super.write(ctx, msg, promise);
    }

    int model(String type, String name) {
        String plain = VisualText.plain(name);
        for (Mob mob : mobs) {
            if (!mob.entity().equals(type)) continue;
            for (String expected : mob.names()) {
                if (plain.equalsIgnoreCase(expected) || plain.regionMatches(true, 0, expected + " ", 0, expected.length()+1))
                    return mob.mark_variant();
            }
        }
        return 0;
    }

    private EntityDataMap metadata(EntityDataMap source, Tracked entity) {
        EntityDataMap data = new EntityDataMap();
        data.putAll(source);
        if (entity != null) {
            if (source.containsKey(EntityDataTypes.MARK_VARIANT)) entity.originalMark = source.get(EntityDataTypes.MARK_VARIANT);
            int before = entity.model;
            if (source.containsKey(EntityDataTypes.NAME)) entity.model = model(entity.type, source.get(EntityDataTypes.NAME).toString());
            if (entity.model != 0) data.put(EntityDataTypes.MARK_VARIANT, entity.model);
            else if (before != 0) data.put(EntityDataTypes.MARK_VARIANT, entity.originalMark);
        }
        if (source.containsKey(EntityDataTypes.NAME)) data.put(EntityDataTypes.NAME, text.ranks(source.get(EntityDataTypes.NAME).toString()));
        return data;
    }

    BedrockPacket adapt(BedrockPacket original, State state) {
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
            var tracked = new Tracked(p.getIdentifier());
            state.entities.put(p.getRuntimeEntityId(), tracked);
            state.uniqueToRuntime.put(p.getUniqueEntityId(), p.getRuntimeEntityId());
            p.setMetadata(metadata(source.getMetadata(), tracked));
            return p;
        }
        if (original instanceof AddPlayerPacket source) {
            AddPlayerPacket p = source.clone();
            p.setUsername(text.ranks(p.getUsername()));
            p.setMetadata(metadata(source.getMetadata(), null));
            return p;
        }
        if (original instanceof SetEntityDataPacket source) {
            SetEntityDataPacket p = source.clone();
            p.setMetadata(metadata(source.getMetadata(), state.entities.get(p.getRuntimeEntityId())));
            return p;
        }
        if (original instanceof RemoveEntityPacket p) {
            Long runtime = state.uniqueToRuntime.remove(p.getUniqueEntityId());
            if (runtime != null) state.entities.remove(runtime);
        }
        if (original instanceof ChangeDimensionPacket) {
            state.entities.clear(); state.uniqueToRuntime.clear();
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
