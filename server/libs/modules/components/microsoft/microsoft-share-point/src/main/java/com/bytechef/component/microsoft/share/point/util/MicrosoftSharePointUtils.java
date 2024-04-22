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

package com.bytechef.component.microsoft.share.point.util;

import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.share.point.constant.ColumnType.BOOLEAN;
import static com.bytechef.component.microsoft.share.point.constant.ColumnType.CHOICE;
import static com.bytechef.component.microsoft.share.point.constant.ColumnType.CURRENCY;
import static com.bytechef.component.microsoft.share.point.constant.ColumnType.DATE_TIME;
import static com.bytechef.component.microsoft.share.point.constant.ColumnType.NUMBER;
import static com.bytechef.component.microsoft.share.point.constant.ColumnType.TEXT;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.BASE_URL;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DESCRIPTION;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.LIST_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.NAME;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.PARENT_FOLDER;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.READ_ONLY;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.REQUIRED;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableValueProperty;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.microsoft.share.point.constant.ColumnType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Monika Domiter
 */
public class MicrosoftSharePointUtils {

    private MicrosoftSharePointUtils() {
    }

    public static List<? extends ValueProperty<?>> createPropertiesForListItem(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get(BASE_URL + "/" + inputParameters.getRequiredString(SITE_ID) + "/lists/"
                + inputParameters.getRequiredString(LIST_ID) + "/columns"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<ValueProperty<?>> properties = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map && (!(boolean) map.get(READ_ONLY))) {
                    ModifiableValueProperty<?, ?> property = createProperty(map);

                    if (property != null) {
                        properties.add(property);
                    }
                }
            }
        }
        return properties;
    }

    private static ModifiableValueProperty<?, ?> createProperty(Map<?, ?> map) {
        ColumnType propertyType = getPropertyType(map);

        return propertyType != null ? switch (propertyType) {
            case BOOLEAN -> createBooleanProperty(map);
            case CHOICE -> createChoiceProperty(map);
            case NUMBER, CURRENCY -> createNumberProperty(map);
            case DATE_TIME -> createDateTimeProperty(map);
            case TEXT -> createTextProperty(map);
        } : null;

    }

    private static ColumnType getPropertyType(Map<?, ?> map) {
        if (map.get(BOOLEAN.toString()) != null) {
            return BOOLEAN;
        } else if (map.get(CHOICE.toString()) != null) {
            return CHOICE;
        } else if (map.get(NUMBER.toString()) != null) {
            return NUMBER;
        } else if (map.get(CURRENCY.toString()) != null) {
            return CURRENCY;
        } else if (map.get(DATE_TIME.toString()) != null) {
            return DATE_TIME;
        } else if (map.get(TEXT.toString()) != null) {
            return TEXT;
        }
        return null;
    }

    private static ModifiableValueProperty<?, ?> createBooleanProperty(Map<?, ?> map) {
        return bool((String) map.get(NAME))
            .label((String) map.get(DISPLAY_NAME))
            .description((String) map.get(DESCRIPTION))
            .required((Boolean) map.get(REQUIRED));
    }

    private static ModifiableValueProperty<?, ?> createChoiceProperty(Map<?, ?> map) {
        ModifiableStringProperty choice = string((String) map.get(NAME))
            .label((String) map.get(DISPLAY_NAME))
            .description((String) map.get(DESCRIPTION))
            .required((Boolean) map.get(REQUIRED));

        List<Option<String>> options = new ArrayList<>();

        if (map.get("choice") instanceof Map<?, ?> choiceMap
            && choiceMap.get("choices") instanceof List<?> choiceList) {
            for (Object object : choiceList) {
                options.add(option((String) object, (String) object));
            }
        }

        return choice.options(options);
    }

    private static ModifiableValueProperty<?, ?> createNumberProperty(Map<?, ?> map) {
        return number((String) map.get(NAME))
            .label((String) map.get(DISPLAY_NAME))
            .description((String) map.get(DESCRIPTION))
            .required((Boolean) map.get(REQUIRED));
    }

    private static ModifiableValueProperty<?, ?> createDateTimeProperty(Map<?, ?> map) {
        String displayName = (String) map.get(DISPLAY_NAME);
        String name = (String) map.get(NAME);
        String description = (String) map.get(DESCRIPTION);
        Boolean required = (Boolean) map.get(REQUIRED);

        if (map.get("dateTime") instanceof Map<?, ?> dateTimeMap) {
            if (Objects.equals(dateTimeMap.get("format"), "dateOnly")) {
                return date(name)
                    .label(displayName)
                    .description(description)
                    .required(required);
            } else {
                return dateTime(name)
                    .description(description)
                    .label(displayName)
                    .required(required);
            }
        }
        return null;
    }

    private static ModifiableValueProperty<?, ?> createTextProperty(Map<?, ?> map) {
        ModifiableStringProperty textProperty = string((String) map.get(NAME))
            .description((String) map.get(DESCRIPTION))
            .label((String) map.get(DISPLAY_NAME))
            .required((Boolean) map.get(REQUIRED));

        if (map.get("text") instanceof Map<?, ?> textMap) {
            if ((boolean) textMap.get("allowMultipleLines")) {
                textProperty.controlType(Property.ControlType.TEXT_AREA);
            } else {
                textProperty.maxLength((Integer) textMap.get("maxLength"));
            }
        }

        return textProperty;
    }

    public static String getFolderId(Parameters inputParameters) {
        String parentId = inputParameters.getString(PARENT_FOLDER);

        return (parentId == null) ? "root" : parentId;
    }

    public static List<Option<String>> getFolderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        String encode = URLEncoder.encode("folder ne null", StandardCharsets.UTF_8);

        Map<String, ?> body = context
            .http(http -> http.get(BASE_URL + "/" + inputParameters.getRequiredString(SITE_ID)
                + "/drive/items/root/children?$filter=" + encode))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("value") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get(NAME), (String) map.get(ID)));
                }
            }
        }

        return options;
    }

    public static List<Option<String>> getListIdOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, ?> body =
            context.http(http -> http.get(BASE_URL + "/" + inputParameters.getRequiredString(SITE_ID) + "/lists"))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        return getOptions(body, DISPLAY_NAME);
    }

    public static List<Option<String>> getSiteOptions(
        Parameters inputParameters, Parameters connectionParameters, Map<String, String> dependencyPaths,
        String searchText, ActionContext context) {

        Map<String, ?> body = context.http(http -> http.get(BASE_URL + "?search=*&select=displayName,id,name"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return getOptions(body, NAME);
    }

    private static List<Option<String>> getOptions(Map<String, ?> body, String label) {
        List<Option<String>> options = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    String name = (String) map.get(label);
                    String id = (String) map.get(ID);

                    if (name != null && id != null) {
                        options.add(option(name, id));
                    }
                }
            }
        }

        return options;
    }

}
