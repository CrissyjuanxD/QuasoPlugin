package items.IceBow;

import Managers.ItemManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IceBowTest {
    private JavaPlugin plugin;
    private IceBowLogic logic;
    private NamespacedKey bowKey, playerArrowKey, mobArrowKey;

    @BeforeAll static void registry() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() {
        plugin = mock(JavaPlugin.class);
        when(plugin.namespace()).thenReturn("quasoplugin");
        logic = new IceBowLogic(plugin);
        bowKey = new NamespacedKey(plugin, "ice_bow");
        playerArrowKey = new NamespacedKey(plugin, "player_ice_arrow");
        mobArrowKey = new NamespacedKey(plugin, "iceologer");
    }

    private ItemStack bow(boolean special) {
        ItemStack bow = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(bow.getType()).thenReturn(Material.BOW);
        when(bow.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.has(bowKey, PersistentDataType.BYTE)).thenReturn(special);
        return bow;
    }

    @Test void successiveShotsAreBothIceArrowsWithoutWaiting() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));
        when(player.getEyeLocation()).thenReturn(new Location(world, 0, 1, 0));
        ItemStack bow = bow(true);
        for (int i = 0; i < 2; i++) {
            EntityShootBowEvent event = mock(EntityShootBowEvent.class);
            Arrow arrow = mock(Arrow.class);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(event.getEntity()).thenReturn(player);
            when(event.getBow()).thenReturn(bow);
            when(event.getProjectile()).thenReturn(arrow);
            when(arrow.getPersistentDataContainer()).thenReturn(data);
            logic.onPlayerShootBow(event);
            verify(data).set(playerArrowKey, PersistentDataType.BYTE, (byte) 1);
            verify(event, never()).setCancelled(anyBoolean());
        }
    }

    @Test void ordinaryBowsAreNotTagged() {
        EntityShootBowEvent event = mock(EntityShootBowEvent.class);
        when(event.getEntity()).thenReturn(mock(Player.class));
        ItemStack ordinary = bow(false);
        when(event.getBow()).thenReturn(ordinary);
        logic.onPlayerShootBow(event);
        verify(event, never()).getProjectile();
        verify(event, never()).setCancelled(anyBoolean());
    }

    @Test void cancelledShotDoesNotTagTheArrow() {
        EntityShootBowEvent event = mock(EntityShootBowEvent.class);
        when(event.isCancelled()).thenReturn(true);
        logic.onPlayerShootBow(event);
        verify(event, never()).getProjectile();
    }

    @Test void bothArrowOriginsApplyTheSameOriginalEffect() {
        for (NamespacedKey key : List.of(playerArrowKey, mobArrowKey)) {
            Arrow arrow = mock(Arrow.class);
            PersistentDataContainer data = mock(PersistentDataContainer.class);
            when(arrow.getPersistentDataContainer()).thenReturn(data);
            when(data.has(key, PersistentDataType.BYTE)).thenReturn(true);
            LivingEntity target = mock(LivingEntity.class);
            World world = mock(World.class);
            when(target.getWorld()).thenReturn(world);
            when(target.getLocation()).thenAnswer(call -> new Location(world, 0, 0, 0));
            when(target.getFreezeTicks()).thenReturn(20);
            EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
            when(event.getEntity()).thenReturn(target);
            logic.handleArrowDamage(event, arrow, mobArrowKey);
            verify(target).setFreezeTicks(180);
            verify(target).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
            verify(target).addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 80, 0));
        }
    }

    @Test void cancelledDamageDoesNotFreezeProtectedTargets() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        Arrow arrow = mock(Arrow.class);
        when(event.isCancelled()).thenReturn(true);
        logic.handleArrowDamage(event, arrow, mobArrowKey);
        verifyNoInteractions(arrow);
        verify(event, never()).getEntity();
    }

    @Test void factoryAndCatalogExposeOneBowWithoutCooldownLore() {
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        try (MockedConstruction<ItemStack> ignored = mockConstruction(ItemStack.class,
                (stack, context) -> when(stack.getItemMeta()).thenReturn(meta))) {
            new IceBowItem(plugin).createIceBow();
            verify(meta).setItemModel(NamespacedKey.minecraft("arco_hielo"));
            verify(data).set(bowKey, PersistentDataType.BYTE, (byte) 1);
            ArgumentCaptor<List<String>> lore = ArgumentCaptor.forClass(List.class);
            verify(meta).setLore(lore.capture());
            assertTrue(lore.getValue().stream().anyMatch(line -> line.contains("8 segundos")));
            assertFalse(lore.getValue().stream().anyMatch(line -> line.contains("Cooldown") || line.contains("Versión para jugadores")));
        }
        assertEquals(1, ItemManager.claves().stream().filter(id -> id.startsWith("arco_hielo")).count());
    }
}
