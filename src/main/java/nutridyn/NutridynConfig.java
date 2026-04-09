package nutridyn;

/**
 * Central config. Sensitive values (emails, passwords) come from environment
 * variables so they are never hardcoded — works for both local runs and
 * GitHub Actions secrets.
 */
public final class NutridynConfig {

    private NutridynConfig() {}

    /**
     * Where screenshots, HTML reports, and CSV are saved.
     * - GitHub Actions: uses RUNNER_TEMP (e.g. /tmp/nutridyn_output)
     * - Local Windows:  falls back to the hardcoded path below
     */
    public static final String OUTPUT_ROOT = getEnvOrDefault(
            "NUTRIDYN_OUTPUT_ROOT",
            "C:/Users/deepa/Documents/Automation/Nutridyn"
    );

    public static final String BASE_URL     = "https://nutridyn.com/";
    public static final String SITEMAP_URL  = "https://nutridyn.com/pub/media/sitemap.xml";

    /**
     * Test credentials — read from environment variables.
     * Set these in GitHub Actions Secrets:
     *   PATIENT_EMAIL, PATIENT_PASSWORD, PRACTITIONER_EMAIL, PRACTITIONER_PASSWORD
     * For local runs, set them as system environment variables OR keep the fallback values below.
     */
    public static final String PATIENT_EMAIL          = getEnvOrDefault("PATIENT_EMAIL",          "gopal.exinentpatient@gmail.com");
    public static final String PATIENT_PASSWORD       = getEnvOrDefault("PATIENT_PASSWORD",       "test@1234");
    public static final String PRACTITIONER_EMAIL     = getEnvOrDefault("PRACTITIONER_EMAIL",     "foo.bar@getastra.live");
    public static final String PRACTITIONER_PASSWORD  = getEnvOrDefault("PRACTITIONER_PASSWORD",  "123456");

    // ── Helper ───────────────────────────────────────────────────────────────
    private static String getEnvOrDefault(String envKey, String defaultValue) {
        String val = System.getenv(envKey);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : defaultValue;
    }
}
