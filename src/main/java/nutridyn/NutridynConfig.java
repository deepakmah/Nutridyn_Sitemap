package nutridyn;

/**
 * Central config. All paths and credentials read from environment variables.
 * - GitHub Actions: secrets/variables injected by workflow
 * - Local runs: falls back to hardcoded defaults below
 */
public final class NutridynConfig {

    private NutridynConfig() {}

    /**
     * Where screenshots, HTML reports, and CSV are saved.
     * GitHub Actions sets GITHUB_WORKSPACE automatically.
     * We write into a "test-output" subfolder so artifacts can be uploaded.
     */
    public static final String OUTPUT_ROOT = buildOutputRoot();

    public static final String BASE_URL    = "https://nutridyn.com/";
    public static final String SITEMAP_URL = "https://nutridyn.com/pub/media/sitemap.xml";

    // Credentials — read from GitHub Secrets, fall back to local defaults
    public static final String PATIENT_EMAIL         = getEnv("PATIENT_EMAIL",         "gopal.exinentpatient@gmail.com");
    public static final String PATIENT_PASSWORD      = getEnv("PATIENT_PASSWORD",      "test@1234");
    public static final String PRACTITIONER_EMAIL    = getEnv("PRACTITIONER_EMAIL",    "foo.bar@getastra.live");
    public static final String PRACTITIONER_PASSWORD = getEnv("PRACTITIONER_PASSWORD", "123456");

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String buildOutputRoot() {
        // GitHub Actions sets GITHUB_WORKSPACE automatically (e.g. /home/runner/work/Repo/Repo)
        String ws = System.getenv("GITHUB_WORKSPACE");
        if (ws != null && !ws.trim().isEmpty()) {
            return ws.trim() + "/test-output";
        }
        // Local Windows fallback
        return "C:/Users/deepa/Documents/Automation/Nutridyn";
    }

    private static String getEnv(String key, String defaultVal) {
        String v = System.getenv(key);
        return (v != null && !v.trim().isEmpty()) ? v.trim() : defaultVal;
    }
}
