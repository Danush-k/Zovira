package com.zovira.mail;

/** Transport abstraction so SMTP, a transactional email API, or a log sink can be swapped freely. */
public interface EmailSender {

    void send(EmailMessage message);
}
