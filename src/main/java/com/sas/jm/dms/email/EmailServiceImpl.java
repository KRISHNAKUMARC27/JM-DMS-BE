package com.sas.jm.dms.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
	@Autowired
	private JavaMailSender javaMailSender;

	public String sendTableMail(EmailDetails details, String[] recipients, String body) {
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper mimeMessageHelper;

			mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
			mimeMessageHelper.setFrom("pingtesttriggerapi27@gmail.com", "Jai Maruthi Auto Care");
			mimeMessageHelper.setTo(recipients);

			mimeMessageHelper.setSubject(details.getSubject());

			mimeMessageHelper.setText("Hello", body);
			// Sending the mail
			javaMailSender.send(mimeMessage);
			log.info("sending email " + details.getSubject());
			return "Mail sent Successfully";
		}

		catch (Exception e) {
			e.printStackTrace();
			log.error("Error in sending email\n" + e.getStackTrace());
			return "Error while sending mail!!!";
		}
	}
	
	public String sendMail(EmailDetails mailDetail,String[] recipients) {
		// Try block to check for exceptions handling
		try {

			// Creating a simple mail message object
			SimpleMailMessage emailMessage = new SimpleMailMessage();

			// Setting up necessary details of mail
			emailMessage.setFrom("pingtesttriggerapi27@gmail.com");
			emailMessage.setTo(recipients);
			emailMessage.setSubject(mailDetail.getSubject());
			emailMessage.setText(mailDetail.getMsgBody());

			// Sending the email
			javaMailSender.send(emailMessage);
			return "Email has been sent successfully...";
		}

		// Catch block to handle the exceptions
		catch (Exception e) {
			e.printStackTrace();
			return "Error while Sending email!!!";
		}
	}

}
