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

package com.bytechef.component.date.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.date.helper.action.DateHelperAddTimeAction;
import com.bytechef.component.date.helper.action.DateHelperCompareDatesAction;
import com.bytechef.component.date.helper.action.DateHelperCompareTimesAction;
import com.bytechef.component.date.helper.action.DateHelperConvertAction;
import com.bytechef.component.date.helper.action.DateHelperDateDifferenceAction;
import com.bytechef.component.date.helper.action.DateHelperDateIsInLastAction;
import com.bytechef.component.date.helper.action.DateHelperExtractDateUnitsAction;
import com.bytechef.component.date.helper.action.DateHelperFormatDateAction;
import com.bytechef.component.date.helper.action.DateHelperGetCurrentDateAction;
import com.bytechef.component.date.helper.action.DateHelperGetDaysBetweenAction;
import com.bytechef.component.date.helper.action.DateHelperGetDurationAction;
import com.bytechef.component.date.helper.action.DateHelperGetTimeBetweenAction;
import com.bytechef.component.date.helper.action.DateHelperGetTimeFromNowAction;
import com.bytechef.component.date.helper.action.DateHelperIsBetweenDatesAction;
import com.bytechef.component.date.helper.action.DateHelperIsBetweenTimesAction;
import com.bytechef.component.date.helper.action.DateHelperIsBusinessHoursAction;
import com.bytechef.component.date.helper.action.DateHelperIsWeekendAction;
import com.bytechef.component.date.helper.action.DateHelperSubtractTimeAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Igor Beslic
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class DateHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("dateHelper")
        .title("Date Helper")
        .description("Helper component for date and time manipulation.")
        .icon("path:assets/date-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            DateHelperAddTimeAction.ACTION_DEFINITION,
            DateHelperCompareDatesAction.ACTION_DEFINITION,
            DateHelperCompareTimesAction.ACTION_DEFINITION,
            DateHelperConvertAction.ACTION_DEFINITION,
            DateHelperDateDifferenceAction.ACTION_DEFINITION,
            DateHelperDateIsInLastAction.ACTION_DEFINITION,
            DateHelperExtractDateUnitsAction.ACTION_DEFINITION,
            DateHelperFormatDateAction.ACTION_DEFINITION,
            DateHelperGetCurrentDateAction.ACTION_DEFINITION,
            DateHelperGetDaysBetweenAction.ACTION_DEFINITION,
            DateHelperGetDurationAction.ACTION_DEFINITION,
            DateHelperGetTimeBetweenAction.ACTION_DEFINITION,
            DateHelperGetTimeFromNowAction.ACTION_DEFINITION,
            DateHelperIsBetweenDatesAction.ACTION_DEFINITION,
            DateHelperIsBetweenTimesAction.ACTION_DEFINITION,
            DateHelperIsBusinessHoursAction.ACTION_DEFINITION,
            DateHelperIsWeekendAction.ACTION_DEFINITION,
            DateHelperSubtractTimeAction.ACTION_DEFINITION)
        .clusterElements(
            tool(DateHelperAddTimeAction.ACTION_DEFINITION),
            tool(DateHelperCompareDatesAction.ACTION_DEFINITION),
            tool(DateHelperCompareTimesAction.ACTION_DEFINITION),
            tool(DateHelperConvertAction.ACTION_DEFINITION),
            tool(DateHelperDateDifferenceAction.ACTION_DEFINITION),
            tool(DateHelperDateIsInLastAction.ACTION_DEFINITION),
            tool(DateHelperExtractDateUnitsAction.ACTION_DEFINITION),
            tool(DateHelperFormatDateAction.ACTION_DEFINITION),
            tool(DateHelperGetCurrentDateAction.ACTION_DEFINITION),
            tool(DateHelperGetDaysBetweenAction.ACTION_DEFINITION),
            tool(DateHelperGetDurationAction.ACTION_DEFINITION),
            tool(DateHelperGetTimeBetweenAction.ACTION_DEFINITION),
            tool(DateHelperGetTimeFromNowAction.ACTION_DEFINITION),
            tool(DateHelperIsBetweenDatesAction.ACTION_DEFINITION),
            tool(DateHelperIsBetweenTimesAction.ACTION_DEFINITION),
            tool(DateHelperIsBusinessHoursAction.ACTION_DEFINITION),
            tool(DateHelperIsWeekendAction.ACTION_DEFINITION),
            tool(DateHelperSubtractTimeAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
