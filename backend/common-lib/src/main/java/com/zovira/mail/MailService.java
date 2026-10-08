package com.zovira.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Sends email off the request thread; delivery failures are logged, never surfaced to the caller. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final EmailSender sender;

    public MailService(EmailSender sender) {
        this.sender = sender;
    }

    @Async
    public void send(EmailMessage message) {
        try {
            sender.send(message);
        } catch (RuntimeException e) {
            log.error("Failed to send email '{}' to {}", message.subject(), message.to(), e);
        }
    }
}
