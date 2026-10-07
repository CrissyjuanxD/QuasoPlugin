package ShopSystem;

import Managers.ItemManager;
import items.tienda.ItemsTienda;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CatalogoTiendaTest {

    private static boolean existe(String clave) {
        if (ItemManager.claves().contains(clave)) return true;
        try {
            Material.valueOf(clave.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Test
    void everyShopFitsInTenTradesAndOnlySellsItemsThatExist() {
        Set<String> ids = new HashSet<>();
        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) {
            assertTrue(ids.add(tienda.id()));
            assertTrue(tienda.ofertas().size() <= 10, tienda.id());
            for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
                assertTrue(existe(oferta.producto()), oferta.producto());
                assertTrue(existe(oferta.pago()), oferta.pago());
                assertTrue(oferta.seccion() >= 1 && oferta.seccion() <= CatalogoTienda.DIAS.length);
            }
        }
    }

    @Test
    void armorsEndToolsAndNewMineralsAreNeverSold() {
        List<String> prohibidos = List.of("casco_warden", "peto_warden", "pantalon_warden", "bota_warden",
                "mejora_casco_warden", "mejora_peto_warden", "mejora_pantalon_warden", "mejora_bota_warden",
                "mineral_crudo_cian", "mineral_crudo_verde", "mineral_crudo_morado", "mineral_crudo_gris",
                "fragmento_profundo_cian", "fragmento_profundo_verde", "fragmento_profundo_morado", "fragmento_profundo_gris",
                "lingote_profundo", "cristal_celestita", "lingote_celestita", "plantilla_celestita", "enderking_pearl",
                "espada_celestita", "hacha_celestita", "lanza_celestita", "pico_celestita", "pala_celestita",
                "azada_celestita", "peto_warden_alado");
        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) {
            for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
                assertFalse(prohibidos.contains(oferta.producto()), oferta.producto());
            }
        }
    }

    @Test
    void eachSectionRaisesWhatWasAlreadyThereFifteenPercentUpTo128() {
        CatalogoTienda.Oferta mochila = new CatalogoTienda.Oferta("mochila_nivel_1", 1, "dinocoins", 15, 1, false);
        assertEquals(15, CatalogoTienda.precio(mochila, 1));
        assertEquals(17, CatalogoTienda.precio(mochila, 2));
        assertEquals(20, CatalogoTienda.precio(mochila, 3));
        assertEquals(30, CatalogoTienda.precio(mochila, 6));

        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) {
            for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
                int anterior = 0;
                for (int seccion = oferta.seccion(); seccion <= CatalogoTienda.DIAS.length; seccion++) {
                    int precio = CatalogoTienda.precio(oferta, seccion);
                    assertTrue(precio <= CatalogoTienda.PRECIO_MAXIMO, oferta.producto());
                    assertTrue(precio >= anterior, oferta.producto());
                    if (oferta.fijo()) assertEquals(oferta.cantidadPago(), precio);
                    anterior = precio;
                }
            }
        }
    }

    @Test
    void theDayPicksTheLatestSectionThatAlreadyOpened() {
        assertEquals(1, CatalogoTienda.seccionDelDia(1));
        assertEquals(1, CatalogoTienda.seccionDelDia(19));
        assertEquals(2, CatalogoTienda.seccionDelDia(20));
        assertEquals(2, CatalogoTienda.seccionDelDia(29));
        assertEquals(3, CatalogoTienda.seccionDelDia(30));
        assertEquals(5, CatalogoTienda.seccionDelDia(99));
        assertEquals(6, CatalogoTienda.seccionDelDia(100));
        assertEquals(100, CatalogoTienda.diaDeLaSeccion(6));
    }

    @Test
    void theThirtyNewItemsAreSoldOnceEachInTheirOwnSection() {
        List<String> nuevos = ItemsTienda.claves();
        assertEquals(30, nuevos.size());
        assertEquals(30, new HashSet<>(nuevos).size());
        Map<String, Integer> seccion = new HashMap<>();
        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) {
            for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
                if (ItemsTienda.existe(oferta.producto())) assertNull(seccion.put(oferta.producto(), oferta.seccion()), oferta.producto());
            }
        }
        assertEquals(30, seccion.size());
        for (int i = 0; i < 10; i++) assertEquals(1, seccion.get(nuevos.get(i)), nuevos.get(i));
        for (int i = 10; i < 20; i++) assertEquals(2, seccion.get(nuevos.get(i)), nuevos.get(i));
        for (int i = 20; i < 30; i++) assertTrue(seccion.get(nuevos.get(i)) >= 3, nuevos.get(i));
    }

    @Test
    void everySectionAddsSomethingAndTheLastOneHasEverything() {
        int[] nuevos = new int[CatalogoTienda.DIAS.length + 1];
        int total = 0;
        for (CatalogoTienda.Tienda tienda : CatalogoTienda.tiendas()) {
            for (CatalogoTienda.Oferta oferta : tienda.ofertas()) {
                nuevos[oferta.seccion()]++;
                total++;
                assertTrue(CatalogoTienda.abierta(oferta, CatalogoTienda.DIAS.length));
            }
        }
        for (int seccion = 1; seccion <= CatalogoTienda.DIAS.length; seccion++) assertTrue(nuevos[seccion] > 0, "sección " + seccion);
        assertTrue(total > 100);
    }
}
