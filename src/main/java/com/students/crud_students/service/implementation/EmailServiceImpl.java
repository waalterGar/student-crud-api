package com.students.crud_students.service.implementation;

import com.students.crud_students.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendWelcomeEmail(String to, String studentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            Context context = new Context();
            context.setVariable("studentName", studentName);

            String htmlContent = templateEngine.process("mail/welcome-student", context);

            helper.setTo(to);
            helper.setSubject("¡Bienvenido/a a la plataforma!");
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Welcome email successfully sent to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}", to, e);
            throw new RuntimeException("Error sending email", e);
        }
    }
}