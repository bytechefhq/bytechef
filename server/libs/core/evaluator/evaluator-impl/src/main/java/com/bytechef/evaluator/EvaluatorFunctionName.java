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

package com.bytechef.evaluator;

/**
 * Constants for evaluator function names as used in workflow expressions.
 *
 * @author Ivica Cardic
 */
final class EvaluatorFunctionName {

    static final String ADD = "add";
    static final String ADD_ALL = "addAll";
    static final String AT_ZONE = "atZone";
    static final String BOOLEAN = "boolean";
    static final String BYTE = "byte";
    static final String CHAR = "char";
    static final String CONCAT = "concat";
    static final String CONFIG = "config";
    static final String CONTAINS = "contains";
    static final String DOUBLE = "double";
    static final String EQUALS_IGNORE_CASE = "equalsIgnoreCase";
    static final String FLATTEN = "flatten";
    static final String FLOAT = "float";
    static final String FORMAT = "format";
    static final String INDEX_OF = "indexOf";
    static final String INT = "int";
    static final String JOIN = "join";
    static final String LAST_INDEX_OF = "lastIndexOf";
    static final String LENGTH = "length";
    static final String LONG = "long";
    static final String MINUS_DAYS = "minusDays";
    static final String MINUS_HOURS = "minusHours";
    static final String MINUS_MICROS = "minusMicros";
    static final String MINUS_MILLIS = "minusMillis";
    static final String MINUS_MINUTES = "minusMinutes";
    static final String MINUS_MONTHS = "minusMonths";
    static final String MINUS_SECONDS = "minusSeconds";
    static final String MINUS_WEEKS = "minusWeeks";
    static final String MINUS_YEARS = "minusYears";
    static final String NOW = "now";
    static final String PARSE_DATE = "parseDate";
    static final String PARSE_DATE_TIME = "parseDateTime";
    static final String PLUS_DAYS = "plusDays";
    static final String PLUS_HOURS = "plusHours";
    static final String PLUS_MICROS = "plusMicros";
    static final String PLUS_MILLIS = "plusMillis";
    static final String PLUS_MINUTES = "plusMinutes";
    static final String PLUS_MONTHS = "plusMonths";
    static final String PLUS_SECONDS = "plusSeconds";
    static final String PLUS_WEEKS = "plusWeeks";
    static final String PLUS_YEARS = "plusYears";
    static final String PUT = "put";
    static final String PUT_ALL = "putAll";
    static final String RANGE = "range";
    static final String REMOVE = "remove";
    static final String SET = "set";
    static final String SHORT = "short";
    static final String SIZE = "size";
    static final String SORT = "sort";
    static final String SPLIT = "split";
    static final String SUBSTRING = "substring";
    static final String TIMESTAMP = "timestamp";
    static final String TO_MAP = "toMap";
    static final String UUID = "uuid";

    private EvaluatorFunctionName() {
    }
}
