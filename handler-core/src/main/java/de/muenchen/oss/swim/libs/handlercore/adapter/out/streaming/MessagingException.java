package de.muenchen.oss.swim.libs.handlercore.adapter.out.streaming;

@SuppressWarnings("PMD.MissingSerialVersionUID")
public class MessagingException extends RuntimeException {
    protected MessagingException(final String message) {
        super(message);
    }
}
