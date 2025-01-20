package de.muenchen.oss.swim.dms.adapter.out.s3;

import de.muenchen.oss.swim.dms.application.port.out.FileSystemOutPort;
import de.muenchen.oss.swim.dms.domain.exception.PresignedUrlException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Service;

@Service
public class S3Adapter implements FileSystemOutPort {
    @Override
    @SuppressFBWarnings("URLCONNECTION_SSRF_FD")
    public InputStream getPresignedUrlFile(final String presignedUrl) throws PresignedUrlException {
        try {
            final URI uri = new URI(presignedUrl);
            final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            final int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.getInputStream();
            } else {
                connection.disconnect();
                throw new PresignedUrlException("Failed to download file: " + responseCode);
            }
        } catch (URISyntaxException | IOException e) {
            throw new PresignedUrlException("Error while downloading with presigned url", e);
        }
    }
}
