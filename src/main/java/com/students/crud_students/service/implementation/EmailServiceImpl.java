package com.students.crud_students.service.implementation;

import com.students.crud_students.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl  implements EmailService {
    private final JavaMailSender mailSender;

    @Override
    public void sendWelcomeEmail(String to, String studentName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("¡Bienvenido/a a la plataforma!");
        message.setText("Hola " + studentName + ",\n\n¡Tu cuenta ha sido creada con éxito!\n\nSaludos,\nEl equipo.");

        mailSender.send(message);
    }
}
