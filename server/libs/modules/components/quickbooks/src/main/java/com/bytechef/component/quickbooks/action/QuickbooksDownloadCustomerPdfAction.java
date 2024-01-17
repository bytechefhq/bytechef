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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CUSTOMER_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.DOWNLOAD_CUSTOMER_PDF;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableOption;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.quickbooks.util.QuickbooksUtils;
import com.intuit.ipp.data.Customer;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Mario Cvjetojevic
 */
public final class QuickbooksDownloadCustomerPdfAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DOWNLOAD_CUSTOMER_PDF)
        .title("Download customer pdf")
        .description("Downloads the pdf file of a customer.")
        .properties(
            string(CUSTOMER_ID)
                .label("Customer")
                .description("The id of a customer to download the pdf for.")
                .options((ActionOptionsFunction) QuickbooksDownloadCustomerPdfAction::getAllCustomerOptions))
        .outputSchema(fileEntry())
        .perform(QuickbooksDownloadCustomerPdfAction::perform);

    private QuickbooksDownloadCustomerPdfAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws FMSException, IOException {

        DataService service = QuickbooksUtils.getDataService(connectionParameters);

        Customer customer = new Customer();

        customer.setId(inputParameters.getRequiredString(CUSTOMER_ID));

        try (InputStream inputStream = service.downloadPDF(customer)) {
            return actionContext.file(file -> file.storeContent("QuickbooksCustomer " + customer.getId(), inputStream));
        }
    }

    private static List<ModifiableOption<String>> getAllCustomerOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context)
        throws FMSException {

        DataService dataService = QuickbooksUtils.getDataService(connectionParameters);

        QueryResult queryResult = dataService.executeQuery("select * from Customer");

        return queryResult.getEntities()
            .stream()
            .map(entity -> option(((Customer) entity).getDisplayName(), ((Customer) entity).getId()))
            .toList();
    }
}
