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

package com.bytechef.component.quickbooks.util;

import static com.bytechef.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.component.definition.Parameters;
import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;

/**
 * @author Mario Cvjetojevic
 */
public class QuickbooksUtils {

    public static DataService getDataService(Parameters connectionParameters) throws FMSException {
        return new DataService(
            new Context(
                new OAuth2Authorizer(connectionParameters.getRequiredString(ACCESS_TOKEN)),
                ServiceType.QBO, connectionParameters.getRequiredString("realmId")));
    }
}
