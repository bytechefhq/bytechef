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

import static com.bytechef.evaluator.EvaluatorFunctionDsl.function;
import static com.bytechef.evaluator.EvaluatorFunctionDsl.parameter;
import static com.bytechef.evaluator.EvaluatorFunctionType.BOOLEAN;
import static com.bytechef.evaluator.EvaluatorFunctionType.BYTE;
import static com.bytechef.evaluator.EvaluatorFunctionType.CHARACTER;
import static com.bytechef.evaluator.EvaluatorFunctionType.DATETIME;
import static com.bytechef.evaluator.EvaluatorFunctionType.DOUBLE;
import static com.bytechef.evaluator.EvaluatorFunctionType.FLOAT;
import static com.bytechef.evaluator.EvaluatorFunctionType.INTEGER;
import static com.bytechef.evaluator.EvaluatorFunctionType.LIST;
import static com.bytechef.evaluator.EvaluatorFunctionType.LONG;
import static com.bytechef.evaluator.EvaluatorFunctionType.MAP;
import static com.bytechef.evaluator.EvaluatorFunctionType.NUMBER;
import static com.bytechef.evaluator.EvaluatorFunctionType.SHORT;
import static com.bytechef.evaluator.EvaluatorFunctionType.STRING;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Provides definitions for all built-in evaluator functions registered in {@link SpelEvaluator}.
 *
 * @author Ivica Cardic
 */
@Component
class EvaluatorFunctionDefinitions implements EvaluatorFunctionDefinitionFactory {

    // Collection functions

    static final EvaluatorFunctionDefinition ADD_BOOLEAN = function(EvaluatorFunctionName.ADD)
        .title("Add")
        .description("Adds a boolean element to a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to add to")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The boolean element to add")
                .type(BOOLEAN)
                .required(true))
        .returnType(LIST)
        .example("=add(${list}, true)")
        .toDefinition();

    static final EvaluatorFunctionDefinition ADD_DATETIME = function(EvaluatorFunctionName.ADD)
        .title("Add")
        .description("Adds a datetime element to a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to add to")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The datetime element to add")
                .type(DATETIME)
                .required(true))
        .returnType(LIST)
        .example("=add(${list}, now())")
        .toDefinition();

    static final EvaluatorFunctionDefinition ADD_LIST = function(EvaluatorFunctionName.ADD)
        .title("Add")
        .description("Adds a list element to a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to add to")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The list element to add")
                .type(LIST)
                .required(true))
        .returnType(LIST)
        .example("=add(${list}, ${nestedList})")
        .toDefinition();

    static final EvaluatorFunctionDefinition ADD_MAP = function(EvaluatorFunctionName.ADD)
        .title("Add")
        .description("Adds a map element to a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to add to")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The map element to add")
                .type(MAP)
                .required(true))
        .returnType(LIST)
        .example("=add(${list}, ${map})")
        .toDefinition();

    static final EvaluatorFunctionDefinition ADD_NUMBER = function(EvaluatorFunctionName.ADD)
        .title("Add")
        .description("Adds a number element to a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to add to")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The number element to add")
                .type(NUMBER)
                .required(true))
        .returnType(LIST)
        .example("=add(${list}, 42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition ADD_STRING = function(EvaluatorFunctionName.ADD)
        .title("Add")
        .description("Adds a string element to a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to add to")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The string element to add")
                .type(STRING)
                .required(true))
        .returnType(LIST)
        .example("=add(${list}, 'newItem')")
        .toDefinition();

    static final EvaluatorFunctionDefinition ADD_ALL = function(EvaluatorFunctionName.ADD_ALL)
        .title("Add All")
        .description("Adds all elements from one collection to another list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The target list")
                .type(LIST)
                .required(true),
            parameter("elements")
                .description("The collection of elements to add")
                .type(LIST)
                .required(true))
        .returnType(LIST)
        .example("=addAll(${list1}, ${list2})")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONTAINS_LIST_BOOLEAN = function(EvaluatorFunctionName.CONTAINS)
        .title("Contains")
        .description("Checks whether a list contains a boolean element.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to search in")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The boolean element to search for")
                .type(BOOLEAN)
                .required(true))
        .returnType(BOOLEAN)
        .example("=contains(${list}, true)")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONTAINS_LIST_DATETIME = function(EvaluatorFunctionName.CONTAINS)
        .title("Contains")
        .description("Checks whether a list contains a datetime element.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to search in")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The datetime element to search for")
                .type(DATETIME)
                .required(true))
        .returnType(BOOLEAN)
        .example("=contains(${list}, now())")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONTAINS_LIST_MAP = function(EvaluatorFunctionName.CONTAINS)
        .title("Contains")
        .description("Checks whether a list contains a map element.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to search in")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The map element to search for")
                .type(MAP)
                .required(true))
        .returnType(BOOLEAN)
        .example("=contains(${list}, ${map})")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONTAINS_LIST_NUMBER = function(EvaluatorFunctionName.CONTAINS)
        .title("Contains")
        .description("Checks whether a list contains a number element.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to search in")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The number element to search for")
                .type(NUMBER)
                .required(true))
        .returnType(BOOLEAN)
        .example("=contains(${list}, 42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONTAINS_LIST_STRING = function(EvaluatorFunctionName.CONTAINS)
        .title("Contains")
        .description("Checks whether a list contains a string element.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to search in")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The string element to search for")
                .type(STRING)
                .required(true))
        .returnType(BOOLEAN)
        .example("=contains(${list}, 'item')")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONTAINS_STRING = function(EvaluatorFunctionName.CONTAINS)
        .title("Contains")
        .description("Checks whether a string contains a substring.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("string")
                .description("The string to search in")
                .type(STRING)
                .required(true),
            parameter("substring")
                .description("The substring to search for")
                .type(STRING)
                .required(true))
        .returnType(BOOLEAN)
        .example("=contains('hello world', 'world')")
        .toDefinition();

    static final EvaluatorFunctionDefinition FLATTEN = function(EvaluatorFunctionName.FLATTEN)
        .title("Flatten")
        .description("Flattens a list of lists into a single list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("listOfLists")
                .description("A list containing nested lists")
                .type(LIST)
                .required(true))
        .returnType(LIST)
        .example("=flatten(${nestedList})")
        .toDefinition();

    static final EvaluatorFunctionDefinition JOIN = function(EvaluatorFunctionName.JOIN)
        .title("Join")
        .description("Joins a list of values into a single string using the specified separator.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("separator")
                .description("The separator string")
                .type(STRING)
                .required(true),
            parameter("list")
                .description("The list of values to join")
                .type(LIST)
                .required(true))
        .returnType(STRING)
        .example("=join(',', ${list})")
        .toDefinition();

    static final EvaluatorFunctionDefinition RANGE = function(EvaluatorFunctionName.RANGE)
        .title("Range")
        .description("Generates a list of integers from start to end (inclusive).")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("start")
                .description("The start value (inclusive)")
                .type(NUMBER)
                .required(true),
            parameter("end")
                .description("The end value (inclusive)")
                .type(NUMBER)
                .required(true))
        .returnType(LIST)
        .example("=range(1, 10)")
        .toDefinition();

    static final EvaluatorFunctionDefinition REMOVE_LIST_BOOLEAN = function(EvaluatorFunctionName.REMOVE)
        .title("Remove")
        .description("Removes a boolean element from a list and returns the modified list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to remove from")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The boolean element to remove")
                .type(BOOLEAN)
                .required(true))
        .returnType(LIST)
        .example("=remove(${list}, true)")
        .toDefinition();

    static final EvaluatorFunctionDefinition REMOVE_LIST_DATETIME = function(EvaluatorFunctionName.REMOVE)
        .title("Remove")
        .description("Removes a datetime element from a list and returns the modified list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to remove from")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The datetime element to remove")
                .type(DATETIME)
                .required(true))
        .returnType(LIST)
        .example("=remove(${list}, now())")
        .toDefinition();

    static final EvaluatorFunctionDefinition REMOVE_LIST_MAP = function(EvaluatorFunctionName.REMOVE)
        .title("Remove")
        .description("Removes a map element from a list and returns the modified list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to remove from")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The map element to remove")
                .type(MAP)
                .required(true))
        .returnType(LIST)
        .example("=remove(${list}, ${map})")
        .toDefinition();

    static final EvaluatorFunctionDefinition REMOVE_LIST_NUMBER = function(EvaluatorFunctionName.REMOVE)
        .title("Remove")
        .description("Removes a number element from a list and returns the modified list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to remove from")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The number element to remove")
                .type(NUMBER)
                .required(true))
        .returnType(LIST)
        .example("=remove(${list}, 42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition REMOVE_LIST_STRING = function(EvaluatorFunctionName.REMOVE)
        .title("Remove")
        .description("Removes a string element from a list and returns the modified list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to remove from")
                .type(LIST)
                .required(true),
            parameter("element")
                .description("The string element to remove")
                .type(STRING)
                .required(true))
        .returnType(LIST)
        .example("=remove(${list}, 'item')")
        .toDefinition();

    static final EvaluatorFunctionDefinition REMOVE_MAP = function(EvaluatorFunctionName.REMOVE)
        .title("Remove")
        .description("Removes a key from a map and returns the modified map.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("map")
                .description("The map to remove from")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to remove")
                .type(STRING)
                .required(true))
        .returnType(MAP)
        .example("=remove(${map}, 'key')")
        .toDefinition();

    static final EvaluatorFunctionDefinition SET_BOOLEAN = function(EvaluatorFunctionName.SET)
        .title("Set")
        .description("Sets a boolean element at a specific index in a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to modify")
                .type(LIST)
                .required(true),
            parameter("index")
                .description("The index at which to set the element")
                .type(NUMBER)
                .required(true),
            parameter("element")
                .description("The boolean element value")
                .type(BOOLEAN)
                .required(true))
        .returnType(LIST)
        .example("=set(${list}, 0, true)")
        .toDefinition();

    static final EvaluatorFunctionDefinition SET_DATETIME = function(EvaluatorFunctionName.SET)
        .title("Set")
        .description("Sets a datetime element at a specific index in a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to modify")
                .type(LIST)
                .required(true),
            parameter("index")
                .description("The index at which to set the element")
                .type(NUMBER)
                .required(true),
            parameter("element")
                .description("The datetime element value")
                .type(DATETIME)
                .required(true))
        .returnType(LIST)
        .example("=set(${list}, 0, now())")
        .toDefinition();

    static final EvaluatorFunctionDefinition SET_LIST = function(EvaluatorFunctionName.SET)
        .title("Set")
        .description("Sets a list element at a specific index in a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to modify")
                .type(LIST)
                .required(true),
            parameter("index")
                .description("The index at which to set the element")
                .type(NUMBER)
                .required(true),
            parameter("element")
                .description("The list element value")
                .type(LIST)
                .required(true))
        .returnType(LIST)
        .example("=set(${list}, 0, ${nestedList})")
        .toDefinition();

    static final EvaluatorFunctionDefinition SET_MAP = function(EvaluatorFunctionName.SET)
        .title("Set")
        .description("Sets a map element at a specific index in a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to modify")
                .type(LIST)
                .required(true),
            parameter("index")
                .description("The index at which to set the element")
                .type(NUMBER)
                .required(true),
            parameter("element")
                .description("The map element value")
                .type(MAP)
                .required(true))
        .returnType(LIST)
        .example("=set(${list}, 0, ${map})")
        .toDefinition();

    static final EvaluatorFunctionDefinition SET_NUMBER = function(EvaluatorFunctionName.SET)
        .title("Set")
        .description("Sets a number element at a specific index in a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to modify")
                .type(LIST)
                .required(true),
            parameter("index")
                .description("The index at which to set the element")
                .type(NUMBER)
                .required(true),
            parameter("element")
                .description("The number element value")
                .type(NUMBER)
                .required(true))
        .returnType(LIST)
        .example("=set(${list}, 0, 42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition SET_STRING = function(EvaluatorFunctionName.SET)
        .title("Set")
        .description("Sets a string element at a specific index in a list and returns the new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to modify")
                .type(LIST)
                .required(true),
            parameter("index")
                .description("The index at which to set the element")
                .type(NUMBER)
                .required(true),
            parameter("element")
                .description("The new string element value")
                .type(STRING)
                .required(true))
        .returnType(LIST)
        .example("=set(${list}, 0, 'newValue')")
        .toDefinition();

    static final EvaluatorFunctionDefinition SIZE = function(EvaluatorFunctionName.SIZE)
        .title("Size")
        .description("Returns the number of elements in a list. Returns -1 if the list is null, 0 if no argument.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The list to measure")
                .type(LIST)
                .required(true))
        .returnType(INTEGER)
        .example("=size(${list})")
        .toDefinition();

    static final EvaluatorFunctionDefinition SORT = function(EvaluatorFunctionName.SORT)
        .title("Sort")
        .description("Sorts the elements of a collection in natural order and returns a new list.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("The collection to sort")
                .type(LIST)
                .required(true))
        .returnType(LIST)
        .example("=sort(${list})")
        .toDefinition();

    static final EvaluatorFunctionDefinition TO_MAP = function(EvaluatorFunctionName.TO_MAP)
        .title("To Map")
        .description(
            "Converts a list of maps with 'key' and 'value' entries into a single map.")
        .category(EvaluatorFunctionCategory.COLLECTION)
        .parameters(
            parameter("list")
                .description("A list of maps, each containing 'key' and 'value' entries")
                .type(LIST)
                .required(true))
        .returnType(MAP)
        .example("=toMap(${keyValueList})")
        .toDefinition();

    // String functions

    static final EvaluatorFunctionDefinition CONCAT_STRING = function(EvaluatorFunctionName.CONCAT)
        .title("Concat")
        .description("Concatenates two strings.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("value1")
                .description("The first string")
                .type(STRING)
                .required(true),
            parameter("value2")
                .description("The second string")
                .type(STRING)
                .required(true))
        .returnType(STRING)
        .example("=concat('hello ', 'world')")
        .toDefinition();

    static final EvaluatorFunctionDefinition CONCAT_LIST = function(EvaluatorFunctionName.CONCAT)
        .title("Concat")
        .description("Concatenates two lists.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("value1")
                .description("The first list")
                .type(LIST)
                .required(true),
            parameter("value2")
                .description("The second list")
                .type(LIST)
                .required(true))
        .returnType(LIST)
        .example("=concat(${list1}, ${list2})")
        .toDefinition();

    static final EvaluatorFunctionDefinition EQUALS_IGNORE_CASE = function(EvaluatorFunctionName.EQUALS_IGNORE_CASE)
        .title("Equals Ignore Case")
        .description("Compares two strings for equality, ignoring case differences.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("string1")
                .description("The first string")
                .type(STRING)
                .required(true),
            parameter("string2")
                .description("The second string")
                .type(STRING)
                .required(true))
        .returnType(BOOLEAN)
        .example("=equalsIgnoreCase('Hello', 'hello')")
        .toDefinition();

    static final EvaluatorFunctionDefinition FORMAT_DATETIME = function(EvaluatorFunctionName.FORMAT)
        .title("Format")
        .description("Formats a date, date-time, or instant using an optional pattern.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("value")
                .description("The date/datetime/instant to format")
                .type(DATETIME)
                .required(true),
            parameter("pattern")
                .description("The format pattern (e.g. 'yyyy-MM-dd')")
                .type(STRING)
                .required(false))
        .returnType(STRING)
        .example("=format(now(), 'yyyy-MM-dd')")
        .toDefinition();

    static final EvaluatorFunctionDefinition FORMAT_STRING = function(EvaluatorFunctionName.FORMAT)
        .title("Format")
        .description("Formats a string using String.format with the provided arguments.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("pattern")
                .description("The format pattern string")
                .type(STRING)
                .required(true),
            parameter("args")
                .description("The format arguments")
                .type(STRING)
                .required(false))
        .returnType(STRING)
        .example("=format('Hello %s', 'World')")
        .toDefinition();

    static final EvaluatorFunctionDefinition INDEX_OF = function(EvaluatorFunctionName.INDEX_OF)
        .title("Index Of")
        .description(
            "Returns the index of the first occurrence of a substring within a string. " +
                "Optionally searches from a given index.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("string")
                .description("The string to search in")
                .type(STRING)
                .required(true),
            parameter("search")
                .description("The substring to find")
                .type(STRING)
                .required(true),
            parameter("fromIndex")
                .description("The index to start searching from")
                .type(NUMBER)
                .required(false))
        .returnType(INTEGER)
        .example("=indexOf('hello world', 'world')")
        .toDefinition();

    static final EvaluatorFunctionDefinition LAST_INDEX_OF = function(EvaluatorFunctionName.LAST_INDEX_OF)
        .title("Last Index Of")
        .description(
            "Returns the index of the last occurrence of a substring within a string. " +
                "Optionally searches backward from a given index.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("string")
                .description("The string to search in")
                .type(STRING)
                .required(true),
            parameter("search")
                .description("The substring to find")
                .type(STRING)
                .required(true),
            parameter("fromIndex")
                .description("The index to start searching backward from")
                .type(NUMBER)
                .required(false))
        .returnType(INTEGER)
        .example("=lastIndexOf('hello world hello', 'hello')")
        .toDefinition();

    static final EvaluatorFunctionDefinition LENGTH = function(EvaluatorFunctionName.LENGTH)
        .title("Length")
        .description("Returns the length of a string.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("string")
                .description("The string to measure")
                .type(STRING)
                .required(true))
        .returnType(INTEGER)
        .example("=length('hello')")
        .toDefinition();

    static final EvaluatorFunctionDefinition SPLIT = function(EvaluatorFunctionName.SPLIT)
        .title("Split")
        .description("Splits a string by a delimiter regex pattern and returns a list of substrings.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("string")
                .description("The string to split")
                .type(STRING)
                .required(true),
            parameter("delimiter")
                .description("The regex delimiter pattern")
                .type(STRING)
                .required(true),
            parameter("limit")
                .description("The maximum number of resulting substrings")
                .type(NUMBER)
                .required(false))
        .returnType(LIST)
        .example("=split('a,b,c', ',')")
        .toDefinition();

    static final EvaluatorFunctionDefinition SUBSTRING = function(EvaluatorFunctionName.SUBSTRING)
        .title("Substring")
        .description("Returns a substring of the given string, starting at beginIndex up to an optional endIndex.")
        .category(EvaluatorFunctionCategory.STRING)
        .parameters(
            parameter("string")
                .description("The source string")
                .type(STRING)
                .required(true),
            parameter("beginIndex")
                .description("The beginning index (inclusive)")
                .type(NUMBER)
                .required(true),
            parameter("endIndex")
                .description("The ending index (exclusive)")
                .type(NUMBER)
                .required(false))
        .returnType(STRING)
        .example("=substring('hello world', 0, 5)")
        .toDefinition();

    // DateTime functions

    static final EvaluatorFunctionDefinition AT_ZONE = function(EvaluatorFunctionName.AT_ZONE)
        .title("At Zone")
        .description("Converts an instant to a zoned date-time at the specified time zone.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("instant")
                .description("The instant to convert")
                .type(DATETIME)
                .required(true),
            parameter("zoneId")
                .description("The time zone identifier (e.g. 'America/New_York')")
                .type(STRING)
                .required(true))
        .returnType(DATETIME)
        .example("=atZone(now(), 'America/New_York')")
        .toDefinition();

    static final EvaluatorFunctionDefinition NOW = function(EvaluatorFunctionName.NOW)
        .title("Now")
        .description("Returns the current instant (UTC timestamp).")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters()
        .returnType(DATETIME)
        .example("=now()")
        .toDefinition();

    static final EvaluatorFunctionDefinition PARSE_DATE = function(EvaluatorFunctionName.PARSE_DATE)
        .title("Parse Date")
        .description("Parses a string into a date. Optionally accepts a date format pattern.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("dateString")
                .description("The date string to parse")
                .type(STRING)
                .required(true),
            parameter("pattern")
                .description("The date format pattern (e.g. 'yyyy-MM-dd')")
                .type(STRING)
                .required(false))
        .returnType(DATETIME)
        .example("=parseDate('2025-01-15')")
        .toDefinition();

    static final EvaluatorFunctionDefinition PARSE_DATE_TIME = function(EvaluatorFunctionName.PARSE_DATE_TIME)
        .title("Parse Date Time")
        .description("Parses a string into a date-time. Optionally accepts a date-time format pattern.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("dateTimeString")
                .description("The date-time string to parse")
                .type(STRING)
                .required(true),
            parameter("pattern")
                .description("The date-time format pattern (e.g. 'yyyy-MM-dd HH:mm:ss')")
                .type(STRING)
                .required(false))
        .returnType(DATETIME)
        .example("=parseDateTime('2025-01-15T10:30:00')")
        .toDefinition();

    static final EvaluatorFunctionDefinition TIMESTAMP = function(EvaluatorFunctionName.TIMESTAMP)
        .title("Timestamp")
        .description("Returns the current time as a Unix timestamp in milliseconds.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters()
        .returnType(LONG)
        .example("=timestamp()")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_MICROS = function(EvaluatorFunctionName.MINUS_MICROS)
        .title("Minus Micros")
        .description("Subtracts the specified number of microseconds from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of microseconds to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusMicros(now(), 1000)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_MILLIS = function(EvaluatorFunctionName.MINUS_MILLIS)
        .title("Minus Millis")
        .description("Subtracts the specified number of milliseconds from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of milliseconds to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusMillis(now(), 500)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_SECONDS = function(EvaluatorFunctionName.MINUS_SECONDS)
        .title("Minus Seconds")
        .description("Subtracts the specified number of seconds from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of seconds to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusSeconds(now(), 30)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_MINUTES = function(EvaluatorFunctionName.MINUS_MINUTES)
        .title("Minus Minutes")
        .description("Subtracts the specified number of minutes from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of minutes to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusMinutes(now(), 15)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_HOURS = function(EvaluatorFunctionName.MINUS_HOURS)
        .title("Minus Hours")
        .description("Subtracts the specified number of hours from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of hours to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusHours(now(), 2)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_DAYS = function(EvaluatorFunctionName.MINUS_DAYS)
        .title("Minus Days")
        .description("Subtracts the specified number of days from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of days to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusDays(now(), 7)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_WEEKS = function(EvaluatorFunctionName.MINUS_WEEKS)
        .title("Minus Weeks")
        .description("Subtracts the specified number of weeks from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of weeks to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusWeeks(now(), 1)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_MONTHS = function(EvaluatorFunctionName.MINUS_MONTHS)
        .title("Minus Months")
        .description("Subtracts the specified number of months from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of months to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusMonths(now(), 3)")
        .toDefinition();

    static final EvaluatorFunctionDefinition MINUS_YEARS = function(EvaluatorFunctionName.MINUS_YEARS)
        .title("Minus Years")
        .description("Subtracts the specified number of years from a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of years to subtract")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=minusYears(now(), 1)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_MICROS = function(EvaluatorFunctionName.PLUS_MICROS)
        .title("Plus Micros")
        .description("Adds the specified number of microseconds to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of microseconds to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusMicros(now(), 1000)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_MILLIS = function(EvaluatorFunctionName.PLUS_MILLIS)
        .title("Plus Millis")
        .description("Adds the specified number of milliseconds to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of milliseconds to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusMillis(now(), 500)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_SECONDS = function(EvaluatorFunctionName.PLUS_SECONDS)
        .title("Plus Seconds")
        .description("Adds the specified number of seconds to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of seconds to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusSeconds(now(), 30)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_MINUTES = function(EvaluatorFunctionName.PLUS_MINUTES)
        .title("Plus Minutes")
        .description("Adds the specified number of minutes to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of minutes to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusMinutes(now(), 15)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_HOURS = function(EvaluatorFunctionName.PLUS_HOURS)
        .title("Plus Hours")
        .description("Adds the specified number of hours to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of hours to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusHours(now(), 2)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_DAYS = function(EvaluatorFunctionName.PLUS_DAYS)
        .title("Plus Days")
        .description("Adds the specified number of days to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of days to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusDays(now(), 7)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_WEEKS = function(EvaluatorFunctionName.PLUS_WEEKS)
        .title("Plus Weeks")
        .description("Adds the specified number of weeks to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of weeks to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusWeeks(now(), 1)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_MONTHS = function(EvaluatorFunctionName.PLUS_MONTHS)
        .title("Plus Months")
        .description("Adds the specified number of months to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of months to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusMonths(now(), 3)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PLUS_YEARS = function(EvaluatorFunctionName.PLUS_YEARS)
        .title("Plus Years")
        .description("Adds the specified number of years to a temporal value.")
        .category(EvaluatorFunctionCategory.DATE_TIME)
        .parameters(
            parameter("temporal")
                .description("The temporal value")
                .type(DATETIME)
                .required(true),
            parameter("amount")
                .description("The number of years to add")
                .type(NUMBER)
                .required(true))
        .returnType(DATETIME)
        .example("=plusYears(now(), 1)")
        .toDefinition();

    // Map functions

    static final EvaluatorFunctionDefinition PUT_BOOLEAN = function(EvaluatorFunctionName.PUT)
        .title("Put")
        .description("Adds or replaces a key with a boolean value in a map and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("map")
                .description("The map to modify")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to add or replace")
                .type(STRING)
                .required(true),
            parameter("value")
                .description("The boolean value to associate with the key")
                .type(BOOLEAN)
                .required(true))
        .returnType(MAP)
        .example("=put(${map}, 'active', true)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PUT_DATETIME = function(EvaluatorFunctionName.PUT)
        .title("Put")
        .description("Adds or replaces a key with a datetime value in a map and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("map")
                .description("The map to modify")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to add or replace")
                .type(STRING)
                .required(true),
            parameter("value")
                .description("The datetime value to associate with the key")
                .type(DATETIME)
                .required(true))
        .returnType(MAP)
        .example("=put(${map}, 'createdAt', now())")
        .toDefinition();

    static final EvaluatorFunctionDefinition PUT_LIST = function(EvaluatorFunctionName.PUT)
        .title("Put")
        .description("Adds or replaces a key with a list value in a map and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("map")
                .description("The map to modify")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to add or replace")
                .type(STRING)
                .required(true),
            parameter("value")
                .description("The list value to associate with the key")
                .type(LIST)
                .required(true))
        .returnType(MAP)
        .example("=put(${map}, 'items', ${list})")
        .toDefinition();

    static final EvaluatorFunctionDefinition PUT_MAP = function(EvaluatorFunctionName.PUT)
        .title("Put")
        .description("Adds or replaces a key with a map value in a map and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("map")
                .description("The map to modify")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to add or replace")
                .type(STRING)
                .required(true),
            parameter("value")
                .description("The map value to associate with the key")
                .type(MAP)
                .required(true))
        .returnType(MAP)
        .example("=put(${map}, 'nested', ${otherMap})")
        .toDefinition();

    static final EvaluatorFunctionDefinition PUT_NUMBER = function(EvaluatorFunctionName.PUT)
        .title("Put")
        .description("Adds or replaces a key with a number value in a map and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("map")
                .description("The map to modify")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to add or replace")
                .type(STRING)
                .required(true),
            parameter("value")
                .description("The number value to associate with the key")
                .type(NUMBER)
                .required(true))
        .returnType(MAP)
        .example("=put(${map}, 'count', 42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition PUT_STRING = function(EvaluatorFunctionName.PUT)
        .title("Put")
        .description("Adds or replaces a key with a string value in a map and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("map")
                .description("The map to modify")
                .type(MAP)
                .required(true),
            parameter("key")
                .description("The key to add or replace")
                .type(STRING)
                .required(true),
            parameter("value")
                .description("The string value to associate with the key")
                .type(STRING)
                .required(true))
        .returnType(MAP)
        .example("=put(${map}, 'key', 'value')")
        .toDefinition();

    static final EvaluatorFunctionDefinition PUT_ALL = function(EvaluatorFunctionName.PUT_ALL)
        .title("Put All")
        .description("Merges all entries from one map into another and returns the new map.")
        .category(EvaluatorFunctionCategory.MAP)
        .parameters(
            parameter("targetMap")
                .description("The target map")
                .type(MAP)
                .required(true),
            parameter("sourceMap")
                .description("The map whose entries are added to the target")
                .type(MAP)
                .required(true))
        .returnType(MAP)
        .example("=putAll(${map1}, ${map2})")
        .toDefinition();

    // Type cast functions

    static final EvaluatorFunctionDefinition BOOLEAN_FROM_NUMBER = function(EvaluatorFunctionName.BOOLEAN)
        .title("Boolean")
        .description("Casts a number to a boolean.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(BOOLEAN)
        .example("=boolean(1)")
        .toDefinition();

    static final EvaluatorFunctionDefinition BOOLEAN_FROM_STRING = function(EvaluatorFunctionName.BOOLEAN)
        .title("Boolean")
        .description("Casts a string to a boolean.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(BOOLEAN)
        .example("=boolean('true')")
        .toDefinition();

    static final EvaluatorFunctionDefinition BYTE_FROM_NUMBER = function(EvaluatorFunctionName.BYTE)
        .title("Byte")
        .description("Casts a number to a byte.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(BYTE)
        .example("=byte(42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition BYTE_FROM_STRING = function(EvaluatorFunctionName.BYTE)
        .title("Byte")
        .description("Casts a string to a byte.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(BYTE)
        .example("=byte('42')")
        .toDefinition();

    static final EvaluatorFunctionDefinition CHAR_FROM_NUMBER = function(EvaluatorFunctionName.CHAR)
        .title("Char")
        .description("Casts a number (code point) to a character.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number code point to cast")
                .type(NUMBER)
                .required(true))
        .returnType(CHARACTER)
        .example("=char(65)")
        .toDefinition();

    static final EvaluatorFunctionDefinition CHAR_FROM_STRING = function(EvaluatorFunctionName.CHAR)
        .title("Char")
        .description("Casts a string to a character.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(CHARACTER)
        .example("=char('A')")
        .toDefinition();

    static final EvaluatorFunctionDefinition SHORT_FROM_NUMBER = function(EvaluatorFunctionName.SHORT)
        .title("Short")
        .description("Casts a number to a short.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(SHORT)
        .example("=short(42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition SHORT_FROM_STRING = function(EvaluatorFunctionName.SHORT)
        .title("Short")
        .description("Casts a string to a short.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(SHORT)
        .example("=short('42')")
        .toDefinition();

    static final EvaluatorFunctionDefinition INT_FROM_NUMBER = function(EvaluatorFunctionName.INT)
        .title("Int")
        .description("Casts a number to an integer.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(INTEGER)
        .example("=int(3.14)")
        .toDefinition();

    static final EvaluatorFunctionDefinition INT_FROM_STRING = function(EvaluatorFunctionName.INT)
        .title("Int")
        .description("Casts a string to an integer.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(INTEGER)
        .example("=int('42')")
        .toDefinition();

    static final EvaluatorFunctionDefinition LONG_FROM_NUMBER = function(EvaluatorFunctionName.LONG)
        .title("Long")
        .description("Casts a number to a long.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(LONG)
        .example("=long(42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition LONG_FROM_STRING = function(EvaluatorFunctionName.LONG)
        .title("Long")
        .description("Casts a string to a long.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(LONG)
        .example("=long('42')")
        .toDefinition();

    static final EvaluatorFunctionDefinition FLOAT_FROM_NUMBER = function(EvaluatorFunctionName.FLOAT)
        .title("Float")
        .description("Casts a number to a float.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(FLOAT)
        .example("=float(42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition FLOAT_FROM_STRING = function(EvaluatorFunctionName.FLOAT)
        .title("Float")
        .description("Casts a string to a float.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(FLOAT)
        .example("=float('3.14')")
        .toDefinition();

    static final EvaluatorFunctionDefinition DOUBLE_FROM_NUMBER = function(EvaluatorFunctionName.DOUBLE)
        .title("Double")
        .description("Casts a number to a double.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The number value to cast")
                .type(NUMBER)
                .required(true))
        .returnType(DOUBLE)
        .example("=double(42)")
        .toDefinition();

    static final EvaluatorFunctionDefinition DOUBLE_FROM_STRING = function(EvaluatorFunctionName.DOUBLE)
        .title("Double")
        .description("Casts a string to a double.")
        .category(EvaluatorFunctionCategory.TYPE)
        .parameters(
            parameter("value")
                .description("The string value to cast")
                .type(STRING)
                .required(true))
        .returnType(DOUBLE)
        .example("=double('3.14')")
        .toDefinition();

    // Utility functions

    static final EvaluatorFunctionDefinition CONFIG = function(EvaluatorFunctionName.CONFIG)
        .title("Config")
        .description("Reads a configuration property value from the Spring Environment by property name.")
        .category(EvaluatorFunctionCategory.UTILITY)
        .parameters(
            parameter("propertyName")
                .description("The configuration property name")
                .type(STRING)
                .required(true))
        .returnType(STRING)
        .example("=config('app.setting')")
        .toDefinition();

    static final EvaluatorFunctionDefinition UUID = function(EvaluatorFunctionName.UUID)
        .title("UUID")
        .description("Generates a random UUID (version 4).")
        .category(EvaluatorFunctionCategory.UTILITY)
        .parameters()
        .returnType(STRING)
        .example("=uuid()")
        .toDefinition();

    private static final List<EvaluatorFunctionDefinition> DEFINITIONS = List.of(
        // Collection
        ADD_BOOLEAN,
        ADD_DATETIME,
        ADD_LIST,
        ADD_MAP,
        ADD_NUMBER,
        ADD_STRING,
        ADD_ALL,
        CONTAINS_LIST_BOOLEAN,
        CONTAINS_LIST_DATETIME,
        CONTAINS_LIST_MAP,
        CONTAINS_LIST_NUMBER,
        CONTAINS_LIST_STRING,
        CONTAINS_STRING,
        FLATTEN,
        JOIN,
        RANGE,
        REMOVE_LIST_BOOLEAN,
        REMOVE_LIST_DATETIME,
        REMOVE_LIST_MAP,
        REMOVE_LIST_NUMBER,
        REMOVE_LIST_STRING,
        REMOVE_MAP,
        SET_BOOLEAN,
        SET_DATETIME,
        SET_LIST,
        SET_MAP,
        SET_NUMBER,
        SET_STRING,
        SIZE,
        SORT,
        TO_MAP,

        // String
        CONCAT_LIST,
        CONCAT_STRING,
        EQUALS_IGNORE_CASE,
        FORMAT_DATETIME,
        FORMAT_STRING,
        INDEX_OF,
        LAST_INDEX_OF,
        LENGTH,
        SPLIT,
        SUBSTRING,

        // DateTime
        AT_ZONE,
        MINUS_DAYS,
        MINUS_HOURS,
        MINUS_MICROS,
        MINUS_MILLIS,
        MINUS_MINUTES,
        MINUS_MONTHS,
        MINUS_SECONDS,
        MINUS_WEEKS,
        MINUS_YEARS,
        NOW,
        PARSE_DATE,
        PARSE_DATE_TIME,
        PLUS_DAYS,
        PLUS_HOURS,
        PLUS_MICROS,
        PLUS_MILLIS,
        PLUS_MINUTES,
        PLUS_MONTHS,
        PLUS_SECONDS,
        PLUS_WEEKS,
        PLUS_YEARS,
        TIMESTAMP,

        // Map
        PUT_ALL,
        PUT_BOOLEAN,
        PUT_DATETIME,
        PUT_LIST,
        PUT_MAP,
        PUT_NUMBER,
        PUT_STRING,

        // Type
        BOOLEAN_FROM_NUMBER,
        BOOLEAN_FROM_STRING,
        BYTE_FROM_NUMBER,
        BYTE_FROM_STRING,
        CHAR_FROM_NUMBER,
        CHAR_FROM_STRING,
        DOUBLE_FROM_NUMBER,
        DOUBLE_FROM_STRING,
        FLOAT_FROM_NUMBER,
        FLOAT_FROM_STRING,
        INT_FROM_NUMBER,
        INT_FROM_STRING,
        LONG_FROM_NUMBER,
        LONG_FROM_STRING,
        SHORT_FROM_NUMBER,
        SHORT_FROM_STRING,

        // Utility
        CONFIG,
        UUID);

    @Override
    public List<EvaluatorFunctionDefinition> getDefinitions() {
        return DEFINITIONS;
    }
}
