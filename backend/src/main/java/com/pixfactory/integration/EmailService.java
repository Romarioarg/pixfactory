package com.pixfactory.integration;

import com.pixfactory.domain.EmailMessage;

public interface EmailService {
    EmailMessage send(String to, String subject, String body);
}
