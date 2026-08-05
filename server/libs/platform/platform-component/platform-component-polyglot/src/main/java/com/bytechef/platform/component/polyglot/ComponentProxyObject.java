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

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * Guest-facing proxy exposing components as members: {@code context.component.<componentName>}. Existence checks are
 * delegated to the constructor-injected {@link ComponentCatalog} seam.
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
public final class ComponentProxyObject implements ProxyObject {

    private final String languageId;
    private final ComponentActionInvoker componentActionInvoker;
    private final ComponentCatalog componentCatalog;

    public ComponentProxyObject(
        String languageId, ComponentActionInvoker componentActionInvoker, ComponentCatalog componentCatalog) {

        this.languageId = languageId;
        this.componentActionInvoker = componentActionInvoker;
        this.componentCatalog = componentCatalog;
    }

    @Override
    public Object getMember(String componentName) {
        return new ActionProxyObject(languageId, componentName, componentActionInvoker, componentCatalog);
    }

    @Override
    public Object getMemberKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasMember(String componentName) {
        return componentCatalog.hasComponent(componentName);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException();
    }

}
