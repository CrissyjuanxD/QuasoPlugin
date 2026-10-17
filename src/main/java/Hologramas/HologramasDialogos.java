package Hologramas;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// /holograma: todo el sistema de hologramas en diálogos, solo para admins. Crear, escribir el texto con vista previa,
// cambiar la apariencia, moverlo y girarlo mientras es un borrador que solo ven los admins, y publicarlo
public final class HologramasDialogos {

    private static final LegacyComponentSerializer TEXTO = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private static final ClickCallback.Options BOTONES = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(30)).build();
    private static final double[] PASOS = {0.05, 0.1, 0.25, 0.5, 1, 2};
    private static final int[] REAPERTURAS = {20, 60, 0};

    private static final String MORADO = ChatColor.of("#B79CFF").toString();
    private static final String CELESTE = ChatColor.of("#A8E4FF").toString();
    private static final String AZUL = ChatColor.of("#7DA7FF").toString();
    private static final String VERDE = ChatColor.of("#7BD88F").toString();
    private static final String AMARILLO = ChatColor.of("#FFD166").toString();
    private static final String ROJO = ChatColor.of("#FF6B6B").toString();
    private static final String GRIS = ChatColor.GRAY.toString();
    private static final String NEGRITA = ChatColor.BOLD.toString();

    private final JavaPlugin plugin;
    private final HologramasManager manager;
    // Por admin: cuánto se mueve con cada botón y en cuánto vuelve el menú de posición
    private final Map<UUID, Integer> paso = new HashMap<>();
    private final Map<UUID, Integer> reapertura = new HashMap<>();

    public HologramasDialogos(JavaPlugin plugin, HologramasManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ---------------------------------------------------------------- Menú principal

    public void principal(Player player, String aviso) {
        List<Holograma> todos = manager.todos();
        long publicados = todos.stream().filter(Holograma::publicado).count();
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(CELESTE + "Hay " + AZUL + todos.size() + CELESTE + " hologramas: " + VERDE + publicados
                + " publicados" + CELESTE + " y " + AMARILLO + (todos.size() - publicados) + " borradores" + CELESTE
                + ".\n" + GRIS + "Los borradores solo los ven los admins de hologramas hasta que los publicas."));

        List<ActionButton> botones = new ArrayList<>();
        botones.add(boton(VERDE + "+ Crear holograma", CELESTE + "Se crea delante de ti como borrador", 150,
                p -> crear(p, null)));
        botones.add(boton(MORADO + "Guía", CELESTE + "Cómo se usa todo, colores incluidos", 150, this::guia));
        for (Holograma holograma : todos) {
            Component ayuda = TextoHolograma.convertir(holograma.texto).appendNewline()
                    .append(texto(GRIS + holograma.mundo + " " + coordenadas(holograma) + distancia(player, holograma)));
            botones.add(boton((holograma.publicado ? VERDE + "● " : AMARILLO + "○ ") + CELESTE + holograma.id, ayuda, 150,
                    p -> menu(p, holograma, null)));
        }
        botones.add(boton(GRIS + "Recargar hologramas.yml", CELESTE + "Vuelve a leer el archivo y crea de nuevo los hologramas",
                150, p -> {
                    manager.cargar();
                    principal(p, VERDE + "Se recargó hologramas.yml.");
                }));
        mostrar(player, dialogo(MORADO + NEGRITA + "Hologramas", cuerpo, List.of(),
                DialogType.multiAction(botones, cerrar(), 2)));
    }

    // ---------------------------------------------------------------- Crear y duplicar

    void crear(Player player, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(CELESTE + "Aparece " + AZUL + "2 bloques delante de ti" + CELESTE + " como " + AMARILLO
                + "borrador" + CELESTE + ": solo lo ven los admins hasta que lo publiques. Después lo puedes mover, girar y "
                + "cambiar todo.\n" + GRIS + "Nombre: minúsculas, números, - y _ (máximo 32)."));
        // La entrada no se puede llamar "id": Paper guarda ahí el id del botón y el diálogo fallaba con "Not a list"
        List<DialogInput> entradas = List.of(
                DialogInput.text("nombre", texto(CELESTE + "Nombre del holograma")).width(300).maxLength(32).build(),
                textoMultilinea("texto", "&#B79CFF&lMi holograma\n&7Escribe aquí"));
        mostrar(player, dialogo(VERDE + NEGRITA + "Crear holograma", cuerpo, entradas, DialogType.confirmation(
                formulario(VERDE + "Crear", (p, respuesta) -> {
                    String id = limpio(respuesta.getText("nombre")).toLowerCase(Locale.ROOT);
                    if (!HologramasManager.idValido(id)) {
                        crear(p, ROJO + "Nombre no válido: usa minúsculas, números, - o _.");
                    } else if (manager.get(id) != null) {
                        crear(p, ROJO + "Ya hay un holograma llamado " + id + ".");
                    } else {
                        Holograma holograma = manager.crear(id, delante(p), respuesta.getText("texto"));
                        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
                        menu(p, holograma, VERDE + "Holograma creado como borrador. Solo los admins lo ven.");
                    }
                }),
                volver(p -> principal(p, null)))));
    }

    private void duplicar(Player player, Holograma original, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(CELESTE + "La copia tiene el mismo texto y apariencia, aparece delante de ti y empieza como "
                + AMARILLO + "borrador" + CELESTE + "."));
        List<DialogInput> entradas = List.of(DialogInput.text("nombre", texto(CELESTE + "Nombre de la copia")).width(300)
                .maxLength(32).initial(original.id.length() <= 28 ? original.id + "_2" : "").build());
        mostrar(player, dialogo(AZUL + NEGRITA + "Duplicar " + original.id, cuerpo, entradas, DialogType.confirmation(
                formulario(VERDE + "Duplicar", (p, respuesta) -> {
                    String id = limpio(respuesta.getText("nombre")).toLowerCase(Locale.ROOT);
                    if (!HologramasManager.idValido(id)) duplicar(p, original, ROJO + "Nombre no válido.");
                    else if (manager.get(id) != null) duplicar(p, original, ROJO + "Ya hay un holograma llamado " + id + ".");
                    else {
                        Holograma copia = manager.duplicar(original, id, delante(p));
                        menu(p, copia, VERDE + "Copia creada como borrador.");
                    }
                }),
                volver(p -> menu(p, original, null)))));
    }

    // ---------------------------------------------------------------- Menú de un holograma

    void menu(Player player, Holograma holograma, String aviso) {
        if (manager.get(holograma.id) != holograma) {
            principal(player, ROJO + "Ese holograma ya no existe.");
            return;
        }
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(DialogBody.plainMessage(TextoHolograma.convertir(holograma.texto), 300));
        cuerpo.add(mensaje((holograma.publicado ? VERDE + "● Publicado: lo ven todos." : AMARILLO
                + "○ Borrador: solo lo ven los admins de hologramas.") + "\n" + GRIS + holograma.mundo + " "
                + coordenadas(holograma) + distancia(player, holograma)));

        List<ActionButton> botones = new ArrayList<>();
        botones.add(boton(CELESTE + "✎ Texto", CELESTE + "Escribir el texto con colores y ver cómo queda", 150,
                p -> textoDialogo(p, holograma, null, null)));
        botones.add(boton(CELESTE + "⚙ Apariencia", CELESTE + "Fondo, sombra, transparencia, brillo, hacia dónde mira...",
                150, p -> apariencia(p, holograma, null)));
        botones.add(boton(CELESTE + "✥ Posición", CELESTE + "Moverlo, girarlo o ponerlo donde miras", 150,
                p -> posicion(p, holograma, null)));
        botones.add(holograma.publicado
                ? boton(AMARILLO + "Volver a borrador", CELESTE + "Lo esconde de los jugadores", 150, p -> {
                    holograma.publicado = false;
                    manager.actualizar(holograma);
                    menu(p, holograma, AMARILLO + "Ahora es un borrador: solo lo ven los admins.");
                })
                : boton(VERDE + "Publicar", CELESTE + "Que lo vean todos los jugadores", 150, p -> {
                    holograma.publicado = true;
                    manager.actualizar(holograma);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
                    menu(p, holograma, VERDE + "Publicado: ya lo ven todos.");
                }));
        botones.add(boton(CELESTE + "Señalar", CELESTE + "Partículas en el holograma durante 3 segundos (solo tú)", 150,
                p -> senalar(p, holograma)));
        botones.add(boton(CELESTE + "Ir", CELESTE + "Teletransportarte frente al holograma", 150, p -> {
            Location lugar = holograma.ubicacion();
            if (lugar == null) {
                menu(p, holograma, ROJO + "El mundo " + holograma.mundo + " no está cargado.");
                return;
            }
            Vector frente = lugar.getDirection().setY(0);
            if (frente.lengthSquared() < 0.01) frente = new Vector(0, 0, 1);
            Location destino = lugar.clone().add(frente.normalize().multiply(3)).subtract(0, 1.5, 0);
            destino.setDirection(lugar.toVector().subtract(destino.toVector()));
            p.teleport(destino);
            menu(p, holograma, null);
        }));
        botones.add(boton(AZUL + "Duplicar", CELESTE + "Hacer una copia delante de ti", 150, p -> duplicar(p, holograma, null)));
        botones.add(boton(ROJO + "Eliminar", CELESTE + "Pide confirmación", 150, p -> eliminar(p, holograma)));
        mostrar(player, dialogo(MORADO + NEGRITA + "Holograma " + holograma.id, cuerpo, List.of(),
                DialogType.multiAction(botones, volver(p -> principal(p, null)), 2)));
    }

    private void eliminar(Player player, Holograma holograma) {
        mostrar(player, dialogo(ROJO + NEGRITA + "Eliminar " + holograma.id,
                List.of(mensaje(CELESTE + "¿Eliminar el holograma " + AZUL + holograma.id + CELESTE + "? Desaparece para todos "
                        + "y no se puede deshacer.")), List.of(),
                DialogType.confirmation(
                        boton(ROJO + "Sí, eliminar", null, 150, p -> {
                            manager.eliminar(holograma);
                            principal(p, VERDE + "Se eliminó el holograma " + holograma.id + ".");
                        }),
                        volver(p -> menu(p, holograma, null)))));
    }

    private void senalar(Player player, Holograma holograma) {
        Location lugar = holograma.ubicacion();
        if (lugar == null || !lugar.getWorld().equals(player.getWorld())) {
            menu(player, holograma, ROJO + "Tienes que estar en el mismo mundo que el holograma.");
            return;
        }
        new BukkitRunnable() {
            int veces = 0;

            @Override
            public void run() {
                if (!player.isOnline() || veces++ >= 12) {
                    cancel();
                    return;
                }
                player.spawnParticle(Particle.END_ROD, lugar, 6, 0.08, 0.35, 0.08, 0);
                player.spawnParticle(Particle.WAX_ON, lugar.clone().add(0, -0.2, 0), 3, 0.3, 0.05, 0.3, 0);
            }
        }.runTaskTimer(plugin, 0L, 5L);
        player.sendMessage(texto(CELESTE + "Señalando " + AZUL + holograma.id + CELESTE + ". ")
                .append(texto(MORADO + NEGRITA + "[Volver al menú]")
                        .clickEvent(ClickEvent.runCommand("/holograma editar " + holograma.id))));
    }

    // ---------------------------------------------------------------- Texto

    // Arriba la vista previa del texto, abajo lo que se escribe. "Vista previa" no guarda: solo muestra cómo queda
    private void textoDialogo(Player player, Holograma holograma, String borrador, String aviso) {
        String actual = borrador != null ? borrador : holograma.texto;
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(GRIS + (borrador != null ? "Vista previa (sin guardar):" : "Así se ve ahora:")));
        cuerpo.add(DialogBody.plainMessage(TextoHolograma.convertir(actual), 300));
        cuerpo.add(mensaje(GRIS + "&a &b &c... colores · &#B79CFF hex · &l negrita · &o cursiva · &n subrayado · &m tachado · "
                + "&k mágico · &r quitar formato · <gradient:#B79CFF:#A8E4FF>texto</gradient> · <rainbow>texto</rainbow> · "
                + "Enter o \\n nueva línea · \\u2764 o pegar unicodes. Más en la guía."));
        List<DialogInput> entradas = List.of(textoMultilinea("texto", actual));
        List<ActionButton> botones = List.of(
                formulario(AZUL + "Vista previa", (p, respuesta) -> textoDialogo(p, holograma, respuesta.getText("texto"), null)),
                formulario(VERDE + "Guardar", (p, respuesta) -> {
                    holograma.texto = respuesta.getText("texto");
                    manager.actualizar(holograma);
                    textoDialogo(p, holograma, null, VERDE + "Texto guardado."
                            + (holograma.publicado ? " Ya lo ven todos." : " Sigue siendo borrador."));
                }));
        mostrar(player, dialogo(CELESTE + NEGRITA + "✎ Texto de " + holograma.id, cuerpo, entradas,
                DialogType.multiAction(botones, volver(p -> menu(p, holograma, null)), 2)));
    }

    // ---------------------------------------------------------------- Apariencia

    private void apariencia(Player player, Holograma holograma, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(GRIS + "Al guardar el cambio se ve al momento en el holograma. Cada opción está explicada en la guía."));
        List<DialogInput> entradas = List.of(
                opciones("fondo", "Fondo", holograma.fondo.name(),
                        opcion("DEFECTO", "Gris de Minecraft"), opcion("NINGUNO", "Sin fondo"), opcion("COLOR", "Color propio")),
                DialogInput.text("color_fondo", texto(CELESTE + "Color del fondo (hex, si elegiste color propio)"))
                        .width(200).maxLength(7).initial(holograma.colorFondo).build(),
                DialogInput.numberRange("opacidad_fondo", texto(CELESTE + "Opacidad del fondo %"), 0, 100)
                        .step(5f).initial(entre(holograma.opacidadFondo, 0, 100)).width(300).build(),
                DialogInput.numberRange("opacidad_texto", texto(CELESTE + "Opacidad del texto %"), 10, 100)
                        .step(5f).initial(entre(holograma.opacidadTexto, 10, 100)).width(300).build(),
                DialogInput.bool("sombra", texto(CELESTE + "Sombra en las letras")).initial(holograma.sombra).build(),
                DialogInput.bool("ver_a_traves", texto(CELESTE + "Se ve a través de los bloques")).initial(holograma.verATraves).build(),
                opciones("billboard", "Hacia dónde mira", holograma.billboard.name(),
                        opcion("CENTER", "Siempre hacia el jugador"), opcion("VERTICAL", "Gira solo a los lados"),
                        opcion("HORIZONTAL", "Gira solo arriba y abajo"), opcion("FIXED", "Fijo (como lo gires en Posición)")),
                opciones("alineacion", "Alineación del texto", holograma.alineacion.name(),
                        opcion("CENTER", "Centrado"), opcion("LEFT", "Izquierda"), opcion("RIGHT", "Derecha")),
                DialogInput.numberRange("escala", texto(CELESTE + "Tamaño (1 = normal)"), 0.25f, 10f)
                        .step(0.25f).initial(entre(holograma.escala, 0.25f, 10)).width(300).build(),
                DialogInput.numberRange("brillo", texto(CELESTE + "Brillo (-1 = la luz del lugar)"), -1, 15)
                        .step(1f).initial(entre(holograma.brillo, -1, 15)).width(300).build(),
                DialogInput.numberRange("ancho_linea", texto(CELESTE + "Ancho máximo de línea (píxeles)"), 50, 1000)
                        .step(10f).initial(entre(holograma.anchoLinea, 50, 1000)).width(300).build(),
                DialogInput.numberRange("distancia", texto(CELESTE + "Se ve desde (bloques)"), 16, 256)
                        .step(16f).initial(entre(holograma.distancia, 16, 256)).width(300).build());
        mostrar(player, dialogo(CELESTE + NEGRITA + "⚙ Apariencia de " + holograma.id, cuerpo, entradas,
                DialogType.confirmation(
                        formulario(VERDE + "Guardar", (p, r) -> {
                            String error = aplicarApariencia(holograma, r);
                            if (error == null) manager.actualizar(holograma);
                            apariencia(p, holograma, error != null ? ROJO + error : VERDE + "Apariencia guardada.");
                        }),
                        volver(p -> menu(p, holograma, null)))));
    }

    private String aplicarApariencia(Holograma holograma, DialogResponseView r) {
        String color = limpio(r.getText("color_fondo"));
        Holograma.Fondo fondo = Holograma.Fondo.valueOf(r.getText("fondo"));
        if (fondo == Holograma.Fondo.COLOR && !Holograma.hexValido(color)) return "El color del fondo tiene que ser un hex como #1A1A2E.";
        holograma.fondo = fondo;
        if (Holograma.hexValido(color)) holograma.colorFondo = color.startsWith("#") ? color.toUpperCase(Locale.ROOT)
                : "#" + color.toUpperCase(Locale.ROOT);
        holograma.opacidadFondo = Math.round(r.getFloat("opacidad_fondo"));
        holograma.opacidadTexto = Math.round(r.getFloat("opacidad_texto"));
        holograma.sombra = Boolean.TRUE.equals(r.getBoolean("sombra"));
        holograma.verATraves = Boolean.TRUE.equals(r.getBoolean("ver_a_traves"));
        holograma.billboard = Display.Billboard.valueOf(r.getText("billboard"));
        holograma.alineacion = TextDisplay.TextAlignment.valueOf(r.getText("alineacion"));
        holograma.escala = r.getFloat("escala");
        holograma.brillo = Math.round(r.getFloat("brillo"));
        holograma.anchoLinea = Math.round(r.getFloat("ancho_linea"));
        holograma.distancia = Math.round(r.getFloat("distancia"));
        return null;
    }

    // ---------------------------------------------------------------- Posición

    void posicion(Player player, Holograma holograma, String aviso) {
        if (manager.get(holograma.id) != holograma) {
            principal(player, ROJO + "Ese holograma ya no existe.");
            return;
        }
        double p = PASOS[paso.getOrDefault(player.getUniqueId(), 3)];
        int vuelta = REAPERTURAS[reapertura.getOrDefault(player.getUniqueId(), 0)];
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(CELESTE + "Posición: " + AZUL + holograma.mundo + " " + coordenadas(holograma) + CELESTE
                + "\nGiro: " + AZUL + String.format(Locale.ROOT, "%.0f°", holograma.yaw) + CELESTE + " · Inclinación: " + AZUL
                + String.format(Locale.ROOT, "%.0f°", holograma.pitch) + "\n" + GRIS + "Girar e inclinar solo se nota con "
                + "\"Fijo\" en Apariencia. " + (vuelta > 0 ? "Tras cada cambio el menú vuelve en " + vuelta / 20 + " s para que veas cómo quedó."
                : "Tras cada cambio el menú no vuelve: usa el botón del chat.")));

        String pasoTexto = String.format(Locale.ROOT, "%s", p);
        List<ActionButton> botones = new ArrayList<>();
        botones.add(mover(holograma, AZUL + "X -" + pasoTexto, "Hacia el oeste", -p, 0, 0, 0, 0));
        botones.add(mover(holograma, AZUL + "X +" + pasoTexto, "Hacia el este", p, 0, 0, 0, 0));
        botones.add(boton(MORADO + "Paso: " + pasoTexto, CELESTE + "Cuánto se mueve con cada botón", 100, j -> {
            paso.put(j.getUniqueId(), (paso.getOrDefault(j.getUniqueId(), 3) + 1) % PASOS.length);
            posicion(j, holograma, null);
        }));
        botones.add(mover(holograma, AZUL + "Y -" + pasoTexto, "Bajar", 0, -p, 0, 0, 0));
        botones.add(mover(holograma, AZUL + "Y +" + pasoTexto, "Subir", 0, p, 0, 0, 0));
        botones.add(boton(CELESTE + "Traer aquí", CELESTE + "A la altura de tus ojos, donde estás", 100, j -> {
            Location ojos = j.getEyeLocation();
            holograma.mover(ojos);
            cambio(j, holograma, "Traído a tu posición.");
        }));
        botones.add(mover(holograma, AZUL + "Z -" + pasoTexto, "Hacia el norte", 0, 0, -p, 0, 0));
        botones.add(mover(holograma, AZUL + "Z +" + pasoTexto, "Hacia el sur", 0, 0, p, 0, 0));
        botones.add(boton(CELESTE + "Donde miro", CELESTE + "Pegado al bloque que miras (hasta 30 bloques)", 100,
                j -> dondeMiro(j, holograma)));
        botones.add(mover(holograma, AZUL + "Girar -15°", "Girar a la izquierda", 0, 0, 0, -15, 0));
        botones.add(mover(holograma, AZUL + "Girar +15°", "Girar a la derecha", 0, 0, 0, 15, 0));
        botones.add(boton(CELESTE + "Mirar hacia mí", CELESTE + "Gira el holograma hacia donde estás", 100, j -> {
            Location lugar = holograma.ubicacion();
            if (lugar == null || !lugar.getWorld().equals(j.getWorld())) {
                posicion(j, holograma, ROJO + "Tienes que estar en el mismo mundo.");
                return;
            }
            Vector haciaMi = j.getEyeLocation().toVector().subtract(lugar.toVector());
            Location giro = lugar.clone().setDirection(haciaMi);
            holograma.yaw = giro.getYaw();
            holograma.pitch = 0;
            cambio(j, holograma, "Ahora mira hacia ti.");
        }));
        botones.add(mover(holograma, AZUL + "Inclinar -15°", "Inclinar hacia arriba", 0, 0, 0, 0, -15));
        botones.add(mover(holograma, AZUL + "Inclinar +15°", "Inclinar hacia abajo", 0, 0, 0, 0, 15));
        botones.add(boton(CELESTE + "Coordenadas", CELESTE + "Escribir la posición y el giro exactos", 100,
                j -> coordenadasExactas(j, holograma, null)));
        botones.add(boton(MORADO + "Volver tras: " + (vuelta > 0 ? vuelta / 20 + " s" : "no"),
                CELESTE + "Cuándo vuelve este menú después de mover", 100, j -> {
                    reapertura.put(j.getUniqueId(), (reapertura.getOrDefault(j.getUniqueId(), 0) + 1) % REAPERTURAS.length);
                    posicion(j, holograma, null);
                }));
        botones.add(boton(CELESTE + "Señalar", CELESTE + "Partículas 3 segundos (solo tú)", 100, j -> senalar(j, holograma)));
        botones.add(boton(GRIS + "« Menú", null, 100, j -> menu(j, holograma, null)));
        mostrar(player, dialogo(CELESTE + NEGRITA + "✥ Posición de " + holograma.id, cuerpo, List.of(),
                DialogType.multiAction(botones, volver(j -> menu(j, holograma, null)), 3)));
    }

    private ActionButton mover(Holograma holograma, String etiqueta, String ayuda, double dx, double dy, double dz,
                               float dYaw, float dPitch) {
        return boton(etiqueta, CELESTE + ayuda, 100, j -> {
            holograma.x += dx;
            holograma.y += dy;
            holograma.z += dz;
            holograma.yaw = normalizar(holograma.yaw + dYaw);
            holograma.pitch = Math.max(-90, Math.min(90, holograma.pitch + dPitch));
            cambio(j, holograma, null);
        });
    }

    private void dondeMiro(Player player, Holograma holograma) {
        RayTraceResult golpe = player.rayTraceBlocks(30);
        if (golpe == null || golpe.getHitBlockFace() == null) {
            posicion(player, holograma, ROJO + "No estás mirando ningún bloque a menos de 30 bloques.");
            return;
        }
        BlockFace cara = golpe.getHitBlockFace();
        Vector normal = cara.getDirection();
        Location lugar = golpe.getHitPosition().add(normal.clone().multiply(0.05)).toLocation(player.getWorld());
        if (cara == BlockFace.UP) lugar.add(0, 0.5, 0);
        holograma.mover(lugar);
        if (cara != BlockFace.UP && cara != BlockFace.DOWN) {
            holograma.yaw = lugar.clone().setDirection(normal).getYaw();
            holograma.pitch = 0;
        }
        cambio(player, holograma, cara == BlockFace.UP || cara == BlockFace.DOWN ? "Puesto sobre el bloque."
                : "Pegado a la pared y mirando hacia afuera (con \"Fijo\" queda plano en la pared).");
    }

    private void coordenadasExactas(Player player, Holograma holograma, String aviso) {
        List<DialogBody> cuerpo = new ArrayList<>();
        if (aviso != null) cuerpo.add(mensaje(aviso));
        cuerpo.add(mensaje(GRIS + "Mundo: " + holograma.mundo + ". Usa punto para los decimales, por ejemplo 120.5"));
        List<DialogInput> entradas = List.of(
                numero("x", "X", holograma.x), numero("y", "Y", holograma.y), numero("z", "Z", holograma.z),
                numero("yaw", "Giro (-180 a 180)", holograma.yaw), numero("pitch", "Inclinación (-90 a 90)", holograma.pitch));
        mostrar(player, dialogo(CELESTE + NEGRITA + "Coordenadas de " + holograma.id, cuerpo, entradas,
                DialogType.confirmation(
                        formulario(VERDE + "Guardar", (j, r) -> {
                            try {
                                double x = Double.parseDouble(limpio(r.getText("x")));
                                double y = Double.parseDouble(limpio(r.getText("y")));
                                double z = Double.parseDouble(limpio(r.getText("z")));
                                float yaw = Float.parseFloat(limpio(r.getText("yaw")));
                                float pitch = Float.parseFloat(limpio(r.getText("pitch")));
                                if (!Double.isFinite(x + y + z + yaw + pitch) || Math.abs(x) > 3e7 || Math.abs(z) > 3e7)
                                    throw new NumberFormatException();
                                holograma.x = x;
                                holograma.y = y;
                                holograma.z = z;
                                holograma.yaw = normalizar(yaw);
                                holograma.pitch = Math.max(-90, Math.min(90, pitch));
                                cambio(j, holograma, "Coordenadas guardadas.");
                            } catch (NumberFormatException e) {
                                coordenadasExactas(j, holograma, ROJO + "Algún número no es válido.");
                            }
                        }),
                        volver(j -> posicion(j, holograma, null)))));
    }

    // Aplica el cambio de posición y vuelve al menú tras el tiempo elegido, para que se vea el holograma sin el menú encima
    private void cambio(Player player, Holograma holograma, String aviso) {
        manager.actualizar(holograma);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
        player.sendActionBar(texto(CELESTE + holograma.id + " " + AZUL + coordenadas(holograma) + GRIS + " · giro "
                + String.format(Locale.ROOT, "%.0f°", holograma.yaw)));
        int vuelta = REAPERTURAS[reapertura.getOrDefault(player.getUniqueId(), 0)];
        String mensaje = aviso == null ? null : VERDE + aviso;
        if (vuelta == 0) {
            player.sendMessage(texto(CELESTE + (aviso != null ? aviso + " " : "Movido. "))
                    .append(texto(MORADO + NEGRITA + "[✥ Seguir moviendo]")
                            .clickEvent(ClickEvent.runCommand("/holograma posicion " + holograma.id))));
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) posicion(player, holograma, mensaje);
        }, vuelta);
    }

    // ---------------------------------------------------------------- Guía

    void guia(Player player) {
        List<DialogBody> cuerpo = List.of(
                mensaje(MORADO + NEGRITA + "Cómo se usa\n" + CELESTE
                        + "1. " + AZUL + "Crear holograma" + CELESTE + ": le pones nombre y texto. Aparece 2 bloques delante "
                        + "de ti como " + AMARILLO + "borrador" + CELESTE + " (solo lo ven los admins).\n"
                        + "2. " + AZUL + "✎ Texto" + CELESTE + ": escribe y pulsa Vista previa para ver cómo queda sin guardar.\n"
                        + "3. " + AZUL + "⚙ Apariencia" + CELESTE + ": fondo, sombra, transparencia, brillo, tamaño...\n"
                        + "4. " + AZUL + "✥ Posición" + CELESTE + ": muévelo con los botones, tráelo a ti o ponlo donde miras.\n"
                        + "5. " + VERDE + "Publicar" + CELESTE + ": desde ese momento lo ven todos. Con Volver a borrador se esconde."),
                mensaje(MORADO + NEGRITA + "Colores y formato\n" + CELESTE
                        + "&0 " + ChatColor.BLACK + "negro " + CELESTE + "&1 " + ChatColor.DARK_BLUE + "azul oscuro "
                        + CELESTE + "&2 " + ChatColor.DARK_GREEN + "verde oscuro " + CELESTE + "&3 " + ChatColor.DARK_AQUA
                        + "turquesa " + CELESTE + "&4 " + ChatColor.DARK_RED + "rojo oscuro " + CELESTE + "&5 "
                        + ChatColor.DARK_PURPLE + "morado " + CELESTE + "&6 " + ChatColor.GOLD + "dorado " + CELESTE + "&7 "
                        + ChatColor.GRAY + "gris " + CELESTE + "&8 " + ChatColor.DARK_GRAY + "gris oscuro " + CELESTE + "&9 "
                        + ChatColor.BLUE + "azul " + CELESTE + "&a " + ChatColor.GREEN + "verde " + CELESTE + "&b "
                        + ChatColor.AQUA + "celeste " + CELESTE + "&c " + ChatColor.RED + "rojo " + CELESTE + "&d "
                        + ChatColor.LIGHT_PURPLE + "rosa " + CELESTE + "&e " + ChatColor.YELLOW + "amarillo " + CELESTE + "&f "
                        + ChatColor.WHITE + "blanco\n" + CELESTE
                        + "&l " + ChatColor.BOLD + "negrita" + ChatColor.RESET + CELESTE + " · &o " + ChatColor.ITALIC + "cursiva"
                        + ChatColor.RESET + CELESTE + " · &n " + ChatColor.UNDERLINE + "subrayado" + ChatColor.RESET + CELESTE
                        + " · &m " + ChatColor.STRIKETHROUGH + "tachado" + ChatColor.RESET + CELESTE + " · &k mágico · &r quita todo\n"
                        + "Hex: " + AZUL + "&#7DA7FF" + CELESTE + " pone ese color exacto (" + ChatColor.of("#7DA7FF") + "así"
                        + CELESTE + ").\nComo en el chat, un color nuevo quita la negrita: pon el color primero y después "
                        + "&l (&#B79CFF&lTítulo)."),
                mensaje(MORADO + NEGRITA + "Degradados y más (MiniMessage)\n" + CELESTE
                        + "<gradient:#B79CFF:#A8E4FF>texto</gradient> degradado entre dos o más colores\n"
                        + "<rainbow>texto</rainbow> arcoíris · <#FF66AA>texto</#FF66AA> hex\n"
                        + "<bold> <italic> <underlined> <strikethrough> <obfuscated> y se cierran con </bold>...\n"
                        + "Se pueden mezclar con los códigos &."),
                mensaje(MORADO + NEGRITA + "Líneas y unicodes\n" + CELESTE
                        + "Nueva línea: Enter en el cuadro de texto, \\n o <br>.\n"
                        + "Unicodes: pégalos tal cual (★ ❤ ☠ ⚔ ✿ ۞) o escríbelos como \\u2764 (sale ❤). También sirven los "
                        + "unicodes del resource pack del server."),
                mensaje(MORADO + NEGRITA + "Apariencia\n" + CELESTE
                        + AZUL + "Fondo" + CELESTE + ": el gris de Minecraft, ninguno, o un color hex con su opacidad.\n"
                        + AZUL + "Opacidad del texto" + CELESTE + ": 100% es sólido; menos es transparente (mínimo 10%).\n"
                        + AZUL + "Sombra" + CELESTE + ": sombra detrás de cada letra, como en el chat.\n"
                        + AZUL + "Se ve a través de los bloques" + CELESTE + ": se ve aunque haya paredes delante.\n"
                        + AZUL + "Hacia dónde mira" + CELESTE + ": siempre hacia el jugador, girando solo a los lados, solo "
                        + "arriba y abajo, o fijo con el giro que le pongas en Posición (para carteles en paredes).\n"
                        + AZUL + "Brillo" + CELESTE + ": -1 usa la luz del lugar (de noche se oscurece); 15 siempre brillante.\n"
                        + AZUL + "Tamaño" + CELESTE + " (de 0.25 a 10; 1 es el normal), " + AZUL + "ancho de línea" + CELESTE + " (cuándo pasa a la línea "
                        + "siguiente) y " + AZUL + "desde cuántos bloques se ve" + CELESTE + "."),
                mensaje(MORADO + NEGRITA + "Posición\n" + CELESTE
                        + "Los botones X, Y y Z lo mueven lo que diga el Paso (de 0.05 a 2 bloques). Traer aquí lo pone a la "
                        + "altura de tus ojos; Donde miro lo pega al bloque que miras y, si es una pared, lo deja mirando "
                        + "hacia afuera. Girar e inclinar se notan con Fijo. Volver tras elige cuándo vuelve el menú después de "
                        + "mover, para que lo veas sin el menú encima. Señalar marca el holograma con partículas.\n"
                        + GRIS + "Comandos: /holograma, /holograma crear, /holograma editar <nombre>, /holograma posicion <nombre>, "
                        + "/holograma guia, /holograma recargar."));
        mostrar(player, dialogo(MORADO + NEGRITA + "Guía de hologramas", cuerpo, List.of(),
                DialogType.notice(volver(p -> principal(p, null)))));
    }

    // ---------------------------------------------------------------- Utilidades

    private Location delante(Player player) {
        Location ojos = player.getEyeLocation();
        Vector frente = ojos.getDirection().setY(0);
        if (frente.lengthSquared() < 0.01) frente = new Vector(0, 0, 1);
        Location lugar = ojos.clone().add(frente.normalize().multiply(2));
        lugar.setYaw(normalizar(player.getLocation().getYaw() + 180));
        lugar.setPitch(0);
        return lugar;
    }

    // Un valor editado a mano en el yml fuera de rango rompería el diálogo
    private static float entre(float valor, float minimo, float maximo) {
        return Math.max(minimo, Math.min(maximo, valor));
    }

    static float normalizar(float yaw) {
        float valor = yaw % 360;
        if (valor > 180) valor -= 360;
        if (valor <= -180) valor += 360;
        return valor;
    }

    private static String coordenadas(Holograma holograma) {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", holograma.x, holograma.y, holograma.z);
    }

    private static String distancia(Player player, Holograma holograma) {
        Location lugar = holograma.ubicacion();
        if (lugar == null || !lugar.getWorld().equals(player.getWorld())) return "";
        return String.format(Locale.ROOT, " (a %.0f bloques)", lugar.distance(player.getLocation()));
    }

    private static String limpio(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static DialogInput textoMultilinea(String llave, String inicial) {
        return DialogInput.text(llave, texto(CELESTE + "Texto del holograma")).width(300).maxLength(2000)
                .initial(inicial == null ? "" : inicial)
                .multiline(TextDialogInput.MultilineOptions.create(20, 120)).build();
    }

    private static DialogInput numero(String llave, String etiqueta, double valor) {
        return DialogInput.text(llave, texto(CELESTE + etiqueta)).width(200).maxLength(16)
                .initial(String.format(Locale.ROOT, "%.2f", valor)).build();
    }

    private static SingleOptionDialogInput.OptionEntry opcion(String id, String nombre) {
        return SingleOptionDialogInput.OptionEntry.create(id, texto(CELESTE + nombre), false);
    }

    // La opción que tiene ahora el holograma sale marcada
    private static DialogInput opciones(String llave, String etiqueta, String actual, SingleOptionDialogInput.OptionEntry... lista) {
        List<SingleOptionDialogInput.OptionEntry> entradas = new ArrayList<>();
        for (SingleOptionDialogInput.OptionEntry entrada : lista) {
            entradas.add(SingleOptionDialogInput.OptionEntry.create(entrada.id(), entrada.display(), entrada.id().equals(actual)));
        }
        return DialogInput.singleOption(llave, texto(CELESTE + etiqueta), entradas).width(300).build();
    }

    private void mostrar(Player player, Dialog dialog) {
        if (player.isOnline() && player.hasPermission(HologramasManager.PERMISO)) player.showDialog(dialog);
    }

    private static Dialog dialogo(String titulo, List<DialogBody> cuerpo, List<DialogInput> entradas, DialogType tipo) {
        DialogBase base = DialogBase.builder(texto(titulo)).canCloseWithEscape(true).pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE).body(cuerpo).inputs(entradas).build();
        return Dialog.create(builder -> builder.empty().base(base).type(tipo));
    }

    private static DialogBody mensaje(String texto) {
        return DialogBody.plainMessage(texto(texto), 360);
    }

    // Cada botón vuelve a mirar que tenga el permiso antes de hacer algo
    private static ActionButton boton(String etiqueta, Object ayuda, int ancho, Consumer<Player> accion) {
        Component tooltip = ayuda == null ? null : ayuda instanceof Component c ? c : texto(ayuda.toString());
        return ActionButton.create(texto(etiqueta), tooltip, ancho, DialogAction.customClick((respuesta, audiencia) -> {
            if (audiencia instanceof Player p && p.hasPermission(HologramasManager.PERMISO)) accion.accept(p);
        }, BOTONES));
    }

    private static ActionButton formulario(String etiqueta, BiConsumer<Player, DialogResponseView> accion) {
        return ActionButton.create(texto(etiqueta), null, 150, DialogAction.customClick((respuesta, audiencia) -> {
            if (audiencia instanceof Player p && p.hasPermission(HologramasManager.PERMISO)) accion.accept(p, respuesta);
        }, BOTONES));
    }

    private static ActionButton volver(Consumer<Player> accion) {
        return boton(GRIS + "« Volver", null, 150, accion);
    }

    private static ActionButton cerrar() {
        return ActionButton.create(texto(ROJO + "Cerrar"), null, 150, null);
    }

    private static Component texto(String legacy) {
        return TEXTO.deserialize(legacy);
    }
}
