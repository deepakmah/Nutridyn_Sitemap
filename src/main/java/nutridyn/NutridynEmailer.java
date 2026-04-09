package nutridyn;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.util.Properties;

/**
 * Sends the HTML test report by email after each automation run.
 *
 * Credentials are read from environment variables — never hardcoded.
 * In GitHub Actions these are injected via repository Secrets:
 *   GMAIL_SENDER_EMAIL  →  the Gmail account that sends the report
 *   GMAIL_APP_PASSWORD  →  16-char Gmail App Password (not your real password)
 *   REPORT_RECIPIENT    →  who receives the report (e.g. sangeetha.arora@exinent.com)
 */
public final class NutridynEmailer {

    private NutridynEmailer() {}

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int    SMTP_PORT = 587;

    public static void sendReport(String htmlReportPath, int passed, int failed, int total) {

        // ── Read credentials from environment variables ──────────────────────
        String senderEmail = System.getenv("GMAIL_SENDER_EMAIL");
        String appPassword  = System.getenv("GMAIL_APP_PASSWORD");
        String recipient    = System.getenv("REPORT_RECIPIENT");

        // Validate — skip sending if any value is missing
        if (isBlank(senderEmail) || isBlank(appPassword) || isBlank(recipient)) {
            System.err.println("⚠️  Email skipped: one or more environment variables are missing.");
            System.err.println("    Required: GMAIL_SENDER_EMAIL, GMAIL_APP_PASSWORD, REPORT_RECIPIENT");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        final String finalSender   = senderEmail;
        final String finalPassword = appPassword;

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(finalSender, finalPassword);
            }
        });

        try {
            double passRate     = total > 0 ? (passed * 100.0 / total) : 0;
            String overallStatus = failed > 0 ? "FAILED" : "PASSED";
            String statusIcon    = failed > 0 ? "❌" : "✅";
            String bodyColor     = failed > 0 ? "#dc3545" : "#28a745";

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(finalSender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(
                "NutriDyn Automation — " + statusIcon + " " + overallStatus
                + " | " + NutridynReporting.RUN_DATE
                + " " + NutridynReporting.RUN_TIME.replace("-", ":")
            );

            // ── HTML email body ──────────────────────────────────────────────
            String htmlBody = "<!DOCTYPE html><html><body style=\"font-family:Segoe UI,Arial,sans-serif;"
                + "padding:30px;background:#f5f7fa\">"
                + "<div style=\"max-width:600px;margin:0 auto;background:#fff;border-radius:12px;"
                + "padding:30px;box-shadow:0 4px 15px rgba(0,0,0,0.1)\">"
                + "<div style=\"background:linear-gradient(135deg,#667eea,#764ba2);color:#fff;"
                + "padding:25px;border-radius:10px;text-align:center;margin-bottom:25px\">"
                + "<h1 style=\"margin:0;font-weight:300\">NutriDyn Automation</h1>"
                + "<p style=\"margin:8px 0 0\">Test Execution Report</p>"
                + "</div>"
                + "<div style=\"text-align:center;margin-bottom:25px\">"
                + "<span style=\"display:inline-block;padding:10px 30px;border-radius:25px;"
                + "background:" + bodyColor + ";color:#fff;font-weight:bold;font-size:1.2em\">"
                + statusIcon + " " + overallStatus + "</span>"
                + "</div>"
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
                + "<p style=\"margin:0;color:#555\">📎 The full HTML report with screenshots "
                + "is attached. Open <strong>TestReport.html</strong> in any browser "
                + "to review each step with full-page screenshots.</p>"
                + "</div>"
                + "<hr style=\"border:none;border-top:1px solid #dee2e6\"/>"
                + "<p style=\"color:#aaa;font-size:12px;text-align:center;margin:15px 0 0\">"
                + "Sent automatically by NutriDyn Automation Framework via GitHub Actions</p>"
                + "</div></body></html>";

            // ── Multipart: HTML body + HTML report attached ──────────────────
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(htmlBody, "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(bodyPart);

            File report = new File(htmlReportPath);
            if (report.exists()) {
                MimeBodyPart attachPart = new MimeBodyPart();
                attachPart.attachFile(report);
                attachPart.setFileName(
                    "NutriDyn_TestReport_"
                    + NutridynReporting.RUN_DATE + "_"
                    + NutridynReporting.RUN_TIME + ".html"
                );
                multipart.addBodyPart(attachPart);
                System.out.println("📎 Attaching report: " + report.getAbsolutePath());
            } else {
                System.out.println("⚠️  Report file not found at: " + htmlReportPath
                    + " — sending summary email only.");
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Report email sent successfully to: " + recipient);

        } catch (Exception e) {
            System.err.println("❌ Failed to send report email: " + e.getMessage());
            e.printStackTrace();
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
