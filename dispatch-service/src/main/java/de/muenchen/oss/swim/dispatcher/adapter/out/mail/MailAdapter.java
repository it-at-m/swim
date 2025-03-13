package de.muenchen.oss.swim.dispatcher.adapter.out.mail;

import de.muenchen.oss.swim.dispatcher.application.port.out.NotificationOutPort;
import de.muenchen.oss.swim.dispatcher.domain.model.ErrorDetails;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailAdapter implements NotificationOutPort {
    private final MailProperties mailProperties;
    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    private final static String MAIL_ADDRESS_DELIMITER = ";";
    private final static String SUBJECT_OK = "OK";
    private final static String SUBJECT_ERROR = "ERROR";

    @Override
    public void sendDispatchErrors(final List<String> recipients, final String useCase, final Map<String, Throwable> errors) {
        final String parsedRecipients = String.join(MAIL_ADDRESS_DELIMITER, recipients);
        final String subject = this.buildSubject(true, String.format(this.getMessage("dispatchErrors.subject"), useCase));
        final String body = String.format(
                this.getMessage("dispatchErrors.message"),
                useCase, errors.size(),
                errors.entrySet().stream()
                        .map(e -> String.format("- %s: %s", e.getKey(), e.getValue().getMessage()))
                        .reduce((a, b) -> String.format("%s%n%s", a, b)).orElse(""));
        this.sendMail(parsedRecipients, subject, body, Map.of());
    }

    @Override
    public void sendProtocol(final List<String> recipients, final String useCase, final String protocolName, final InputStream protocol,
            final List<String> missingFiles, final List<String> missingInProtocol) {
        final String parsedRecipients = String.join(MAIL_ADDRESS_DELIMITER, recipients);
        final boolean missmatch = !missingFiles.isEmpty() || !missingInProtocol.isEmpty();
        final String subject = this.buildSubject(missmatch, String.format(this.getMessage("protocol.subject"), protocolName, useCase));
        final String body = String.format(
                this.getMessage("protocol.message"),
                useCase, protocolName, missingFiles.size(), missingInProtocol.size());
        final Map<String, InputStream> attachments = new HashMap<>();
        attachments.put(protocolName, protocol);
        if (!missingFiles.isEmpty()) {
            attachments.put("missing_files.txt", fileListToInputStream(missingFiles));
        }
        if (!missingInProtocol.isEmpty()) {
            attachments.put("missing_in_protocol.txt", fileListToInputStream(missingInProtocol));
        }
        this.sendMail(parsedRecipients, subject, body, attachments);
    }

    @Override
    public void sendProtocolError(final List<String> recipients, final String useCase, final String protocolPath, final Throwable error) {
        final String parsedRecipients = String.join(MAIL_ADDRESS_DELIMITER, recipients);
        final String subject = this.buildSubject(true, String.format(this.getMessage("protocolError.subject"), protocolPath, useCase));
        final String body = String.format(
                this.getMessage("protocolError.message"),
                useCase, protocolPath, error.getClass(), error.getMessage());
        this.sendMail(parsedRecipients, subject, body, Map.of());
    }

    @Override
    public void sendFileError(final List<String> recipients, final String useCase, final String filePath, final ErrorDetails inputError,
            final Throwable error) {
        final String parsedRecipients = String.join(MAIL_ADDRESS_DELIMITER, recipients);
        final String subject = this.buildSubject(true, String.format(this.getMessage("fileErrorHandlerError.subject"), filePath, useCase));
        final String body = String.format(
                this.getMessage("fileErrorHandlerError.message"),
                useCase, filePath, inputError.source(),
                error.getClass(), error.getMessage(),
                inputError.className(), inputError.message(), inputError.stacktrace());
        this.sendMail(parsedRecipients, subject, body, Map.of());
    }

    @Override
    public void sendFileError(final List<String> recipients, final String useCase, final String filePath, final ErrorDetails error) {
        final String parsedRecipients = String.join(MAIL_ADDRESS_DELIMITER, recipients);
        final String subject = this.buildSubject(true, String.format(this.getMessage("fileError.subject"), filePath, useCase));
        final String body = String.format(
                this.getMessage("fileError.message"),
                useCase, filePath, error.source(), error.className(), error.message(), error.stacktrace());
        this.sendMail(parsedRecipients, subject, body, Map.of());
    }

    /**
     * Send a mail.
     *
     * @param recipients Recipients the mail should be sent to. Multiple can be separated by ";".
     * @param subject Subject of the mail.
     * @param body Body of the mail.
     * @param attachments Map of filename and InputStreams of attachments for the mail.
     */
    protected void sendMail(final String recipients, final String subject, final String body, final Map<String, InputStream> attachments) {
        final MimeMessage message = mailSender.createMimeMessage();
        try {
            final MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(mailProperties.getFromAddress());
            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(body);
            for (final Map.Entry<String, InputStream> attachment : attachments.entrySet()) {
                helper.addAttachment(attachment.getKey(), new ByteArrayResource(attachment.getValue().readAllBytes()));
            }
        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }
        mailSender.send(message);
    }

    /**
     * Get InputStream of list of file paths.
     * Each file path is a new line.
     *
     * @param files List of file paths.
     * @return InputStream of the file paths.
     */
    protected InputStream fileListToInputStream(final List<String> files) {
        final String content = String.join("\n", files);
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Build mail subject.
     * Pattern: {@code <prefix><state>: <message>}
     *
     * @param isError If is error else ok.
     * @param message The message.
     * @return Built subject.
     */
    protected String buildSubject(final boolean isError, final String message) {
        final String state = isError ? SUBJECT_ERROR : SUBJECT_OK;
        return String.format("%s%s: %s", this.mailProperties.getMailSubjectPrefix(), state, message);
    }

    /**
     * Resolve localized message.
     * Uses {@link MailProperties#getLocale()}.
     *
     * @param code The message code to resolve the message.
     * @return The resolved message.
     */
    protected String getMessage(final String code) {
        return this.messageSource.getMessage(code, null, this.mailProperties.getLocale());
    }
}
