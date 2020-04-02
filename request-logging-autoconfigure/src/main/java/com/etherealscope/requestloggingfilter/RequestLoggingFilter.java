package com.etherealscope.requestloggingfilter;

import com.etherealscope.requestloggingfilter.RequestLoggingProperties.Mask;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.core.annotation.Order;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.etherealscope.requestloggingfilter.CommonUtils.NOTHING;
import static com.etherealscope.requestloggingfilter.CommonUtils.enumerationAsStream;
import static com.etherealscope.requestloggingfilter.CommonUtils.byteArrayToString;
import static com.etherealscope.requestloggingfilter.MaskUtils.filterMasks;
import static com.etherealscope.requestloggingfilter.MaskUtils.maskBody;
import static com.etherealscope.requestloggingfilter.MaskUtils.maskHeaders;
import static com.etherealscope.requestloggingfilter.MaskUtils.maskQueryParams;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.web.util.WebUtils.getNativeRequest;
import static org.springframework.web.util.WebUtils.getNativeResponse;

@Slf4j
@Order(OrderedFilter.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String BEFORE_REQUEST_MESSAGE = "--- REQUEST LOG START ---";
    private static final String AFTER_REQUEST_MESSAGE = "--- REQUEST LOG END ---";
    private static final String BEFORE_RESPONSE_MESSAGE = "--- RESPONSE LOG START ---";
    private static final String AFTER_RESPONSE_MESSAGE = "--- RESPONSE LOG END ---";

    RequestLoggingProperties props;

    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {

        long start = 0;
        if (props.isIncludeTimeElapsed()) {
            start = currentTimeMillis();
        }

        String method = request.getMethod();
        String contentType = request.getContentType();
        String servletPath = request.getServletPath();

        if (shouldLogRequestBody(method, contentType) && !(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }

        if (shouldCacheResponse() && !(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }

        try {
            if (props.getResponse().isEnabled()) {
                List<Mask> requestMasks = filterMasks(method, servletPath, props.getRequest().getMasks());
                log.debug(getRequestLogMessage(request, requestMasks));
            }
            filterChain.doFilter(request, response);
        } finally {
            if (props.getResponse().isEnabled()) {
                List<Mask> responseMasks = filterMasks(method, servletPath, props.getResponse().getMasks());
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
                if (contentType != null && contentType.contains(s)) {
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
                if (contentType != null && contentType.contains(s)) {
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

    private String getRequestLogMessage(HttpServletRequest request, List<Mask> masks) {
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

        if (props.getRequest().isIncludeIpAddress()) {
            logMap.put("Ip-Address: ", request.getRemoteAddr());
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
                + logMap.entrySet().stream().map(e -> e.getKey() + e.getValue()).collect(joining("\n"))
                + "\n"
                + AFTER_REQUEST_MESSAGE;
    }

    private String getResponseLogMessage(HttpServletResponse response, List<Mask> masks) {
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
                + logMap.entrySet().stream().map(e -> e.getKey() + e.getValue()).collect(joining("\n"))
                + "\n"
                + AFTER_RESPONSE_MESSAGE;
    }

    private String getResponsePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            return byteArrayToString(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding(), props.getResponse().getMaxPayloadSize());
        }
        return NOTHING;
    }

    private String getRequestPayload(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper != null) {
            return byteArrayToString(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding(), props.getRequest().getMaxPayloadSize());
        }
        return NOTHING;
    }

    @SneakyThrows
    private void updateResponse(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {
            wrapper.copyBodyToResponse();
        }
    }

}