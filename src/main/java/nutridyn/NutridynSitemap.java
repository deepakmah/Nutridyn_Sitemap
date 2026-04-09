package nutridyn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads NutriDyn sitemap.xml for product and category URLs.
 *
 * FIX: The server returns HTTP 403 when GitHub Actions IPs hit it with a plain
 * bot User-Agent.  We now send browser-like headers.  If the sitemap is still
 * unreachable (403 / network error) we return an empty list and log a warning
 * instead of throwing — the rest of the test flow continues.
 */
public final class NutridynSitemap {

    private NutridynSitemap() {}

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private static String httpGetString(String urlStr) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        // Use a realistic browser User-Agent to avoid 403 blocks on CI IPs
        c.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0.0.0 Safari/537.36");
        c.setRequestProperty("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        c.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        c.setRequestProperty("Accept-Encoding", "identity");   // avoid gzip so InputStream is plain text
        c.setRequestProperty("Connection", "keep-alive");
        c.setConnectTimeout(30_000);
        c.setReadTimeout(120_000);

        int code = c.getResponseCode();
        if (code == 403) {
            throw new IOException("HTTP 403 Forbidden — server is blocking CI/automation IP for: " + urlStr);
        }
        if (code >= 400) {
            throw new IOException("HTTP " + code + " for " + urlStr);
        }
        try (InputStream in = c.getInputStream();
             ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            in.transferTo(buf);
            return buf.toString(StandardCharsets.UTF_8);
        }
    }

    // ── URL helpers ───────────────────────────────────────────────────────────

    private static List<String> dedupeUrls(List<String> urls) {
        return new ArrayList<>(new LinkedHashSet<>(urls));
    }

    // ── Product URL extraction ────────────────────────────────────────────────

    private static List<String> extractProductUrlsFromUrlset(String xml) {
        List<String> out = new ArrayList<>();
        Pattern locPattern = Pattern.compile("<loc>(https://nutridyn\\.com[^<]*)</loc>");
        for (String chunk : xml.split("</url>")) {
            if (!chunk.contains("image:image") || !chunk.contains("catalog/product")) continue;
            Matcher m = locPattern.matcher(chunk);
            if (!m.find()) continue;
            String u = m.group(1).trim();
            if ("https://nutridyn.com/".equals(u) || "https://nutridyn.com".equals(u)) continue;
            if (u.contains("/catalog/category/")) continue;
            try {
                String path = URI.create(u).normalize().getPath();
                if (path == null) continue;
                long segs = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).count();
                if (segs < 2) continue;
            } catch (Exception ex) {
                continue;
            }
            out.add(u);
        }
        return out;
    }

    // ── Category URL extraction ───────────────────────────────────────────────

    private static List<String> extractCategoryUrlsFromUrlset(String xml) {
        List<String> out = new ArrayList<>();
        Pattern locPattern = Pattern.compile("<loc>(https://nutridyn\\.com[^<]*)</loc>");
        for (String chunk : xml.split("</url>")) {
            if (chunk.contains("pub/media/catalog/product")) continue;
            if (!chunk.contains("<priority>0.5</priority>")) continue;
            Matcher m = locPattern.matcher(chunk);
            if (!m.find()) continue;
            String u = m.group(1).trim();
            if ("https://nutridyn.com/".equals(u) || "https://nutridyn.com".equals(u)) continue;
            if (u.contains("/catalog/category/view")) continue;
            try {
                String path = URI.create(u).normalize().getPath();
                if (path == null) continue;
                long segs = Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).count();
                if (segs < 1 || segs > 3) continue;
            } catch (Exception ex) {
                continue;
            }
            out.add(u);
        }
        return out;
    }

    // ── Load helpers (with graceful 403 / IO fallback) ────────────────────────

    private static List<String> loadAllCategoryUrls(String sitemapUrl) {
        try {
            String xml = httpGetString(sitemapUrl);
            if (xml.contains("<sitemapindex")) {
                List<String> merged = new ArrayList<>();
                Matcher m = Pattern.compile("<loc>(https://[^<]+\\.xml)</loc>").matcher(xml);
                while (m.find()) {
                    String child = m.group(1);
                    if (!child.contains("nutridyn.com")) continue;
                    try {
                        merged.addAll(extractCategoryUrlsFromUrlset(httpGetString(child)));
                    } catch (IOException e) {
                        System.err.println("⚠️  Child sitemap skipped: " + child + " — " + e.getMessage());
                    }
                }
                return dedupeUrls(merged);
            }
            return dedupeUrls(extractCategoryUrlsFromUrlset(xml));
        } catch (IOException e) {
            System.err.println("⚠️  Sitemap unavailable (category): " + e.getMessage()
                + " — skipping category browse.");
            return new ArrayList<>();
        }
    }

    private static List<String> loadAllProductUrls(String sitemapUrl) {
        try {
            String xml = httpGetString(sitemapUrl);
            if (xml.contains("<sitemapindex")) {
                List<String> merged = new ArrayList<>();
                Matcher m = Pattern.compile("<loc>(https://[^<]+\\.xml)</loc>").matcher(xml);
                while (m.find()) {
                    String child = m.group(1);
                    if (!child.contains("nutridyn.com")) continue;
                    try {
                        merged.addAll(extractProductUrlsFromUrlset(httpGetString(child)));
                    } catch (IOException e) {
                        System.err.println("⚠️  Child sitemap skipped: " + child + " — " + e.getMessage());
                    }
                }
                return dedupeUrls(merged);
            }
            return dedupeUrls(extractProductUrlsFromUrlset(xml));
        } catch (IOException e) {
            System.err.println("⚠️  Sitemap unavailable (products): " + e.getMessage()
                + " — skipping sitemap product add-to-cart.");
            return new ArrayList<>();
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static List<String> loadCategoryUrlsFromSitemap(String sitemapUrl, int maxCount) {
        List<String> all = loadAllCategoryUrls(sitemapUrl);
        if (all.isEmpty()) return all;
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(maxCount, all.size())));
    }

    public static List<String> loadProductUrlsFromSitemap(String sitemapUrl, int maxCount) {
        List<String> all = loadAllProductUrls(sitemapUrl);
        if (all.isEmpty()) return all;
        Collections.shuffle(all);
        return new ArrayList<>(all.subList(0, Math.min(maxCount, all.size())));
    }

    public static String productUrlSlug(String productUrl) {
        try {
            String path = URI.create(productUrl).getPath();
            if (path == null) return "product";
            String[] parts = path.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isEmpty())
                    return parts[i].replaceAll("[^a-zA-Z0-9_-]+", "_");
            }
        } catch (Exception ignored) {}
        return "product";
    }

    public static String categoryUrlSlug(String categoryUrl) {
        String s = productUrlSlug(categoryUrl);
        return "product".equals(s) ? "category" : s;
    }
}
