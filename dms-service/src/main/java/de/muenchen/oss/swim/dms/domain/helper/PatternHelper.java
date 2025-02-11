package de.muenchen.oss.swim.dms.domain.helper;

import com.fasterxml.jackson.databind.JsonNode;
import de.muenchen.oss.swim.dms.domain.exception.MetadataException;
import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatternHelper {
    public static final Pattern PATTERN = Pattern.compile("^s/(.+)/(.+)/(m?)$");
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
     */
    public String applyPattern(@NotBlank final String fullPattern, @NotBlank final String input, final JsonNode metadataJson) throws MetadataException {
        if (Strings.isBlank(fullPattern)) {
            return input;
        }
        final Matcher patternMatcher = PATTERN.matcher(fullPattern);
        if (patternMatcher.matches() && patternMatcher.groupCount() == 3) {
            // extract values from substitute pattern
            final Pattern regex = Pattern.compile(patternMatcher.group(1));
            final String substitution = patternMatcher.group(2);
            final String options = patternMatcher.group(3);
            // init map for string substitution
            final Map<String, String> subsitutionMap = new HashMap<>();
            // apply regex to input
            final Matcher regexMatcher = regex.matcher(input);
            if (regexMatcher.matches()) {
                // unnamed groups
                for (int i = 1; i <= regexMatcher.groupCount(); i++) {
                    subsitutionMap.put(String.valueOf(i), regexMatcher.group(i));
                }
                // named groups
                regexMatcher.namedGroups().forEach(
                        (name, i) ->
                                subsitutionMap.put(name, regexMatcher.group(i))
                );
            } else {
                final String message = String.format("Input '%s' does not match supplied pattern '%s'", input, regex);
                throw new IllegalArgumentException(message);
            }
            // load metadata if enabled
            if (options.contains(OPTION_METADATA)) {
                if (metadataJson == null) {
                    throw new IllegalArgumentException("Metadata json is null but option defined");
                }
                metadataHelper.getIndexFields(metadataJson).forEach(
                        (key, value) ->
                                subsitutionMap.put("if." + key, value)
                );
            }
            // do substitution
            final StringSubstitutor substitutor = new StringSubstitutor(subsitutionMap);
            return substitutor.replace(substitution);
        } else {
            final String message = String.format("Input pattern '%s' does not match pattern syntax '%s'", fullPattern, PATTERN);
            throw new IllegalArgumentException(message);
        }
    }
}
