package studios.viciont.quasogeyser;

import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

/** One Bedrock entity per Java display: no extra Java entities or ticking world scans. */
final class QuasoDisplayEntity extends Entity {
    private int revision;
    QuasoDisplayEntity(EntitySpawnContext context) { super(context); }
    @Override protected void initializeMetadata() {
        super.initializeMetadata();
        setFlag(EntityFlag.HAS_GRAVITY,false);
        setFlag(EntityFlag.HAS_COLLISION,false);
        setFlag(EntityFlag.NO_AI,true);
    }
    void number(String name,float value) {
        if(!Float.isFinite(value)) value=0;
        propertyManager.addProperty(new FloatProperty(Identifier.of("quaso:"+name),1000000,-1000000,0f),Math.max(-1000000,Math.min(1000000,value)));
    }
    void integer(String name,int value) {
        propertyManager.addProperty(new IntProperty(Identifier.of("quaso:"+name),1000000,0,0),value);
    }
    void vector(String prefix,Vector3f value) {
        number(prefix+"_x",value.getX());number(prefix+"_y",value.getY());number(prefix+"_z",value.getZ());
    }
    void translation(EntityMetadata<Vector3f,?> m) { vector("t",m.getValue()); }
    void displayScale(EntityMetadata<Vector3f,?> m) { vector("s",m.getValue()); }
    void left(EntityMetadata<Quaternionf,?> m) { vector("l",euler(m.getValue())); }
    void right(EntityMetadata<Quaternionf,?> m) { vector("r",euler(m.getValue())); }
    void duration(IntEntityMetadata m) { number("duration",Math.max(0,Math.min(100,m.getPrimitiveValue()/20f))); }
    void brightness(IntEntityMetadata m) { integer("bright",m.getPrimitiveValue()==-1?0:1); }
    void block(IntEntityMetadata m) {
        integer("item",0);
        String state=BlockState.of(m.getPrimitiveValue()).toString();
        integer("translucent",state.startsWith("minecraft:purple_stained_glass")?1:0);
        integer("display_model",DisplayBridge.blockModel(state));
    }
    void item(EntityMetadata<ItemStack,?> m) {
        integer("item",1);
        integer("translucent",0);
        ItemStack stack=m.getValue();int model=0;
        if(stack!=null && stack.getAmount()>0) {
            var mapping=session.getItemMappings().getMapping(stack);
            String name=mapping.getJavaItem().javaKey().asString();
            var components=stack.getDataComponentsPatch();
            if(name.equals("minecraft:player_head") && components!=null) {
                var profile=components.get(DataComponentTypes.PROFILE);
                if(profile!=null && profile.getProfile()!=null) {
                    var textures=profile.getProfile().getProperty("textures");
                    if(textures!=null)model=DisplayBridge.profileModel(textures.getValue());
                }
            } else model=DisplayBridge.itemModel(name);
        }
        integer("display_model",model);
    }
    @Override public void updateBedrockMetadata() {
        if(propertyManager.hasProperties())integer("revision",revision=(revision+1)%1000001);
        super.updateBedrockMetadata();
        updateBedrockEntityProperties();
    }
    /** ZYX decomposition; left and right rotations stay separate around nonuniform scaling. */
    static Vector3f euler(Quaternionf input) {
        double norm=Math.sqrt(input.getX()*input.getX()+input.getY()*input.getY()+input.getZ()*input.getZ()+input.getW()*input.getW());
        if(!Double.isFinite(norm)||norm<1e-8)return Vector3f.ZERO;
        double x=input.getX()/norm,y=input.getY()/norm,z=input.getZ()/norm,w=input.getW()/norm;
        double ry=Math.asin(Math.max(-1,Math.min(1,2*(w*y-z*x))));
        double rx,rz;
        if(Math.abs(Math.cos(ry))>1e-6) {
            rx=Math.atan2(2*(w*x+y*z),1-2*(x*x+y*y));
            rz=Math.atan2(2*(w*z+x*y),1-2*(y*y+z*z));
        } else {
            rx=0;rz=Math.atan2(-2*(x*y-w*z),1-2*(x*x+z*z));
        }
        return Vector3f.from(Math.toDegrees(rx),Math.toDegrees(ry),Math.toDegrees(rz));
    }
}
