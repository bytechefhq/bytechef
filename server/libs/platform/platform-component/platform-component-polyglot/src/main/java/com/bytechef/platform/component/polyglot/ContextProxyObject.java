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

package com.bytechef.platform.component.polyglot;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class ContextProxyObject implements ProxyObject {

    private static final List<String> CONTEXT_MEMBER_KEYS = List.of("component", "log");
    private static final List<String> LOG_LEVELS = List.of("trace", "debug", "info", "warn", "error");

    private final String languageId;
    private final ComponentActionInvoker componentActionInvoker;
    private final ComponentCatalog componentCatalog;
    private final ContextLogWriter contextLogWriter;

    @SuppressFBWarnings("EI")
    public ContextProxyObject(
        String languageId, ComponentActionInvoker componentActionInvoker, ComponentCatalog componentCatalog,
        ContextLogWriter contextLogWriter) {

        this.languageId = languageId;
        this.componentActionInvoker = componentActionInvoker;
        this.componentCatalog = componentCatalog;
        this.contextLogWriter = contextLogWriter;
    }

    @Override
    public Object getMember(String name) {
        if (Objects.equals(name, "component")) {
            return new ComponentProxyObject(languageId, componentActionInvoker, componentCatalog);
        }

        if (Objects.equals(name, "log")) {
            return (ProxyExecutable) this::log;
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        return ProxyArray.fromList(new ArrayList<>(CONTEXT_MEMBER_KEYS));
    }

    @Override
    public boolean hasMember(String name) {
        return CONTEXT_MEMBER_KEYS.contains(name);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException();
    }

    private static String toLogLevel(Value levelValue) {
        String level = levelValue.isString() ? levelValue.asString() : String.valueOf(levelValue);

        String normalizedLevel = level.toLowerCase(Locale.ROOT);

        if (!LOG_LEVELS.contains(normalizedLevel)) {
            throw new IllegalArgumentException(
                "Unknown log level '%s', valid levels are: %s".formatted(level, String.join(", ", LOG_LEVELS)));
        }

        return normalizedLevel;
    }

    private static String toLogMessage(Value messageValue) {
        if (messageValue.isString()) {
            return messageValue.asString();
        }

        return String.valueOf(messageValue);
    }

    private Object log(Value... arguments) {
        if (arguments.length < 2) {
            throw new IllegalArgumentException("context.log requires a level and a message");
        }

        String level = toLogLevel(arguments[0]);
        String message = toLogMessage(arguments[1]);

        contextLogWriter.write(level, message);

        return null;
    }

    public String languageId() {
        return languageId;
    }

    public ComponentActionInvoker componentActionInvoker() {
        return componentActionInvoker;
    }

    public ComponentCatalog componentCatalog() {
        return componentCatalog;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (ContextProxyObject) obj;
        return Objects.equals(this.languageId, that.languageId) &&
            Objects.equals(this.componentActionInvoker, that.componentActionInvoker) &&
            Objects.equals(this.componentCatalog, that.componentCatalog) &&
            Objects.equals(this.contextLogWriter, that.contextLogWriter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(languageId, componentActionInvoker, componentCatalog, contextLogWriter);
    }

    @Override
    public String toString() {
        return "ContextProxyObject[" +
            "languageId=" + languageId + ']';
    }

}
