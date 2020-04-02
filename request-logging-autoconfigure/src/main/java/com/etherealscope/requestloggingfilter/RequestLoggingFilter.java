package com.etherealscope.requestloggingfilter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@Order(OrderedFilter.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String NOTHING = "[nothing]";
    private static final String UNKNOWN = "[unknown]";
    private static final String MASK = "*****";
    private static final String BEFORE_REQUEST_MESSAGE = "--- REQUEST LOG START ---";
    private static final String AFTER_REQUEST_MESSAGE = "--- REQUEST LOG END ---";
    private static final String BEFORE_RESPONSE_MESSAGE = "--- RESPONSE LOG START ---";
    private static final String AFTER_RESPONSE_MESSAGE = "--- RESPONSE LOG END ---";
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    RequestLoggingProperties props;

    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {

        long start = 0;
        if (props.isIncludeTimeElapsed()) {
            start = currentTimeMillis();
        }

        if (shouldLogRequestBody(request.getMethod(), request.getContentType()) && !(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }

        if (shouldCacheResponse() && !(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }

        String method = request.getMethod();
        String servletPath = request.getServletPath();

        try {
            if (props.getResponse().isEnabled()) {
                List<RequestLoggingProperties.Mask> requestMasks = Stream.of(
                        props
                                .getRequest()
                                .getMasks())
                        .filter(m -> maskMatches(m, method, servletPath))
                        .collect(toList());
                log.debug(getRequestLogMessage(request, requestMasks));
            }
            filterChain.doFilter(request, response);
        } finally {
            if (props.getResponse().isEnabled()) {
                List<RequestLoggingProperties.Mask> responseMasks = Stream.of(
                        props
                                .getResponse()
                                .getMasks())
                        .filter(m -> maskMatches(m, method, servletPath))
                        .collect(toList());
                log.debug(getResponseLogMessage(response, responseMasks));
            }
            if (shouldCacheResponse()) {
                updateResponse(response);
            }
        }

        if (props.isIncludeTimeElapsed()) {
            log.debug("Request time elapsed: " + (currentTimeMillis() - start) + " ms");
        }
    }

    private boolean shouldLogRequestBody(String method, String contentType) {
        if(!props.getRequest().isEnabled() || !props.getRequest().isIncludePayload()) {
            return false;
        }
        if (!"POST".equals(method) && !"PUT".equals(method) && !"PATCH".equals(method)) {
            return false;
        }
        if (props.getRequest().getWhiteListedContentTypes().length > 0) {
            for (String s : props.getRequest().getWhiteListedContentTypes()) {
                if (contentType == null || contentType.contains(s)) {
                    return true;
                }
            }
            return false;
        }
        if (props.getRequest().getBlackListedContentTypes().length > 0) {
            for (String s : props.getRequest().getBlackListedContentTypes()) {
                if (s != null && contentType.contains(s)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean shouldCacheResponse() {
        return props.getResponse().isEnabled() && props.getResponse().isIncludePayload();
    }

    private boolean shouldLogResponseBody(String contentType) {
        if(!shouldCacheResponse()) {
            return false;
        }
        if (props.getResponse().getWhiteListedContentTypes().length > 0) {
            for (String s : props.getResponse().getWhiteListedContentTypes()) {
                if (contentType == null || contentType.contains(s)) {
                    return true;
                }
            }
            return false;
        }
        if (props.getResponse().getBlackListedContentTypes().length > 0) {
            for (String s : props.getResponse().getBlackListedContentTypes()) {
                if (s != null && contentType.contains(s)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getRequestLogMessage(HttpServletRequest request, List<RequestLoggingProperties.Mask> masks) {
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("Method: ", request.getMethod());
        logMap.put("Url: ", request.getRequestURL().toString());

        if (props.getRequest().isIncludeQueryParams()) {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            request.getParameterMap().forEach((key, value) -> queryParams.addAll(key, Arrays.asList(value)));
            maskQueryParams(queryParams, masks);
            logMap.put("Query-Params: ", queryParams);
        }

        if (props.getRequest().isIncludeHeaders()) {
            Map<String, String> headers = new LinkedHashMap<>();
            enumerationAsStream(request.getHeaderNames()).forEach(s -> headers.put(s, request.getHeader(s)));
            maskHeaders(headers, masks);
            logMap.put("Headers: ", headers);
        }

        if (props.getRequest().isIncludeClientInfo()) {
            logMap.put("Ip: ", request.getRemoteAddr());
            logMap.put("Principal: ", request.getUserPrincipal().getName());
            logMap.put("Session-Id: ", request.getSession().getId());
        }

        logMap.put("Content-Type: ", request.getContentType());
        logMap.put("Content-Length: ", request.getContentLengthLong());
        logMap.put("Character-Encoding: ", request.getCharacterEncoding());

        if (shouldLogRequestBody(request.getMethod(), request.getContentType())) {
            logMap.put("Body: ", maskBody(getRequestPayload(request), request.getContentType(), masks));
        }

        return "\n"
                + BEFORE_REQUEST_MESSAGE
                + "\n"
                + logMap.entrySet().stream().map(e -> e.getKey() + e.getValue()).collect(Collectors.joining("\n"))
                + "\n"
                + AFTER_REQUEST_MESSAGE;
    }

    private String getResponseLogMessage(HttpServletResponse response, List<RequestLoggingProperties.Mask> masks) {
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("Status-Code: ", response.getStatus());
        logMap.put("Content-Type: ", response.getContentType());
        logMap.put("Character-Encoding: ", response.getCharacterEncoding());

        if (props.getResponse().isIncludeHeaders()) {
            Map<String, String> headers = new LinkedHashMap<>();
            response.getHeaderNames().forEach(s -> headers.put(s, response.getHeader(s)));
            maskHeaders(headers, masks);
            logMap.put("Headers: ", headers);
        }

        if (shouldLogResponseBody(response.getContentType())) {
            logMap.put("Body: ", maskBody(getResponsePayload(response), response.getContentType(), masks));
        }

        return "\n"
                + BEFORE_RESPONSE_MESSAGE
                + "\n"
                + logMap.entrySet().stream().map(e -> e.getKey() + e.getValue()).collect(Collectors.joining("\n"))
                + "\n"
                + AFTER_RESPONSE_MESSAGE;
    }

    private String getResponsePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            return transformByteArrayToString(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding(), props.getResponse().getMaxPayloadSize());
        }
        return NOTHING;
    }

    private String getRequestPayload(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            return transformByteArrayToString(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding(), props.getRequest().getMaxPayloadSize());
        }
        return NOTHING;
    }

    private String transformByteArrayToString(byte[] byteArrayContent, String encoding, int maxPayloadSize) {
        if (byteArrayContent != null && byteArrayContent.length > 0) {
            int length = Math.min(byteArrayContent.length, maxPayloadSize);
            try {
                return new String(byteArrayContent, 0, length, encoding);
            } catch (UnsupportedEncodingException ex) {
                log.warn("Unsupported encoding in request or response {}", encoding);
                return UNKNOWN;
            }
        }
        return NOTHING;
    }

    @SneakyThrows
    private void updateResponse(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            wrapper.copyBodyToResponse();
        }
    }

    private boolean maskMatches(RequestLoggingProperties.Mask mask, String method, String servletPath) {
        return (mask.getMethod() == null || mask.getMethod().matches(method)) && MATCHER.match(mask.getPathMatcher(), servletPath);
    }

    private String maskBody(String body, String contentType,  List<RequestLoggingProperties.Mask> masks) {
        String newBody = body;
        if (contentType == null || contentType.contains(APPLICATION_JSON_VALUE)) {
            for (RequestLoggingProperties.Mask mask : masks) {
                for (String field : mask.getMaskedJsonFields()) {
                    newBody = newBody.replaceAll(String.format("(\"%s\":)(\"[^\"]+\")", field), String.format("$1\"%s\"", MASK));
                }
            }
        }
        if (contentType == null || contentType.contains(APPLICATION_FORM_URLENCODED_VALUE)) {
            for (RequestLoggingProperties.Mask mask : masks) {
                for (String param : mask.getMaskedQueryParams()) {
                    newBody = newBody.replaceAll(String.format("%s=([^&]+)", param), String.format("%s=%s", param, MASK));
                }
            }
        }
        return newBody;
    }

    private void maskHeaders(Map<String, String> headers, List<RequestLoggingProperties.Mask> masks) {
        for (RequestLoggingProperties.Mask mask : masks) {
            for (String header : mask.getMaskedHeaders()) {
                headers.replace(header, MASK);
            }
        }
    }

    private void maskQueryParams(MultiValueMap<String, String> queryParams, List<RequestLoggingProperties.Mask> masks) {
        for (RequestLoggingProperties.Mask mask : masks) {
            for (String param : mask.getMaskedQueryParams()) {
                queryParams.replace(param, Collections.singletonList(MASK));
            }
        }
    }

    private <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new Iterator<T>() {
                            public T next() {
                                return e.nextElement();
                            }
                            public boolean hasNext() {
                                return e.hasMoreElements();
                            }
                        },
                        Spliterator.ORDERED), false);
    }

}