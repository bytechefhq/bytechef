/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class QuickbooksUtilsTest {

    @Test
    public void getDataServiceTest() throws FMSException {
        try (MockedConstruction<OAuth2Authorizer> oAuth2AuthorizerMockedConstruction = mockConstruction(
            OAuth2Authorizer.class)) {

            try (MockedConstruction<Context> contextMockedConstruction = mockConstruction(Context.class)) {
                try (MockedConstruction<DataService> dataServiceMockedConstruction = mockConstruction(
                    DataService.class)) {

                    QuickbooksUtils.getDataService(mock(Parameters.class));

                    List<OAuth2Authorizer> oAuth2Authorizers = oAuth2AuthorizerMockedConstruction.constructed();

                    Assertions.assertEquals(1, oAuth2Authorizers.size());

                    List<Context> contexts = contextMockedConstruction.constructed();

                    Assertions.assertEquals(1, contexts.size());

                    List<DataService> dataServices = dataServiceMockedConstruction.constructed();

                    Assertions.assertEquals(1, dataServices.size());
                }
            }
        }
    }
}
