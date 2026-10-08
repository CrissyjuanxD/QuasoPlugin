package Web;

import Dificultades.Change;
import Events.MissionSystem.BaseMission;
import Events.MissionSystem.Mission;
import Events.MissionSystem.MissionHandler;
import Events.MissionSystem.MissionObjective;
import Events.MissionSystem.TipoMision;
import Habilidades.HabilidadesType;
import Handlers.ChangesHandler;
import Managers.ItemManager;
import Managers.MobManager;
import Trabajos.Trabajo;
import org.bukkit.Keyed;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmokingRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

// Todo lo del juego que la web muestra y que sale del plugin: items, misiones, crafteos, trabajos, habilidades y mobs.
// Se arma en el hilo principal (crea ItemStacks) con los mismos datos que usa el servidor, así cualquier cambio en el
// plugin llega solo a la web
final class Catalogo {

    // El ícono de cada trabajo en la web (el del menú es un papel con textura del resource pack)
    private static final Map<String, String> ITEM_TRABAJO = Map.of(
            "guerrero", "iron_sword", "mineria", "iron_pickaxe", "lenador", "iron_axe",
            "constructor", "bricks", "granjero", "wheat", "pescador", "fishing_rod");

    private Catalogo() {}

    static Map<String, Object> armar(ItemManager itemManager, MissionHandler missionHandler, ChangesHandler changesHandler,
                                     String version, Logger log) {
        ItemsWeb web = new ItemsWeb();
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("version", 1);
        raiz.put("plugin", version);

        // Items custom: los mismos que da /giveqp
        Map<String, Object> items = new LinkedHashMap<>();
        if (itemManager != null) {
            for (String id : ItemManager.claves()) {
                try {
                    ItemStack item = itemManager.getItem(id, 1, null);
                    if (item == null) continue;
                    web.registrar(id, item);
                    items.put(id, web.item(item, true));
                } catch (Throwable error) {
                    log.warning("[Web] No se pudo leer el item " + id + ": " + error);
                }
            }
        }
        raiz.put("items", items);

        // Misiones con sus objetivos y los dos grupos de la recompensa
        List<Object> misiones = new ArrayList<>();
        if (missionHandler != null) {
            for (Mission mission : missionHandler.getMissions().values()) {
                if (!(mission instanceof BaseMission base)) continue;
                try {
                    misiones.add(mision(base, web));
                } catch (Throwable error) {
                    log.warning("[Web] No se pudo leer la misión " + mission.getMissionNumber() + ": " + error);
                }
            }
        }
        raiz.put("misiones", misiones);

        // Crafteos de cada cambio (uno, extra, dos, tres) sin registrarlos
        List<Object> recetas = new ArrayList<>();
        if (changesHandler != null) {
            for (Change change : changesHandler.getChanges()) {
                List<Recipe> lista;
                try {
                    lista = change.recetas();
                } catch (Throwable error) {
                    log.warning("[Web] No se pudieron leer las recetas del cambio " + change.id() + ": " + error);
                    continue;
                }
                for (Recipe recipe : lista) {
                    Map<String, Object> receta = receta(recipe, change.id(), web);
                    if (receta != null) recetas.add(receta);
                }
            }
        }
        raiz.put("recetas", recetas);

        List<Object> trabajos = new ArrayList<>();
        for (Trabajo trabajo : Trabajo.values()) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("id", trabajo.id());
            t.put("nombre", trabajo.nombre());
            t.put("icono", trabajo.icono());
            t.put("color", trabajo.color());
            t.put("item", ITEM_TRABAJO.getOrDefault(trabajo.id(), "paper"));
            t.put("descripcion", String.join(" ", trabajo.descripcion()));
            t.put("fuentes", trabajo.fuentes());
            trabajos.add(t);
        }
        raiz.put("trabajos", trabajos);

        List<Object> habilidades = new ArrayList<>();
        for (HabilidadesType tipo : HabilidadesType.values()) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("id", tipo.name().toLowerCase(Locale.ROOT));
            h.put("nombre", tipo.getDisplayName());
            List<String> niveles = new ArrayList<>();
            for (int nivel = 1; nivel <= HabilidadesType.NIVELES; nivel++) niveles.add(tipo.descripcion(nivel));
            h.put("niveles", niveles);
            habilidades.add(h);
        }
        raiz.put("habilidades", habilidades);

        List<Object> costos = new ArrayList<>();
        for (int nivel = 1; nivel <= HabilidadesType.NIVELES; nivel++) {
            HabilidadesType.Costo costo = HabilidadesType.costo(nivel);
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("xp", costo.xp());
            c.put("bloque", ItemsWeb.clave(costo.bloque()));
            c.put("cantidad", costo.cantidad());
            c.put("nombre", costo.nombre().toLowerCase(Locale.ROOT));
            c.put("dinocoins", costo.dinocoins());
            costos.add(c);
        }
        raiz.put("costosHabilidad", costos);

        List<Object> mobs = new ArrayList<>();
        for (MobManager.InfoMob info : MobManager.infoMobs()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", info.id());
            m.put("nombre", info.nombre());
            m.put("color", info.color());
            m.put("etapa", info.etapa());
            m.put("vida", info.vida());
            m.put("donde", info.donde());
            m.put("texto", info.texto());
            m.put("huevo", info.huevo());
            mobs.add(m);
        }
        raiz.put("mobs", mobs);
        return raiz;
    }

    private static Map<String, Object> mision(BaseMission mission, ItemsWeb web) {
        int n = mission.getMissionNumber();
        int padre = mission.getParentMission();
        TipoMision tipo = TipoMision.de(n, padre);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("n", n);
        m.put("tipo", tipo.name().toLowerCase(Locale.ROOT));
        m.put("nombre", mission.getName());
        m.put("descripcion", mission.getDescription() == null ? "" : mission.getDescription().replaceAll("\\s*\\n\\s*", " ").trim());
        m.put("dificultad", mission.getDifficulty().name().toLowerCase(Locale.ROOT));
        m.put("dinocoins", mission.getCoins());
        m.put("dia", tipo == TipoMision.NORMAL ? n : tipo == TipoMision.EXTRA ? padre : 0);
        m.put("padre", padre);
        List<Object> objetivos = new ArrayList<>();
        for (MissionObjective objetivo : mission.getObjectives()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("texto", objetivo.label());
            o.put("meta", objetivo.target());
            o.put("formato", objetivo.format().name().toLowerCase(Locale.ROOT));
            objetivos.add(o);
        }
        m.put("objetivos", objetivos);
        List<Object> recompensas = new ArrayList<>();
        for (List<ItemStack> grupo : mission.itemsRecompensa()) {
            List<Object> items = new ArrayList<>();
            for (ItemStack item : grupo) {
                Map<String, Object> ref = web.ref(item);
                if (ref != null) items.add(ref);
            }
            recompensas.add(items);
        }
        m.put("recompensas", recompensas);
        return m;
    }

    private static Map<String, Object> receta(Recipe recipe, String etapa, ItemsWeb web) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", recipe instanceof Keyed keyed ? keyed.getKey().getKey() : "receta");
        r.put("etapa", etapa);
        r.put("resultado", web.ref(recipe.getResult()));
        if (recipe instanceof ShapedRecipe shaped) {
            r.put("tipo", "crafteo");
            r.put("forma", List.of(shaped.getShape()));
            Map<String, Object> ingredientes = new LinkedHashMap<>();
            for (Map.Entry<Character, RecipeChoice> entry : shaped.getChoiceMap().entrySet()) {
                Map<String, Object> eleccion = web.eleccion(entry.getValue());
                if (eleccion != null) ingredientes.put(String.valueOf(entry.getKey()), eleccion);
            }
            r.put("ingredientes", ingredientes);
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            r.put("tipo", "crafteo_libre");
            List<Object> lista = new ArrayList<>();
            for (RecipeChoice choice : shapeless.getChoiceList()) lista.add(web.eleccion(choice));
            r.put("lista", lista);
        } else if (recipe instanceof CookingRecipe<?> cooking) {
            r.put("tipo", recipe instanceof BlastingRecipe ? "alto_horno" : recipe instanceof SmokingRecipe ? "ahumador" : "horno");
            r.put("entrada", web.eleccion(cooking.getInputChoice()));
            r.put("segundos", cooking.getCookingTime() / 20.0);
        } else if (recipe instanceof SmithingTransformRecipe smithing) {
            r.put("tipo", "herreria");
            r.put("plantilla", web.eleccion(smithing.getTemplate()));
            r.put("base", web.eleccion(smithing.getBase()));
            r.put("adicion", web.eleccion(smithing.getAddition()));
        } else {
            return null;
        }
        return r;
    }
}
