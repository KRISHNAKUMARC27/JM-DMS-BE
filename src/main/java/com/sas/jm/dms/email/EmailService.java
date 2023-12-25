package com.sas.jm.dms.email;

public interface EmailService {

	public String sendTableMail(EmailDetails details, String[] recipients, String string);
	public String sendMail(EmailDetails mailDetail,String[] recipients);
}
