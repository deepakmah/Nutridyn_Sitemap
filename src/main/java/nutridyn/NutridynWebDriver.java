package nutridyn;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

public final class NutridynWebDriver {

    private NutridynWebDriver() {}

    /**
     * Creates a ChromeDriver instance.
     * - If the environment variable CI=true (set automatically by GitHub Actions),
     *   headless mode is enabled so Chrome can run on a server with no display.
     * - For local runs it opens a visible browser window as before.
     */
    public static WebDriver createChrome() {
        ChromeOptions options = new ChromeOptions();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);

        // Detect GitHub Actions (or any CI environment)
        boolean isCI = "true".equalsIgnoreCase(System.getenv("CI"));

        if (isCI) {
            options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1280,900"
            );
            System.out.println("🤖 CI detected — Chrome running in headless mode.");
        } else {
            options.addArguments(
                "--start-maximized",
                "--disable-save-password-bubble",
                "--password-store=basic",
                "--disable-features=PasswordLeakDetection"
            );
        }

        WebDriver driver = new ChromeDriver(options);
        driver.manage().window().maximize();
        return driver;
    }
}
