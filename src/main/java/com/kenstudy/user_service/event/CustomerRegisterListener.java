package com.kenstudy.user_service.event;

import com.kenstudy.user_service.model.Users;
import com.kenstudy.user_service.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.random.RandomGenerator;


@Component
@Slf4j
public class CustomerRegisterListener {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0789";
    private final UserService userService;
    private final JavaMailSender javaMailSender;


    @Autowired
    public CustomerRegisterListener(UserService userService, JavaMailSender javaMailSender) {
        this.userService = userService;
        this.javaMailSender = javaMailSender;
    }

    @EventListener
    @Async("eventTaskExecutor")
    public void confirmListener(CustomerRegisterEvent registerEvent) throws MessagingException,
            UnsupportedEncodingException {
        Users users = registerEvent.getUsers();
        RandomGenerator generator = RandomGenerator.getDefault();
        String token = generator.ints(20, 0, CHARACTERS.length())
                .mapToObj(CHARACTERS::charAt).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();

        //persist token
        userService.createVerificationToken(users, token);

        String toAddress = users.getEmail();
        String fromAddress = "kenobago@gmail.com";
        String senderName = "Ken-study Tech";
        String subject = "Please verify your registration";
        String content = "<p>Dear [[name]],</p>" + "<p>Please click the link below to verify your " +
                "account registration:<p> <br/>" + "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3>" +
                "<p>This link expires in 15mins </p>" + "Thank you,<br>" + "Ken-study Tech.";

        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom(fromAddress, senderName);
        helper.setTo(toAddress);
        helper.setSubject(subject);

        content = content.replace("[[name]]", users.getName());
        log.info("Retrieving apiRL:::: {} ", registerEvent.getApiUrl());
        String verifyURL = "http://localhost:4000" + registerEvent.getApiUrl() + "/confirm-registration?code=" + token;

        content = content.replace("[[URL]]", verifyURL);

        helper.setText(content, true);
        try {
            javaMailSender.send(message);
        } catch (MailException ex) {
            log.error("Mail send failed: {}", ex.getMessage(), ex);
            throw ex;
        }

    }
}
