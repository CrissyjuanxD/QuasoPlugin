package Events.MissionSystem;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Mission54Test {
    private Player player;
    private PlayerInventory inventory;
    private Mission54 mission;

    @BeforeAll static void registry() { support.PaperTestRegistry.initialize(); }
    @BeforeEach void setup() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.namespace()).thenReturn("quasoplugin");
        Server server = mock(Server.class);
        PluginManager plugins = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(plugins);
        MissionUtils.init(plugin);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        mission = spy(new Mission54(plugin, mock(MissionHandler.class)));
        // Exercise the real inventory detector without sending rewards or notifications.
        doNothing().when(mission).mark(player, "arma");
    }

    private ItemStack item(String id, boolean metaPresent) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getAmount()).thenReturn(1);
        when(stack.getType()).thenReturn(Material.DIAMOND_SWORD);
        when(stack.hasItemMeta()).thenReturn(metaPresent);
        if (metaPresent) {
            ItemMeta meta = mock(ItemMeta.class);
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(stack.getItemMeta()).thenReturn(meta);
            when(meta.getPersistentDataContainer()).thenReturn(pdc);
            // This is the same persistent ID assigned by EndItems to Celestita weapons.
            when(pdc.get(items.EndItems.ITEM_KEY, PersistentDataType.STRING)).thenReturn(id);
        }
        return stack;
    }

    @Test void ordinaryItemsAndEmptySlotsDoNotFailOnRepeatedInventoryChecks() {
        ItemStack plain = item(null, false);
        ItemStack namedVanilla = item(null, true);
        when(inventory.getContents()).thenReturn(new ItemStack[]{null, plain, namedVanilla});
        for (int i = 0; i < 3; i++) assertDoesNotThrow(() -> mission.tick(player));
        verify(mission, never()).mark(player, "arma");
    }

    @ParameterizedTest
    @ValueSource(strings = {"espada_celestita", "hacha_celestita", "lanza_celestita"})
    void recognizesEveryCelestitaWeaponAmongOrdinaryItems(String id) {
        ItemStack plain = item(null, false);
        ItemStack weapon = item(id, true);
        ItemStack namedVanilla = item(null, true);
        when(inventory.getContents()).thenReturn(new ItemStack[]{plain, null, weapon, namedVanilla});
        assertDoesNotThrow(() -> mission.tick(player));
        verify(mission).mark(player, "arma");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pico_celestita", "pala_celestita", "azada_celestita", "espada_floral"})
    void unrelatedCustomItemsDoNotCompleteTheWeaponMission(String id) {
        ItemStack other = item(id, true);
        when(inventory.getContents()).thenReturn(new ItemStack[]{other});
        mission.tick(player);
        verify(mission, never()).mark(player, "arma");
    }

    @Test void craftingPredicatesAlsoRejectMissingIdsForAnyNumberOfAcceptedItems() {
        ItemStack plain = item(null, false);
        ItemStack namedVanilla = item(null, true);
        ItemStack weapon = item("espada_celestita", true);
        for (String[] ids : new String[][]{
                {"espada_celestita"},
                {"espada_celestita", "espada_floral"},
                {"espada_celestita", "hacha_celestita", "lanza_celestita"}}) {
            Predicate<ItemStack> predicate = MisionCrafteo.id(ids);
            assertFalse(predicate.test(null));
            assertFalse(predicate.test(plain));
            assertFalse(predicate.test(namedVanilla));
            assertTrue(predicate.test(weapon));
        }
    }
}
