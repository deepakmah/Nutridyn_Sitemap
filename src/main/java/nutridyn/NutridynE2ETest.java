package nutridyn;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.*;

/**
 * End-to-end TestNG suite.
 *
 * Key design decisions:
 *  • TC03 and TC04 both depend only on TC02 (login), NOT on each other —
 *    so a sitemap timeout in TC03 does NOT cause TC04 to be skipped.
 *  • @AfterSuite(alwaysRun=true) sends the email report even when tests fail.
 */
public class NutridynE2ETest {

    private WebDriver driver;

    @BeforeSuite
    public void initReporting() {
        NutridynReporting.beginNewRun();
    }

    /** Always runs after the full suite — emails the report pass or fail. */
    @AfterSuite(alwaysRun = true)
    public void sendEmailReport() {
        NutridynEmailer.sendReport(
            NutridynReporting.passedSteps,
            NutridynReporting.failedSteps,
            NutridynReporting.totalSteps
        );
    }

    @BeforeClass
    public void startBrowser() {
        driver = NutridynWebDriver.createChrome();
    }

    @AfterClass(alwaysRun = true)
    public void stopBrowser() {
        if (driver != null) { driver.quit(); driver = null; }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test(description = "Open storefront and accept cookies")
    public void TC01_openHomeAndAcceptCookies() throws Exception {
        NutridynFlow.openHomeAndAcceptCookies(driver);
    }

    @Test(dependsOnMethods = "TC01_openHomeAndAcceptCookies",
          description = "Log in as patient")
    public void TC02_loginPatient() throws Exception {
        NutridynFlow.loginPatient(driver);
    }

    // TC03 depends on TC02 only — sitemap timeout here will NOT skip TC04
    @Test(dependsOnMethods = "TC02_loginPatient",
          description = "Browse 3-4 random category pages from sitemap")
    public void TC03_browseSitemapCategories() throws Exception {
        NutridynFlow.browseRandomCategoriesFromSitemap(driver);
    }

    // TC04 also depends on TC02 only — runs independently of TC03
    @Test(dependsOnMethods = "TC02_loginPatient",
          description = "Add 2-3 random products from sitemap to cart")
    public void TC04_addSitemapProductsToCart() throws Exception {
        NutridynFlow.addRandomSitemapProductsToCart(driver);
    }

    // TC05 depends on TC04 — needs products in cart before checkout
    @Test(dependsOnMethods = "TC04_addSitemapProductsToCart",
          description = "Open mini-cart and proceed to checkout")
    public void TC05_openCartAndCheckout() throws Exception {
        NutridynFlow.openCartAndCheckout(driver);
    }

    @Test(dependsOnMethods = "TC05_openCartAndCheckout",
          description = "Return home via header logo")
    public void TC06_goHomeAfterCheckout() throws Exception {
        NutridynFlow.goHomeViaHeaderLogoAfterCheckout(driver);
    }

    @Test(dependsOnMethods = "TC06_goHomeAfterCheckout",
          description = "Account menu then home — prep for practitioner login")
    public void TC07_openAccountThenHome() throws Exception {
        NutridynFlow.openAccountThenHomeForPractitionerLogin(driver);
    }

    @Test(dependsOnMethods = "TC07_openAccountThenHome",
          description = "Log in as practitioner")
    public void TC08_loginPractitioner() throws Exception {
        NutridynFlow.loginPractitioner(driver);
    }

    @Test(dependsOnMethods = "TC08_loginPractitioner",
          description = "My Orders, first order view, Subscriptions, subscription view")
    public void TC09_practitionerOrdersAndSubscriptions() throws Exception {
        NutridynFlow.practitionerOrdersAndSubscriptions(driver);
    }

    @Test(dependsOnMethods = "TC09_practitionerOrdersAndSubscriptions",
          description = "Lists, My Patients, Address Book")
    public void TC10_practitionerListsPatientsAddress() throws Exception {
        NutridynFlow.practitionerListsPatientsAddress(driver);
    }

    @Test(dependsOnMethods = "TC10_practitionerListsPatientsAddress",
          description = "NutriScripts and View NutriScript")
    public void TC11_practitionerNutriScripts() throws Exception {
        NutridynFlow.practitionerNutriScripts(driver);
    }

    @Test(dependsOnMethods = "TC11_practitionerNutriScripts",
          description = "3X4 Genetics, NutriDyn Connect, Pro, Applet, Connect Links")
    public void TC12_practitionerConnectGenetics() throws Exception {
        NutridynFlow.practitionerConnectGeneticsAndLinks(driver);
    }

    @Test(dependsOnMethods = "TC12_practitionerConnectGenetics",
          description = "Home via logo and logout")
    public void TC13_practitionerHomeAndLogout() throws Exception {
        NutridynFlow.practitionerHomeLogoAndLogout(driver);
    }
}
