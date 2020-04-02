package com.etherealscope.requestloggingfilter;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import javax.annotation.PostConstruct;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.MediaType.TEXT_XML_VALUE;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ConfigurationProperties("ethereal.logging")
public class RequestLoggingProperties {

    private static final String[] DEFAULT_CONTENT_TYPES = new String[] {
            APPLICATION_JSON_VALUE,
            APPLICATION_FORM_URLENCODED_VALUE,
            TEXT_PLAIN_VALUE,
            TEXT_XML_VALUE,
            APPLICATION_XML_VALUE
    };

    boolean enabled = true;
    boolean includeTimeElapsed = true;
    Request request = new Request();
    Response response = new Response();

    @PostConstruct
    void init() {

    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Request {
        boolean enabled = true;
        boolean includeHeaders = true;
        boolean includePayload = true;
        boolean includeQueryParams = true;
        boolean includeClientInfo = false;
        int maxPayloadSize = 4096;
        String[] whiteListedContentTypes = DEFAULT_CONTENT_TYPES;
        String[] blackListedContentTypes = new String[] {};
        Mask[] masks = new Mask[] {};
        //incl excl url
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Response {
        boolean enabled = true;
        boolean includeHeaders = true;
        boolean includePayload = true;
        int maxPayloadSize = 4096;
        String[] whiteListedContentTypes = DEFAULT_CONTENT_TYPES;
        String[] blackListedContentTypes = new String[] {};
        Mask[] masks = new Mask[] {};
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Mask {
        HttpMethod method;
        String pathMatcher;
        String[] maskedJsonFields = new String[] {};
        String[] maskedQueryParams = new String[] {};
        String[] maskedHeaders = new String[] {};
    }
}
