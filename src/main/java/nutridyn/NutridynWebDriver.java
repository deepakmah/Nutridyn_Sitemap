package nutridyn;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.HashMap;
import java.util.Map;

public final class NutridynWebDriver {

    private NutridynWebDriver() {}

    /**
     * Creates ChromeDriver.
     * If CI=true (GitHub Actions sets this automatically) → headless mode.
     * Otherwise → visible browser for local runs.
     */
    public static WebDriver createChrome() {
        ChromeOptions options = new ChromeOptions();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        options.setExperimentalOption("prefs", prefs);

        boolean isCI = "true".equalsIgnoreCase(System.getenv("CI"));

        if (isCI) {
            options.addArguments(
                "--headless=new",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--window-size=1280,900",
                "--disable-save-password-bubble",
                "--password-store=basic",
                "--disable-features=PasswordLeakDetection"
            );
            System.out.println("🤖 CI mode — Chrome running headless.");
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
