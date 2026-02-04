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

package com.bytechef.component.xero.connection;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;
import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.ApplyResponse.ofHeaders;
import static com.bytechef.component.definition.Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE;
import static com.bytechef.component.definition.Authorization.BEARER;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 */
public class XeroConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.xero.com/api.xro/2.0")
        .authorizations(
            authorization(OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .apply(XeroConnection::getApplyResponse)
                .authorizationUrl((connection, context) -> "https://login.xero.com/identity/connect/authorize")
                .scopes((connection, context) -> {
                    Map<String, Boolean> map = new HashMap<>();

                    map.put("offline_access", true);
                    map.put("openid", false);
                    map.put("profile", false);
                    map.put("email", false);
                    map.put("accounting.transactions", true);
                    map.put("accounting.transactions.read", false);
                    map.put("accounting.invoices", false);
                    map.put("accounting.invoices.read", false);
                    map.put("accounting.payments", false);
                    map.put("accounting.payments.read", false);
                    map.put("accounting.banktransactions", false);
                    map.put("accounting.banktransactions.read", false);
                    map.put("accounting.manualjournals", false);
                    map.put("accounting.manualjournals.read", false);
                    map.put("accounting.reports.read", false);
                    map.put("accounting.reports.aged.read", false);
                    map.put("accounting.reports.balancesheets.read", false);
                    map.put("accounting.reports.banksummary.read", false);
                    map.put("accounting.reports.budgetsummary.read", false);
                    map.put("accounting.reports.executivesummary.read", false);
                    map.put("accounting.reports.profitandloss.read", false);
                    map.put("accounting.reports.trialbalance.read", false);
                    map.put("accounting.reports.taxreports.read", false);
                    map.put("accounting.reports.tenninetynine.read", false);
                    map.put("accounting.journals.read", false);
                    map.put("accounting.settings", false);
                    map.put("accounting.settings.read", true);
                    map.put("accounting.contacts", true);
                    map.put("accounting.contacts.read", false);
                    map.put("accounting.attachments", false);
                    map.put("accounting.attachments.read", false);
                    map.put("accounting.budgets.read", false);
                    map.put("payroll.employees", false);
                    map.put("payroll.employees.read", false);
                    map.put("payroll.payruns", false);
                    map.put("payroll.payruns.read", false);
                    map.put("payroll.payslip", false);
                    map.put("payroll.payslip.read", false);
                    map.put("payroll.timesheets", false);
                    map.put("payroll.timesheets.read", false);
                    map.put("payroll.settings", false);
                    map.put("payroll.settings.read", false);
                    map.put("files", false);
                    map.put("files.read", false);
                    map.put("assets", false);
                    map.put("assets.read", false);
                    map.put("projects", false);
                    map.put("projects.read", false);
                    map.put("paymentservices", false);
                    map.put("bankfeeds", false);
                    map.put("finance.accountingactivity.read", false);
                    map.put("finance.cashvalidation.read", false);
                    map.put("finance.statements.read", false);
                    map.put("finance.bankstatementsplus.read", false);
                    map.put("practicemanager.job", false);
                    map.put("practicemanager.job.read", false);
                    map.put("practicemanager.client", false);
                    map.put("practicemanager.client.read", false);
                    map.put("practicemanager.staff", false);
                    map.put("practicemanager.staff.read", false);
                    map.put("practicemanager.time", false);
                    map.put("practicemanager.time.read", false);
                    map.put("einvoicing", false);
                    map.put("app.connections", false);
                    map.put("marketplace.billing", false);

                    return map;
                })
                .tokenUrl((connection, context) -> "https://identity.xero.com/connect/token")
                .refreshUrl((connection, context) -> "https://identity.xero.com/connect/token"));

    private static ApplyResponse getApplyResponse(Parameters connectionParameters, Context context) {
        return ofHeaders(
            Map.of(
                AUTHORIZATION, List.of(BEARER + " " + connectionParameters.getRequiredString(ACCESS_TOKEN)),
                "Xero-tenant-id", List.of(getTenantId(connectionParameters.getRequiredString(ACCESS_TOKEN), context))));
    }

    private XeroConnection() {
    }

    private static String getTenantId(String accessToken, Context context) {
        Http.Response response = context
            .http(http -> http.get("https://api.xero.com/connections"))
            .body(
                Body.of(
                    Map.of(
                        "Authorization", BEARER + " " + accessToken,
                        "Content-Type", "application/json")))
            .header(AUTHORIZATION, BEARER + " " + accessToken)
            .configuration(
                Http.responseType(Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute();

        Object body = response.getBody();

        if (body instanceof Map<?, ?> map) {
            return (String) map.get("tenantId");
        }

        if (body instanceof List<?>) {
            List<?> tenantList = (List<?>) response.getBody();

            if (tenantList.getFirst() instanceof Map<?, ?> map) {
                return (String) map.get("tenantId");
            }
        }
        throw new RuntimeException("Xero did not return any Tenants.");
    }
}
