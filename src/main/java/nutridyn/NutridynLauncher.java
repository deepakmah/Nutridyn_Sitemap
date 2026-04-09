package nutridyn;

import org.openqa.selenium.WebDriver;

/**
 * Entry point for both local runs and GitHub Actions.
 * After the E2E flow completes, it emails the HTML report.
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

        // Build path to the HTML report written during this run
        String htmlReport = NutridynConfig.OUTPUT_ROOT
                + "/html/" + NutridynReporting.RUN_DATE
                + "/" + NutridynReporting.RUN_TIME
                + "/TestReport.html";

        // Send email — credentials come from environment variables
        NutridynEmailer.sendReport(
                htmlReport,
                NutridynReporting.passedSteps,
                NutridynReporting.failedSteps,
                NutridynReporting.totalSteps
        );
    }
}
