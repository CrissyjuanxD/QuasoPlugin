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
import items.WardenGun;
import items.ArcoSculk;
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
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
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

    private static final int BLAST_TIME = WardenCaveItems.MINUTOS_FUNDICION * 60 * 20;
    private static final double MINING_ZOMBIE_CHANCE = 0.15;
    private static final double TISSUE_CHANCE = 0.4;
    private static final int GHAST_REPLACE_CHANCE = 25;
    // Si ya hay estos ghasts cerca no se cambian más zombies (en el cielo abierto del Abismo se veían demasiados)
    private static final int GHAST_NEARBY_MAX = 3;
    private static final double GHAST_NEARBY_RADIUS = 48;

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
    private Bosses.UltraWardenAltar ultraAltar;
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
        ultraAltar = new Bosses.UltraWardenAltar(plugin);
        Bukkit.getPluginManager().registerEvents(ultraAltar, plugin);
        ultraAltar.restoreLoaded();
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
        if (ultraAltar != null) HandlerList.unregisterAll(ultraAltar);
        ultraAltar = null;
        HandlerList.unregisterAll(this);
        isApplied = false;
    }

    // Recetas de la Warden Cave: cada mineral crudo en el alto horno da el fragmento de su bioma, con 8 fragmentos de un
    // bioma sale su Lingote Profundo y cada pieza pide 5 lingotes de su bioma (40 minerales). La Energía usa materiales
    // generales (fragmentos de cualquier bioma, Tejido Sculk y echo shard) y el Núcleo Sónico del Ultra Warden va en
    // cada mejora. Los items vanilla van con exactChoice para que no entren mochilas o almas, que también son echo shards
    private void registerRecipes() {
        RecipeChoice anyFragment = RecipeChoice.exactChoice(WardenCaveItems.allFragments());
        RecipeChoice energy = RecipeChoice.exactChoice(WardenCaveItems.createWardenEnergy());
        RecipeChoice tissue = RecipeChoice.exactChoice(WardenCaveItems.createSculkTissue(1));
        RecipeChoice core = RecipeChoice.exactChoice(WardenCaveItems.createSonicCore(1));
        RecipeChoice gold = RecipeChoice.exactChoice(new ItemStack(Material.GOLD_INGOT));
        RecipeChoice echo = RecipeChoice.exactChoice(new ItemStack(Material.ECHO_SHARD));
        RecipeChoice template = RecipeChoice.exactChoice(new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));

        for (WardenCaveItems.Variant variant : WardenCaveItems.Variant.values()) {
            addRecipe(new BlastingRecipe(key(variant.fragmentId()), WardenCaveItems.createFragment(variant),
                    RecipeChoice.exactChoice(WardenCaveItems.createRawOre(variant), WardenCaveItems.createOldRawOre(variant)),
                    1.0f, BLAST_TIME));

            ShapedRecipe ingot = new ShapedRecipe(key(variant.ingotId()), WardenCaveItems.createColoredIngot(variant));
            ingot.shape("FFF", "FOF", "FFF");
            ingot.setIngredient('F', RecipeChoice.exactChoice(WardenCaveItems.createFragment(variant)));
            ingot.setIngredient('O', gold);
            addRecipe(ingot);
        }

        ShapedRecipe wardenEnergy = new ShapedRecipe(key("energia_warden"), WardenCaveItems.createWardenEnergy());
        wardenEnergy.shape("FTF", "TET", "FTF");
        wardenEnergy.setIngredient('F', anyFragment);
        wardenEnergy.setIngredient('T', tissue);
        wardenEnergy.setIngredient('E', echo);
        addRecipe(wardenEnergy);

        upgradeRecipe("mejora_casco_warden", upgrades.createHelmetWardenUpgrade(),
                souls.createInfestedSkeletonSoul(), WardenCaveItems.Variant.CIAN, energy, core, template);
        upgradeRecipe("mejora_botas_warden", upgrades.createBootsWardenUpgrade(),
                souls.createInfestedCaveSpiderSoul(), WardenCaveItems.Variant.VERDE, energy, core, template);
        upgradeRecipe("mejora_peto_warden", upgrades.createChestplateWardenUpgrade(),
                souls.createInfestedGhastSoul(), WardenCaveItems.Variant.MORADO, energy, core, template);
        upgradeRecipe("mejora_pantalon_warden", upgrades.createLeggingsWardenUpgrade(),
                souls.createInfestedCreeperSoul(), WardenCaveItems.Variant.GRIS, energy, core, template);

        armorRecipe("casco_warden", armor.createWardenHelmet(), upgrades.createHelmetWardenUpgrade(),
                Material.NETHERITE_HELMET, WardenCaveItems.Variant.CIAN);
        armorRecipe("peto_warden", armor.createWardenChestplate(), upgrades.createChestplateWardenUpgrade(),
                Material.NETHERITE_CHESTPLATE, WardenCaveItems.Variant.MORADO);
        armorRecipe("pantalon_warden", armor.createWardenLeggings(), upgrades.createLeggingsWardenUpgrade(),
                Material.NETHERITE_LEGGINGS, WardenCaveItems.Variant.GRIS);
        armorRecipe("botas_warden", armor.createWardenBoots(), upgrades.createBootsWardenUpgrade(),
                Material.NETHERITE_BOOTS, WardenCaveItems.Variant.VERDE);

        // Lo que sale del Ultra Warden: la Warden Gun, el Arco de Sculk y las Botas Escaladoras
        ShapedRecipe gun = new ShapedRecipe(key(WardenGun.ID), WardenGun.create());
        gun.shape("ENE", "W W", "E E");
        gun.setIngredient('E', echo);
        gun.setIngredient('N', core);
        gun.setIngredient('W', energy);
        addRecipe(gun);

        addRecipe(new SmithingTransformRecipe(key(ArcoSculk.ID), ArcoSculk.create(), core,
                new RecipeChoice.MaterialChoice(Material.BOW), energy));
        addRecipe(new SmithingTransformRecipe(key(WardenArmor.CLIMBING_ID), armor.createClimbingWardenBoots(), core,
                new RecipeChoice.MaterialChoice(Material.NETHERITE_BOOTS),
                RecipeChoice.exactChoice(new ItemStack(Material.HONEY_BLOCK))));
    }

    // Mejora de una pieza: 4 Lingotes Profundos y el alma de su bioma, 1 energía, 1 Núcleo Sónico y la plantilla de netherite
    private void upgradeRecipe(String id, ItemStack upgrade, ItemStack soul, WardenCaveItems.Variant variant,
                               RecipeChoice energy, RecipeChoice core, RecipeChoice template) {
        ShapedRecipe recipe = new ShapedRecipe(key(id), upgrade);
        recipe.shape("LAL", "WTN", "L L");
        recipe.setIngredient('L', RecipeChoice.exactChoice(WardenCaveItems.createColoredIngot(variant)));
        recipe.setIngredient('A', RecipeChoice.exactChoice(soul));
        recipe.setIngredient('W', energy);
        recipe.setIngredient('T', template);
        recipe.setIngredient('N', core);
        addRecipe(recipe);
    }

    // En la mesa de herrería: mejora + pieza floral + el 5to lingote de su bioma (onPrepareSmithing no deja usar una de
    // Netherite común). Se quedan los encantamientos de la pieza
    private void armorRecipe(String id, ItemStack result, ItemStack upgrade, Material base, WardenCaveItems.Variant variant) {
        addRecipe(new SmithingTransformRecipe(key(id), result, RecipeChoice.exactChoice(upgrade),
                new RecipeChoice.MaterialChoice(base), RecipeChoice.exactChoice(WardenCaveItems.createColoredIngot(variant))));
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

    // La herrería deja la base como está y le pone lo del resultado encima, así que se mira qué se mejora: la armadura
    // de Warden solo sale de la floral (Netherite » Floral » Warden), las Botas Escaladoras de unas Botas de Warden y el
    // Arco de Sculk de un arco normal o del Arco Mejorado
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!(event.getInventory().getRecipe() instanceof Keyed keyed) || !recipeKeys.contains(keyed.getKey())) return;
        String recipe = keyed.getKey().getKey();
        ItemStack base = event.getInventory().getInputEquipment();
        boolean ok;
        if (recipe.equals(WardenArmor.CLIMBING_ID)) {
            ok = armor.isWardenArmor(base) && !WardenArmor.isClimbing(base);
        } else if (recipe.equals(ArcoSculk.ID)) {
            ok = ArcoSculk.puedeMejorar(base);
        } else {
            items.FloralItems.Pieza pieza = items.FloralItems.pieza(base);
            ok = pieza != null && pieza.esArmadura();
        }
        if (!ok) event.setResult(null);
    }

    // Los minerales crudos que quedaron con el lore viejo (4 minutos) se cambian al nuevo al entrar, al abrir un cofre o
    // al agarrarlos del suelo. Los hornos no se tocan para no cortar lo que se está fundiendo (igual aceptan los viejos)
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updateRawOres(event.getPlayer().getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        updateRawOres(event.getPlayer().getInventory());
        if (!(event.getInventory() instanceof FurnaceInventory)) updateRawOres(event.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        if (WardenCaveItems.updateRawOreLore(stack)) event.getItem().setItemStack(stack);
    }

    private static void updateRawOres(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && WardenCaveItems.updateRawOreLore(item)) inventory.setItem(slot, item);
        }
    }

    // El 40% de los mobs de la Warden Cave suelta 1 Tejido Sculk (va en la Energía de Warden). Solo si lo mató un
    // jugador y no sale de spawners ni de lo que invocan los bosses. HIGH: el Warden Zombie limpia sus drops en NORMAL
    @EventHandler(priority = EventPriority.HIGH)
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.getKiller() == null || !mob.getWorld().getName().equals(QuasoPlugin.WORLD_NAME)) return;
        if (!infestedSkeleton.isCustomMob(mob) && !infestedCaveSpider.isCustomMob(mob) && !infestedGhast.isCustomMob(mob)
                && !infestedCreeper.isCustomMob(mob) && !wardenZombie.isCustomMob(mob)) return;
        if (Bosses.BaseBoss.esMinion(mob) || mob.fromMobSpawner()) return;
        CreatureSpawnEvent.SpawnReason reason = mob.getEntitySpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.SPAWNER || reason == CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) return;
        if (random.nextDouble() < TISSUE_CHANCE) event.getDrops().add(WardenCaveItems.createSculkTissue(1));
    }

    // Partículas de sculk cada vez que el alto horno termina un Fragmento Profundo (3 minutos por mineral)
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

    // El ghast vanilla casi nunca pasa su chequeo de spawn (1 de cada 20), así que en el Abismo salían pocos. Uno de
    // cada 4 zombies que spawnean ahí se cambia por un Infested Ghast unos bloques más arriba, uno por uno (así la
    // mobcap sigue contando igual), mientras no haya ya 3 ghasts cerca
    private void replaceWithGhast(Zombie zombie) {
        if (!zombie.isValid() || zombie.isDead()) return;
        Location base = zombie.getLocation();
        if (base.getWorld().getNearbyEntitiesByType(Ghast.class, base, GHAST_NEARBY_RADIUS).size() >= GHAST_NEARBY_MAX) return;
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
