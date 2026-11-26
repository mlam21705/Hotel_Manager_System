package com.groupt1.hms.service.impl;

import com.groupt1.hms.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSenderServiceImpl implements EmailSenderService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendResetPasswordEmail(String email, String code) {
        if(mailSender == null){throw new IllegalStateException("Mail sender is not initialized");}
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your Password Reset Code");
            message.setText("Your password reset verification code is: " + code + "\n\nThis code will expire in 10 minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email", e);
        }
    }

    @Override
    public void sendNotificationEmail(String email, String message) {
        if(mailSender == null){throw new IllegalStateException("Mail sender is not initialized");}
        try{
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(email);
            mailMessage.setSubject("Notification");
            mailMessage.setText(message);
            mailSender.send(mailMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send notification email", e);
        }
    }


}
