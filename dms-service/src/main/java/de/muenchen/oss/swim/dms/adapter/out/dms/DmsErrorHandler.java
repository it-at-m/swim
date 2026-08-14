package de.muenchen.oss.swim.dms.adapter.out.dms;

import de.muenchen.oss.refarch.integration.dms.model.DmsErrorResponse;
import de.muenchen.oss.swim.dms.domain.exception.DmsException;
import java.util.concurrent.Callable;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
class DmsErrorHandler {
    private static final String DMS_EXCEPTION_MESSAGE = "Dms request failed with status code %s and error response: %s";

    private final DmsMapper dmsMapper;

    protected <T> T handleError(final Callable<T> callable) {
        try {
            return callable.call();
        } catch (final WebClientResponseException e) {
            DmsErrorResponse dmsError = null;
            try {
                dmsError = e.getResponseBodyAs(DmsErrorResponse.class);
            } catch (final RuntimeException ignored) {
            }
            final HttpStatus httpStatus = HttpStatus.valueOf(e.getStatusCode().value());
            final String message = String.format(DMS_EXCEPTION_MESSAGE, e.getStatusCode(), dmsError);
            throw new DmsException(dmsMapper.toDomain(dmsError), httpStatus, message, e);
        } catch (final Exception e) {
            throw new DmsException("Dms request failed with unexpected error", e);
        }
    }
}
