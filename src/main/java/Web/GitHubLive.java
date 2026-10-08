package Web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

// Sube los archivos de la web al repositorio de datos en vivo con la API de GitHub. Cada subida es un commit nuevo
// sin historia (la rama se mueve a la fuerza), así el repositorio nunca crece. Después avisa por ntfy.sh con el
// commit para que las páginas abiertas se actualicen al instante
final class GitHubLive {

    static final class ErrorGitHub extends IOException {
        final int status;

        ErrorGitHub(int status, String mensaje) {
            super("GitHub respondió " + status + (mensaje == null || mensaje.isBlank() ? "" : ": " + mensaje));
            this.status = status;
        }
    }

    private static final String API = "https://api.github.com";

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    private final String repo;
    private final String rama;
    private final String token;
    private final String ntfy;
    // El árbol del último commit: cada subida solo manda los archivos que cambiaron
    private String arbol;

    GitHubLive(String repo, String rama, String token, String ntfy) {
        this.repo = repo;
        this.rama = rama;
        this.token = token;
        this.ntfy = ntfy;
    }

    String repo() {
        return repo;
    }

    // archivos: ruta -> contenido (texto). Devuelve el commit nuevo
    String subir(Map<String, String> archivos, String mensaje) throws IOException, InterruptedException {
        if (arbol == null) arbol = arbolActual();
        String treeSha;
        try {
            treeSha = crearArbol(archivos);
        } catch (ErrorGitHub error) {
            if (error.status == 409) {
                // Repositorio vacío: la API de git necesita un primer commit
                inicializar();
                arbol = arbolActual();
                treeSha = crearArbol(archivos);
            } else if (error.status == 404 || error.status == 422) {
                // El árbol guardado ya no existe (alguien movió la rama): se vuelve a leer
                arbol = arbolActual();
                treeSha = crearArbol(archivos);
            } else {
                throw error;
            }
        }

        JsonObject commit = new JsonObject();
        commit.addProperty("message", mensaje);
        commit.addProperty("tree", treeSha);
        commit.add("parents", new JsonArray());
        String commitSha = pedir("POST", "/repos/" + repo + "/git/commits", commit).get("sha").getAsString();

        JsonObject ref = new JsonObject();
        ref.addProperty("sha", commitSha);
        ref.addProperty("force", true);
        try {
            pedir("PATCH", "/repos/" + repo + "/git/refs/heads/" + rama, ref);
        } catch (ErrorGitHub error) {
            if (error.status != 404 && error.status != 422) throw error;
            JsonObject nueva = new JsonObject();
            nueva.addProperty("ref", "refs/heads/" + rama);
            nueva.addProperty("sha", commitSha);
            pedir("POST", "/repos/" + repo + "/git/refs", nueva);
        }
        arbol = treeSha;
        avisar(commitSha);
        return commitSha;
    }

    private String crearArbol(Map<String, String> archivos) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        if (arbol != null) body.addProperty("base_tree", arbol);
        JsonArray tree = new JsonArray();
        for (Map.Entry<String, String> entry : archivos.entrySet()) {
            JsonObject f = new JsonObject();
            f.addProperty("path", entry.getKey());
            f.addProperty("mode", "100644");
            f.addProperty("type", "blob");
            f.addProperty("content", entry.getValue());
            tree.add(f);
        }
        body.add("tree", tree);
        return pedir("POST", "/repos/" + repo + "/git/trees", body).get("sha").getAsString();
    }

    // El árbol del commit al que apunta la rama (null si la rama no existe)
    private String arbolActual() throws IOException, InterruptedException {
        try {
            JsonObject ref = pedir("GET", "/repos/" + repo + "/git/ref/heads/" + rama, null);
            String commitSha = ref.getAsJsonObject("object").get("sha").getAsString();
            JsonObject commit = pedir("GET", "/repos/" + repo + "/git/commits/" + commitSha, null);
            return commit.getAsJsonObject("tree").get("sha").getAsString();
        } catch (ErrorGitHub error) {
            if (error.status == 404 || error.status == 409) return null;
            throw error;
        }
    }

    private void inicializar() throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("message", "Datos en vivo de Croissants");
        body.addProperty("content", Base64.getEncoder().encodeToString(
                "# Datos en vivo de Croissants\n\nLos sube QuasoPlugin; los lee la web.\n".getBytes(StandardCharsets.UTF_8)));
        body.addProperty("branch", rama);
        pedir("PUT", "/repos/" + repo + "/contents/README.md", body);
    }

    private void avisar(String commitSha) {
        if (ntfy == null || ntfy.isBlank()) return;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://ntfy.sh/" + ntfy))
                    .timeout(Duration.ofSeconds(10))
                    .header("Title", "croissants")
                    .POST(HttpRequest.BodyPublishers.ofString(commitSha, StandardCharsets.UTF_8))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Sin aviso las páginas igual se actualizan solas cada minuto
        }
    }

    private JsonObject pedir(String metodo, String ruta, JsonObject body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(API + ruta))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "QuasoPlugin-Web");
        if (body == null) {
            builder.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json");
            builder.method(metodo, HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        }
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            String mensaje = "";
            try {
                mensaje = JsonParser.parseString(response.body()).getAsJsonObject().get("message").getAsString();
            } catch (Exception ignored) {
                // Respuesta sin JSON
            }
            throw new ErrorGitHub(status, mensaje);
        }
        String texto = response.body();
        return texto == null || texto.isBlank() ? new JsonObject() : JsonParser.parseString(texto).getAsJsonObject();
    }
}
