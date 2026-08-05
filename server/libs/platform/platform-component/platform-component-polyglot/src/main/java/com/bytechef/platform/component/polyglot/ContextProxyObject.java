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

import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * Root guest-facing proxy passed into a guest {@code perform} function as the {@code context} argument. Exposes a
 * single {@code component} member backed by {@link ComponentProxyObject}.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class ContextProxyObject implements ProxyObject {

    private final String languageId;
    private final ComponentActionInvoker componentActionInvoker;
    private final ComponentCatalog componentCatalog;

    public ContextProxyObject(
        String languageId, ComponentActionInvoker componentActionInvoker, ComponentCatalog componentCatalog) {

        this.languageId = languageId;
        this.componentActionInvoker = componentActionInvoker;
        this.componentCatalog = componentCatalog;
    }

    @Override
    public Object getMember(String name) {
        if (Objects.equals(name, "component")) {
            return new ComponentProxyObject(languageId, componentActionInvoker, componentCatalog);
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMember(String name) {
        return Objects.equals(name, "component");
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException();
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
            Objects.equals(this.componentCatalog, that.componentCatalog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(languageId, componentActionInvoker, componentCatalog);
    }

    @Override
    public String toString() {
        return "ContextProxyObject[" +
            "languageId=" + languageId + ']';
    }

}
