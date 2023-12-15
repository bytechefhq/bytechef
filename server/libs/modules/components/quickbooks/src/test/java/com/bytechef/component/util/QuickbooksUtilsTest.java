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

import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

public class QuickbooksUtilsTest {

    @Test
    public void getDataServiceTest() throws FMSException {
        try (MockedConstruction<OAuth2Authorizer> oAuth2AuthorizerMockedConstruction =
            Mockito.mockConstruction(OAuth2Authorizer.class)) {
            try (MockedConstruction<Context> intuitContextMockedConstruction =
                Mockito.mockConstruction(Context.class)) {
                try (MockedConstruction<DataService> dataServiceMockedConstruction =
                    Mockito.mockConstruction(DataService.class)) {

                    QuickbooksUtils.getDataService("");

                    List<OAuth2Authorizer> oAuth2AuthorizerList = oAuth2AuthorizerMockedConstruction.constructed();
                    List<Context> intuitContextList = intuitContextMockedConstruction.constructed();
                    List<DataService> dataServiceList = dataServiceMockedConstruction.constructed();

                    Assertions.assertEquals(
                        1, oAuth2AuthorizerList.size(),
                        "One instance of com.intuit.ipp.security.OAuth2Authorizer is enough!");
                    Assertions.assertEquals(
                        1, intuitContextList.size(),
                        "One instance of com.intuit.ipp.core.Context is enough!");
                    Assertions.assertEquals(
                        1, dataServiceList.size(),
                        "One instance of com.intuit.ipp.services.DataService is enough!");
                }
            }
        }
    }
}
