package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.User;

public class PasswordResetEmailNotification {
    private final MailClient mailClient;

    public PasswordResetEmailNotification(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    public void send(User user, String resetUrl) {
        String subject = "Reset your Medicine Drugstore password";
        String body = "Hello " + dash(user.getName()) + ",\n\n"
                + "We received a request to reset the password for " + dash(user.getEmail()) + ".\n"
                + "Open this link to choose a new password. It expires in 30 minutes.\n\n"
                + resetUrl + "\n\n"
                + "If you did not ask for this, you can ignore this email.\n\n"
                + "Medicine Drugstore\n";
        mailClient.send("PASSWORD_RESET", user.getEmail(), subject, body);
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
