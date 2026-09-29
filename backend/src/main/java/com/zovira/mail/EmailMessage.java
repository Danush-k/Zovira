package com.zovira.mail;

public record EmailMessage(String to, String subject, String html, String text) {
}
