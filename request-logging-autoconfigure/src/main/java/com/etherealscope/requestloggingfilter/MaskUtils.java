package com.etherealscope.requestloggingfilter;

import com.etherealscope.requestloggingfilter.RequestLoggingProperties.Mask;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class MaskUtils {

    static final String MASK = "*****";
    static final AntPathMatcher MATCHER = new AntPathMatcher();

    static List<Mask> filterMasks(String method, String servletPath, Mask... masks) {
        return Stream.of(masks)
                .filter(m -> maskMatches(m, method, servletPath))
                .collect(toList());
    }

    static String maskBody(String body, String contentType, List<Mask> masks) {
        String maskedBody = body;
        if (contentType == null || contentType.contains(APPLICATION_JSON_VALUE)) {
            for (Mask mask : masks) {
                for (String field : mask.getMaskedJsonFields()) {
                    maskedBody = maskedBody.replaceAll(String.format("(\"%s\":)(\"[^\"]+\")", field), String.format("$1\"%s\"", MASK));
                }
            }
        }
        if (contentType == null || contentType.contains(APPLICATION_FORM_URLENCODED_VALUE)) {
            for (Mask mask : masks) {
                for (String param : mask.getMaskedQueryParams()) {
                    maskedBody = maskedBody.replaceAll(String.format("%s=([^&]+)", param), String.format("%s=%s", param, MASK));
                }
            }
        }
        return maskedBody;
    }

    static void maskHeaders(Map<String, String> headers, List<Mask> masks) {
        for (Mask mask : masks) {
            for (String header : mask.getMaskedHeaders()) {
                headers.replace(header, MASK);
            }
        }
    }

    static void maskQueryParams(MultiValueMap<String, String> queryParams, List<Mask> masks) {
        for (Mask mask : masks) {
            for (String param : mask.getMaskedQueryParams()) {
                queryParams.replace(param, singletonList(MASK));
            }
        }
    }

    private static boolean maskMatches(Mask mask, String method, String servletPath) {
        return (mask.getMethod() == null || mask.getMethod().matches(method)) && MATCHER.match(mask.getPathMatcher(), servletPath);
    }

}
