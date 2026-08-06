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

import java.util.Map;
import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * Guest-facing proxy exposing a single component's actions as callable members:
 * {@code context.component.<componentName>.<actionName>(input, connectionName?)}. Dispatches through the
 * constructor-injected {@link ComponentActionInvoker} seam so this class carries no knowledge of how a component action
 * is actually looked up or performed.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class ActionProxyObject implements ProxyObject {

    private final String languageId;
    private final String componentName;
    private final ComponentActionInvoker componentActionInvoker;
    private final ComponentCatalog componentCatalog;

    public ActionProxyObject(
        String languageId, String componentName, ComponentActionInvoker componentActionInvoker,
        ComponentCatalog componentCatalog) {

        this.languageId = languageId;
        this.componentName = componentName;
        this.componentActionInvoker = componentActionInvoker;
        this.componentCatalog = componentCatalog;
    }

    @Override
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public ProxyExecutable getMember(String actionName) {
        return arguments -> {
            Map<String, ?> inputParameters = Map.of();

            if (arguments.length > 0) {
                inputParameters = (Map<String, ?>) PolyglotValues.copyToJavaValue(arguments[0]);
            }

            String connectionName = arguments.length > 1 && !arguments[1].isNull() ? arguments[1].asString() : null;

            Map<String, ?> clusterElements = null;

            if (arguments.length > 2 && !arguments[2].isNull()) {
                clusterElements = (Map<String, ?>) PolyglotValues.copyFromPolyglotContext(
                    PolyglotValues.copyToJavaValue(arguments[2]));
            }

            Object result;

            try {
                result = componentActionInvoker.invoke(
                    componentName, actionName, (Map) PolyglotValues.copyFromPolyglotContext(inputParameters),
                    connectionName, clusterElements);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            if (result == null) {
                return null;
            }

            return PolyglotValues.copyToGuestValue(result, languageId);
        };
    }

    @Override
    public Object getMemberKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMember(String actionName) {
        return componentCatalog.hasAction(componentName, actionName);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException();
    }

    public String languageId() {
        return languageId;
    }

    public String componentName() {
        return componentName;
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
        var that = (ActionProxyObject) obj;
        return Objects.equals(this.languageId, that.languageId) &&
            Objects.equals(this.componentName, that.componentName) &&
            Objects.equals(this.componentActionInvoker, that.componentActionInvoker) &&
            Objects.equals(this.componentCatalog, that.componentCatalog);
    }

    @Override
    public int hashCode() {
        return Objects.hash(languageId, componentName, componentActionInvoker, componentCatalog);
    }

    @Override
    public String toString() {
        return "ActionProxyObject[" +
            "languageId=" + languageId + ", " +
            "componentName=" + componentName + ']';
    }

}
