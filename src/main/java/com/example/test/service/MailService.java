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

    public void sendOtpMail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

            String subject = "Mã OTP Khôi Phục Mật Khẩu";
            String content = "<p>Xin chào,</p>"
                    + "<p>Mã OTP của bạn là: <b>" + otp + "</b></p>"
                    + "<p>Mã này có hiệu lực trong 10 phút.</p>"
                    + "<p>Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>"
                    + "<hr><p>Hệ thống tự động, vui lòng không trả lời email này.</p>";

            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true); // true để gửi email HTML

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Lỗi gửi mail: " + e.getMessage());
        }
    }
}
