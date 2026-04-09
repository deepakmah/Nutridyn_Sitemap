package nutridyn;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.zip.*;

/**
 * Sends the HTML test report + screenshots by email after each run.
 * Credentials are read from environment variables (GitHub Secrets) — never hardcoded.
 *
 * Required GitHub Secrets:
 *   GMAIL_SENDER_EMAIL   — Gmail address that sends the email
 *   GMAIL_APP_PASSWORD   — 16-char Gmail App Password (NOT your real password)
 *
 * Required GitHub Variable:
 *   REPORT_RECIPIENT     — e.g. sangeetha.arora@exinent.com
 */
public final class NutridynEmailer {

    private NutridynEmailer() {}

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    public static void sendReport(int passed, int failed, int total) {

        String senderEmail = System.getenv("GMAIL_SENDER_EMAIL");
        String appPassword  = System.getenv("GMAIL_APP_PASSWORD");
        String recipient    = System.getenv("REPORT_RECIPIENT");

        if (isBlank(senderEmail) || isBlank(appPassword) || isBlank(recipient)) {
            System.err.println("⚠️  Email skipped — missing env vars: "
                + "GMAIL_SENDER_EMAIL, GMAIL_APP_PASSWORD, REPORT_RECIPIENT");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, appPassword);
            }
        });

        try {
            double passRate      = total > 0 ? (passed * 100.0 / total) : 0;
            String overallStatus = failed > 0 ? "FAILED" : "PASSED";
            String statusIcon    = failed > 0 ? "❌" : "✅";
            String bodyColor     = failed > 0 ? "#dc3545" : "#28a745";

            // ── Locate HTML report ───────────────────────────────────────────
            String htmlDir = NutridynConfig.OUTPUT_ROOT
                + "/html/" + NutridynReporting.RUN_DATE
                + "/" + NutridynReporting.RUN_TIME;
            File htmlReport = new File(htmlDir, "TestReport.html");

            // ── Locate screenshots folder ────────────────────────────────────
            String ssDir = NutridynConfig.OUTPUT_ROOT
                + "/screenshots/" + NutridynReporting.RUN_DATE
                + "/" + NutridynReporting.RUN_TIME;
            File screenshotsFolder = new File(ssDir);

            // ── Build email ──────────────────────────────────────────────────
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(
                "NutriDyn Automation — " + statusIcon + " " + overallStatus
                + " | " + NutridynReporting.RUN_DATE
                + " " + NutridynReporting.RUN_TIME.replace("-", ":")
            );

            // ── HTML body ────────────────────────────────────────────────────
            String htmlBody = "<!DOCTYPE html><html><body style=\"font-family:Segoe UI,Arial,sans-serif;"
                + "padding:30px;background:#f5f7fa\">"
                + "<div style=\"max-width:620px;margin:0 auto;background:#fff;border-radius:12px;"
                + "padding:30px;box-shadow:0 4px 15px rgba(0,0,0,.1)\">"
                + "<div style=\"background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;"
                + "padding:25px;border-radius:10px;text-align:center;margin-bottom:25px\">"
                + "<h1 style=\"margin:0;font-weight:300\">NutriDyn Automation</h1>"
                + "<p style=\"margin:8px 0 0\">Test Execution Report</p></div>"
                + "<div style=\"text-align:center;margin-bottom:25px\">"
                + "<span style=\"display:inline-block;padding:10px 30px;border-radius:25px;"
                + "background:" + bodyColor + ";color:#fff;font-weight:bold;font-size:1.2em\">"
                + statusIcon + " " + overallStatus + "</span></div>"
                + "<table style=\"width:100%;border-collapse:collapse;margin-bottom:25px\">"
                + row("📅 Run Date",    NutridynReporting.RUN_DATE)
                + row("⏰ Run Time",    NutridynReporting.RUN_TIME.replace("-", ":"))
                + row("📊 Total Steps", String.valueOf(total))
                + row("✅ Passed",      "<strong style=\"color:#28a745\">" + passed + "</strong>")
                + row("❌ Failed",      "<strong style=\"color:#dc3545\">" + failed + "</strong>")
                + row("📈 Pass Rate",   String.format("%.1f%%", passRate))
                + "</table>"
                + "<div style=\"background:#f8f9fa;border-radius:8px;padding:15px;"
                + "border-left:4px solid #667eea;margin-bottom:20px\">"
                + "<p style=\"margin:0;color:#555\">📎 Attachments:<br>"
                + "&nbsp;&nbsp;• <strong>TestReport.html</strong> — open in any browser "
                + "to see all steps with full-page screenshots<br>"
                + "&nbsp;&nbsp;• <strong>screenshots.zip</strong> — all PNG screenshots "
                + "from this run</p></div>"
                + "<hr style=\"border:none;border-top:1px solid #dee2e6\"/>"
                + "<p style=\"color:#aaa;font-size:12px;text-align:center;margin:15px 0 0\">"
                + "Sent automatically by NutriDyn Automation via GitHub Actions</p>"
                + "</div></body></html>";

            Multipart multipart = new MimeMultipart();

            // Body part
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(bodyPart);

            // Attach HTML report
            if (htmlReport.exists()) {
                MimeBodyPart attachHtml = new MimeBodyPart();
                attachHtml.attachFile(htmlReport);
                attachHtml.setFileName("NutriDyn_TestReport_"
                    + NutridynReporting.RUN_DATE + "_"
                    + NutridynReporting.RUN_TIME + ".html");
                multipart.addBodyPart(attachHtml);
                System.out.println("📎 Attaching HTML report: " + htmlReport.getAbsolutePath());
            } else {
                System.out.println("⚠️  HTML report not found at: " + htmlReport.getAbsolutePath());
            }

            // Attach screenshots as a zip (keeps email size manageable)
            if (screenshotsFolder.exists() && screenshotsFolder.isDirectory()) {
                File zipFile = zipScreenshots(screenshotsFolder);
                if (zipFile != null) {
                    MimeBodyPart attachZip = new MimeBodyPart();
                    attachZip.attachFile(zipFile);
                    attachZip.setFileName("screenshots_"
                        + NutridynReporting.RUN_DATE + "_"
                        + NutridynReporting.RUN_TIME + ".zip");
                    multipart.addBodyPart(attachZip);
                    System.out.println("📎 Attaching screenshots zip: " + zipFile.getAbsolutePath());
                }
            } else {
                System.out.println("⚠️  Screenshots folder not found at: " + screenshotsFolder.getAbsolutePath());
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Report email sent successfully to: " + recipient);

        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Zips all PNG files in the screenshots folder into a temp file. */
    private static File zipScreenshots(File folder) {
        try {
            File zipFile = File.createTempFile("nutridyn_screenshots_", ".zip");
            zipFile.deleteOnExit();
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                File[] pngs = folder.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
                if (pngs == null || pngs.length == 0) return null;
                for (File png : pngs) {
                    zos.putNextEntry(new ZipEntry(png.getName()));
                    Files.copy(png.toPath(), zos);
                    zos.closeEntry();
                }
            }
            return zipFile;
        } catch (IOException e) {
            System.err.println("⚠️  Could not create screenshots zip: " + e.getMessage());
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String row(String label, String value) {
        return "<tr>"
            + "<td style=\"padding:10px 14px;border:1px solid #dee2e6;"
            + "background:#f8f9fa;font-weight:600;width:40%\">" + label + "</td>"
            + "<td style=\"padding:10px 14px;border:1px solid #dee2e6\">" + value + "</td>"
            + "</tr>";
    }
}
