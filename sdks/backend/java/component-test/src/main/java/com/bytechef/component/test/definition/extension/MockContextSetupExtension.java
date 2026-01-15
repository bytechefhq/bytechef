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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Nikolina Spehar
 */
public class MockContextSetupExtension implements BeforeEachCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Context mockedContext = mock(Context.class);
        Http.Executor mockedExecutor = mock(Http.Executor.class);
        Http.Response mockedResponse = mock(Http.Response.class);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        ExtensionContext.Store store = extensionContext.getStore(Namespace.create(MockContextSetupExtension.class));
        store.put(Context.class, mockedContext);
        store.put(Response.class, mockedResponse);
        store.put(Executor.class, mockedExecutor);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {

        return parameterContext.getParameter()
            .getType() == Context.class
            || parameterContext.getParameter()
                .getType() == Http.Response.class
            || parameterContext.getParameter()
                .getType() == Http.Executor.class;
    }

    @Override
    public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
        throws ParameterResolutionException {

        ExtensionContext.Store extensionContextStore = extensionContext.getStore(
            Namespace.create(MockContextSetupExtension.class));

        if (parameterContext.getParameter()
            .getType() == Context.class) {

            return extensionContextStore.get(Context.class);

        } else if (parameterContext.getParameter()
            .getType() == Http.Response.class) {

            return extensionContextStore.get(Http.Response.class);

        } else if (parameterContext.getParameter()
            .getType() == Http.Executor.class) {

            return extensionContextStore.get(Http.Executor.class);

        } else {
            throw new ParameterResolutionException(
                "Unsupported parameter type: " + parameterContext.getParameter()
                    .getType());
        }
    }
}
