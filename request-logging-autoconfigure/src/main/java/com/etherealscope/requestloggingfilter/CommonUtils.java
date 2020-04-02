package com.etherealscope.requestloggingfilter;

import lombok.extern.slf4j.Slf4j;

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

}
