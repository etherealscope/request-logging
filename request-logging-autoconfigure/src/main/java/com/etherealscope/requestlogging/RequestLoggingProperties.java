package com.etherealscope.requestlogging;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import javax.annotation.PostConstruct;

import static com.etherealscope.requestlogging.StatusCode.SC_ANY;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

/**
 * Configuration properties for request logging
 */
@Data
@FieldDefaults(level = PRIVATE)
@ConfigurationProperties("ethereal.logging")
public class RequestLoggingProperties {

    private static final String[] DEFAULT_CONTENT_TYPES = new String[] {
            APPLICATION_JSON_VALUE,
            APPLICATION_FORM_URLENCODED_VALUE,
            TEXT_PLAIN_VALUE,
            TEXT_XML_VALUE,
            APPLICATION_XML_VALUE
    };

    /**
     * If false, there will be no logging filter registered.
     */
    boolean enabled = true;
    /**
     * If true, at the filter end elapsed time will be logged.
     */
    boolean includeTimeElapsed = true;
    /**
     * Response status codes when to log request and response.
     */
    StatusCode[] statusCodes = new StatusCode[] {SC_ANY};
    /**
     * Request related config.
     */
    Request request = new Request();
    /**
     * Response related config
     */
    Response response = new Response();

    @Data
    @FieldDefaults(level = PRIVATE)
    public static class Request {

        /**
         * If false, no request will be logged.
         */
        boolean enabled = true;
        /**
         * If false, no request headers will be logged.
         */
        boolean includeHeaders = true;
        /**
         * If false, no request payload will be logged.
         */
        boolean includePayload = true;
        /**
         * If false, no request params will be logged.
         */
        boolean includeQueryParams = true;
        /**
         * If true, user ip address will be logged.
         */
        boolean includeIpAddress = false;
        /**
         * Size, where body will be cut.
         */
        int maxPayloadSize = 4096;
        /**
         * White listed content types where to log body.
         */
        String[] whiteListedContentTypes = DEFAULT_CONTENT_TYPES;
        /**
         * Black listed content types where to log body.
         */
        String[] blackListedContentTypes = new String[] {};
        /**
         * White listed servlet paths where to log request.
         */
        String[] whiteListedServletPaths = new String[] {};
        /**
         * Black listed servlet paths where to log request.
         */
        String[] blackListedServletPaths = new String[] {};
        /**
         * Masks to apply for request due to security.
         */
        Mask[] masks = new Mask[] {};
    }

    @Data
    @FieldDefaults(level = PRIVATE)
    public static class Response {

        /**
         * If false, no response will be logged.
         */
        boolean enabled = true;
        /**
         * If false, no response headers will be logged.
         */
        boolean includeHeaders = true;
        /**
         * If false, no response payload will be logged.
         */
        boolean includePayload = true;
        /**
         * Size, where body will be cut.
         */
        int maxPayloadSize = 4096;
        /**
         * White listed content types where to log body.
         */
        String[] whiteListedContentTypes = DEFAULT_CONTENT_TYPES;
        /**
         * Black listed content types where to log body.
         */
        String[] blackListedContentTypes = new String[] {};
        /**
         * White listed servlet paths where to log response.
         */
        String[] whiteListedServletPaths = new String[] {};
        /**
         * Black listed servlet paths where to log response.
         */
        String[] blackListedServletPaths = new String[] {};
        /**
         * Masks to apply for response due to security.
         */
        Mask[] masks = new Mask[] {};
    }

    @Data
    @FieldDefaults(level = PRIVATE)
    public static class Mask {

        /**
         * Http method to apply for.
         */
        HttpMethod method;
        /**
         * Path matcher like /users/**, /users/*, /users.
         */
        String pathMatcher;
        /**
         * Json field names to mask like password, newPassword, oldPassword.
         */
        String[] maskedJsonFields = new String[] {};
        /**
         * Query params to mask like password, newPassword, oldPassword.
         */
        String[] maskedQueryParams = new String[] {};
        /**
         * Headers to mask like authorization, cookie.
         */
        String[] maskedHeaders = new String[] {};
    }

    @PostConstruct
    void init() {
        if (statusCodes == null || statusCodes.length == 0) {
            throw new IllegalArgumentException("Status codes cannot be null or empty");
        }
        if (request.maxPayloadSize <= 0 || response.maxPayloadSize <= 0) {
            throw new IllegalArgumentException("Max payload size cannot be negative");
        }
        if (request.blackListedContentTypes == null || response.blackListedContentTypes == null) {
            throw new IllegalArgumentException("Black listed content types cannot be null, empty array required");
        }
        if (request.whiteListedContentTypes == null || response.whiteListedContentTypes == null) {
            throw new IllegalArgumentException("White listed content types cannot be null, empty array required");
        }
        if (request.blackListedServletPaths == null || response.blackListedServletPaths == null) {
            throw new IllegalArgumentException("Black listed servlet paths cannot be null, empty array required");
        }
        if (request.whiteListedServletPaths == null || response.whiteListedServletPaths == null) {
            throw new IllegalArgumentException("White listed servlet paths cannot be null, empty array required");
        }
        if (request.masks == null || response.masks == null) {
            throw new IllegalArgumentException("Masks cannot be null, empty array required");
        }
        if (request.blackListedContentTypes.length > 0 && request.whiteListedContentTypes.length > 0) {
            throw new IllegalArgumentException("You cannot set black list together with white list for content types");
        }
        if (response.blackListedContentTypes.length > 0 && response.whiteListedContentTypes.length > 0) {
            throw new IllegalArgumentException("You cannot set black list together with white list for content types");
        }
        for (Mask mask : request.masks) {
            if (mask.maskedQueryParams == null || mask.maskedHeaders == null || mask.maskedJsonFields == null) {
                throw new IllegalArgumentException("Mask query params, headers and json fields are not allowed to be null");
            }
        }
        for (Mask mask : response.masks) {
            if (mask.maskedQueryParams == null || mask.maskedHeaders == null || mask.maskedJsonFields == null) {
                throw new IllegalArgumentException("Mask query params, headers and json fields are not allowed to be null");
            }
        }
    }

}
