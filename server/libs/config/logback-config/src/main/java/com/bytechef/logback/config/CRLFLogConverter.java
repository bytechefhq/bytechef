/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.logback.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

/**
 * Log filter to prevent attackers from forging log entries by submitting input containing CRLF characters. CRLF
 * characters are replaced with a red colored _ character.
 *
 * @see <a href="https://owasp.org/www-community/attacks/Log_Injection">Log Forging Description</a>
 * @see <a href="https://github.com/jhipster/generator-jhipster/issues/14949">JHipster issue</a>
 */
public class CRLFLogConverter extends CompositeConverter<ILoggingEvent> {

    public static final Marker CRLF_SAFE_MARKER = MarkerFactory.getMarker("CRLF_SAFE");

    private static final String[] SAFE_LOGGERS = {
        "org.springframework.boot.autoconfigure",
        "org.springframework.boot.diagnostics",
        "org.quartz.core",
    };
    private static final Map<String, AnsiElement> ELEMENTS;

    static {
        Map<String, AnsiElement> ansiElements = new HashMap<>();

        ansiElements.put("faint", AnsiStyle.FAINT);
        ansiElements.put("red", AnsiColor.RED);
        ansiElements.put("green", AnsiColor.GREEN);
        ansiElements.put("yellow", AnsiColor.YELLOW);
        ansiElements.put("blue", AnsiColor.BLUE);
        ansiElements.put("magenta", AnsiColor.MAGENTA);
        ansiElements.put("cyan", AnsiColor.CYAN);

        ELEMENTS = Collections.unmodifiableMap(ansiElements);
    }

    @Override
    protected String transform(ILoggingEvent event, String in) {
        AnsiElement element = ELEMENTS.get(getFirstOption());

        List<Marker> markers = event.getMarkerList();

        if (markers != null && !markers.isEmpty()) {
            Marker marker = markers.getFirst();

            if (marker.contains(CRLF_SAFE_MARKER) || isLoggerSafe(event)) {
                return in;
            }
        }

        String replacement = element == null ? "_" : toAnsiString("_", element);

        return in.replaceAll("[\n\r\t]", replacement);
    }

    protected boolean isLoggerSafe(ILoggingEvent event) {
        for (String safeLogger : SAFE_LOGGERS) {
            String loggerName = event.getLoggerName();

            if (loggerName.startsWith(safeLogger)) {
                return true;
            }
        }

        return false;
    }

    protected String toAnsiString(String in, AnsiElement element) {
        return AnsiOutput.toString(element, in);
    }
}
