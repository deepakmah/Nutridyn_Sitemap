package nutridyn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/**
 * Reads NutriDyn sitemap.xml for product and category URLs.
 *
 * The nutridyn.com server blocks / times out connections from GitHub Actions
 * IPs. Both public methods return an EMPTY LIST (never throw) when the sitemap
 * is unreachable — the calling flow methods handle that gracefully.
 */
public final class NutridynSitemap {

    private NutridynSitemap() {}

    // ── HTTP ─────────────────────────────────────────────────────────────────

    private static String httpGetString(String urlStr) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        // Browser-like headers to avoid 403 blocks
        c.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0.0.0 Safari/537.36");
        c.setRequestProperty("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        c.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        c.setRequestProperty("Accept-Encoding", "identity");
        c.setRequestProperty("Connection", "keep-alive");
        // Short timeouts — fail fast so the test doesn't hang 30 s
        c.setConnectTimeout(10_000);
        c.setReadTimeout(20_000);

        int code = c.getResponseCode();
        if (code >= 400) {
            throw new IOException("HTTP " + code + " for " + urlStr);
        }
        try (InputStream in = c.getInputStream();
             ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            in.transferTo(buf);
            return buf.toString(StandardCharsets.UTF_8);
        }
    }

    // ── Extraction helpers ───────────────────────────────────────────────────

    private static List<String> dedupeUrls(List<String> urls) {
        return new ArrayList<>(new LinkedHashSet<>(urls));
    }

    private static List<String> extractProductUrlsFromUrlset(String xml) {
        List<String> out = new ArrayList<>();
        Pattern loc = Pattern.compile("<loc>(https://nutridyn\\.com[^<]*)</loc>");
        for (String chunk : xml.split("</url>")) {
            if (!chunk.contains("image:image") || !chunk.contains("catalog/product")) continue;
            Matcher m = loc.matcher(chunk);
            if (!m.find()) continue;
            String u = m.group(1).trim();
            if (u.equals("https://nutridyn.com/") || u.equals("https://nutridyn.com")) continue;
            if (u.contains("/catalog/category/")) continue;
            try {
                long segs = Arrays.stream(URI.create(u).normalize().getPath().split("/"))
                    .filter(s -> !s.isEmpty()).count();
                if (segs < 2) continue;
            } catch (Exception ex) { continue; }
            out.add(u);
        }
        return out;
    }

    private static List<String> extractCategoryUrlsFromUrlset(String xml) {
        List<String> out = new ArrayList<>();
        Pattern loc = Pattern.compile("<loc>(https://nutridyn\\.com[^<]*)</loc>");
        for (String chunk : xml.split("</url>")) {
            if (chunk.contains("pub/media/catalog/product")) continue;
            if (!chunk.contains("<priority>0.5</priority>")) continue;
            Matcher m = loc.matcher(chunk);
            if (!m.find()) continue;
            String u = m.group(1).trim();
            if (u.equals("https://nutridyn.com/") || u.equals("https://nutridyn.com")) continue;
            if (u.contains("/catalog/category/view")) continue;
            try {
                long segs = Arrays.stream(URI.create(u).normalize().getPath().split("/"))
                    .filter(s -> !s.isEmpty()).count();
                if (segs < 1 || segs > 3) continue;
            } catch (Exception ex) { continue; }
            out.add(u);
        }
        return out;
    }

    // ── Internal loaders — NEVER throw, return empty list on any failure ──────

    private static List<String> loadAllProductUrls(String sitemapUrl) {
        try {
            String xml = httpGetString(sitemapUrl);
            if (xml.contains("<sitemapindex")) {
                List<String> merged = new ArrayList<>();
                Matcher m = Pattern.compile("<loc>(https://[^<]+\\.xml)</loc>").matcher(xml);
                while (m.find()) {
                    String child = m.group(1);
                    if (!child.contains("nutridyn.com")) continue;
                    try { merged.addAll(extractProductUrlsFromUrlset(httpGetString(child))); }
                    catch (Exception e) { System.err.println("Child sitemap skipped: " + e.getMessage()); }
                }
                return dedupeUrls(merged);
            }
            return dedupeUrls(extractProductUrlsFromUrlset(xml));
        } catch (Exception e) {
            System.err.println("⚠️  Sitemap product fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<String> loadAllCategoryUrls(String sitemapUrl) {
        try {
            String xml = httpGetString(sitemapUrl);
            if (xml.contains("<sitemapindex")) {
                List<String> merged = new ArrayList<>();
                Matcher m = Pattern.compile("<loc>(https://[^<]+\\.xml)</loc>").matcher(xml);
                while (m.find()) {
                    String child = m.group(1);
                    if (!child.contains("nutridyn.com")) continue;
                    try { merged.addAll(extractCategoryUrlsFromUrlset(httpGetString(child))); }
                    catch (Exception e) { System.err.println("Child sitemap skipped: " + e.getMessage()); }
                }
                return dedupeUrls(merged);
            }
            return dedupeUrls(extractCategoryUrlsFromUrlset(xml));
        } catch (Exception e) {
            System.err.println("⚠️  Sitemap category fetch failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns up to {@code maxCount} shuffled product URLs, or empty list if sitemap is down. */
    public static List<String> loadProductUrlsFromSitemap(String sitemapUrl, int maxCount) {
        List<String> all = loadAllProductUrls(sitemapUrl);
        if (all.isEmpty()) return all;
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(maxCount, all.size())));
    }

    /** Returns up to {@code maxCount} shuffled category URLs, or empty list if sitemap is down. */
    public static List<String> loadCategoryUrlsFromSitemap(String sitemapUrl, int maxCount) {
        List<String> all = loadAllCategoryUrls(sitemapUrl);
        if (all.isEmpty()) return all;
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(maxCount, all.size())));
    }

    public static String productUrlSlug(String productUrl) {
        try {
            String[] parts = URI.create(productUrl).getPath().split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isEmpty())
                    return parts[i].replaceAll("[^a-zA-Z0-9_-]+", "_");
            }
        } catch (Exception ignored) {}
        return "product";
    }

    public static String categoryUrlSlug(String url) {
        String s = productUrlSlug(url);
        return "product".equals(s) ? "category" : s;
    }
}
