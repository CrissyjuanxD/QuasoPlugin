package studios.viciont.quasogeyser;

import com.google.gson.Gson;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.*;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Displays del catálogo Quaso: el servidor conserva entidades, visibilidad e interacciones. */
final class DisplayBridge {
    private Identifier identifier;
    record Model(int id, String key, String block, Map<String,String> states, String profile) {}
    static final Model[] MODELS = load();
    private BedrockEntityDefinition definition;
    static Model[] load() {
        try (var reader = new InputStreamReader(DisplayBridge.class.getResourceAsStream("/displays.json"), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Model[].class);
        } catch (Exception ex) { throw new IllegalStateException("Falta el catálogo de displays", ex); }
    }
    void define(GeyserDefineEntitiesEvent event) {
        identifier = Identifier.of("quaso:display");
        CustomEntityDefinition custom = CustomEntityDefinition.of(identifier);
        event.register(custom);
        definition = (BedrockEntityDefinition) custom;
    }
    void properties(GeyserDefineEntityPropertiesEvent event) {
        if (definition == null) return;
        int max = Arrays.stream(MODELS).mapToInt(Model::id).max().orElseThrow();
        event.registerIntegerProperty(identifier, Identifier.of("quaso:display_model"), 0, max, 0);
        for (String p : List.of("item", "bright", "translucent")) event.registerIntegerProperty(identifier, Identifier.of("quaso:"+p), 0, 1, 0);
        event.registerIntegerProperty(identifier, Identifier.of("quaso:revision"), 0, 1000000, 0);
        event.registerFloatProperty(identifier, Identifier.of("quaso:duration"), 0, 100, 0f);
        for (String prefix : List.of("t", "l", "s", "r")) for (char axis : new char[]{'x','y','z'})
            event.registerFloatProperty(identifier, Identifier.of("quaso:"+prefix+'_'+axis), -1000000, 1000000, prefix.equals("s") ? 1f : 0f);

        var base = EntityTypeDefinition.baseBuilder(Entity.class)
                .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
                .addTranslator(MetadataTypes.INT, Entity::setAir)
                .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setCustomName)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setCustomNameVisible)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
                .addTranslator(null) // Displays never acquire gravity.
                .addTranslator(MetadataTypes.POSE, (e,m) -> e.setPose(m.getValue()))
                .addTranslator(MetadataTypes.INT, Entity::setFreezing).build();
        var display = EntityTypeBase.baseInherited(QuasoDisplayEntity.class, base)
                .addTranslator(null) // interpolation delay (Quaso uses zero)
                .addTranslator(MetadataTypes.INT, QuasoDisplayEntity::duration)
                .addTranslator(null) // position interpolation handled by Geyser movement
                .addTranslator(MetadataTypes.VECTOR3, QuasoDisplayEntity::translation)
                .addTranslator(MetadataTypes.VECTOR3, QuasoDisplayEntity::displayScale)
                .addTranslator(MetadataTypes.QUATERNION, QuasoDisplayEntity::left)
                .addTranslator(MetadataTypes.QUATERNION, QuasoDisplayEntity::right)
                .addTranslator(null) // fixed billboard used by these models
                .addTranslator(MetadataTypes.INT, QuasoDisplayEntity::brightness)
                .addTranslator(null).addTranslator(null).addTranslator(null)
                .addTranslator(null).addTranslator(null).addTranslator(null).build();
        var blocks = VanillaEntityType.inherited(QuasoDisplayEntity::new, display)
                .type(EntityType.BLOCK_DISPLAY).heightAndWidth(0).offset(0).bedrockDefinition(definition)
                .addTranslator(MetadataTypes.BLOCK_STATE, QuasoDisplayEntity::block).build();
        var items = VanillaEntityType.inherited(QuasoDisplayEntity::new, display)
                .type(EntityType.ITEM_DISPLAY).heightAndWidth(0).offset(0).bedrockDefinition(definition)
                .addTranslator(MetadataTypes.ITEM_STACK, QuasoDisplayEntity::item)
                .addTranslator(null).build(); // Grave items use the NONE transform.
        Registries.JAVA_ENTITY_TYPES.register(GeyserEntityType.ofVanilla(EntityType.BLOCK_DISPLAY), blocks);
        Registries.JAVA_ENTITY_TYPES.register(GeyserEntityType.ofVanilla(EntityType.ITEM_DISPLAY), items);
    }
    static int blockModel(String state) {
        int bracket=state.indexOf('[');
        String name=bracket<0?state:state.substring(0,bracket);
        Map<String,String> values=new HashMap<>();
        if (bracket>=0) for (String entry:state.substring(bracket+1,state.length()-1).split(",")) {
            String[] pair=entry.split("=",2);if(pair.length==2) values.put(pair[0],pair[1]);
        }
        for (Model model:MODELS) if (name.equals(model.block()) &&
                (model.states()==null || model.states().entrySet().stream().allMatch(e->e.getValue().equals(values.get(e.getKey()))))) return model.id();
        return 0;
    }
    static int itemModel(String name) {
        // An item has the default block shape, not an arbitrary waterlogged/double slab variant.
        String state = name.equals("minecraft:smooth_stone_slab") ? name+"[type=bottom]" : name;
        int exact=blockModel(state);if(exact!=0)return exact;
        for(Model model:MODELS)if(name.equals(model.block()))return model.id();
        return 0;
    }
    static int profileModel(String encoded) {
        try {
            var json=com.google.gson.JsonParser.parseString(new String(Base64.getDecoder().decode(encoded),StandardCharsets.UTF_8)).getAsJsonObject();
            String url=json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            String hash=url.substring(url.lastIndexOf('/')+1);
            for(Model model:MODELS)if(hash.equals(model.profile()))return model.id();
        } catch (RuntimeException ignored) { }
        return 0;
    }
}
