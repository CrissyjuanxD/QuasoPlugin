package Dificultades;

import Armors.WardenArmor;
import Dificultades.CustomMobs.InfestedCaveSpider;
import Dificultades.CustomMobs.InfestedCreeper;
import Dificultades.CustomMobs.InfestedGhast;
import Dificultades.CustomMobs.InfestedSkeleton;
import Dificultades.CustomMobs.WardenZombie;
import InfestedCaves.WardenBiome;
import InfestedCaves.WardenBiomeMap;
import org.bukkit.event.entity.CreatureSpawnEvent;
import imp.crissyjuanxd.QuasoPlugin;
import items.InfestedSoulsItems;
import items.WardenCaveItems;
import items.WardenUpgrades;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TwoChanges implements Listener, Change {

    private static final int BLAST_TIME = 4800;
    private static final double MINING_ZOMBIE_CHANCE = 0.15;
    private static final int GHAST_REPLACE_CHANCE = 60;

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private boolean isApplied = false;

    private final InfestedSkeleton infestedSkeleton;
    private final InfestedCaveSpider infestedCaveSpider;
    private final InfestedGhast infestedGhast;
    private final InfestedCreeper infestedCreeper;
    private final WardenZombie wardenZombie;

    private final WardenUpgrades upgrades;
    private final WardenArmor armor;
    private final InfestedSoulsItems souls;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    // Mientras no es null, addRecipe solo junta las recetas (para la web) en vez de registrarlas
    private List<Recipe> recolectando;

    public TwoChanges(JavaPlugin plugin) {
        this.plugin = plugin;
        this.infestedSkeleton = new InfestedSkeleton(plugin);
        this.infestedCaveSpider = new InfestedCaveSpider(plugin);
        this.infestedGhast = new InfestedGhast(plugin);
        this.infestedCreeper = new InfestedCreeper(plugin);
        this.wardenZombie = new WardenZombie(plugin);
        this.upgrades = new WardenUpgrades(plugin);
        this.armor = new WardenArmor(plugin);
        this.souls = new InfestedSoulsItems(plugin);
    }

    @Override
    public String id() {
        return "dos";
    }

    @Override
    public String description() {
        return "Warden Cave: mobs infestados por bioma, Warden Zombie y recetas de la armadura";
    }

    @Override
    public boolean isApplied() {
        return isApplied;
    }

    // Abre la segunda etapa: los mobs de la Warden Cave y las recetas de la armadura
    @Override
    public void apply() {
        if (isApplied) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        infestedSkeleton.apply();
        infestedCaveSpider.apply();
        infestedGhast.apply();
        infestedCreeper.apply();
        wardenZombie.apply();
        registerRecipes();
        isApplied = true;
    }

    // Deshace todo lo de apply(): saca las recetas y borra los mobs de la etapa
    @Override
    public void revert() {
        if (!isApplied) return;
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key, true);
        }
        recipeKeys.clear();
        infestedSkeleton.revert();
        infestedCaveSpider.revert();
        infestedGhast.revert();
        infestedCreeper.revert();
        wardenZombie.revert();
        HandlerList.unregisterAll(this);
        isApplied = false;
    }

    // Recetas de la Warden Cave: cada mineral crudo en el alto horno da el fragmento de su bioma, el lingote acepta
    // fragmentos de cualquier bioma y cada mejora pide los de su bioma. Los items vanilla van con exactChoice
    // para que no entren mochilas o almas, que también son echo shards
    private void registerRecipes() {
        RecipeChoice anyFragment = RecipeChoice.exactChoice(WardenCaveItems.allFragments());
        RecipeChoice ingot = RecipeChoice.exactChoice(WardenCaveItems.createDeepIngot());
        RecipeChoice energy = RecipeChoice.exactChoice(WardenCaveItems.createWardenEnergy());
        RecipeChoice gold = RecipeChoice.exactChoice(new ItemStack(Material.GOLD_INGOT));
        RecipeChoice echo = RecipeChoice.exactChoice(new ItemStack(Material.ECHO_SHARD));
        RecipeChoice template = RecipeChoice.exactChoice(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));

        for (WardenCaveItems.Variant variant : WardenCaveItems.Variant.values()) {
            addRecipe(new BlastingRecipe(key(variant.fragmentId()), WardenCaveItems.createFragment(variant),
                    RecipeChoice.exactChoice(WardenCaveItems.createRawOre(variant)), 1.0f, BLAST_TIME));
        }

        ShapedRecipe deepIngot = new ShapedRecipe(key("lingote_profundo"), WardenCaveItems.createDeepIngot());
        deepIngot.shape("FGF", "GEG", "FGF");
        deepIngot.setIngredient('F', anyFragment);
        deepIngot.setIngredient('G', gold);
        deepIngot.setIngredient('E', echo);
        addRecipe(deepIngot);

        ShapedRecipe wardenEnergy = new ShapedRecipe(key("energia_warden"), WardenCaveItems.createWardenEnergy());
        wardenEnergy.shape("ELE", "LEL", "ELE");
        wardenEnergy.setIngredient('E', echo);
        wardenEnergy.setIngredient('L', ingot);
        addRecipe(wardenEnergy);

        upgradeRecipe("mejora_casco_warden", upgrades.createHelmetWardenUpgrade(),
                souls.createInfestedSkeletonSoul(), WardenCaveItems.Variant.CIAN, ingot, energy, template);
        upgradeRecipe("mejora_botas_warden", upgrades.createBootsWardenUpgrade(),
                souls.createInfestedCaveSpiderSoul(), WardenCaveItems.Variant.VERDE, ingot, energy, template);
        upgradeRecipe("mejora_peto_warden", upgrades.createChestplateWardenUpgrade(),
                souls.createInfestedGhastSoul(), WardenCaveItems.Variant.MORADO, ingot, energy, template);
        upgradeRecipe("mejora_pantalon_warden", upgrades.createLeggingsWardenUpgrade(),
                souls.createInfestedCreeperSoul(), WardenCaveItems.Variant.GRIS, ingot, energy, template);

        armorRecipe("casco_warden", armor.createWardenHelmet(), upgrades.createHelmetWardenUpgrade(),
                Material.NETHERITE_HELMET, ingot);
        armorRecipe("peto_warden", armor.createWardenChestplate(), upgrades.createChestplateWardenUpgrade(),
                Material.NETHERITE_CHESTPLATE, ingot);
        armorRecipe("pantalon_warden", armor.createWardenLeggings(), upgrades.createLeggingsWardenUpgrade(),
                Material.NETHERITE_LEGGINGS, ingot);
        armorRecipe("botas_warden", armor.createWardenBoots(), upgrades.createBootsWardenUpgrade(),
                Material.NETHERITE_BOOTS, ingot);
    }

    // Mejora de una pieza: 4 fragmentos y el alma de su bioma, 2 lingotes, 1 energía y la plantilla de netherite
    private void upgradeRecipe(String id, ItemStack upgrade, ItemStack soul, WardenCaveItems.Variant variant,
                               RecipeChoice ingot, RecipeChoice energy, RecipeChoice template) {
        ShapedRecipe recipe = new ShapedRecipe(key(id), upgrade);
        recipe.shape("FAF", "LTL", "FWF");
        recipe.setIngredient('F', RecipeChoice.exactChoice(WardenCaveItems.createFragment(variant)));
        recipe.setIngredient('A', RecipeChoice.exactChoice(soul));
        recipe.setIngredient('L', ingot);
        recipe.setIngredient('T', template);
        recipe.setIngredient('W', energy);
        addRecipe(recipe);
    }

    // En la mesa de herrería: mejora + pieza de netherite + 1 lingote. Se quedan los encantamientos de la pieza
    private void armorRecipe(String id, ItemStack result, ItemStack upgrade, Material base, RecipeChoice ingot) {
        addRecipe(new SmithingTransformRecipe(key(id), result,
                RecipeChoice.exactChoice(upgrade), new RecipeChoice.MaterialChoice(base), ingot));
    }

    @Override
    public List<Recipe> recetas() {
        List<Recipe> lista = new ArrayList<>();
        recolectando = lista;
        try {
            registerRecipes();
        } finally {
            recolectando = null;
        }
        return lista;
    }

    private void addRecipe(Recipe recipe) {
        if (recolectando != null) {
            recolectando.add(recipe);
            return;
        }
        NamespacedKey key = ((Keyed) recipe).getKey();
        if (Bukkit.getRecipe(key) == null) Bukkit.addRecipe(recipe, true);
        recipeKeys.add(key);
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }

    // Partículas de sculk cada vez que el alto horno termina un Fragmento Profundo (4 minutos por mineral)
    @EventHandler(ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed)) return;
        NamespacedKey key = keyed.getKey();
        if (!key.getNamespace().equals(plugin.getName().toLowerCase(java.util.Locale.ROOT)) || !key.getKey().startsWith("fragmento_profundo_")) return;
        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        loc.getWorld().playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 0.7f);
        loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 20, 0.3, 0.2, 0.3, 0.05);
    }

    // El juego spawnea los mobs base de cada bioma con su mobcap (los pone el datapack) y acá se convierten
    // ahí mismo en su versión infestada: Cian esqueleto, Verde araña, Morado ghast, Gris creeper y el zombie en todos.
    // No se cancela el spawn: antes se cancelaba y se creaba otro mob, la mobcap no lo contaba y salían cientos por tick
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        if (!event.getLocation().getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;

        switch (event.getEntity()) {
            case Skeleton skeleton -> infestedSkeleton.infest(skeleton);
            case CaveSpider spider -> infestedCaveSpider.infest(spider);
            case Ghast ghast -> infestedGhast.infest(ghast);
            case Creeper creeper -> infestedCreeper.infest(creeper);
            case Zombie zombie when zombie.getType() == EntityType.ZOMBIE -> {
                wardenZombie.infest(zombie);
                if (inAbyss(zombie.getLocation()) && random.nextInt(100) < GHAST_REPLACE_CHANCE) {
                    Bukkit.getScheduler().runTask(plugin, () -> replaceWithGhast(zombie));
                }
            }
            default -> { }
        }
    }

    private boolean inAbyss(Location loc) {
        return WardenBiomeMap.forSeed(loc.getWorld().getSeed()).biomeAt(loc.getBlockX(), loc.getBlockZ())
                == WardenBiome.ABISMO_FLOTANTE;
    }

    // El ghast vanilla casi nunca pasa su chequeo de spawn (1 de cada 20), así que en el Abismo salían pocos. Parte de
    // los zombies que spawnean ahí se cambian por un Infested Ghast unos bloques más arriba, uno por uno, así la
    // mobcap sigue contando igual
    private void replaceWithGhast(Zombie zombie) {
        if (!zombie.isValid() || zombie.isDead()) return;
        Location base = zombie.getLocation();
        for (int up = 6; up <= 18; up += 3) {
            Location spot = base.clone().add(0, up, 0);
            if (!openAir(spot)) continue;
            zombie.remove();
            Ghast ghast = spot.getWorld().spawn(spot, Ghast.class);
            infestedGhast.infest(ghast);
            return;
        }
    }

    // Un cubo de 5x5x5 de aire (lo que necesita un ghast)
    private boolean openAir(Location center) {
        for (int dx = -2; dx <= 2; dx += 2) {
            for (int dz = -2; dz <= 2; dz += 2) {
                for (int dy = -2; dy <= 2; dy += 2) {
                    if (!center.clone().add(dx, dy, dz).getBlock().isEmpty()) return false;
                }
            }
        }
        return true;
    }

    // Al minar un Mineral Profundo hay 15% de que salga un Warden Zombie al lado
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (WardenBiome.fromOre(block.getType()) == null) return;
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        if (random.nextDouble() >= MINING_ZOMBIE_CHANCE) return;

        Location playerLoc = event.getPlayer().getLocation();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Location spot = block.getLocation().add(0.5, 0, 0.5);
            if (!hasRoom(spot)) spot = behind(playerLoc);
            wardenZombie.spawnWardenZombie(spot);
            spot.getWorld().playSound(spot, Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.4f);
        });
    }

    private boolean hasRoom(Location loc) {
        Block feet = loc.getBlock();
        return feet.isPassable() && feet.getRelative(BlockFace.UP).isPassable()
                && feet.getRelative(BlockFace.DOWN).getType().isSolid();
    }

    private Location behind(Location playerLoc) {
        Vector back = playerLoc.getDirection().setY(0);
        if (back.lengthSquared() < 1.0E-4) back = new Vector(1, 0, 0);
        Location loc = playerLoc.clone().add(back.normalize().multiply(-1.5));
        return hasRoom(loc) ? loc : playerLoc;
    }
}
