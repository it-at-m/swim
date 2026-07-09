package de.muenchen.oss.swim.dms.application.usecase.helper;

import de.muenchen.oss.swim.dms.configuration.SwimDmsProperties;
import de.muenchen.oss.swim.dms.domain.model.DmsContentObjectRequest;
import de.muenchen.oss.swim.dms.domain.model.DmsIncomingRequest;
import de.muenchen.oss.swim.dms.domain.model.UseCase;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import de.muenchen.oss.swim.libs.handlercore.domain.helper.PatternHelper;
import de.muenchen.oss.swim.libs.handlercore.domain.model.FileReference;
import de.muenchen.oss.swim.libs.handlercore.domain.model.Metadata;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestResolverHelper {
    private final SwimDmsProperties swimDmsProperties;
    private final PatternHelper patternHelper;

    /**
     * Build subject from metadata file.
     *
     * @param metadata Parsed metadata file.
     * @return Constructed subject.
     */
    protected String subjectFromMetadata(final Metadata metadata) throws MetadataException {
        // validate metadata provided
        if (metadata == null) {
            throw new MetadataException("Metadata is required");
        }
        // map index fields with prefix to subject
        final Map<String, String> indexFields = metadata.indexFields();
        return indexFields.entrySet().stream()
                // filter for prefix
                .filter(i -> i.getKey().startsWith(swimDmsProperties.getMetadataSubjectPrefix()))
                // sort
                .sorted(Map.Entry.comparingByKey())
                // build subject string
                .map(i -> String.format(
                        "%s (%s)",
                        i.getValue(),
                        i.getKey().replaceFirst("^" + Pattern.quote(swimDmsProperties.getMetadataSubjectPrefix()), "")))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Resolve parameters for new ContentObject.
     *
     * @param file The file to resolve the values for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @return Resolved parameters for new ContentObject.
     */
    public DmsContentObjectRequest resolveContentObjectParameters(final FileReference file, final UseCase useCase,
            final Metadata metadata, final InputStream content) {
        // resolve ContentObject name
        final String contentObjectName = String.format("%s.%s",
                this.patternHelper.applyPattern(useCase.getContentObject().getFilenameOverwritePattern(), file.getFileNameWithoutExtension(), metadata),
                file.getFileExtension());
        // resolve ContentObject subject
        final String contentObjectSubjectPattern = useCase.getContentObject().getSubjectPattern();
        final String contentObjectSubject = StringUtils.isNotBlank(contentObjectSubjectPattern)
                ? this.patternHelper.applyPattern(contentObjectSubjectPattern, file.getFileNameWithoutExtension(), metadata)
                : null;
        return new DmsContentObjectRequest(contentObjectName, contentObjectSubject, content);
    }

    /**
     * Resolve parameters for new Incoming.
     *
     * @param file The file to resolve the values for.
     * @param metadata Parsed metadata file.
     * @param useCase The use case.
     * @return Resolved parameters for new Incoming.
     */
    public DmsIncomingRequest resolveIncomingParameters(final FileReference file, final UseCase useCase,
            final Metadata metadata, final DmsContentObjectRequest contentObjectRequest) throws MetadataException {
        // TODO use basename instead of filename?
        // resolve name for Incoming
        final String incomingName;
        if (StringUtils.isBlank(useCase.getIncoming().getIncomingNamePattern())) {
            // use resolved ContentObject name (filename) if no pattern for Incoming name is defined
            // resolved in this case means the UseCase#filenameOverwritePattern is applied first
            // extension is removed
            incomingName = contentObjectRequest.getNameWithoutExtension();
        } else {
            // else apply pattern to original filename
            final String patternIncomingName = this.patternHelper.applyPattern(useCase.getIncoming().getIncomingNamePattern(),
                    file.getFileNameWithoutExtension(), metadata);
            incomingName = StringUtils.isNotBlank(patternIncomingName) ? patternIncomingName :
            // fallback to default if empty name via pattern
                    contentObjectRequest.getNameWithoutExtension();
        }
        // resolve subject for Incoming
        final String incomingSubject;
        if (StringUtils.isNotBlank(useCase.getIncoming().getIncomingSubjectPattern())) {
            incomingSubject = this.patternHelper.applyPattern(useCase.getIncoming().getIncomingSubjectPattern(),
                    file.getFileNameWithoutExtension(), metadata);
        } else if (useCase.getIncoming().isMetadataSubject()) {
            incomingSubject = this.subjectFromMetadata(metadata);
        } else {
            incomingSubject = null;
        }
        return new DmsIncomingRequest(incomingName, incomingSubject);
    }
}
