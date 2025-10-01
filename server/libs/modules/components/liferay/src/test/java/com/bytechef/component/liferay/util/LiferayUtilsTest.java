package com.bytechef.component.liferay.util;

import com.bytechef.component.definition.Option;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Nikolina Spehar
 */
class LiferayUtilsTest {

    @Test
    void testGetContextNameOptions() {
        List<Option<String>> result = LiferayUtils.getContextNameOptions();

        assertEquals(getExpectedContextNameOptions(), result);
    }

    @Test
    void testGetServiceOptions() {

    }

    @Test
    void testGetContextServices() {
    }

    @Test
    void testCreateParameters() {
    }

    @Test
    void testGetServiceHttpData() {
    }

    private static List<Option<String>> getExpectedContextNameOptions() {
        List<Option<String>> expectedContextNameOptions = new ArrayList<>();

        expectedContextNameOptions.add(option("PORTAL", "portal"));
        expectedContextNameOptions.add(option("ACCOUNT", "account"));
        expectedContextNameOptions.add(option("ASSET", "asset"));
        expectedContextNameOptions.add(option("ASSETLIST", "assetlist"));
        expectedContextNameOptions.add(option("AUDIT", "audit"));
        expectedContextNameOptions.add(option("BACKGROUNDTASK", "backgroundtask"));
        expectedContextNameOptions.add(option("BATCHENGINE", "batchengine"));
        expectedContextNameOptions.add(option("BLOGS", "blogs"));
        expectedContextNameOptions.add(option("CALENDAR", "calendar"));
        expectedContextNameOptions.add(option("COMMENT", "comment"));
        expectedContextNameOptions.add(option("COMMERCE", "commerce"));
        expectedContextNameOptions.add(option("CONTACT", "contact"));
        expectedContextNameOptions.add(option("CT", "ct"));
        expectedContextNameOptions.add(option("DDL", "ddl"));
        expectedContextNameOptions.add(option("DDM", "ddm"));
        expectedContextNameOptions.add(option("DEPOT", "depot"));
        expectedContextNameOptions.add(option("DISPATCH", "dispatch"));
        expectedContextNameOptions.add(option("FRAGMENT", "fragment"));
        expectedContextNameOptions.add(option("JOURNAL", "journal"));
        expectedContextNameOptions.add(option("KALEO", "kaleo"));
        expectedContextNameOptions.add(option("KALEOFORMS", "kaleoforms"));
        expectedContextNameOptions.add(option("KB", "kb"));
        expectedContextNameOptions.add(option("LAYOUT", "layout"));
        expectedContextNameOptions.add(option("LAYOUTUTILITYPAGE", "layoututilitypage"));
        expectedContextNameOptions.add(option("LISTTYPE", "listtype"));
        expectedContextNameOptions.add(option("MARKETPLACE", "marketplace"));
        expectedContextNameOptions.add(option("MB", "mb"));
        expectedContextNameOptions.add(option("NOTIFICATION", "notification"));
        expectedContextNameOptions.add(option("OAUTHCLIENT", "oauthclient"));
        expectedContextNameOptions.add(option("OBJECT", "object"));
        expectedContextNameOptions.add(option("PORTALLANGUAGEOVERRRIDE", "portallanguageoverride"));
        expectedContextNameOptions.add(option("REDIRECT", "redirect"));
        expectedContextNameOptions.add(option("REMOTEAPP", "remoteapp"));
        expectedContextNameOptions.add(option("SAP", "sap"));
        expectedContextNameOptions.add(option("SAVEDCONTENTENTRY", "savedcontententry"));
        expectedContextNameOptions.add(option("SEGMENTS", "segments"));
        expectedContextNameOptions.add(option("SHARING", "sharing"));
        expectedContextNameOptions.add(option("SITENAVIGATION", "sitenavigation"));
        expectedContextNameOptions.add(option("STYLEBOOK", "stylebook"));
        expectedContextNameOptions.add(option("SXP", "sxp"));
        expectedContextNameOptions.add(option("TRANSLATION", "translation"));
        expectedContextNameOptions.add(option("TRASH", "trash"));
        expectedContextNameOptions.add(option("WIKI", "wiki"));

        return expectedContextNameOptions;
    }
}
