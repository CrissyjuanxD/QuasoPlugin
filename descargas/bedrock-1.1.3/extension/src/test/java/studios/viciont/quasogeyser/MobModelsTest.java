package studios.viciont.quasogeyser;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.geysermc.geyser.api.entity.EntityData;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.property.type.GeyserIntEntityProperty;
import org.geysermc.geyser.api.entity.type.GeyserEntity;
import org.geysermc.geyser.api.event.java.ServerSpawnEntityEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.session.GeyserSession;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Reproduce nombre oculto/renombre usando la entidad Paper, sin NAME en un paquete Bedrock. */
public final class MobModelsTest {
    public static void main(String[] args) {
        try (var identifiers = mockStatic(Identifier.class, CALLS_REAL_METHODS)) {
            identifiers.when(() -> Identifier.of(anyString(), anyString()))
                .thenAnswer(c -> org.geysermc.geyser.impl.IdentifierImpl.of(c.getArgument(0), c.getArgument(1)));
            run();
        }
    }
    private static void run() {
        Identifier modelId = Identifier.of("quaso:model"), zombieId = Identifier.of("minecraft:zombie");
        MobModels models = new MobModels();
        var define = mock(GeyserDefineEntityPropertiesEvent.class);
        var property = mock(GeyserIntEntityProperty.class);
        when(define.registerIntegerProperty(any(),any(),eq(0),eq(9999),eq(0))).thenReturn(property);
        models.define(define);
        verify(define,times(8)).registerIntegerProperty(any(),eq(modelId),eq(0),eq(9999),eq(0));
        check(models.model("minecraft:zombie", "§x§f§f§b§b§c§cZombie Floral 30/30 ♥")==9301,"Hex y vida");
        check(models.model("minecraft:bee", "Mini Abeja Floral")==9314,"Mini abeja modelo 3");
        check(models.model("minecraft:bee", "Abeja Floral")==9313,"Abeja Floral modelo 2");
        check(models.model("minecraft:zombie", "Infested Zombie")==9302,"Warden infestado");
        check(models.model("minecraft:skeleton", "Zombie Floral")==0,"No mezcla especies");
        UUID uuid = UUID.randomUUID();
        var session = mock(GeyserSession.class);
        var entities = mock(EntityData.class); when(session.entities()).thenReturn(entities);
        var bedrock = mock(GeyserEntity.class); when(bedrock.geyserId()).thenReturn(42L);
        when(entities.byUuid(uuid)).thenReturn(bedrock);
        doAnswer(c -> { c.getArgument(0,Runnable.class).run(); return null; }).when(session).executeInEventLoop(any());
        var spawn = mock(ServerSpawnEntityEvent.class);
        when(spawn.connection()).thenReturn(session); when(spawn.uuid()).thenReturn(uuid);
        var definition = mock(GeyserEntityDefinition.class);
        when(definition.identifier()).thenReturn(zombieId);
        when(spawn.definition()).thenReturn(definition);
        doAnswer(c -> { c.<Consumer<GeyserEntity>>getArgument(0).accept(bedrock); return null; }).when(spawn).preSpawnConsumer(any());
        var paper = mock(Entity.class); when(paper.isValid()).thenReturn(true);
        when(paper.getCustomName()).thenReturn("§dZombie Floral");
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getEntity(uuid)).thenReturn(paper);
            models.spawn(spawn); models.tick(Set.of(session));
            verify(bedrock).updateProperty(property,9301);
            when(paper.getCustomName()).thenReturn("Zombie BloodMoon"); models.tick(Set.of(session));
            verify(bedrock).updateProperty(property,9303);
            when(paper.getCustomName()).thenReturn(null); models.tick(Set.of(session));
            verify(bedrock).updateProperty(property,0);
            when(entities.byUuid(uuid)).thenReturn(null); models.tick(Set.of(session));
            clearInvocations(bedrock);
            when(entities.byUuid(uuid)).thenReturn(bedrock); models.tick(Set.of(session));
            verifyNoInteractions(bedrock); // Deja de enviar al descargar la entidad del cliente.
            models.spawn(spawn); clearInvocations(bedrock); models.tick(Set.of()); models.tick(Set.of(session));
            verifyNoInteractions(bedrock); // Cierre de sesión limpia su seguimiento.
        }
        System.out.println("OK: 8 propiedades, nombres ocultos, 3 variantes/renombre, vanilla y limpieza de sesión.");
    }
    static void check(boolean b,String reason) { if (!b) throw new AssertionError(reason); }
}
