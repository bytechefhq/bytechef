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

import java.util.HashMap;
import java.util.Map;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * @author Ivica Cardic
 */
final class MemberAndHashMapProxy implements ProxyObject, ProxyHashMap {

    private final ProxyObject memberProxy;
    private final ProxyHashMap hashProxy;

    MemberAndHashMapProxy(Map<String, Object> map) {
        this.memberProxy = ProxyObject.fromMap(map);
        this.hashProxy = ProxyHashMap.from(new HashMap<>(map));
    }

    @Override
    public Object getMember(String key) {
        return memberProxy.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
        return memberProxy.getMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
        return memberProxy.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getHashSize() {
        return hashProxy.getHashSize();
    }

    @Override
    public boolean hasHashEntry(Value key) {
        return hashProxy.hasHashEntry(key);
    }

    @Override
    public Object getHashValue(Value key) {
        return hashProxy.getHashValue(key);
    }

    @Override
    public void putHashEntry(Value key, Value value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getHashEntriesIterator() {
        return hashProxy.getHashEntriesIterator();
    }
}
