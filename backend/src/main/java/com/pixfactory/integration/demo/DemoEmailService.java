package com.pixfactory.integration.demo;

import com.pixfactory.domain.EmailMessage;
import com.pixfactory.integration.EmailService;
import com.pixfactory.repo.EmailMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class DemoEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(DemoEmailService.class);
    private final EmailMessageRepository repository;

    public DemoEmailService(EmailMessageRepository repository) {
        this.repository = repository;
    }

    @Override
    public EmailMessage send(String to, String subject, String body) {
        EmailMessage message = new EmailMessage();
        message.setRecipient(to);
        message.setSubject(subject);
        message.setBody(body);
        message.setStatus("SENT");
        message.setProvider("DEMO");
        EmailMessage saved = repository.save(message);
        log.info("EMAIL DEMO | Para: {} | Assunto: {} | Status: SENT", to, subject);
        return saved;
    }
}
