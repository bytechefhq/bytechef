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

package com.bytechef.component.shopify.action;

import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.shopify.util.ShopifyOptionsUtils;
import com.bytechef.component.shopify.util.ShopifyUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
abstract class AbstractShopifyActionTest {

    protected final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("unchecked")
    protected final ArgumentCaptor<List<Object>> listArgumentCaptor = forClass(List.class);
    @SuppressWarnings("unchecked")
    protected final ArgumentCaptor<Map<String, Object>> mapArgumentCaptor = forClass(Map.class);
    protected final Context mockedContext = mock(Context.class);
    protected MockedStatic<ShopifyOptionsUtils> shopifyOptionsUtilsMockedStatic;
    protected MockedStatic<ShopifyUtils> shopifyUtilsMockedStatic;
    protected final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach() {
        shopifyUtilsMockedStatic = mockStatic(ShopifyUtils.class);
        shopifyOptionsUtilsMockedStatic = mockStatic(ShopifyOptionsUtils.class);
    }

    @AfterEach
    void afterEach() {
        shopifyUtilsMockedStatic.close();
        shopifyOptionsUtilsMockedStatic.close();
    }
}
