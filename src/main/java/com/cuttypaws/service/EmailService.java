package com.cuttypaws.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TaskExecutor taskExecutor;

    @Autowired
    public EmailService(JavaMailSender javaMailSender, TaskExecutor taskExecutor) {
        this.javaMailSender = javaMailSender;
        this.taskExecutor = taskExecutor;
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        taskExecutor.execute(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                message.setFrom("no-reply@kinjomarket.com");
                javaMailSender.send(message);
                log.info("Email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", to, e.getMessage());
            }
        });
    }
}
