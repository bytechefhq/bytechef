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

package com.bytechef.message.broker.memory.listener;

import com.bytechef.message.broker.memory.MemoryMessageBroker;
import com.bytechef.message.route.MessageRoute;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * @author Ivica Cardic
 */
public class MemoryListenerEndpointRegistrar {

    private final MemoryMessageBroker memoryMessageBroker;

    public MemoryListenerEndpointRegistrar(MemoryMessageBroker memoryMessageBroker) {
        this.memoryMessageBroker = memoryMessageBroker;
    }

    public void registerListenerEndpoint(MessageRoute messageRoute, Object delegate, String methodName) {
        memoryMessageBroker.receive(
            messageRoute,
            message -> {
                try {
                    new MethodInvoker(delegate, methodName).invoke(new Object[] {
                        message
                    });
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    private static class MethodInvoker {
        private final Object delegate;
        private final Set<Method> methods;

        MethodInvoker(Object delegate, String methodName) {
            this.delegate = delegate;
            this.methods = new HashSet<>();

            Class<?> c = delegate.getClass();

            ReflectionUtils.doWithMethods(
                c,
                (method) -> {
                    ReflectionUtils.makeAccessible(method);
                    this.methods.add(method);
                },
                new MostSpecificMethodFilter(methodName, c));

            Assert.isTrue(
                !this.methods.isEmpty(),
                "Cannot find a suitable method named [" + c.getName() + "#" + methodName + "] - " +
                    "is the method public and has the proper arguments");
        }

        void invoke(Object[] arguments) throws InvocationTargetException, IllegalAccessException {
            Object[] message = new Object[] {
                arguments[0]
            };

            for (Method m : this.methods) {
                Class<?>[] types = m.getParameterTypes();
                Object[] args =
                    types.length == 2 && types[0].isInstance(arguments[0]) && types[1].isInstance(arguments[1])
                        ? arguments : message;

                if (types[0].isInstance(args[0])) {
                    m.invoke(this.delegate, args);

                    return;
                }
            }

        }
    }

    private record MostSpecificMethodFilter(String methodName, Class<?> c) implements ReflectionUtils.MethodFilter {

        public boolean matches(Method method) {
            if (Modifier.isPublic(method.getModifiers()) && this.methodName.equals(method.getName()) &&
                method.equals(ClassUtils.getMostSpecificMethod(method, this.c))) {

                Class<?>[] parameterTypes = method.getParameterTypes();

                return parameterTypes.length == 2 && String.class.equals(parameterTypes[1]) ||
                    parameterTypes.length == 1;
            } else {
                return false;
            }
        }
    }
}
