package Events.BuildBattle;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.util.*;

public class BuildBattleParcel {
    private final int id;

    private final int minX, maxX, minY, maxY, minZ, maxZ;
    private final Location centerBottomCache;

    private UUID ownerUUID;
    private String ownerName;

    // Mapas de puntos para no sobreescribir entre votantes
    private final Map<UUID, Integer> votesOriginality = new HashMap<>();
    private final Map<UUID, Integer> votesComplexity = new HashMap<>();
    private final Map<UUID, Integer> votesVisual = new HashMap<>();

    // Puntos acumulados de rondas anteriores (desempate o Torneo)
    private int baseOriginality = 0;
    private int baseComplexity = 0;
    private int baseVisual = 0;

    // Punto de guardado temporal para mantener ranking superior intacto
    private int storedGlobalScore = -1;

    private UUID textDisplayUUID;
    private final org.bukkit.World cachedWorld;

    private boolean eliminated = false;

    public BuildBattleParcel(int id, Location p1, Location p2) {
        this.id = id;
        this.cachedWorld = p1.getWorld();

        this.minX = Math.min(p1.getBlockX(), p2.getBlockX());
        this.maxX = Math.max(p1.getBlockX(), p2.getBlockX());

        this.minY = Math.min(p1.getBlockY(), p2.getBlockY());
        this.maxY = Math.max(p1.getBlockY(), p2.getBlockY()) + 60;

        this.minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        this.maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        this.centerBottomCache = new Location(cachedWorld, (minX + maxX) / 2.0, minY + 2, (minZ + maxZ) / 2.0);
    }

    public boolean isInside(Location loc) {
        if (loc.getWorld() != cachedWorld) return false;
        return isInsideFast(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    // Optimización Extrema: Método matemático sin instanciar objetos Location
    public boolean isInsideFast(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Set<Long> getIntersectingChunks() {
        Set<Long> chunks = new HashSet<>();
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                chunks.add(((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32));
            }
        }
        return chunks;
    }

    public int getEntityCount() {
        int count = 0;
        if (cachedWorld != null) {
            for (org.bukkit.entity.Entity ent : cachedWorld.getEntities()) {
                if (isInside(ent.getLocation()) && !(ent instanceof org.bukkit.entity.Player) && !(ent instanceof TextDisplay) && !(ent instanceof org.bukkit.entity.Painting) && !(ent instanceof org.bukkit.entity.ItemFrame)) {
                    count++;
                }
            }
        }
        return count;
    }

    public void clearParcel() {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(cachedWorld);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            CuboidRegion airRegion = new CuboidRegion(weWorld, BlockVector3.at(minX, minY + 1, minZ), BlockVector3.at(maxX, maxY, maxZ));
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) airRegion, BlockTypes.AIR.getDefaultState());

            CuboidRegion floorRegion = new CuboidRegion(weWorld, BlockVector3.at(minX, minY, minZ), BlockVector3.at(maxX, minY, maxZ));
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) floorRegion, BlockTypes.GRAY_CONCRETE.getDefaultState());
        } catch (Exception e) {
            e.printStackTrace();
        }
        removeTextDisplay();
    }

    public void fillFloor(Material mat) {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(cachedWorld);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            CuboidRegion floorRegion = new CuboidRegion(weWorld, BlockVector3.at(minX, minY, minZ), BlockVector3.at(maxX, minY, maxZ));
            editSession.setBlocks((com.sk89q.worldedit.regions.Region) floorRegion, BukkitAdapter.adapt(mat.createBlockData()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
        updateTextDisplay();
    }

    public void resetOwnerAndPoints() {
        this.ownerUUID = null;
        this.ownerName = null;
        this.eliminated = false;
        resetPoints();
    }

    public void resetPoints() {
        votesOriginality.clear();
        votesComplexity.clear();
        votesVisual.clear();
        baseOriginality = 0;
        baseComplexity = 0;
        baseVisual = 0;
        storedGlobalScore = -1;
        this.eliminated = false;
        removeTextDisplay();
    }

    // --- NUEVO MÉTODO PARA EL TORNEO ---
    public void saveRoundPoints() {
        baseOriginality += votesOriginality.values().stream().mapToInt(Integer::intValue).sum();
        baseComplexity += votesComplexity.values().stream().mapToInt(Integer::intValue).sum();
        baseVisual += votesVisual.values().stream().mapToInt(Integer::intValue).sum();

        // Limpiamos los votos de la ronda para que los jueces puedan votar de nuevo,
        // pero el visualizador y el getTotalPoints() incluirán la base guardada de arriba.
        votesOriginality.clear();
        votesComplexity.clear();
        votesVisual.clear();
    }
    // -----------------------------------

    public void prepareForTieBreaker() {
        storedGlobalScore = getTotalPoints();
        votesOriginality.clear();
        votesComplexity.clear();
        votesVisual.clear();
        baseOriginality = 0;
        baseComplexity = 0;
        baseVisual = 0;
        updateTextDisplay();
    }

    public void setStoredGlobalScore(int score) {
        this.storedGlobalScore = score;
    }

    public void updateTextDisplay() {
        if (textDisplayUUID != null) {
            org.bukkit.entity.Entity ent = Bukkit.getEntity(textDisplayUUID);
            if (ent instanceof TextDisplay) {
                ((TextDisplay) ent).setText(getDisplayText());
                return;
            }
        }
        Location center = getCenterBottom().add(0, 3, 10);
        TextDisplay display = (TextDisplay) center.getWorld().spawnEntity(center, EntityType.TEXT_DISPLAY);
        display.setBillboard(Display.Billboard.CENTER);
        display.setText(getDisplayText());
        display.setDefaultBackground(true);
        display.setShadowed(true);
        this.textDisplayUUID = display.getUniqueId();
    }

    private String getDisplayText() {
        String displayOwner = ownerName != null ? ownerName : "Nadie";

        if (eliminated) {
            String cMint = ChatColor.of("#98FF98").toString();
            return cMint + "§l★ PARCELA #" + id + " ★\n" +
                    cMint + "§l★ " + displayOwner + " ★\n\n" +
                    "§7(Ya participó)\n\n" +
                    cMint + "§lTOTAL: §f§l" + getDisplayPoints() + " PUNTOS";
        }

        String c1 = ChatColor.of("#F977F9").toString();
        String c2 = ChatColor.of("#ae52e3").toString();

        return c1 + "§l★ PUNTAJE DE PARCELA #" + id + " ★\n" +
                c2 + "§l★ " + displayOwner + " ★\n\n" +
                "§a§lOriginalidad: §f" + getPointsOriginality() + "\n" +
                "§b§lComplejidad: §f" + getPointsComplexity() + "\n" +
                "§6§lImpacto Visual: §f" + getPointsVisual() + "\n\n" +
                c2 + "§lTOTAL: §f§l" + getDisplayPoints() + " PUNTOS";
    }

    public void removeTextDisplay() {
        if (textDisplayUUID != null) {
            org.bukkit.entity.Entity ent = Bukkit.getEntity(textDisplayUUID);
            if (ent != null) ent.remove();
            textDisplayUUID = null;
        }
    }

    public boolean hasVotedAll(int totalVoters) {
        return votesOriginality.size() >= totalVoters &&
                votesComplexity.size() >= totalVoters &&
                votesVisual.size() >= totalVoters;
    }

    public int getPointsOriginality() { return baseOriginality + votesOriginality.values().stream().mapToInt(Integer::intValue).sum(); }
    public int getPointsComplexity() { return baseComplexity + votesComplexity.values().stream().mapToInt(Integer::intValue).sum(); }
    public int getPointsVisual() { return baseVisual + votesVisual.values().stream().mapToInt(Integer::intValue).sum(); }
    public int getTotalPoints() { return getPointsOriginality() + getPointsComplexity() + getPointsVisual(); }

    public double getSortPoints() {
        if (storedGlobalScore != -1) {
            return storedGlobalScore + (getTotalPoints() * 0.001); // Se le añade una fracción imperceptible con los votos extras.
        }
        return getTotalPoints();
    }

    public String getDisplayPoints() {
        if (storedGlobalScore != -1) {
            return getTotalPoints() + " §8(" + storedGlobalScore + ")";
        }
        return String.valueOf(getTotalPoints());
    }

    public int getId() { return id; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public void setOwner(UUID uuid, String name) { this.ownerUUID = uuid; this.ownerName = name; }

    public Location getCenterBottom() {
        return centerBottomCache.clone();
    }

    public void setVoteOriginality(UUID voter, int p) { votesOriginality.put(voter, p); updateTextDisplay(); }
    public void setVoteComplexity(UUID voter, int p) { votesComplexity.put(voter, p); updateTextDisplay(); }
    public void setVoteVisual(UUID voter, int p) { votesVisual.put(voter, p); updateTextDisplay(); }
}