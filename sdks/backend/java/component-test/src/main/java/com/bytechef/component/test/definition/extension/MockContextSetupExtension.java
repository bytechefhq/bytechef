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

package com.bytechef.component.test.definition.extension;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
public class MockContextSetupExtension implements BeforeEachCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor = forClass(ConfigurationBuilder.class);
        Context mockedContext = mock(Context.class);
        Http.Executor mockedExecutor = mock(Http.Executor.class);
        Http.Response mockedResponse = mock(Http.Response.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor = forClass(ContextFunction.class);
        Http mockedHttp = mock(Http.class);

        when(mockedContext.http(httpFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                return value.apply(mockedHttp);
            });
        when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Store extensionContextStore = extensionContext.getStore(Namespace.create(MockContextSetupExtension.class));

        extensionContextStore.put(Context.class, mockedContext);
        extensionContextStore.put(Response.class, mockedResponse);
        extensionContextStore.put(Executor.class, mockedExecutor);
        extensionContextStore.put(Http.class, mockedHttp);
        extensionContextStore.put("httpFunctionArgumentCaptor", httpFunctionArgumentCaptor);
        extensionContextStore.put("configurationBuilderArgumentCaptor", configurationBuilderArgumentCaptor);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {

        Parameter parameter = parameterContext.getParameter();
        Type type = parameter.getParameterizedType();

        return parameter.getType() == Context.class ||
            parameter.getType() == Http.Response.class ||
            parameter.getType() == Http.Executor.class ||
            parameter.getType() == Http.class ||
            (parameter.getType() == ArgumentCaptor.class && type instanceof ParameterizedType pt &&
                (pt.getActualTypeArguments()[0].equals(ConfigurationBuilder.class) ||
                    (pt.getActualTypeArguments()[0] instanceof ParameterizedType nestedPt &&
                        nestedPt.getRawType()
                            .equals(ContextFunction.class))));
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {

        Store extensionContextStore = extensionContext.getStore(Namespace.create(MockContextSetupExtension.class));

        Parameter parameter = parameterContext.getParameter();
        Type type = parameter.getParameterizedType();

        if (parameter.getType() == Context.class) {
            return extensionContextStore.get(Context.class);
        }

        if (parameter.getType() == Http.Response.class) {
            return extensionContextStore.get(Http.Response.class);
        }

        if (parameter.getType() == Http.Executor.class) {
            return extensionContextStore.get(Http.Executor.class);
        }

        if (parameter.getType() == Http.class) {
            return extensionContextStore.get(Http.class);
        }

        if (parameter.getType() == ArgumentCaptor.class && type instanceof ParameterizedType pt) {
            Type actualTypeArgument = pt.getActualTypeArguments()[0];

            if (actualTypeArgument.equals(ConfigurationBuilder.class)) {
                return extensionContextStore.get("configurationBuilderArgumentCaptor");
            }

            if (actualTypeArgument instanceof ParameterizedType nestedPt) {
                Type rawType = nestedPt.getRawType();

                if (rawType.equals(ContextFunction.class)) {
                    return extensionContextStore.get("httpFunctionArgumentCaptor");
                }
            }
        }

        throw new ParameterResolutionException(
            "Unsupported parameter type: " + parameter.getType());
    }
}
