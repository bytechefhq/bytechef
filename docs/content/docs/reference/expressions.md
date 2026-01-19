---
description: "ByteChef expressions guide. Learn to dynamically transform workflow data. Learn how to implement this in your ByteChef workflows."
title: Expressions
---
<<<<<<< HEAD

=======
# Expression Cheat Sheet
>>>>>>> origin/master

### Expressions and types

Formula and text expressions used in ByteChef are primarily written using SpEL (Spring Expression language) with some constraints - simple, yet powerful expression language.

SpEL is based on Java ([reference documentation](https://docs.spring.io/spring-framework/reference/core/expressions.html)), but no prior Java knowledge is needed to use it.

The easiest way to learn SpEL is looking at examples which are further down this page. Some attention should be paid to data types, described in more detail in the next section.

## Data types and structures

The data types used in the execution engine, SpEL expressions and data structures are Java based. These are also the data type names that appear in code completion hints. In most cases ByteChef can automatically convert between Java data types and JSON formats.

Below is the list of the most common data types. In Java types column package names are omitted for brevity, they are usually:
- Primitive types and basic objects: [`java.lang`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/package-summary.html)
- Collections: [`java.util`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/package-summary.html)
- Date/Time: [`java.time`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/package-summary.html)

### Basic (primitive data types)

| Java type | ByteChef Type | Description |
|-----------|---------------|-------------|
| `null` | `nullable` | Represents the absence of a value |
| [`String`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html) | `string` | UTF-8 encoded text |
| [`Boolean`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Boolean.html) | `bool` | Represents `true` or `false` values |
| [`Integer`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Integer.html) | `integer` | 32-bit signed integer |
| [`Long`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Long.html) | `integer` | 64-bit signed integer |
| [`Float`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Float.html) | `number` | 32-bit floating point number |
| [`Double`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/Double.html) | `number` | 64-bit floating point number |
| [`LocalTime`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalTime.html) | `time` | Time without timezone (HH:MM:SS) |
| [`LocalDate`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDate.html) | `date` | Date without timezone (YYYY-MM-DD) |
| [`LocalDateTime`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/time/LocalDateTime.html) | `date-time` | Date and time without timezone |
| [`UUID`](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/UUID.html) | `string` | Universally Unique Identifier (e.g., `123e4567-e89b-12d3-a456-426614174000`) |

### Objects/Maps

In ByteChef, objects are implemented as `Map`s, which store key-value pairs. The keys are strings, and values can be of any type. This is similar to JSON objects.

Example:
```java
{
  "name": "John",
  "age": 30,
  "active": true
}
```

### Arrays/Lists

In ByteChef, arrays are implemented using `List`s, which are ordered collections that can contain duplicate elements. The `Collection` interface is the root interface for all collection types in Java, including `List`, `Set`, and `Queue`.

Example:
```java
[1, 2, 3, 4, 5]
["apple", "banana", "cherry"]
```

### Date/Time

See [Handling data/time](#handling-data-time) for detailed description of how to deal with date and time in ByteChef.

## Expression syntax

### Formula expressions

In ByteChef `formula` expression starts with `=` and `${}` is used to access values.

For example:

```
=${httpClient_1.body.amount} + 1
```

will access value defined in `${httpClient_1.body.amount}` nested structure and increase it for `1`.

### Text expressions

Also, it is allowed to write a `text` expression like

```
${httpClient_1.body.amount} is total amount
```

where value defined in `${httpClient_1.body.amount}` nested structure will be merged with `' is total amount'` literal part.

The same expression can be written as a formula expression:

```
=${httpClient_1.body.amount} + ' is total amount'
```

## Basics

Most of the literals are similar to JSON ones, in fact in many cases JSON structure is valid SpEL. There are a few notable exceptions:

- Lists are written using curly braces: `{"firstElement", "secondElement"}`, as `[]` is used to access elements in array
- Strings can be quoted with either `'` or `"`
- Field names in maps do not need to be quoted (e.g., `{name: "John"}` is valid SpEL, but not valid JSON)

| Expression | Result | Type |
|------------|--------|------|
| `'Hello World'` | `"Hello World"` | `String` |
| `true` | `true` | `Boolean` |
| `null` | `null` | `Null` |
| `{}` | an empty list | `List[Unknown]` |
| `{1,2,3,4}` | a list of integers from 1 to 4 | `List[Integer]` |
| `{:}` | an empty object | `Map{}` |
| `{john:300, alex:400}` | an object (name-value collection) | `Map{alex: Integer(400), john: Integer(300)}` |
| `#input` | variable | - |
| `'AA' + 'BB'` | `"AABB"` | `String` |

## Arithmetic Operators

The `+`, `-`, `*` arithmetic operators work as expected.

| Operator | Equivalent symbolic operator | Example expression | Result |
|----------|-----------------------------|-------------------|--------|
| `div` | `/` | `7 div 2` | `3` |
| `div` | `/` | `7.0 div 2` | `2.3333333333` |
| `mod` | `%` | `23 mod 7` | `2` |

## Conditional Operators

| Expression | Result | Type |
|------------|--------|------|
| `2 == 2` | `true` | `Boolean` |
| `2 > 1` | `true` | `Boolean` |
| `true AND false` | `false` | `Boolean` |
| `true && false` | `false` | `Boolean` |
| `true OR false` | `true` | `Boolean` |
| `true \|\| false` | `true` | `Boolean` |
| `2 > 1 ? 'a' : 'b'` | `"a"` | `String` |
| `2 < 1 ? 'a' : 'b'` | `"b"` | `String` |
| `nonNullVar == null ? 'Unknown' : 'Success'` | `"Success"` | `String` |
| `nullVar == null ? 'Unknown' : 'Success'` | `"Unknown"` | `String` |
| `nullVar?:'Unknown'` | `"Unknown"` | `String` |
| `'john'?:'Unknown'` | `"john"` | `String` |

## Relational Operators

| Operator | Equivalent symbolic operator | Example expression | Result |
|----------|-----------------------------|-------------------|--------|
| `lt` | `<` | `3 lt 5` | `true` |
| `gt` | `>` | `4 gt 4` | `false` |
| `le` | `<=` | `3 le 5` | `true` |
| `ge` | `>=` | `4 ge 4` | `true` |
| `eq` | `==` | `3 eq 3` | `true` |
| `ne` | `!=` | `4 ne 2` | `true` |
| `not` | `!` | `not true` | `false` |

## String Operators

| Expression | Result | Type |
|------------|--------|------|
| `'AA' + 'BB'` | `"AABB"` | `String` |

## Method Invocations

As ByteChef uses Java types, some objects contain additional methods, but they are not allowed to be called directly on those types.

For example, this is **not** allowed:
```
'someValue'.substring(4)
```

Instead, use the built-in function:
```
substring('someValue', 4)
```

ByteChef provides built-in functions to help with various type operations.

## Accessing Elements of a List or a Map

| Expression | Result | Type |
|------------|--------|------|
| `{1,2,3,4}[0]` | `1` | `Integer` |
| `{jan:300, alex:400}[alex]` | a value of field 'alex', which is `400` | `Integer` |
| `{jan:300, alex:400}['alex']` | `400` | `Integer` |
| `{jan:{age:24}, alex:{age: 30}}['alex']['age']` | `30` | `Integer` |
| `{foo: 1L, bar: 2L, tar: 3L}.?[#this.key == "foo" OR #this.value > 2L]` | `{'tar': 3, 'foo': 1}` | `Map[String, Long]` |

Attempting to access non-present elements will cause exceptions. For lists, they are thrown at runtime, and for maps, they occur before deployment during expression validation.

| Expression | Error |
|------------|------------------------------------------------|
| `{1,2,3,4}[4]` | Runtime error: Index out of bounds |
| `{jan:300, alex:400}['anna']` | Compilation error: No property 'anna' in map |

## Filtering Lists

Special variable `#this` is used to operate on a single element of a list. 

- Filtering all elements uses the syntax: `.?[condition]`
- To get the first matching element: `.^[condition]`
- To get the last matching element: `.$[condition]`

| Expression | Result | Type |
|------------|--------|------|
| `{1,2,3,4}.?[#this ge 3]` | `{3, 4}` | `List[Integer]` |
| `usersList.?[#this.firstName == 'john']` | `{'john doe'}` | `List[String]` |
| `{1,2,3,4}.^[#this ge 3]` | `{3}` | `Integer` |
| `{1,2,3,4}.$[#this ge 3]` | `{4}` | `Integer` |

## Transforming Lists

Special variable `#this` is used to operate on a single element of a list.

For the examples below, assume `listOfPersons` contains:
```json
[
  {"name": "Alex", "age": 42}, 
  {"name": "John", "age": 24}
]
```

| Expression | Result | Type |
|------------|--------|------|
| `{1,2,3,4}.![#this * 2]` | `{2, 4, 6, 8}` | `List[Integer]` |
| `listOfPersons.![#this.name]` | `{'Alex', 'John'}` | `List[String]` |
| `listOfPersons.![#this.age]` | `{42, 24}` | `List[Integer]` |
| `listOfPersons.![7]` | `{7, 7}` | `List[Integer]` |
| `listOfPersons.![{key: #this.name, value: #this.age}]` | `[{"key": "Alex", "value": 42}, {"key": "John", "value": 24}]` | `List[Record]` |
| `toMap(listOfPersons.![{key: #this.name, value: #this.age}])` | `{Alex: 42, John: 24}` | `Map[String, Integer]` |

Note: `toMap()` function can be applied to lists of maps where each map contains `key` and `value` properties. For other list operations, see the List Functions section.

## Safe Navigation

When accessing nested structures, handle null fields to avoid errors. SpEL's safe navigation operator (`?.`) is a shorthand for the conditional operator: `someVar?.b` is equivalent to `someVar != null ? someVar.b : null`.

| Expression | #var value | Result | Type |
|------------|------------|--------|------|
| `var.foo` | `{foo: 5}` | `5` | `Integer` |
| `var.foo` | `null` | `java.lang.NullPointerException` | `java.lang.NullPointerException` |
| `var?.foo` | `{foo: 5}` | `5` | `Integer` |
| `var?.foo` | `null` | `null` | `Null` |

## Invoking Static Methods

ByteChef does not allow calling static Java methods directly.

For example, this is **not** allowed:
```
T(java.lang.Math).PI
```

Instead, use the equivalent built-in functions provided by ByteChef.

## Chaining with Dot Operator

| Expression | Result | Type |
|------------|--------|------|
| `{1, 2, 3, 4}.?[#this > 1].![#this > 2 ? #this * 2 : #this]` | `{2, 6, 8}` | `List[Double]` |

## Type Conversions

Type conversion in ByteChef can be done either implicitly or explicitly.

### Explicit Conversions

Explicit conversions are available as built-in functions. See the [Type Conversion Functions](#type-conversion-functions) section for details.

### Implicit Conversions

SpEL provides many built-in implicit conversions that are also available in ByteChef. These include conversions between various numeric types and between `String` and other value types.

Implicit conversion occurs when an input value of one type is used in a context that expects a different type. The system will automatically attempt to convert the value to the expected type.

#### Common Implicit Conversions

| Input value | Input type | Converts to |
|-------------|------------|-------------|
| `12.34f` | `Float` | `Double` |
| `42` | `Integer` | `Long` |
| `'Europe/Warsaw'` | `String` | `ZoneId` |
| `'+01:00'` | `String` | `ZoneOffset` |
| `'09:00'` | `String` | `LocalTime` |
| `'2020-07-01'` | `String` | `LocalDate` |
| `'2020-07-01T09:00'` | `String` | `LocalDateTime` |
| `'en_GB'` | `String` | `Locale` |
| `'ISO-8859-1'` | `String` | `Charset` |
| `'USD'` | `String` | `Currency` |
| `'bf3bb3e0-b359-4e18-95dd-1d89c7dc5135'` | `String` | `UUID` |

#### Usage Examples

| Expression | Input value | Input type | Target type |
|------------|-------------|------------|-------------|
| `atZone(now(), 'Europe/Warsaw')` | `'Europe/Warsaw'` | `String` | `ZoneId` |
| `'' + 42` | `'42'` | `Integer` | `String` |

## Built-in functions

### Type Conversion Functions

| Function | Description |
|---|---|
| boolean(value) | Converts a value to a boolean. |
| byte(value) | Converts a value to a byte. |
| char(value) | Converts a value to a character. |
| float(value) | Converts a value to a float. |
| double(value) | Converts a value to a double. |
| int(value) | Converts a value to an integer. |
| long(value) | Converts a value to a long. |
| short(value) | Converts a value to a short. |

### String Functions

| Function | Description |
|---|---|
| concat(str1, str2) | Concatenates two strings or two lists. |
| contains(str, substr) | Checks if a string contains a substring. |
| format(formatStr, args...) | Formats a string using a format string and arguments (similar to String.format). |
| indexOf(str, substr) | Returns the index of the first occurrence of a substring in a string. |
| join(list, delimiter) | Joins a list of strings with a delimiter. |
| lastIndexOf(str, substr) | Returns the index of the last occurrence of a substring in a string. |
| length(str) | Returns the length of a string. |
| split(str, delimiter) | Splits a string by a delimiter and returns a list of strings. |
| substring(str, start, end) | Returns a substring from start index to end index. |

### Date and Time Functions

| Function | Description |
|---|---|
| atZone(instant, zoneId) | Converts an instant to a zoned date-time with the specified time zone. |
| format(date, [format]) | Formats a date using the specified format. If no format is provided, uses ISO format. |
| minusDays(date, days) | Subtracts the specified number of days from a date. |
| minusHours(date, hours) | Subtracts the specified number of hours from a date. |
| minusMicros(date, micros) | Subtracts the specified number of microseconds from a date. |
| minusMillis(date, millis) | Subtracts the specified number of milliseconds from a date. |
| minusMinutes(date, minutes) | Subtracts the specified number of minutes from a date. |
| minusMonths(date, months) | Subtracts the specified number of months from a date. |
| minusSeconds(date, seconds) | Subtracts the specified number of seconds from a date. |
| minusWeeks(date, weeks) | Subtracts the specified number of weeks from a date. |
| minusYears(date, years) | Subtracts the specified number of years from a date. |
| now() | Returns the current date and time. |
| parseDate(dateStr, [format]) | Parses a string into a date. If no format is provided, uses ISO format. |
| parseDateTime(dateTimeStr, [format]) | Parses a string into a date-time. If no format is provided, uses ISO format. |
| plusDays(date, days) | Adds the specified number of days to a date. |
| plusHours(date, hours) | Adds the specified number of hours to a date. |
| plusMicros(date, micros) | Adds the specified number of microseconds to a date. |
| plusMillis(date, millis) | Adds the specified number of milliseconds to a date. |
| plusMinutes(date, minutes) | Adds the specified number of minutes to a date. |
| plusMonths(date, months) | Adds the specified number of months to a date. |
| plusSeconds(date, seconds) | Adds the specified number of seconds to a date. |
| plusWeeks(date, weeks) | Adds the specified number of weeks to a date. |
| plusYears(date, years) | Adds the specified number of years to a date. |
| timestamp() | Returns the current timestamp in milliseconds. |

### List Functions

| Function | Description |
|---|---|
| add(list, element) | Adds an element to a list and returns a new list. |
| addAll(list1, list2) | Adds all elements from list2 to list1 and returns a new list. |
| concat(list1, list2) | Concatenates two lists. |
| contains(list, element) | Returns true if list contains the specified element. |
| flatten(list) | Flattens a nested list into a single list. |
| range(start, end) | Creates a list of integers from start to end (inclusive). |
| remove(list, element) | Removes an element from a list and returns the modified list. |
| set(list, index, element) | Sets an element at a specific index in a list and returns the modified list. |
| size(list) | Returns the size of a list. If list is null returns -1. |
| sort(list) | Sorts a list in ascending order. |

### Map Functions

| Function | Description |
|---|---|
| put(map, key, value) | Adds a key-value pair to a map and returns a new map. |
| putAll(map1, map2) | Adds all key-value pairs from map2 to map1 and returns a new map. |
| remove(map, key) | Removes a key-value pair from a map and returns the modified map. |
| size(map) | Returns the size of a map. |
| toMap(list) | Converts a list of maps with "key" and "value" entries to a single map. |

### System Functions

| Function | Description |
|---|---|
| uuid() | Generates a random UUID. |