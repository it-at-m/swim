package de.muenchen.oss.swim.libs.handlercore.adapter.out.s3;

import de.muenchen.oss.swim.libs.handlercore.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.PresignedUrlException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Service;

@Service
public class S3Adapter implements FileSystemOutPort {
    private static final int CONNECTION_TIMEOUT = 5 * 1000;
    private static final int READ_TIMEOUT = 60 * 1000;

    @Override
    @SuppressFBWarnings(
            value = "URLCONNECTION_SSRF_FD",
            justification = "The presigned URL is generated by trusted S3 service"
    )
    public InputStream getPresignedUrlFile(final String presignedUrl) throws PresignedUrlException {
        HttpURLConnection connection = null;
        int responseCode = -1;
        try {
            final URI uri = new URI(presignedUrl);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestMethod("GET");
            responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.getInputStream();
            } else {
                throw new PresignedUrlException("Failed to download file: " + responseCode);
            }
        } catch (URISyntaxException | IOException e) {
            throw new PresignedUrlException("Error while downloading with presigned url", e);
        } finally {
            if (connection != null && responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
            }
        }
    }
}
