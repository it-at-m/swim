package de.muenchen.oss.swim.matching.application.port.out;

import java.io.InputStream;
import org.springframework.validation.annotation.Validated;

@Validated
public interface DmsOutPort {
    InputStream getExportContent();
}
