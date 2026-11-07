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

package com.bytechef.test.voice;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import java.lang.reflect.Method;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

/**
 * @author Ivica Cardic
 */
public final class JsonSupport {

    private JsonSupport() {
    }

    public static Context.Json create() {
        return Mockito.mock(Context.Json.class, JsonSupport::answer);
    }

    private static Object answer(InvocationOnMock invocation) {
        Method method = invocation.getMethod();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        if ("read".equals(methodName) && parameterTypes.length == 1 && parameterTypes[0] == String.class) {
            return JsonUtils.read((String) invocation.getArgument(0));
        }

        if ("read".equals(methodName) && parameterTypes.length == 2 && parameterTypes[0] == String.class
            && parameterTypes[1] == Class.class) {

            return JsonUtils.read((String) invocation.getArgument(0), (Class<?>) invocation.getArgument(1));
        }

        if ("read".equals(methodName) && parameterTypes.length == 2 && parameterTypes[0] == String.class
            && parameterTypes[1] == TypeReference.class) {

            TypeReference<?> typeReference = invocation.getArgument(1);

            return JsonUtils.read((String) invocation.getArgument(0), typeReference.getType());
        }

        if ("readMap".equals(methodName) && parameterTypes.length == 1 && parameterTypes[0] == String.class) {
            return JsonUtils.readMap((String) invocation.getArgument(0));
        }

        if ("readMap".equals(methodName) && parameterTypes.length == 2 && parameterTypes[0] == String.class
            && parameterTypes[1] == Class.class) {

            return JsonUtils.readMap((String) invocation.getArgument(0), (Class<?>) invocation.getArgument(1));
        }

        if ("write".equals(methodName) && parameterTypes.length == 1 && parameterTypes[0] == Object.class) {
            return JsonUtils.write(invocation.getArgument(0));
        }

        throw new UnsupportedOperationException(
            "Context.Json." + methodName + " is not supported by the voice contract test");
    }
}
