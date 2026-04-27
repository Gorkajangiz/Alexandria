package services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class IGDBService {

    private static final String CLIENT_ID = "3wr5arw16kfu2vhd6lhuyt5fn4moc2";
    private static final String CLIENT_SECRET = "sznzqk7pnjn53bn8sub5vlzypn29vc";

    /**
     * IGDB image size tokens (from smallest to largest):
     *   t_thumb (90x128), t_cover_small (90x128), t_logo_med (284x160),
     *   t_screenshot_med (569x320), t_cover_big (264x374),
     *   t_720p (1280x720), t_1080p (1920x1080)
     * We use t_cover_big for card thumbnails and t_1080p for detail views.
     */
    public static final String SIZE_CARD = "t_1080p";
    public static final String SIZE_HERO = "t_1080p";

    private String accessToken;
    private HttpClient client;

    public IGDBService() {
        this.accessToken = null;
        this.client = HttpClient.newHttpClient();
    }

    private void renovarToken() throws Exception {
        String params = String.format("client_id=%s&client_secret=%s&grant_type=client_credentials",
                CLIENT_ID, CLIENT_SECRET);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://id.twitch.tv/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(params))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            this.accessToken = json.getString("access_token");
        } else {
            throw new RuntimeException("No se pudo obtener el token de acceso de Twitch");
        }
    }

    private void ensureToken() throws Exception {
        if (accessToken == null) {
            renovarToken();
        }
    }

    /**
     * Search for games. Returns up to 20 results.
     * Cover URLs are returned in t_cover_big quality (ready for HD cards).
     */
    public List<JSONObject> buscarJuegos(String nombre) {
        return buscarJuegos(nombre, false);
    }

    public List<JSONObject> buscarJuegos(String nombre, boolean soloCalificados) {
        List<JSONObject> juegos = new ArrayList<>();

        try {
            ensureToken();

            String query;
            if (nombre == null || nombre.trim().isEmpty()) {
                query = "fields name, cover.url, cover.image_id, total_rating, first_release_date, summary, genres.name; where total_rating >= 60 & aggregated_rating_count > 3 & cover != null & first_release_date != null; sort first_release_date desc; limit 200;";
            } else {
                if (soloCalificados) {
                    query = String.format(
                            "fields name, cover.url, cover.image_id, total_rating, first_release_date, summary, genres.name; search \"%s\"; where total_rating >= 1; limit 200;",
                            nombre.replace("\"", "")
                    );
                } else {
                    query = String.format(
                            "fields name, cover.url, cover.image_id, total_rating, first_release_date, summary, genres.name; search \"%s\"; limit 200;",
                            nombre.replace("\"", "")
                    );
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.igdb.com/v4/games"))
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(query))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray resultados = new JSONArray(response.body());
                for (int i = 0; i < resultados.length(); i++) {
                    JSONObject obj = resultados.getJSONObject(i);
                    // Upgrade cover URL to high quality
                    if (obj.has("cover") && obj.getJSONObject("cover").has("url")) {
                        JSONObject cover = obj.getJSONObject("cover");
                        String hdUrl = upgradeImageUrl(cover.getString("url"), SIZE_CARD);
                        cover.put("url", hdUrl);
                    }
                    juegos.add(obj);
                }
            } else if (response.statusCode() == 401) {
                // Token expired — refresh and retry once
                accessToken = null;
                renovarToken();
                return buscarJuegos(nombre, soloCalificados);
            }

        } catch (Exception e) {
            System.err.println("Error en la búsqueda de IGDB: " + e.getMessage());
        }

        return juegos;
    }

    public JSONObject getGameDetails(int id) {
        try {
            ensureToken();

            String query = String.format(
                "fields name, summary, cover.url, first_release_date, total_rating, genres.name; where id = %d;", id);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.igdb.com/v4/games"))
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(query))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONArray resultados = new JSONArray(response.body());
                if (resultados.length() > 0) {
                    JSONObject obj = resultados.getJSONObject(0);
                    if (obj.has("cover") && obj.getJSONObject("cover").has("url")) {
                        JSONObject cover = obj.getJSONObject("cover");
                        String hdUrl = upgradeImageUrl(cover.getString("url"), SIZE_CARD);
                        cover.put("url", hdUrl);
                    }
                    return obj;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<JSONObject> getGamesMetadata(List<String> ids) {
        if (ids == null || ids.isEmpty()) return new java.util.ArrayList<>();
        try {
            ensureToken();
            String idString = String.join(",", ids);
            String query = "fields name, cover.url, total_rating, first_release_date, summary, genres.name; where id = (" + idString + "); limit 500;";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.igdb.com/v4/games"))
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer " + accessToken)
                    .POST(HttpRequest.BodyPublishers.ofString(query))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                org.json.JSONArray arr = new org.json.JSONArray(response.body());
                List<JSONObject> list = new java.util.ArrayList<>();
                for (int i = 0; i < arr.length(); i++) list.add(arr.getJSONObject(i));
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.ArrayList<>();
    }

    /**
     * Upgrades an IGDB image URL to a target size token.
     * IGDB URLs look like: //images.igdb.com/igdb/image/upload/t_thumb/co1234.jpg
     * We replace the size segment and prepend https://.
     */
    public static String upgradeImageUrl(String rawUrl, String sizeToken) {
        if (rawUrl == null || rawUrl.isEmpty()) return rawUrl;
        // Ensure https
        String url = rawUrl.startsWith("//") ? "https:" + rawUrl : rawUrl;
        // Replace any existing size token with the desired one
        return url.replaceAll("/t_[a-z0-9_]+/", "/" + sizeToken + "/");
    }
}