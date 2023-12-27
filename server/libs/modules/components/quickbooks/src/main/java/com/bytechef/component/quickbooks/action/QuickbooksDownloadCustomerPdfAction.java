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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DOWNLOADCUSTOMERPDF;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.option;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OptionsDataSource;
import com.bytechef.hermes.component.definition.OptionsDataSource.OptionsResponse;
import com.bytechef.hermes.component.definition.Parameters;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import java.io.InputStream;

/**
 * @author Mario Cvjetojevic
 */
public final class QuickbooksDownloadCustomerPdfAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DOWNLOADCUSTOMERPDF)
        .title("Download customer pdf")
        .description("Downloads the pdf file of a customer.")
        .properties(
            string(CUSTOMER_ID)
                .label("Customer")
                .description("The id of a customer to download the pdf for.")
                .options(
                    (OptionsDataSource.ActionOptionsFunction) QuickbooksDownloadCustomerPdfAction::getAllCustomers))
        .outputSchema(fileEntry())
        .perform(QuickbooksDownloadCustomerPdfAction::perform);

    private QuickbooksDownloadCustomerPdfAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws FMSException {

        DataService service = QuickbooksUtils.getDataService(connectionParameters.getRequiredString(ACCESS_TOKEN));

        Customer customer = new Customer();

        customer.setId(inputParameters.getRequiredString(CUSTOMER_ID));

        InputStream inputStream = service.downloadPDF(customer);

        return actionContext.file(file -> file.storeContent(
            "QuickbooksCustomer " + customer.getId(), inputStream));
    }

    private static OptionsResponse getAllCustomers(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {
        return new OptionsResponse(QuickbooksUtils
            .getDataService(connectionParameters.getRequiredString(ACCESS_TOKEN))
            .executeQuery("select * from Customer")
            .getEntities()
            .stream()
            .map(entity -> option(((Customer) entity).getDisplayName(), ((Customer) entity).getId()))
            .toList());
    }
}
