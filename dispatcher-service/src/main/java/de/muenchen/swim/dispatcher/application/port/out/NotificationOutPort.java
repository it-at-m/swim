package de.muenchen.swim.dispatcher.application.port.out;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;

@Validated
public interface NotificationOutPort {
    /**
     * Notify about errors while dispatching.
     *
     * @param recipients The recipients to send the notification to.
     * @param useCase The name of the use case the errors where thrown in.
     * @param errors The errors that where thrown.
     */
    void sendDispatchErrors(@NotEmpty List<String> recipients, @NotBlank String useCase, @NotEmpty Map<String, Exception> errors);

    /**
     * Send protocol with validation information.
     *
     * @param recipients The recipients to send the protocol to.
     * @param useCase The name of the use case the protocol was found for.
     * @param protocolName The name of the protocol file.
     * @param protocol The content of the protocol.
     * @param missingFiles Files missing in file system but present in protocol.
     * @param missingInProtocol Files missing in protocol but present in file system.
     */
    void sendProtocol(@NotEmpty List<String> recipients, @NotBlank String useCase, @NotBlank String protocolName, @NotNull InputStream protocol,
            @NotNull List<String> missingFiles, @NotNull List<String> missingInProtocol);

    /**
     * Send error thrown while processing protocol file.
     *
     * @param recipients The recipients to notify.
     * @param useCase The name of the use case the protocol was found for.
     * @param protocolPath The path of the protocol file.
     * @param error The error thrown while processing protocol.
     */
    void sendProtocolError(@NotEmpty List<String> recipients, @NotBlank String useCase, @NotBlank String protocolPath, @NotNull Exception error);

    /**
     * Send error thrown while marking file as finished.
     *
     * @param recipients The recipients to notify.
     * @param useCase The name of the use case the file is in.
     * @param filePath The path of the file.
     * @param error The error thrown.
     */
    void sendFileFinishError(@NotEmpty List<String> recipients, @NotBlank String useCase, @NotBlank String filePath, @NotNull Exception error);
}
