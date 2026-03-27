package com.cuttypaws.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TaskExecutor taskExecutor;
    private final TemplateEngine templateEngine;

    @Autowired
    public EmailService(
            JavaMailSender javaMailSender,
            TaskExecutor taskExecutor,
            TemplateEngine templateEngine
    ) {
        this.javaMailSender = javaMailSender;
        this.taskExecutor = taskExecutor;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        taskExecutor.execute(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(to);
                message.setSubject(subject);
                message.setText(body);
                message.setFrom("no-reply@cuttypaws.com");
                javaMailSender.send(message);
                log.info("Plain text email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send plain text email to {}: {}", to, e.getMessage(), e);
            }
        });
    }

    @Async
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        taskExecutor.execute(() -> {
            try {
                Context context = new Context();
                context.setVariables(variables);

                String htmlBody = templateEngine.process(templateName, context);

                MimeMessage message = javaMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(
                        message,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        "UTF-8"
                );

                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                helper.setFrom("no-reply@cuttypaws.com");

                ClassPathResource logo = new ClassPathResource("static/images/cuttypaws-logo.png");
                if (logo.exists()) {
                    helper.addInline("cuttypawsLogo", logo, "image/png");
                }

                javaMailSender.send(message);
                log.info("Template email sent successfully to: {}", to);
            } catch (Exception e) {
                log.error("Failed to send template email to {}: {}", to, e.getMessage(), e);
            }
        });
    }
}