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

package com.bytechef.component.date.helper.constants;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;

/**
 * @author Igor Beslic
 * @author Monika Kušter
 * @author Nikolina Spehar
 */
public class DateHelperConstants {

    public static final String BUSINESS_HOURS_START = "businessHoursStart";
    public static final String BUSINESS_HOURS_END = "businessHoursEnd";
    public static final String BUSINESS_WEEK_START = "businessWeekStart";
    public static final String BUSINESS_WEEK_END = "businessWeekEnd";
    public static final String COMPARISON = "comparison";
    public static final String DATE = "date";
    public static final String DATE_A = "dateA";
    public static final String DATE_B = "dateB";
    public static final String DATE_FORMAT = "dateFormat";
    public static final String DATE_TIMESTAMP = "dateTimestamp";
    public static final String DAY = "day";
    public static final String DAY_OF_WEEK = "dayOfWeek";
    public static final String DURATION = "duration";
    public static final String END_DATE = "endDate";
    public static final String HOUR = "hour";
    public static final String INCLUSIVE = "inclusive";
    public static final String INCLUSIVE_SECONDS = "inclusiveSeconds";
    public static final String INPUT_DATE = "inputDate";
    public static final String IN_LAST = "inLast";
    public static final String MINUTE = "minute";
    public static final String MONTH = "month";
    public static final String MONTH_NAME = "monthName";
    public static final String RESOLUTION = "resolution";
    public static final String SECOND = "second";
    public static final String START_DATE = "startDate";
    public static final String TIME = "time";
    public static final String TIME_A = "timeA";
    public static final String TIME_B = "timeB";
    public static final String TIME_ZONE = "timeZone";
    public static final String UNIT = "unit";
    public static final String UNIX_TIMESTAMP = "UnixTimestamp";
    public static final String YEAR = "year";

    public static final ModifiableStringProperty DATE_FORMAT_PROPERTY = string(DATE_FORMAT)
        .label("Date Format")
        .description("Here's what each part of the format (eg. YYYY) means: " +
            "yyyy : Year (4 digits) " +
            "yy : Year (2 digits) " +
            "MMMM : Month (full name) " +
            "MMM : Month (short name) " +
            "MM : Month (2 digits) " +
            "EEE : Day (short name) " +
            "dd : Day (2 digits) " +
            "HH : Hour (2 digits) " +
            "mm : Minute (2 digits) " +
            "ss : Second (2 digits).")
        .options(
            option("EEE MMM dd yyyy HH:mm:ss", "EEE MMM dd yyyy HH:mm:ss", "Sun Sep 17 2023 11:23:58"),
            option("EEE MMM dd HH:mm:ss yyyy", "EEE MMM dd HH:mm:ss yyyy", "Sun Sep 17 11:23:58 2023"),
            option("MMMM dd yyyy HH:mm:ss", "MMMM dd yyyy HH:mm:ss", "September 17 2023 11:23:58"),
            option("MMMM dd yyyy", "MMMM dd yyyy", "September 17 2023"),
            option("MMM dd yyyy", "MMM dd yyyy", "Sep 17 2023"),
            option("yyyy-MM-ddTHH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "2023-09-17T11:23:58"),
            option("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "2023-09-17 11:23:58"),
            option("yyyy-MM-dd", "yyyy-MM-dd", "2023-09-17"),
            option("MM-dd-yyyy", "MM-dd-yyyy", "09-17-2023"),
            option("MM/dd/yyyy", "MM/dd/yyyy", "09/17/2023"),
            option("MM/dd/yy", "MM/dd/yy", "09/17/23"),
            option("dd-MM-yyyy", "dd-MM-yyyy", "17-09-2023"),
            option("dd/MM/yyyy", "dd/MM/yyyy", "17/09/2023"),
            option("dd.MM.yyyy", "dd.MM.yyyy", "17.09.2023"),
            option("dd/MM/yy", "dd/MM/yy", "17/09/23"),
            option("dd.MM.yy", "dd.MM.yy", "17.09.23"),
            option("Time in unix format", UNIX_TIMESTAMP, "1694949838"))
        .required(true);

    public static final ModifiableStringProperty RESOLUTION_PROPERTY = string(RESOLUTION)
        .label("Resolution")
        .description(
            "The resolution at which the dates will be compared. Chosen resolution will be the smallest time unit " +
                "that will be compared.")
        .options(
            option("Year", YEAR),
            option("Month", MONTH),
            option("Day", DAY),
            option("Hour", HOUR),
            option("Minute", MINUTE),
            option("Second", SECOND))
        .required(true);

    public static final ModifiableStringProperty UNIT_PROPERTY = string(UNIT)
        .label("Unit")
        .description("The unit of difference between the two dates.")
        .options(
            option("Year", YEAR),
            option("Month", MONTH),
            option("Day", DAY),
            option("Hour", HOUR),
            option("Minute", MINUTE),
            option("Second", SECOND))
        .required(true);

    private DateHelperConstants() {
    }
}
