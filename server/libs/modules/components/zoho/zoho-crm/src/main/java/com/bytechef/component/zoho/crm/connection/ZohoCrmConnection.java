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

package com.bytechef.component.zoho.crm.connection;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.zoho.commons.ZohoConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class ZohoCrmConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = ZohoConnection.createConnection(
        "/crm/v7", getScopes(), false);

    private static Map<String, Boolean> getScopes() {
        Map<String, Boolean> map = new HashMap<>();

        map.put("ZohoCRM.org.ALL", false);
        map.put("ZohoCRM.org.CREATE", false);
        map.put("ZohoCRM.org.READ", true);
        map.put("ZohoCRM.org.UPDATE", false);
        map.put("ZohoCRM.org.DELETE", false);
        map.put("ZohoCRM.apis.READ", false);
        map.put("ZohoCRM.Zia.enrichment.READ", false);
        map.put("ZohoCRM.Zia.enrichment.ALL", false);
        getSettingsScopes(map);
        map.put("ZohoCRM.users.ALL", true);
        map.put("ZohoCRM.users.CREATE", false);
        map.put("ZohoCRM.users.READ", false);
        map.put("ZohoCRM.users.UPDATE", false);
        map.put("ZohoCRM.users.DELETE", false);
        map.put("ZohoCRM.templates.READ", false);
        map.put("ZohoCRM.templates.email.READ", false);
        map.put("ZohoCRM.templates.inventory.READ", false);
        map.put("ZohoCRM.modules.ALL", false);
        map.put("ZohoCRM.modules.entity_scores.READ", false);
        map.put("ZohoCRM.modules.READ", false);
        getMassConvertScopes(map);
        getMassUpdateScopes(map);
        getMassDeleteScopes(map);
        getShareScopes(map);
        getSendMailScopes(map);
        map.put("ZohoCRM.change_owner.CREATE", false);
        map.put("ZohoCRM.change_owner.READ", false);
        map.put("ZohoCRM.files.CREATE", false);
        map.put("ZohoCRM.files.READ", false);
        map.put("ZohoCRM.bulk.ALL", false);
        map.put("ZohoCRM.bulk.CREATE", false);
        map.put("ZohoCRM.bulk.READ", false);
        map.put("ZohoCRM.bulk.UPDATE", false);
        map.put("ZohoCRM.bulk.DELETE", false);
        map.put("ZohoCRM.bulk.backup.ALL", false);
        map.put("ZohoCRM.bulk.backup.CREATE", false);
        map.put("ZohoCRM.bulk.backup.READ", false);
        map.put("ZohoCRM.bulk.backup.UPDATE", false);
        map.put("ZohoCRM.features.READ", false);
        map.put("ZohoCRM.composite_requests.CUSTOM", false);
        map.put("ZohoCRM.notifications.ALL", false);
        map.put("ZohoCRM.notifications.CREATE", false);
        map.put("ZohoCRM.notifications.READ", false);
        map.put("ZohoCRM.notifications.UPDATE", false);
        map.put("ZohoCRM.notifications.DELETE", false);
        map.put("ZohoCRM.coql.READ", false);

        return map;
    }

    private static void getSendMailScopes(Map<String, Boolean> map) {
        map.put("ZohoCRM.send_mail.all.CREATE", false);
        map.put("ZohoCRM.send_mail.leads.CREATE", false);
        map.put("ZohoCRM.send_mail.accounts.CREATE", false);
        map.put("ZohoCRM.send_mail.contacts.CREATE", false);
        map.put("ZohoCRM.send_mail.deals.CREATE", false);
        map.put("ZohoCRM.send_mail.quotes.CREATE", false);
        map.put("ZohoCRM.send_mail.salesorders.CREATE", false);
        map.put("ZohoCRM.send_mail.purchaseorders.CREATE", false);
        map.put("ZohoCRM.send_mail.invoices.CREATE", false);
        map.put("ZohoCRM.send_mail.cases.CREATE", false);
        map.put("ZohoCRM.send_mail.custom.CREATE", false);
    }

    private static void getMassDeleteScopes(Map<String, Boolean> map) {
        map.put("ZohoCRM.mass_delete.READ", false);
        map.put("ZohoCRM.mass_delete.DELETE", false);
        map.put("ZohoCRM.mass_delete.leads.READ", false);
        map.put("ZohoCRM.mass_delete.leads.DELETE", false);
        map.put("ZohoCRM.mass_delete.accounts.READ", false);
        map.put("ZohoCRM.mass_delete.accounts.DELETE", false);
        map.put("ZohoCRM.mass_delete.contacts.READ", false);
        map.put("ZohoCRM.mass_delete.contacts.DELETE", false);
        map.put("ZohoCRM.mass_delete.deals.READ", false);
        map.put("ZohoCRM.mass_delete.deals.DELETE", false);
        map.put("ZohoCRM.mass_delete.campaigns.READ", false);
        map.put("ZohoCRM.mass_delete.campaigns.DELETE", false);
        map.put("ZohoCRM.mass_delete.tasks.READ", false);
        map.put("ZohoCRM.mass_delete.tasks.DELETE", false);
        map.put("ZohoCRM.mass_delete.cases.READ", false);
        map.put("ZohoCRM.mass_delete.cases.DELETE", false);
        map.put("ZohoCRM.mass_delete.events.READ", false);
        map.put("ZohoCRM.mass_delete.events.DELETE", false);
        map.put("ZohoCRM.mass_delete.calls.READ", false);
        map.put("ZohoCRM.mass_delete.calls.DELETE", false);
        map.put("ZohoCRM.mass_delete.solutions.READ", false);
        map.put("ZohoCRM.mass_delete.solutions.DELETE", false);
        map.put("ZohoCRM.mass_delete.products.READ", false);
        map.put("ZohoCRM.mass_delete.products.DELETE", false);
        map.put("ZohoCRM.mass_delete.vendors.READ", false);
        map.put("ZohoCRM.mass_delete.vendors.DELETE", false);
        map.put("ZohoCRM.mass_delete.pricebooks.READ", false);
        map.put("ZohoCRM.mass_delete.pricebooks.DELETE", false);
        map.put("ZohoCRM.mass_delete.quotes.READ", false);
        map.put("ZohoCRM.mass_delete.quotes.DELETE", false);
        map.put("ZohoCRM.mass_delete.salesorders.READ", false);
        map.put("ZohoCRM.mass_delete.salesorders.DELETE", false);
        map.put("ZohoCRM.mass_delete.purchaseorders.READ", false);
        map.put("ZohoCRM.mass_delete.purchaseorders.DELETE", false);
        map.put("ZohoCRM.mass_delete.invoices.READ", false);
        map.put("ZohoCRM.mass_delete.invoices.DELETE", false);
        map.put("ZohoCRM.mass_delete.services.READ", false);
        map.put("ZohoCRM.mass_delete.services.DELETE", false);
        map.put("ZohoCRM.mass_delete.appointments.READ", false);
        map.put("ZohoCRM.mass_delete.appointments.DELETE", false);
        map.put("ZohoCRM.mass_delete.custom.READ", false);
        map.put("ZohoCRM.mass_delete.custom.DELETE", false);
    }

    private static void getMassUpdateScopes(Map<String, Boolean> map) {
        map.put("ZohoCRM.mass_update.READ", false);
        map.put("ZohoCRM.mass_update.UPDATE", false);
        map.put("ZohoCRM.mass_update.leads.READ", false);
        map.put("ZohoCRM.mass_update.leads.UPDATE", false);
        map.put("ZohoCRM.mass_update.accounts.READ", false);
        map.put("ZohoCRM.mass_update.accounts.UPDATE", false);
        map.put("ZohoCRM.mass_update.contacts.READ", false);
        map.put("ZohoCRM.mass_update.contacts.UPDATE", false);
        map.put("ZohoCRM.mass_update.deals.READ", false);
        map.put("ZohoCRM.mass_update.deals.UPDATE", false);
        map.put("ZohoCRM.mass_update.campaigns.READ", false);
        map.put("ZohoCRM.mass_update.campaigns.UPDATE", false);
        map.put("ZohoCRM.mass_update.calls.READ", false);
        map.put("ZohoCRM.mass_update.calls.UPDATE", false);
        map.put("ZohoCRM.mass_update.meetings.READ", false);
        map.put("ZohoCRM.mass_update.meetings.UPDATE", false);
        map.put("ZohoCRM.mass_update.tasks.READ", false);
        map.put("ZohoCRM.mass_update.tasks.UPDATE", false);
        map.put("ZohoCRM.mass_update.solutions.READ", false);
        map.put("ZohoCRM.mass_update.solutions.UPDATE", false);
        map.put("ZohoCRM.mass_update.products.READ", false);
        map.put("ZohoCRM.mass_update.products.UPDATE", false);
        map.put("ZohoCRM.mass_update.vendors.READ", false);
        map.put("ZohoCRM.mass_update.vendors.UPDATE", false);
        map.put("ZohoCRM.mass_update.pricebooks.READ", false);
        map.put("ZohoCRM.mass_update.pricebooks.UPDATE", false);
        map.put("ZohoCRM.mass_update.quotes.READ", false);
        map.put("ZohoCRM.mass_update.quotes.UPDATE", false);
        map.put("ZohoCRM.mass_update.salesorders.READ", false);
        map.put("ZohoCRM.mass_update.salesorders.UPDATE", false);
        map.put("ZohoCRM.mass_update.purchaseorders.READ", false);
        map.put("ZohoCRM.mass_update.purchaseorders.UPDATE", false);
        map.put("ZohoCRM.mass_update.invoices.READ", false);
        map.put("ZohoCRM.mass_update.invoices.UPDATE", false);
        map.put("ZohoCRM.mass_update.custom.READ", false);
        map.put("ZohoCRM.mass_update.custom.UPDATE", false);
    }

    private static void getMassConvertScopes(Map<String, Boolean> map) {
        map.put("ZohoCRM.mass_convert.CREATE", false);
        map.put("ZohoCRM.mass_convert.READ", false);
        map.put("ZohoCRM.mass_convert.leads.CREATE", false);
        map.put("ZohoCRM.mass_convert.leads.READ", false);
        map.put("ZohoCRM.mass_convert.quotes.CREATE", false);
        map.put("ZohoCRM.mass_convert.quotes.READ", false);
        map.put("ZohoCRM.mass_convert.salesorders.CREATE", false);
        map.put("ZohoCRM.mass_convert.salesorders.READ", false);
    }

    private static void getShareScopes(Map<String, Boolean> map) {
        map.put("ZohoCRM.share.ALL", false);
        map.put("ZohoCRM.share.CREATE", false);
        map.put("ZohoCRM.share.READ", false);
        map.put("ZohoCRM.share.UPDATE", false);
        map.put("ZohoCRM.share.DELETE", false);
        map.put("ZohoCRM.share.leads.ALL", false);
        map.put("ZohoCRM.share.leads.CREATE", false);
        map.put("ZohoCRM.share.leads.READ", false);
        map.put("ZohoCRM.share.leads.UPDATE", false);
        map.put("ZohoCRM.share.leads.DELETE", false);
        map.put("ZohoCRM.share.accounts.ALL", false);
        map.put("ZohoCRM.share.accounts.CREATE", false);
        map.put("ZohoCRM.share.accounts.READ", false);
        map.put("ZohoCRM.share.accounts.UPDATE", false);
        map.put("ZohoCRM.share.accounts.DELETE", false);
        map.put("ZohoCRM.share.contacts.ALL", false);
        map.put("ZohoCRM.share.contacts.CREATE", false);
        map.put("ZohoCRM.share.contacts.READ", false);
        map.put("ZohoCRM.share.contacts.UPDATE", false);
        map.put("ZohoCRM.share.contacts.DELETE", false);
        map.put("ZohoCRM.share.deals.ALL", false);
        map.put("ZohoCRM.share.deals.CREATE", false);
        map.put("ZohoCRM.share.deals.READ", false);
        map.put("ZohoCRM.share.deals.UPDATE", false);
        map.put("ZohoCRM.share.deals.DELETE", false);
        map.put("ZohoCRM.share.campaigns.ALL", false);
        map.put("ZohoCRM.share.campaigns.CREATE", false);
        map.put("ZohoCRM.share.campaigns.READ", false);
        map.put("ZohoCRM.share.campaigns.UPDATE", false);
        map.put("ZohoCRM.share.campaigns.DELETE", false);
        map.put("ZohoCRM.share.cases.ALL", false);
        map.put("ZohoCRM.share.cases.CREATE", false);
        map.put("ZohoCRM.share.cases.READ", false);
        map.put("ZohoCRM.share.cases.UPDATE", false);
        map.put("ZohoCRM.share.cases.DELETE", false);
        map.put("ZohoCRM.share.solutions.ALL", false);
        map.put("ZohoCRM.share.solutions.CREATE", false);
        map.put("ZohoCRM.share.solutions.READ", false);
        map.put("ZohoCRM.share.solutions.UPDATE", false);
        map.put("ZohoCRM.share.solutions.DELETE", false);
        map.put("ZohoCRM.share.products.ALL", false);
        map.put("ZohoCRM.share.products.CREATE", false);
        map.put("ZohoCRM.share.products.READ", false);
        map.put("ZohoCRM.share.products.UPDATE", false);
        map.put("ZohoCRM.share.products.DELETE", false);
        map.put("ZohoCRM.share.vendors.ALL", false);
        map.put("ZohoCRM.share.vendors.CREATE", false);
        map.put("ZohoCRM.share.vendors.READ", false);
        map.put("ZohoCRM.share.vendors.UPDATE", false);
        map.put("ZohoCRM.share.vendors.DELETE", false);
        map.put("ZohoCRM.share.pricebooks.ALL", false);
        map.put("ZohoCRM.share.pricebooks.CREATE", false);
        map.put("ZohoCRM.share.pricebooks.READ", false);
        map.put("ZohoCRM.share.pricebooks.UPDATE", false);
        map.put("ZohoCRM.share.pricebooks.DELETE", false);
        map.put("ZohoCRM.share.quotes.ALL", false);
        map.put("ZohoCRM.share.quotes.CREATE", false);
        map.put("ZohoCRM.share.quotes.READ", false);
        map.put("ZohoCRM.share.quotes.UPDATE", false);
        map.put("ZohoCRM.share.quotes.DELETE", false);
        map.put("ZohoCRM.share.salesorders.ALL", false);
        map.put("ZohoCRM.share.salesorders.CREATE", false);
        map.put("ZohoCRM.share.salesorders.READ", false);
        map.put("ZohoCRM.share.salesorders.UPDATE", false);
        map.put("ZohoCRM.share.salesorders.DELETE", false);
        map.put("ZohoCRM.share.purchaseorders.ALL", false);
        map.put("ZohoCRM.share.purchaseorders.CREATE", false);
        map.put("ZohoCRM.share.purchaseorders.READ", false);
        map.put("ZohoCRM.share.purchaseorders.UPDATE", false);
        map.put("ZohoCRM.share.purchaseorders.DELETE", false);
        map.put("ZohoCRM.share.invoices.ALL", false);
        map.put("ZohoCRM.share.invoices.CREATE", false);
        map.put("ZohoCRM.share.invoices.READ", false);
        map.put("ZohoCRM.share.invoices.UPDATE", false);
        map.put("ZohoCRM.share.invoices.DELETE", false);
        map.put("ZohoCRM.share.custom.ALL", false);
        map.put("ZohoCRM.share.custom.CREATE", false);
        map.put("ZohoCRM.share.custom.READ", false);
        map.put("ZohoCRM.share.custom.UPDATE", false);
        map.put("ZohoCRM.share.custom.DELETE", false);
    }

    private static void getSettingsScopes(Map<String, Boolean> map) {
        map.put("ZohoCRM.settings.workflow_rules.ALL", false);
        map.put("ZohoCRM.settings.workflow_rules.CREATE", false);
        map.put("ZohoCRM.settings.workflow_rules.READ", false);
        map.put("ZohoCRM.settings.workflow_rules.UPDATE", false);
        map.put("ZohoCRM.settings.workflow_rules.DELETE", false);
        map.put("ZohoCRM.settings.automation_actions.ALL", false);
        map.put("ZohoCRM.settings.automation_actions.CREATE", false);
        map.put("ZohoCRM.settings.automation_actions.READ", false);
        map.put("ZohoCRM.settings.automation_actions.UPDATE", false);
        map.put("ZohoCRM.settings.automation_actions.DELETE", false);
        map.put("ZohoCRM.settings.data_sharing.ALL", false);
        map.put("ZohoCRM.settings.data_sharing.CREATE", false);
        map.put("ZohoCRM.settings.data_sharing.READ", false);
        map.put("ZohoCRM.settings.data_sharing.UPDATE", false);
        map.put("ZohoCRM.settings.data_sharing.DELETE", false);
        map.put("ZohoCRM.settings.duplicate_check_preference.ALL", false);
        map.put("ZohoCRM.settings.duplicate_check_preference.CREATE", false);
        map.put("ZohoCRM.settings.duplicate_check_preference.READ", false);
        map.put("ZohoCRM.settings.duplicate_check_preference.UPDATE", false);
        map.put("ZohoCRM.settings.duplicate_check_preference.DELETE", false);
        map.put("ZohoCRM.settings.intelligence.ALL", false);
        map.put("ZohoCRM.settings.intelligence.CREATE", false);
        map.put("ZohoCRM.settings.intelligence.READ", false);
        map.put("ZohoCRM.settings.cadences.ALL", false);
        map.put("ZohoCRM.settings.cadences.READ", false);
        map.put("ZohoCRM.settings.fields.ALL", false);
        map.put("ZohoCRM.settings.fields.CREATE", false);
        map.put("ZohoCRM.settings.fields.READ", false);
        map.put("ZohoCRM.settings.fields.UPDATE", false);
        map.put("ZohoCRM.settings.fields.DELETE", false);
        map.put("ZohoCRM.settings.layouts.ALL", false);
        map.put("ZohoCRM.settings.layouts.READ", false);
        map.put("ZohoCRM.settings.layouts.UPDATE", false);
        map.put("ZohoCRM.settings.layouts.POST", false);
        map.put("ZohoCRM.settings.layouts.DELETE", false);
        map.put("ZohoCRM.settings.module.ALL", false);
        map.put("ZohoCRM.settings.module.CREATE", false);
        map.put("ZohoCRM.settings.module.READ", false);
        map.put("ZohoCRM.settings.module.UPDATE", false);
        map.put("ZohoCRM.settings.related_lists.ALL", false);
        map.put("ZohoCRM.settings.related_lists.READ", false);
        map.put("ZohoCRM.settings.profiles.ALL", false);
        map.put("ZohoCRM.settings.profiles.CREATE", false);
        map.put("ZohoCRM.settings.profiles.READ", true);
        map.put("ZohoCRM.settings.profiles.UPDATE", false);
        map.put("ZohoCRM.settings.profiles.DELETE", false);
        map.put("ZohoCRM.settings.custom_views.ALL", false);
        map.put("ZohoCRM.settings.custom_views.READ", false);
        map.put("ZohoCRM.settings.custom_views.UPDATE", false);
        map.put("ZohoCRM.settings.roles.ALL", false);
        map.put("ZohoCRM.settings.roles.CREATE", false);
        map.put("ZohoCRM.settings.roles.READ", true);
        map.put("ZohoCRM.settings.roles.UPDATE", false);
        map.put("ZohoCRM.settings.roles.DELETE", false);
        map.put("ZohoCRM.settings.territories.ALL", false);
        map.put("ZohoCRM.settings.territories.CREATE", false);
        map.put("ZohoCRM.settings.territories.READ", false);
        map.put("ZohoCRM.settings.territories.UPDATE", false);
        map.put("ZohoCRM.settings.territories.DELETE", false);
        map.put("ZohoCRM.settings.scoring_rules.ALL", false);
        map.put("ZohoCRM.settings.scoring_rules.CREATE", false);
        map.put("ZohoCRM.settings.scoring_rules.READ", false);
        map.put("ZohoCRM.settings.scoring_rules.UPDATE", false);
        map.put("ZohoCRM.settings.scoring_rules.DELETE", false);
        map.put("ZohoCRM.settings.signals.ALL", false);
        map.put("ZohoCRM.settings.signals.READ", false);
        map.put("ZohoCRM.settings.variables.ALL", false);
        map.put("ZohoCRM.settings.variables.CREATE", false);
        map.put("ZohoCRM.settings.variables.READ", false);
        map.put("ZohoCRM.settings.variables.UPDATE", false);
        map.put("ZohoCRM.settings.variables.DELETE", false);
        map.put("ZohoCRM.settings.variable_groups.READ", false);
        map.put("ZohoCRM.settings.variable_groups.ALL", false);
        map.put("ZohoCRM.settings.map_dependency.ALL", false);
        map.put("ZohoCRM.settings.map_dependency.CREATE", false);
        map.put("ZohoCRM.settings.map_dependency.READ", false);
        map.put("ZohoCRM.settings.map_dependency.UPDATE", false);
        map.put("ZohoCRM.settings.map_dependency.DELETE", false);
        map.put("ZohoCRM.settings.user_groups.ALL", false);
        map.put("ZohoCRM.settings.user_groups.CREATE", false);
        map.put("ZohoCRM.settings.user_groups.READ", false);
        map.put("ZohoCRM.settings.user_groups.UPDATE", false);
        map.put("ZohoCRM.settings.user_groups.DELETE", false);
        map.put("ZohoCRM.settings.tags.ALL", false);
        map.put("ZohoCRM.settings.tags.CREATE", false);
        map.put("ZohoCRM.settings.tags.READ", false);
        map.put("ZohoCRM.settings.tags.UPDATE", false);
        map.put("ZohoCRM.settings.tags.DELETE", false);
        map.put("ZohoCRM.settings.pipeline.ALL", false);
        map.put("ZohoCRM.settings.pipeline.CREATE", false);
        map.put("ZohoCRM.settings.pipeline.READ", false);
        map.put("ZohoCRM.settings.pipeline.UPDATE", false);
        map.put("ZohoCRM.settings.wizards.READ", false);
        map.put("ZohoCRM.settings.assignment_rules.READ", false);
        map.put("ZohoCRM.settings.users_unavailability.ALL", false);
        map.put("ZohoCRM.settings.users_unavailability.CREATE", false);
        map.put("ZohoCRM.settings.users_unavailability.READ", false);
        map.put("ZohoCRM.settings.users_unavailability.UPDATE", false);
        map.put("ZohoCRM.settings.users_unavailability.DELETE", false);
        map.put("ZohoCRM.settings.clientportal.ALL", false);
        map.put("ZohoCRM.settings.clientportal.CREATE", false);
        map.put("ZohoCRM.settings.clientportal.READ", false);
        map.put("ZohoCRM.settings.clientportal.UPDATE", false);
        map.put("ZohoCRM.settings.clientportal.DELETE", false);
        map.put("ZohoCRM.settings.fiscal_year.READ", false);
        map.put("ZohoCRM.settings.fiscal_year.UPDATE", false);
        map.put("ZohoCRM.settings.record_locking_configurations.ALL", false);
        map.put("ZohoCRM.settings.record_locking_configurations.CREATE", false);
        map.put("ZohoCRM.settings.record_locking_configurations.READ", false);
        map.put("ZohoCRM.settings.record_locking_configurations.UPDATE", false);
        map.put("ZohoCRM.settings.record_locking_configurations.DELETE", false);
        map.put("ZohoCRM.settings.business_hours.ALL", false);
        map.put("ZohoCRM.settings.business_hours.CREATE", false);
        map.put("ZohoCRM.settings.business_hours.READ", false);
        map.put("ZohoCRM.settings.business_hours.UPDATE", false);
        map.put("ZohoCRM.settings.business_hours.DELETE", false);
        map.put("ZohoCRM.settings.currencies.CREATE", false);
        map.put("ZohoCRM.settings.currencies.READ", false);
        map.put("ZohoCRM.settings.currencies.UPDATE", false);
        map.put("ZohoCRM.settings.emails.READ", false);
        map.put("ZohoCRM.settings.mailmerge.CREATE", false);
        map.put("ZohoCRM.settings.unsubscribe.ALL", false);
        map.put("ZohoCRM.settings.unsubscribe.CREATE", false);
        map.put("ZohoCRM.settings.unsubscribe.READ", false);
        map.put("ZohoCRM.settings.unsubscribe.UPDATE", false);
        map.put("ZohoCRM.settings.unsubscribe.DELETE", false);
        map.put("ZohoCRM.settings.global_picklist.ALL", false);
        map.put("ZohoCRM.settings.global_picklist.CREATE", false);
        map.put("ZohoCRM.settings.global_picklist.READ", false);
        map.put("ZohoCRM.settings.global_picklist.UPDATE", false);
        map.put("ZohoCRM.settings.global_picklist.DELETE", false);
        map.put("ZohoCRM.settings.recycle_bin.READ", false);
        map.put("ZohoCRM.settings.recycle_bin.DELETE", false);
    }

    private ZohoCrmConnection() {
    }
}
