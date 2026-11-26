package com.groupt1.hms.service;

public interface EmailSenderService {
    void sendResetPasswordEmail(String email, String code);
    void sendNotificationEmail(String email, String message);
}
