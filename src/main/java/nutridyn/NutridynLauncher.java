package nutridyn;

import org.openqa.selenium.WebDriver;

/**
 * Entry point for both local runs and GitHub Actions.
 * After the E2E flow completes (pass or fail), it emails the HTML report.
 */
public final class NutridynLauncher {

    public static void main(String[] args) throws Exception {
        NutridynReporting.beginNewRun();
        WebDriver driver = NutridynWebDriver.createChrome();
        try {
            NutridynFlow.runEndToEnd(driver);
        } finally {
            driver.quit();
        }

        // Send email — paths built internally from NutridynConfig.OUTPUT_ROOT
        NutridynEmailer.sendReport(
            NutridynReporting.passedSteps,
            NutridynReporting.failedSteps,
            NutridynReporting.totalSteps
        );
    }
}
