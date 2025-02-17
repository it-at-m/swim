package de.muenchen.oss.swim.dms.domain.helper;

import com.fasterxml.jackson.databind.JsonNode;
import de.muenchen.oss.swim.libs.handlercore.domain.exception.MetadataException;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatternHelper {
    public static final String RAW_PATTERN = "^s/(.+)/(.+)/(m?)$";
    public static final Pattern PATTERN = Pattern.compile(RAW_PATTERN);
    public static final String OPTION_METADATA = "m";

    private final MetadataHelper metadataHelper;

    /**
     * Apply substitution pattern.
     * See {@link PatternHelper#PATTERN}
     *
     * @param fullPattern The pattern.
     * @param input The input to apply the first pattern to.
     * @param metadataJson The parsed metadata file.
     * @return The result of the applied pattern.
     * @throws MetadataException If map can't be extracted from metadata json.
     */
    public String applyPattern(@NotBlank final String fullPattern, @NotBlank final String input, final JsonNode metadataJson) throws MetadataException {
        if (Strings.isBlank(fullPattern)) {
            return input;
        }
        final Matcher patternMatcher = PATTERN.matcher(fullPattern);
        if (patternMatcher.matches() && patternMatcher.groupCount() == 3) {
            // extract values from substitute pattern
            final String regex = patternMatcher.group(1);
            final String substitution = patternMatcher.group(2);
            final String options = patternMatcher.group(3);
            // resolve substitution map via regex and input
            final Map<String, String> substitutionMap = new HashMap<>(this.resolveSubstitutionMapFromInput(regex, input));
            // load metadata if enabled
            if (options.contains(OPTION_METADATA)) {
                substitutionMap.putAll(this.resolveSubstitutionMapFromMetadata(metadataJson));
            }
            // do substitution
            final StringSubstitutor substitutor = new StringSubstitutor(substitutionMap);
            return substitutor.replace(substitution);
        } else {
            final String message = String.format("Input pattern '%s' does not match pattern syntax '%s'", fullPattern, PATTERN);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Resolve substitution map from regex and input.
     * Returns map of numbered and named match groups.
     *
     * @param regexPattern The regex pattern to apply.
     * @param input The input to apply the pattern to.
     * @return Map of matching groups.
     */
    protected Map<String, String> resolveSubstitutionMapFromInput(final String regexPattern, final String input) {
        final Map<String, String> substitutionMap = new HashMap<>();
        final Pattern regex = Pattern.compile(regexPattern);
        final Matcher regexMatcher = regex.matcher(input);
        if (regexMatcher.matches()) {
            // unnamed groups
            for (int i = 1; i <= regexMatcher.groupCount(); i++) {
                substitutionMap.put(String.valueOf(i), regexMatcher.group(i));
            }
            // named groups
            regexMatcher.namedGroups().forEach(
                    (name, i) -> substitutionMap.put(name, regexMatcher.group(i)));
        } else {
            final String message = String.format("Input '%s' does not match supplied pattern '%s'", input, regexPattern);
            throw new IllegalArgumentException(message);
        }
        return substitutionMap;
    }

    /**
     * Resolve substitution map from metadata file.
     *
     * @param metadataJson The parsed json of the metadata file.
     * @return Substitution map based on metadata file.
     * @throws MetadataException If map can't be extracted from metadata json.
     */
    protected Map<String, String> resolveSubstitutionMapFromMetadata(final JsonNode metadataJson) throws MetadataException {
        if (metadataJson == null) {
            throw new IllegalArgumentException("Metadata json is null but option defined");
        }
        return metadataHelper.getIndexFields(metadataJson).entrySet().stream()
                .collect(Collectors.toMap(e -> "if." + e.getKey(), Map.Entry::getValue));
    }
}
