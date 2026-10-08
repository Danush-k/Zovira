package com.zovira.mail;

import org.springframework.web.util.HtmlUtils;

/**
 * Branded transactional email layouts. All interpolated values are HTML-escaped; styles are
 * inlined because most mail clients strip {@code <style>} blocks.
 */
public final class EmailTemplates {

    private EmailTemplates() {
    }

    public static EmailMessage verifyEmail(String to, String name, String link) {
        return action(to, "Verify your email address",
                "Welcome to Zovira, " + name,
                "Confirm your email address to secure your account and start placing orders. "
                        + "This link expires in 24 hours.",
                "Verify email", link);
    }

    public static EmailMessage resetPassword(String to, String name, String link) {
        return action(to, "Reset your Zovira password",
                "Reset your password",
                "Hi " + name + ", we received a request to reset your password. This link expires in 1 hour. "
                        + "If you did not ask for this, you can safely ignore this email.",
                "Choose a new password", link);
    }

    public static EmailMessage passwordChanged(String to, String name) {
        return notice(to, "Your Zovira password was changed",
                "Password changed",
                "Hi " + name + ", the password for your Zovira account was just changed and other devices were "
                        + "signed out. If this wasn't you, reset your password immediately.");
    }

    public static EmailMessage notice(String to, String subject, String heading, String body) {
        String html = layout(heading, "<p style=\"margin:0 0 16px;line-height:1.6;color:#403f3a\">"
                + HtmlUtils.htmlEscape(body) + "</p>");
        return new EmailMessage(to, subject, html, heading + "\n\n" + body);
    }

    public static EmailMessage action(String to, String subject, String heading, String body, String cta,
            String link) {
        String safeLink = HtmlUtils.htmlEscape(link);
        String html = layout(heading,
                "<p style=\"margin:0 0 24px;line-height:1.6;color:#403f3a\">" + HtmlUtils.htmlEscape(body) + "</p>"
                        + "<a href=\"" + safeLink + "\" style=\"display:inline-block;background:#155a50;color:#ffffff;"
                        + "text-decoration:none;font-weight:600;padding:12px 22px;border-radius:10px\">"
                        + HtmlUtils.htmlEscape(cta) + "</a>"
                        + "<p style=\"margin:24px 0 0;font-size:13px;color:#74736b\">Or paste this link into your "
                        + "browser:<br><span style=\"word-break:break-all;color:#155a50\">" + safeLink + "</span></p>");
        return new EmailMessage(to, subject, html, heading + "\n\n" + body + "\n\n" + cta + ": " + link);
    }

    private static String layout(String heading, String content) {
        return "<!doctype html><html><body style=\"margin:0;background:#f5f5f2;font-family:-apple-system,"
                + "'Segoe UI',Roboto,Helvetica,Arial,sans-serif\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"padding:32px 16px\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" style=\"max-width:520px;background:#ffffff;"
                + "border:1px solid #e7e6e1;border-radius:16px;padding:32px\"><tr><td>"
                + "<div style=\"font-size:22px;font-weight:800;letter-spacing:-0.5px;color:#155a50;"
                + "margin-bottom:24px\">zovira</div>"
                + "<h1 style=\"margin:0 0 12px;font-size:20px;color:#1b1a18\">" + HtmlUtils.htmlEscape(heading) + "</h1>"
                + content
                + "</td></tr></table>"
                + "<p style=\"font-size:12px;color:#9c9b93;margin-top:16px\">You are receiving this email because "
                + "of activity on your Zovira account.</p>"
                + "</td></tr></table></body></html>";
    }
}
