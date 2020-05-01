package com.etherealscope.requestlogging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Math.min;

@Slf4j
class CommonUtils {

    static final String NOTHING = "[nothing]";
    static final String UNKNOWN = "[unknown]";
    static final AntPathMatcher MATCHER = new AntPathMatcher();

    static String byteArrayToString(byte[] byteArray, String encoding, int maxLength) {
        if (byteArray != null && byteArray.length > 0) {
            int length = min(byteArray.length, maxLength);
            try {
                return new String(byteArray, 0, length, encoding);
            } catch (UnsupportedEncodingException ex) {
                log.warn("Unsupported encoding in request or response {}", encoding);
                return UNKNOWN;
            }
        }
        return NOTHING;
    }

    static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
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

    static boolean servletPathEnabled(String servletPath, String[] whiteList, String[] blackList) {
        if (whiteList.length == 0 && blackList.length == 0) {
            return true;
        }
        if (whiteList.length > 0) {
            for (String s: whiteList) {
                if(MATCHER.match(s, servletPath)) {
                    return true;
                }
            }
            return false;
        }
        for (String s: blackList) {
            if(MATCHER.match(s, servletPath)) {
                    return false;
            }
        }
        return true;
    }

}
