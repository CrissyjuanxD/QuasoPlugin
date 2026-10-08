package Trabajos;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static Trabajos.TrabajosTexto.*;

// La guía de /trabajos como diálogo: un menú de temas y cada tema en su propia ventana con "Volver"
final class TrabajosGuia {

    private static final LegacyComponentSerializer TEXTO = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final ClickCallback.Options BOTONES = ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build();
    private static final ChatColor CELESTE = ChatColor.of("#9FE2E8");

    private enum Tema {
        ENTRAR("Cómo entrar", "Lo que cuesta y cómo elegir tu trabajo."),
        CAMBIAR("Cambiar de trabajo", "La espera de 24 horas y tu progreso."),
        RECOMPENSAS("Recompensas", "Las DinoCoins de cada nivel."),
        BONUS("Bonus y avisos", "El bonus cada 5 niveles."),
        REGLAS("Reglas", "Lo que no da XP."),
        COMANDOS("Comandos y misiones", "Los comandos y las 10 misiones de cada trabajo.");

        private final String titulo;
        private final String resumen;

        Tema(String titulo, String resumen) {
            this.titulo = titulo;
            this.resumen = resumen;
        }
    }

    private TrabajosGuia() {}

    static void abrir(Player player) {
        player.showDialog(principal());
    }

    private static Dialog principal() {
        List<ActionButton> botones = new ArrayList<>();
        for (Tema tema : Tema.values()) {
            botones.add(boton(CAFE + tema.titulo, GRIS + tema.resumen, () -> seccion(tema)));
        }
        for (Trabajo trabajo : Trabajo.values()) {
            botones.add(boton(nombre(trabajo), GRIS + "Cómo se gana XP de " + trabajo.nombre() + ".", () -> trabajo(trabajo)));
        }

        ItemStack libro = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = libro.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        libro.setItemMeta(meta);

        List<DialogBody> cuerpo = List.of(
                DialogBody.item(libro).showTooltip(false).description(DialogBody.plainMessage(texto(CREMA + "Elige un oficio y gana "
                        + DORADO + "DinoCoins" + CREMA + " y experiencia mientras juegas."), 250)).build(),
                DialogBody.plainMessage(texto(GRIS + "Hay " + BLANCO + Trabajo.values().length + " trabajos" + GRIS + " y cada uno tiene "
                        + BLANCO + Trabajo.NIVEL_MAXIMO + " niveles" + GRIS + ". Elige qué quieres leer:"), 300));
        return dialogo(CAFE + "" + ChatColor.BOLD + "✎ Guía de Trabajos", cuerpo,
                DialogType.multiAction(botones, ActionButton.create(texto(ROSA + "Cerrar"), null, 150, null), 2));
    }

    private static Dialog seccion(Tema tema) {
        List<String> lineas = switch (tema) {
            case ENTRAR -> List.of(
                    CREMA + "Entrar a un trabajo cuesta:\n"
                            + GRIS + " · " + DORADO + TrabajosManager.COSTO_MONEDAS + " DinoCoins\n"
                            + GRIS + " · " + BLANCO + TrabajosManager.COSTO_NIVELES + " niveles de experiencia\n"
                            + GRIS + " · " + CELESTE + TrabajosManager.COSTO_DIAMANTES + " diamantes",
                    GRIS + "Las DinoCoins salen del inventario y, si no alcanzan, de tus monederos.",
                    CREMA + "Abre " + BLANCO + "/menu" + CREMA + " (Trabajos) o " + BLANCO + "/trabajos" + CREMA + " y haz clic en el papel del trabajo que quieras. Solo puedes tener "
                            + BLANCO + "un trabajo" + CREMA + " a la vez.");
            case CAMBIAR -> List.of(
                    CREMA + "Después de entrar tienes que esperar " + BLANCO + "24 horas" + CREMA + " para cambiarte. Cambiarte cuesta lo mismo que entrar.",
                    SALVIA + "Tu nivel en cada trabajo se guarda: " + CREMA + "si vuelves a uno sigues donde lo dejaste.");
            case RECOMPENSAS -> {
                StringBuilder tabla = new StringBuilder(CREMA + "Cada nivel te da DinoCoins y experiencia de Minecraft. Mientras más alto, más paga:\n");
                for (int desde = 1; desde <= Trabajo.NIVEL_MAXIMO; desde += 20) {
                    int monedas = TrabajoNiveles.monedasPorNivel(desde);
                    tabla.append('\n').append(GRIS).append("Niveles ").append(desde).append(" a ").append(desde + 19).append(": ")
                            .append(DORADO).append(monedas).append(monedas == 1 ? " DinoCoin" : " DinoCoins");
                }
                yield List.of(tabla.toString(), GRIS + "Las DinoCoins van a tus monederos; lo que no entra, al inventario.");
            }
            case BONUS -> {
                StringBuilder tabla = new StringBuilder(CREMA + "Cada " + BLANCO + "5 niveles" + CREMA + " hay un bonus de DinoCoins que crece con el nivel:\n");
                for (int nivel : new int[]{5, 25, 50, 75, 100}) {
                    tabla.append('\n').append(GRIS).append("Nivel ").append(nivel).append(": ").append(DORADO).append('+').append(TrabajoNiveles.bonus(nivel));
                }
                yield List.of(tabla.toString(),
                        CREMA + "Al subir de nivel sale una " + SALVIA + "barra verde" + CREMA + " con un sonido; cada 5 niveles, un "
                                + DORADO + "título" + CREMA + " con el bonus.",
                        GRIS + "Cada 25 niveles se entera todo el server.");
            }
            case REGLAS -> List.of(
                    CREMA + "Para que nadie haga granjas:\n\n"
                            + GRIS + " · " + CREMA + "Los bloques que pones no dan XP al romperlos.\n"
                            + GRIS + " · " + CREMA + "Los mobs de spawner no cuentan.\n"
                            + GRIS + " · " + CREMA + "Si estás AFK (5 minutos sin moverte) no ganas XP.\n"
                            + GRIS + " · " + CREMA + "Solo cuenta en supervivencia.",
                    SALVIA + "No hay tope de XP: " + CREMA + "puedes trabajar todo lo que quieras.");
            case COMANDOS -> List.of(
                    BLANCO + "/menu" + GRIS + " · " + CREMA + "El menú principal; Trabajos abre este menú.\n"
                            + BLANCO + "/trabajos" + GRIS + " · " + CREMA + "Abre el menú.\n"
                            + BLANCO + "/trabajos info" + GRIS + " · " + CREMA + "Tu trabajo, nivel, lo que falta y la recompensa.\n"
                            + BLANCO + "/trabajos guia" + GRIS + " · " + CREMA + "Abre esta guía.",
                    CREMA + "Cada trabajo tiene " + DORADO + "10 misiones" + CREMA + " (llegar al nivel 10, 20... 100). Están en las últimas páginas del menú de misiones y siempre están activas.");
        };
        List<DialogBody> cuerpo = new ArrayList<>();
        for (String linea : lineas) cuerpo.add(DialogBody.plainMessage(texto(linea), 300));
        return dialogo(CAFE + "" + ChatColor.BOLD + tema.titulo, cuerpo, volver());
    }

    private static Dialog trabajo(Trabajo trabajo) {
        ItemStack papel = new ItemStack(Material.PAPER);
        ItemMeta meta = papel.getItemMeta();
        meta.setItemModel(NamespacedKey.minecraft(trabajo.modelo()));
        papel.setItemMeta(meta);

        StringBuilder fuentes = new StringBuilder(BEIGE + "Cómo ganar XP:\n");
        for (String fuente : trabajo.fuentes()) fuentes.append('\n').append(GRIS).append(" · ").append(CREMA).append(fuente);

        List<DialogBody> cuerpo = List.of(
                DialogBody.item(papel).showTooltip(false).description(DialogBody.plainMessage(texto(GRIS + String.join(" ", trabajo.descripcion())), 250)).build(),
                DialogBody.plainMessage(texto(fuentes.toString()), 300),
                DialogBody.plainMessage(texto(GRIS + nota(trabajo)), 300));
        return dialogo(color(trabajo) + "" + ChatColor.BOLD + trabajo.icono() + " " + trabajo.nombre(), cuerpo, volver());
    }

    private static String nota(Trabajo trabajo) {
        return switch (trabajo) {
            case GUERRERO -> "Los monstruos élite dan más XP mientras más alto es su nivel. Los jefes le dan XP a todos los que pelearon. Los mobs de spawner no cuentan.";
            case MINERIA -> "Los minerales de deepslate dan un poco más. Si pones un mineral y lo vuelves a romper no da XP.";
            case LENADOR -> "Los troncos descortezados y los que pones tú no cuentan.";
            case CONSTRUCTOR -> "Antorchas, cofres, redstone y minerales no dan XP. Poner un bloque donde ya pusiste otro hace menos de 30 minutos tampoco.";
            case GRANJERO -> "Los cultivos tienen que estar maduros. La caña da 0,3 XP y las bayas 0,5 XP.";
            case PESCADOR -> "En las zonas de pesca del server también cuenta.";
        };
    }

    private static Dialog dialogo(String titulo, List<DialogBody> cuerpo, DialogType tipo) {
        DialogBase base = DialogBase.builder(texto(titulo)).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(cuerpo).build();
        return Dialog.create(builder -> builder.empty().base(base).type(tipo));
    }

    private static DialogType volver() {
        return DialogType.confirmation(
                ActionButton.create(texto(SALVIA + "« Volver"), null, 150,
                        DialogAction.customClick((respuesta, audiencia) -> audiencia.showDialog(principal()), BOTONES)),
                ActionButton.create(texto(ROSA + "Cerrar"), null, 150, null));
    }

    // Cada ventana se arma recién al hacer clic
    private static ActionButton boton(String texto, String ayuda, Supplier<Dialog> destino) {
        return ActionButton.create(texto(texto), texto(ayuda), 150,
                DialogAction.customClick((respuesta, audiencia) -> audiencia.showDialog(destino.get()), BOTONES));
    }

    private static Component texto(String legacy) {
        return TEXTO.deserialize(legacy);
    }
}
