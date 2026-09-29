package com.zovira.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    /**
     * Uses SMTP when {@code SMTP_HOST} is configured; otherwise logs messages so local
     * development never needs a mail server (links in verification emails appear in the log).
     */
    @Bean
    EmailSender emailSender(@Value("${spring.mail.host:}") String smtpHost,
            @Value("${zovira.mail.from}") String from, ObjectProvider<JavaMailSender> javaMailSender) {
        if (smtpHost == null || smtpHost.isBlank()) {
            log.info("SMTP_HOST not set; emails will be written to the application log");
            return message -> log.info("""

                    ---------- Email (not sent: SMTP not configured) ----------
                    To:      {}
                    Subject: {}

                    {}
                    -----------------------------------------------------------""",
                    message.to(), message.subject(), message.text());
        }
        JavaMailSender sender = javaMailSender.getObject();
        return message -> {
            try {
                MimeMessage mime = sender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(message.to());
                helper.setSubject(message.subject());
                helper.setText(message.text(), message.html());
                sender.send(mime);
            } catch (MessagingException e) {
                throw new IllegalStateException("Failed to build email", e);
            }
        };
    }
}
