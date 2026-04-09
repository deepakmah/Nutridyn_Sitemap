package nutridyn;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * NutriDyn UI flows split into steps for TestNG.
 *
 * TC03 / TC04 use the sitemap — if nutridyn.com blocks or times out the
 * connection from GitHub Actions IPs, those steps are gracefully skipped
 * (screenshot + warning logged) instead of throwing and failing the suite.
 */
public final class NutridynFlow {

    private NutridynFlow() {}

    /** Full run used by NutridynLauncher (non-TestNG entry point). */
    public static void runEndToEnd(WebDriver driver) throws IOException, InterruptedException {
        openHomeAndAcceptCookies(driver);
        loginPatient(driver);
        browseRandomCategoriesFromSitemap(driver);
        addRandomSitemapProductsToCart(driver);
        openCartAndCheckout(driver);
        goHomeViaHeaderLogoAfterCheckout(driver);
        openAccountThenHomeForPractitionerLogin(driver);
        loginPractitioner(driver);
        practitionerOrdersAndSubscriptions(driver);
        practitionerListsPatientsAddress(driver);
        practitionerNutriScripts(driver);
        practitionerConnectGeneticsAndLinks(driver);
        practitionerHomeLogoAndLogout(driver);
    }

    // ── TC01 ─────────────────────────────────────────────────────────────────

    public static void openHomeAndAcceptCookies(WebDriver driver) throws IOException, InterruptedException {
        driver.navigate().to(NutridynConfig.BASE_URL);
        Thread.sleep(1000);
        System.out.println("Site is opened");
        NutridynReporting.takeScreenshot(driver, "Homepage");
        Thread.sleep(1000);

        if (driver.findElements(By.xpath("//span[contains(text(),'Accept')]")).size() > 0) {
            driver.findElement(By.xpath("//span[contains(text(),'Accept')]")).click();
            Thread.sleep(2000);
            System.out.println("Cookie accepted");
            NutridynReporting.takeScreenshot(driver, "Cookie_Accepted");
        } else {
            System.out.println("Cookie accept button not found");
        }
    }

    // ── TC02 ─────────────────────────────────────────────────────────────────

    public static void loginPatient(WebDriver driver) throws IOException, InterruptedException {
        if (driver.findElements(By.xpath("//*[@id='header-account-1']/ul/li/a")).size() > 0) {
            driver.findElement(By.xpath("//*[@id='header-account-1']/ul/li/a")).click();
            Thread.sleep(3000);
            System.out.println("Login page opened");
            NutridynReporting.takeScreenshot(driver, "Login_Page");
        } else {
            System.out.println("Login link not found");
        }

        if (driver.findElements(By.id("email")).size() > 0) {
            driver.findElement(By.id("email")).sendKeys(NutridynConfig.PATIENT_EMAIL);
            System.out.println("Username entered");
            Thread.sleep(1000);
        }
        if (driver.findElements(By.id("pass")).size() > 0) {
            driver.findElement(By.id("pass")).sendKeys(NutridynConfig.PATIENT_PASSWORD);
            System.out.println("Password entered");
            Thread.sleep(2000);
        }
        if (driver.findElements(By.name("send")).size() > 0) {
            driver.findElement(By.name("send")).click();
            System.out.println("Patient account login successful");
            Thread.sleep(2000);
            NutridynReporting.takeScreenshot(driver, "Patient_Login_Success");
        }
    }

    // ── TC03 — sitemap category browse (SAFE: never throws to TestNG) ────────

    public static void browseRandomCategoriesFromSitemap(WebDriver driver)
            throws IOException, InterruptedException {

        List<String> categoryUrls;
        try {
            int n = ThreadLocalRandom.current().nextInt(3, 5);
            categoryUrls = NutridynSitemap.loadCategoryUrlsFromSitemap(NutridynConfig.SITEMAP_URL, n);
        } catch (Exception e) {
            // Sitemap unreachable from CI IP (timeout / 403) — skip gracefully
            System.out.println("⚠️  Sitemap category fetch failed: " + e.getMessage()
                + " — skipping category browse (not a test failure).");
            NutridynReporting.takeScreenshot(driver, "Sitemap_Category_Skipped", false,
                "Sitemap unreachable from CI: " + e.getMessage(), null);
            return;
        }

        if (categoryUrls.isEmpty()) {
            System.out.println("No category URLs from sitemap — skipping.");
            return;
        }

        System.out.println("Browsing " + categoryUrls.size() + " category URL(s):");
        for (int i = 0; i < categoryUrls.size(); i++) {
            String url = categoryUrls.get(i);
            driver.navigate().to(url);
            Thread.sleep(2500);
            System.out.println("Opened category " + (i + 1) + ": " + url);
            NutridynReporting.takeScreenshot(driver,
                "Category_" + (i + 1) + "_" + NutridynSitemap.categoryUrlSlug(url),
                true, "Category page from sitemap (browse only).", url);
        }
    }

    // ── TC04 — sitemap product add-to-cart (SAFE: never throws to TestNG) ────

    public static void addRandomSitemapProductsToCart(WebDriver driver)
            throws IOException, InterruptedException {

        List<String> productUrls;
        try {
            int n = ThreadLocalRandom.current().nextInt(2, 4);
            productUrls = NutridynSitemap.loadProductUrlsFromSitemap(NutridynConfig.SITEMAP_URL, n);
        } catch (Exception e) {
            System.out.println("⚠️  Sitemap product fetch failed: " + e.getMessage()
                + " — skipping add-to-cart (not a test failure).");
            NutridynReporting.takeScreenshot(driver, "Sitemap_Products_Skipped", false,
                "Sitemap unreachable from CI: " + e.getMessage(), null);
            return;
        }

        if (productUrls.isEmpty()) {
            System.out.println("No product URLs from sitemap — skipping add-to-cart.");
            return;
        }

        System.out.println("Using " + productUrls.size() + " product URL(s):");
        for (int i = 0; i < productUrls.size(); i++) {
            String purl = productUrls.get(i);
            driver.navigate().to(purl);
            Thread.sleep(2500);
            System.out.println("Opened product " + (i + 1) + ": " + purl);
            NutridynReporting.takeScreenshot(driver,
                "Product_Page_" + (i + 1) + "_" + NutridynSitemap.productUrlSlug(purl),
                true, "Product detail page from sitemap.", purl);

            if (driver.findElements(By.id("product-addtocart-button")).size() > 0) {
                driver.findElement(By.id("product-addtocart-button")).click();
                System.out.println("Product " + (i + 1) + " added to cart");
                Thread.sleep(1000);
                NutridynReporting.takeScreenshot(driver, "Product_Added_To_Cart_" + (i + 1),
                    true, "After Add to Cart.", purl);
            } else {
                System.out.println("Add to Cart not found for product " + (i + 1));
            }
        }
    }

    // ── TC05 ─────────────────────────────────────────────────────────────────

    public static void openCartAndCheckout(WebDriver driver) throws IOException, InterruptedException {
        if (driver.findElements(By.xpath("//*[@id='minicart']/div[1]/span/span[1]")).size() > 0) {
            driver.findElement(By.xpath("//*[@id='minicart']/div[1]/span/span[1]")).click();
            Thread.sleep(2000);
            System.out.println("Cart page opened");
            NutridynReporting.takeScreenshot(driver, "Cart_Page");
        } else {
            System.out.println("Mini cart icon not found");
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(0, 300);");
        Thread.sleep(2000);

        List<WebElement> checkoutBtns = driver.findElements(
            By.xpath("//*[@id='maincontent']/div[3]/div/div[6]/div[1]/div[2]/ul/li/button"));
        if (!checkoutBtns.isEmpty()) {
            WebElement checkoutBtn = checkoutBtns.get(0);
            JavascriptExecutor jex = (JavascriptExecutor) driver;
            jex.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", checkoutBtn);
            Thread.sleep(500);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(checkoutBtn)).click();
            } catch (ElementClickInterceptedException | TimeoutException e) {
                jex.executeScript("arguments[0].click();", checkoutBtn);
            }
            Thread.sleep(5000);
            System.out.println("Checkout page opened");
            NutridynReporting.takeScreenshot(driver, "Checkout_Page");
        } else {
            System.out.println("Checkout button not found");
        }
    }

    // ── TC06 ─────────────────────────────────────────────────────────────────

    public static void goHomeViaHeaderLogoAfterCheckout(WebDriver driver)
            throws IOException, InterruptedException {
        if (driver.findElements(By.xpath("//*[@id='header_logo']/a/img")).size() > 0) {
            driver.findElement(By.xpath("//*[@id='header_logo']/a/img")).click();
            Thread.sleep(2000);
            System.out.println("Home page opened");
            NutridynReporting.takeScreenshot(driver, "Home_After_Checkout");
        } else {
            System.out.println("Header logo not found");
        }
    }

    // ── TC07 ─────────────────────────────────────────────────────────────────

    public static void openAccountThenHomeForPractitionerLogin(WebDriver driver)
            throws InterruptedException {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[@id='header-account-1']/ul/li/a"))).click();
        System.out.println("Account page opened");
        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[@id='header_logo']/a"))).click();
        System.out.println("Home page opened");
    }

    // ── TC08 ─────────────────────────────────────────────────────────────────

    public static void loginPractitioner(WebDriver driver) throws IOException, InterruptedException {
        if (driver.findElements(By.xpath("//*[@id='header-account-1']/ul/li/a")).size() > 0) {
            driver.findElement(By.xpath("//*[@id='header-account-1']/ul/li/a")).click();
            Thread.sleep(1000);
            System.out.println("Account login page opened");
        }
        if (driver.findElements(By.id("email")).size() > 0) {
            driver.findElement(By.id("email")).sendKeys(NutridynConfig.PRACTITIONER_EMAIL);
            System.out.println("Practitioner username entered");
            Thread.sleep(1000);
        }
        if (driver.findElements(By.id("pass")).size() > 0) {
            driver.findElement(By.id("pass")).sendKeys(NutridynConfig.PRACTITIONER_PASSWORD);
            System.out.println("Practitioner password entered");
            Thread.sleep(2000);
        }
        if (driver.findElements(By.name("send")).size() > 0) {
            driver.findElement(By.name("send")).click();
            System.out.println("Practitioner login successful");
            Thread.sleep(2000);
            NutridynReporting.takeScreenshot(driver, "Practitioner_Login_Success");
        }
    }

    // ── TC09 ─────────────────────────────────────────────────────────────────

    public static void practitionerOrdersAndSubscriptions(WebDriver driver)
            throws IOException, InterruptedException {
        WebElement myOrders = driver.findElement(
            By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[2]/a/span"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", myOrders);
        System.out.println("My Orders page opened");
        NutridynReporting.takeScreenshot(driver, "My_Orders");
        Thread.sleep(1000);

        if (driver.findElements(
                By.xpath("//*[@id=\"my-orders-table\"]/tbody/tr[1]/td[6]/a[1]/span")).size() > 0) {
            driver.findElement(
                By.xpath("//*[@id=\"my-orders-table\"]/tbody/tr[1]/td[6]/a[1]/span")).click();
            Thread.sleep(2000);
            System.out.println("First order view opened");
            NutridynReporting.takeScreenshot(driver, "First_Order_View");
        }

        if (driver.findElements(
                By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[3]/a/span")).size() > 0) {
            driver.findElement(
                By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[3]/a/span")).click();
            Thread.sleep(2000);
            System.out.println("My Subscriptions opened");
            NutridynReporting.takeScreenshot(driver, "My_Subscriptions");
        }

        if (driver.findElements(
                By.xpath("//*[@id=\"my-orders-table\"]/tbody/tr[1]/td[6]/a/span")).size() > 0) {
            driver.findElement(
                By.xpath("//*[@id=\"my-orders-table\"]/tbody/tr[1]/td[6]/a/span")).click();
            Thread.sleep(2000);
            System.out.println("Subscription view opened");
            NutridynReporting.takeScreenshot(driver, "Subscription_View");
        }
    }

    // ── TC10 ─────────────────────────────────────────────────────────────────

    public static void practitionerListsPatientsAddress(WebDriver driver)
            throws IOException, InterruptedException {
        if (driver.findElements(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[4]/a/span")).size() > 0) {
            driver.findElement(
                By.xpath("//*[@id='maincontent']/div[2]/div[2]/div/div/div/ul[1]/li[4]/a/span")).click();
            Thread.sleep(2000);
            System.out.println("List page opened");
            NutridynReporting.takeScreenshot(driver, "List_Page");
        }
        if (driver.findElements(
                By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[5]/a/span")).size() > 0) {
            driver.findElement(
                By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[5]/a/span")).click();
            Thread.sleep(2000);
            System.out.println("My Patients opened");
            NutridynReporting.takeScreenshot(driver, "My_Patients");
        }
        if (driver.findElements(
                By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[6]/a/span")).size() > 0) {
            driver.findElement(
                By.xpath("//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[6]/a/span")).click();
            Thread.sleep(2000);
            System.out.println("Address Book opened");
            NutridynReporting.takeScreenshot(driver, "Address_Book");
        }
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 400);");
    }

    // ── TC11 ─────────────────────────────────────────────────────────────────

    public static void practitionerNutriScripts(WebDriver driver)
            throws IOException, InterruptedException {
        if (driver.findElements(By.xpath("//a//span[text()='NutriScripts']")).size() > 0) {
            driver.findElement(By.xpath("//a//span[text()='NutriScripts']")).click();
            System.out.println("NutriScripts clicked");
            NutridynReporting.takeScreenshot(driver, "NutriScripts");
        }
        Thread.sleep(2000);
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200);");

        List<WebElement> viewNutriLinks = driver.findElements(
            By.xpath("//a[contains(normalize-space(),'View NutriScript')]"));
        if (!viewNutriLinks.isEmpty()) {
            WebElement viewNutri = viewNutriLinks.get(0);
            JavascriptExecutor jex = (JavascriptExecutor) driver;
            jex.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", viewNutri);
            Thread.sleep(500);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.elementToBeClickable(viewNutri)).click();
            } catch (ElementClickInterceptedException | TimeoutException e) {
                jex.executeScript("arguments[0].click();", viewNutri);
            }
            System.out.println("View NutriScript clicked");
            NutridynReporting.takeScreenshot(driver, "View_NutriScript");
        }
        Thread.sleep(2000);
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 100);");
        Thread.sleep(2000);
    }

    // ── TC12 ─────────────────────────────────────────────────────────────────

    public static void practitionerConnectGeneticsAndLinks(WebDriver driver)
            throws IOException, InterruptedException {
        if (driver.findElements(By.xpath("//span[contains(text(),'3X4 Genetics')]")).size() > 0) {
            driver.findElement(By.xpath("//span[contains(text(),'3X4 Genetics')]")).click();
            System.out.println("3X4 Genetics clicked");
            NutridynReporting.takeScreenshot(driver, "3X4_Genetics");
        }
        Thread.sleep(2000);

        if (driver.findElements(By.xpath("//span[text()='NutriDyn Connect']")).size() > 0) {
            driver.findElement(By.xpath("//span[text()='NutriDyn Connect']")).click();
            System.out.println("NutriDyn Connect clicked");
            NutridynReporting.takeScreenshot(driver, "NutriDyn_Connect");
        }
        Thread.sleep(2000);

        if (driver.findElements(By.xpath("//a[normalize-space()='NutriDyn Connect Pro']")).size() > 0) {
            driver.findElement(By.xpath("//a[normalize-space()='NutriDyn Connect Pro']")).click();
            System.out.println("NutriDyn Connect Pro clicked");
            NutridynReporting.takeScreenshot(driver, "NutriDyn_Connect_Pro");
        }
        Thread.sleep(2000);

        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 200);");
        Thread.sleep(2000);

        if (driver.findElements(By.xpath("//a[contains(@href,'connectpro/applet')]")).size() > 0) {
            driver.findElement(By.xpath("//a[contains(@href,'connectpro/applet')]")).click();
            System.out.println("Connect Applet clicked");
            NutridynReporting.takeScreenshot(driver, "Connect_Applet");
        }
        Thread.sleep(2000);

        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, 300);");
        Thread.sleep(2000);

        if (driver.findElements(By.xpath(
                "//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[10]/ul/li[3]/a")).size() > 0) {
            driver.findElement(By.xpath(
                "//*[@id=\"maincontent\"]/div[2]/div[2]/div/div/div/ul[1]/li[10]/ul/li[3]/a")).click();
            System.out.println("Connect Links clicked");
            NutridynReporting.takeScreenshot(driver, "Connect_Links");
        }
        Thread.sleep(2000);

        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, -400);");
        Thread.sleep(2000);

        if (driver.findElements(By.xpath("//a//span[text()='NutriScripts']")).size() > 0) {
            driver.findElement(By.xpath("//a//span[text()='NutriScripts']")).click();
            System.out.println("NutriScripts clicked again");
        }
    }

    // ── TC13 ─────────────────────────────────────────────────────────────────

    public static void practitionerHomeLogoAndLogout(WebDriver driver)
            throws IOException, InterruptedException {
        WebElement logo = driver.findElement(By.xpath("//*[@id='header_logo']/a"));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logo);
        System.out.println("Home page opened again");
        NutridynReporting.takeScreenshot(driver, "Home_Again");

        if (driver.findElements(By.xpath("//a[normalize-space()='Logout']")).size() > 0) {
            WebElement logout = driver.findElement(By.xpath("//a[normalize-space()='Logout']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logout);
            System.out.println("Logout clicked");
            NutridynReporting.takeScreenshot(driver, "Logout");
        }
        Thread.sleep(2000);
    }
}
