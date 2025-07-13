package com.example.test.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // Email gửi đi

    public void sendResetPasswordLink(String toEmail, String token) {
        String subject = "Yêu cầu đặt lại mật khẩu";
        String deepLink = "colector://reset-password?token=" + token;
        String redirectLink = "https://backend-springboot-latest.onrender.com/api/open-app/reset-password?token=" + token;

        String content = "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu.</p>"
                + "<p>Nhấn vào liên kết sau để đặt lại mật khẩu:</p>"
                + "<p><a href=\"" + redirectLink + "\">Đặt lại mật khẩu</a></p>"
                + "<p>Hoặc mở app với link: <b>" + deepLink + "</b></p>"
                + "<p>Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>";

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // HTML content
            helper.setFrom(fromEmail, "GREEN HR");

            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Gửi email thất bại", e);
        }
    }
}
