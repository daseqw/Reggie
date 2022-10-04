package all.service.impl;

import all.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;


@Service
@Slf4j
public class MailServiceimpl implements MailService {
    @Autowired
    private JavaMailSender javaMailSender;
//  发送人
    private String from = "546147560@qq.com";
//    private String from = "1543138380@qq.com";
//  接收人
    private String to;
//  标题
    private String subject = "Test";
//  正文
    private String context;
    @Override
    public void send(String mail,String code) {
        //把传进来的邮箱给to,code给context
        to = mail;
        log.info(mail.toString());
        log.info(code.toString());
        context = "本次验证码为:"+code;
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        /*
         * 如果写成 from+"小明"，那么发送人就会变成 小明
         * */
        simpleMailMessage.setFrom(from);
        simpleMailMessage.setTo(to);
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(context);
        javaMailSender.send(simpleMailMessage);
    }
}
