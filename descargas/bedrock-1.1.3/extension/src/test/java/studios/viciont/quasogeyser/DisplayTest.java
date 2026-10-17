package studios.viciont.quasogeyser;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.CustomBedrockEntityDefinition;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.properties.type.*;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;

import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Native entity lifecycle and packets, without a graphical Bedrock client. */
public final class DisplayTest {
    public static void main(String[] args) {
        try(var identifiers=mockStatic(Identifier.class,CALLS_REAL_METHODS);
            var geyser=mockStatic(org.geysermc.geyser.GeyserImpl.class)) {
            geyser.when(org.geysermc.geyser.GeyserImpl::getInstance).thenReturn(mock(org.geysermc.geyser.GeyserImpl.class,RETURNS_DEEP_STUBS));
            identifiers.when(()->Identifier.of(anyString(),anyString())).thenAnswer(c->org.geysermc.geyser.impl.IdentifierImpl.of(c.getArgument(0),c.getArgument(1)));
            run();
        }
    }
    static void run() {
        int honey=DisplayBridge.blockModel("minecraft:honeycomb_block");
        check(honey>0,"Panal de curación visible");
        check(DisplayBridge.blockModel("minecraft:air")==0,"Raíz vacía de la tumba invisible");
        check(DisplayBridge.blockModel("minecraft:pointed_dripstone[vertical_direction=up,thickness=tip,waterlogged=false]")>0,"Aguijón");
        check(DisplayBridge.blockModel("minecraft:pointed_dripstone[waterlogged=true,thickness=tip,vertical_direction=down]")>0,"Propiedades reordenadas");
        check(DisplayBridge.blockModel("minecraft:purple_glazed_terracotta[facing=east]")!=DisplayBridge.blockModel("minecraft:purple_glazed_terracotta[facing=north]"),"Orientaciones distintas");
        check(DisplayBridge.itemModel("minecraft:smooth_stone_slab")==DisplayBridge.blockModel("minecraft:smooth_stone_slab[type=bottom,waterlogged=false]"),"Losa de tumba, no bloque doble");
        long heads=0;
        for(var m:DisplayBridge.MODELS)if(m.profile()!=null) {
            String json="{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/"+m.profile()+"\"}}}";
            check(DisplayBridge.profileModel(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)))==m.id(),"Cabeza de tumba");heads++;
        }
        check(heads==7 && DisplayBridge.profileModel("invalido")==0,"Siete cabezas y perfil malformado");
        // Quaternion -> ZYX -> quaternion, including the tilted and rotating healing cube.
        Random random=new Random(54);
        for(int n=0;n<100;n++) {
            var q=new org.joml.Quaternionf().rotationYXZ(random.nextFloat()*6.28f,random.nextFloat()*2-1,random.nextFloat()*2-1);
            var e=QuasoDisplayEntity.euler(Quaternionf.from(q.x,q.y,q.z,q.w));
            var recovered=new org.joml.Quaternionf().rotationZYX((float)Math.toRadians(e.getZ()),(float)Math.toRadians(e.getY()),(float)Math.toRadians(e.getX()));
            check(Math.abs(q.dot(recovered))>.99999f,"La conversión conserva la rotación");
        }
        List<PropertyType<?,?>> list=new ArrayList<>();Map<String,Integer> indices=new HashMap<>();
        for(String name:List.of("display_model","item","bright","translucent","revision"))list.add(new IntProperty(Identifier.of("quaso:"+name),1000000,0,0));
        list.add(new FloatProperty(Identifier.of("quaso:duration"),100,0,0f));
        for(String prefix:List.of("t","l","s","r"))for(char axis:new char[]{'x','y','z'})
            list.add(new FloatProperty(Identifier.of("quaso:"+prefix+'_'+axis),1000000,-1000000,prefix.equals("s")?1f:0f));
        for(int i=0;i<list.size();i++)indices.put(list.get(i).identifier().toString(),i);
        var properties=mock(GeyserEntityProperties.class);
        when(properties.getProperties()).thenReturn(list);when(properties.isEmpty()).thenReturn(false);
        when(properties.getPropertyIndex(anyString())).thenAnswer(c->indices.getOrDefault(c.getArgument(0),-1));
        var definition=new CustomBedrockEntityDefinition(Identifier.of("quaso:display"),properties);
        var session=mock(GeyserSession.class,RETURNS_DEEP_STUBS);
        var context=mock(EntitySpawnContext.class);
        when(context.session()).thenReturn(session);
        when(context.entityTypeDefinition()).thenReturn(mock(EntityTypeDefinition.class));
        when(context.bedrockEntityDefinition()).thenReturn(definition);
        when(context.position()).thenReturn(Vector3f.from(1,64,2));when(context.motion()).thenReturn(Vector3f.ZERO);
        when(context.geyserId()).thenReturn(123L);when(context.uuid()).thenReturn(UUID.randomUUID());
        List<BedrockPacket> packets=new ArrayList<>();
        doAnswer(c->{packets.add(c.getArgument(0));return null;}).when(session).sendUpstreamPacket(any());
        var entity=new QuasoDisplayEntity(context);entity.spawnEntity();
        var spawn=(AddEntityPacket)packets.getFirst();
        check(spawn.getIdentifier().equals("quaso:display")&&spawn.getPosition().equals(Vector3f.from(1,64,2)),"Una entidad en la posición Java exacta");
        check(!spawn.getMetadata().getFlag(EntityFlag.HAS_GRAVITY)&&!spawn.getMetadata().getFlag(EntityFlag.HAS_COLLISION),"No tapa la hitbox de interacción");
        entity.translation(new ObjectEntityMetadata<>(11,MetadataTypes.VECTOR3,Vector3f.from(.37,.70,.80)));
        entity.displayScale(new ObjectEntityMetadata<>(12,MetadataTypes.VECTOR3,Vector3f.from(-1,1,-.5)));
        entity.left(new ObjectEntityMetadata<>(13,MetadataTypes.QUATERNION,Quaternionf.from(0,.5f,0,.8660254f)));
        entity.duration(new IntEntityMetadata(9,MetadataTypes.INT,2));
        entity.integer("display_model",honey);entity.updateBedrockMetadata();
        var update=(SetEntityDataPacket)packets.getLast();
        Map<Integer,Float> floats=new HashMap<>();update.getProperties().getFloatProperties().forEach(p->floats.put(p.getIndex(),p.getValue()));
        check(Math.abs(floats.get(indices.get("quaso:t_y"))-.7f)<1e-6,"Traslación de la tumba");
        check(floats.get(indices.get("quaso:s_x"))==-1 && floats.get(indices.get("quaso:s_z"))==-.5f,"Escala no uniforme y negativa");
        check(Math.abs(floats.get(indices.get("quaso:l_y"))-60)<.001,"Rotación transmitida");
        check(floats.get(indices.get("quaso:duration"))==.1f,"Interpolación de dos ticks");
        int count=packets.size();entity.updateBedrockMetadata();check(packets.size()==count,"No reenvía un display inmóvil");
        entity.despawnEntity();check(packets.getLast() instanceof RemoveEntityPacket remove&&remove.getUniqueEntityId()==123,"Se elimina al descargar/destruir la entidad");
        System.out.println("OK: tumbas, panal, estados de bloques, cuaterniones, escala negativa, propiedades y ciclo spawn/remove de displays.");
    }
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
}
