---
title: Expressions
description: Expressions in ByteChef provide a powerful way to dynamically access, transform, and manipulate data within your workflows. They allow you to reference data from previous steps, perform calculations, implement conditional logic, and create dynamic configurations for your automation processes.
---

# SpEL Cheat Sheet

## Expressions and types

Formula expressions used in ByteChef are primarily written using SpEL (Spring Expression language) with some constraints - simple, yet powerful expression language.

SpEL is based on Java ([reference documentation](https://docs.spring.io/spring-framework/reference/core/expressions.html)), but no prior Java knowledge is needed to use it.

The easiest way to learn SpEL is looking at examples which are further down this page. Some attention should be paid to data types, described in more detail in the next section.

## Data types and structures

The data types used in the execution engine, SpEL expressions and data structures are Java based.
These are also the data type names that appear in code completion hints.
In most cases ByteChef can automatically convert between Java data types and JSON formats.

Below is the list of the most common data types. In Java types column package names are omitted for brevity,
they are usually `java.lang` (primitives), `java.util` (List, Map) and `java.time`

### Basic (primitive data types)

| Java type      | ByteChef Type | Comment                                    |
| ------------   |---------------|--------------------------------------------|
| null           | nullable      |                                            |
| String         | string        | UTF-8                                      |
| Boolean        | bool          |                                            |
| Integer        | integer       | 32bit                                      |
| Long           | number        | 64bit                                      |
| Float          | number        | single precision                           |
| Double         | number        | double precision                           |
| LocalTime      | time          |                                            |
| LocalDate      | date          |                                            |
| LocalDateTime  | date-time     |                                            |
| UUID           | string        | uuid                                       |

### Objects/maps

In ByteChef objects are referred to as `Maps`;

### Arrays/lists

In ByteChef arrays are referred to as `Lists`;
also in some context `Collection` can be met (it's Java API for handling lists, sets etc.).

### Date/Time

See [Handling data/time](#handling-datetime) for detailed description of how to deal with date and time in ByteChef.


## SpEL syntax

### Formula expressions

In ByteChef `formula` expression starts with `=` and `${}` is used to access values.

For example:

`= ${httpClient_1.body.amount} + 1`

will access value defined in `httpClient_1.body.amount` nested structure and increase it for `1`.

### Text expressions

Also, it is allowed to write an expression like

`${httpClient_1.body.amount} is total amount`

It is processed as a text expression where `httpClient_1.body.amount` defines access to nested structure and is first evaluated and then merged with the literal part.

The same expression can be written as a formula expression:

`=${httpClient_1.body.amount} + ' is total amount'`

### Basics

Most of the literals are similar to JSON ones, in fact in many cases JSON structure is valid SpEL.
There are a few notable exceptions:
- Lists are written using curly braces: `{"firstElement", "secondElement"}`, as `[]` is used to access elements in array
- Strings can be quoted with either `'` or `"`
- Field names in maps do not to be quoted (e.g. `{name: "John"}` is valid SpEL, but not valid JSON)

| Expression             | Result                          | Type                 |
| ------------           | --------                        | --------             |
| `'Hello World'`        | "Hello World"                   | String               |
| `true`                 | true                            | Boolean              |
| `null`                 | null                            | Null                 |
| `{}`                   | an empty list                   | List[Unknown]        |
| `{1,2,3,4}`            | a list of integers from 1 to 4  | List[Integer]        |
| `{:}`                  | an empty object                 | Map{}                |
| `{john:300, alex:400}` | a object (name-value collection)| Map{alex: Integer(400), john: Integer(300)} |
| `#input`               | variable                        |                      |
| `'AA' + 'BB'`          | "AABB"                          | String               |

### Arithmetic Operators

The `+`, `-`, `*` arithmetic operators work as expected.

| Operator | Equivalent symbolic operator | Example expression | Result |
|----------|------------------------------|--------------------|--------|
| `div`    | `/`                          | `7 div 2`          | 3      |
| `div`    | `/`                          | `7.0 div 2`        | 2.3333333333 |
| `mod`    | `%`                          | `23 mod 7`         | 2      |

### Conditional Operators

| Expression                                   | Result    | Type     |
| ------------                                 | --------  | -------- |
| `2 == 2`                                     | true      | Boolean  |
| `2 > 1`                                      | true      | Boolean  |
| `true AND false`                             | false     | Boolean  |
| `true && false`                              | false     | Boolean  |
| `true OR false`                              | true      | Boolean  |
| <code>true &#124;&#124; false</code>         | true      | Boolean  |
| `2 > 1 ? 'a' : 'b'`                          | "a"       | String   |
| `2 < 1 ? 'a' : 'b'`                          | "b"       | String   |
| `nonNullVar == null ? 'Unknown' : 'Success'` | "Success" | String   |
| `nullVar == null ? 'Unknown' : 'Success'`    | "Unknown" | String   |
| `nullVar?:'Unknown'`                         | "Unknown" | String   |
| `'john'?:'Unknown'`                          | "john"    | String   |

### Relational operators

| Operator | Equivalent symbolic operator | Example expression | Result |
|----------|------------------------------|--------------------|--------|
| `lt`     | `<`                          | `3 lt 5`           | true   |
| `gt`     | `>`                          | `4 gt 4`           | false  |
| `le`     | `<=`                         | `3 le 5`           | true   |
| `ge`     | `>=`                         | `4 ge 4`           | true   |
| `eq`     | `==`                         | `3 eq 3`           | true   |
| `ne`     | `!=`                         | `4 ne 2`           | true   |
| `not`    | `!`                          | `not true`         | false  |

### Strings operators

| Expression      | Result      | Type     |
| --------------  | ----------- | -------- |
| `'AA' + 'BB'`   | "AABB"      | String   |

### Method invocations

As ByteChef uses Java types, some objects are more than data containers - they contain additional methods, but they are not allowed to be called directly on those types.

`'someValue'.substring(4)` is not allowed expression.

Instead, this is allowed `substring('someValue', 4)` expression.

ByteChef has built-in [functions](#built-in-functions) to help you do various type operations.

### Accessing elements of a list or a map

| Expression                                                              | Result                                | Type              |
|-------------------------------------------------------------------------|---------------------------------------|-------------------|
| `{1,2,3,4}[0]`                                                          | 1                                     | Integer           |
| `{jan:300, alex:400}[alex]`                                             | a value of field 'alex', which is 400 | Integer           |
| `{jan:300, alex:400}['alex']`                                           | 400                                   | Integer           |
| `{jan:{age:24}}, alex:{age: 30}}}['alex']['age']`                       | 30                                    | Integer           |
| `{foo: 1L, bar: 2L, tar: 3L}.?[#this.key == "foo" OR #this.value > 2L]` |  {'tar': 3, 'foo': 1}                 | Map[String, Long] |


Attempting to access non-present elements will cause exceptions. For lists, they are thrown in runtime and for maps
they occur before deployment of a scenario during expression validation.

| Expression                    | Error                                           |
|-------------------------------|-------------------------------------------------|
| `{1,2,3,4}[4]`                | Runtime error: Index out of bounds              |
| `{jan:300, alex:400}['anna']` | Compilation error: No property 'anna' in map |

### Filtering lists

Special variable `#this` is used to operate on single element of list.
Filtering all the elements uses a syntax of `.?`.
In addition to filtering all the elements, you can retrieve only the first or the last value.
To obtain the first element matching the predicate, the syntax is `.^`.
To obtain the last matching element, the syntax is `.$`.

| Expression                                | Result       | Type          |
| ------------                              |--------------| --------      |
| `{1,2,3,4}.?[#this ge 3]`                 | {3, 4}       | List[Integer] |
| `usersList.?[#this.firstName == 'john']`  | {'john doe'} | List[String]  |
| `{1,2,3,4}.^[#this ge 3]`                 | {3}          | Integer       |
| `{1,2,3,4}.$[#this ge 3]`                 | {4}          | Integer       |

### Transforming lists

Special variable `#this` is used to operate on single element of list.

For the examples in the table below, let's assume that the `listOfPersons` contains the following data:
`{{"name":"Alex", "age": 42}, {"name": "John", "age": 24}}` - it is a list of records.

| Expression                                                     | Result                                                       | Type                 |
|----------------------------------------------------------------|--------------------------------------------------------------|----------------------|
| `{1,2,3,4}.![#this * 2]`                                       | {2, 4, 6, 8}                                                 | List[Integer]        |
| `listOfPersons.![#this.name]`                                  | {'Alex', 'John'}                                             | List[String]         |
| `listOfPersons.![#this.age]`                                   | {42, 24}                                                     | List[Integer]        |
| `listOfPersons.![7]`                                           | {7, 7}                                                       | List[Integer]        |
| `listOfPersons.![{key: #this.name, value: #this.age}]`         | {{"key": "Alex", "value": 42}, {"key": "John", "value": 24}} | List[Record]         |
| `toMap(listOfPersons.![{key: #this.name, value: #this.age}])`  | {Alex: 42, John: 24}                                         | Map[String, Integer] |

Note, that `toMap()` function can be applied to lists of maps, if each map contains two properties: `key` and `value`.
For other operations on lists, please see the list [functions](#built-in-functions).

### Safe navigation

When you access nested structure, you have to take care of null fields, otherwise you'll end up with
error. SpEL provides helpful safe navigation operator, it's basically shorthand for conditional operator:
`someVar?.b` means `someVar != null ? someVar.b : null`

| Expression | `#var` value | Result                         | Type                            |
| ---------- | ------------ | --------                       | --------                        |
| `var.foo`  | {foo: 5}     | 5                              | Integer                         |
| `var.foo`  | null         | java.lang.NullPointerException | java.lang.NullPointerException  |
| `var?.foo` | {foo: 5}     | 5                              | Integer                         |
| `var?.foo` | null         | null                           | Null                            |

### Invoking static methods

ByteChef does not allow to call static Java methods.

`T(java.lang.Math).PI` is not allowed expression.

### Chaining with dot
| Expression                                                   | Result    | Type     |
| ------------                                                 | --------  | -------- |
| `{1, 2, 3, 4}.?[#this > 1].![#this > 2 ? #this * 2 : #this]` | {2, 6, 8} | Double   |

### Type conversions

It is possible to convert from a type to another type and this can be done by implicit and explicit conversion. For explicit conversion check [functions](#built-in-functions).

#### Explicit conversions

Explicit conversions are available as built-in [functions](#built-in-functions).

#### Implicit conversion

SpEL has many built-in implicit conversions that are available also in Nussknacker. Mostly conversions between various
numeric types and between `String` and some useful logical value types. Implicit conversion means that when finding
the "input value" of type "input type" (see the table below) in the context where "target type" is expected, Nussknacker
will try to convert the type of the "input value" to the "target type". This behaviour can be encountered in particular
when passing certain values to method parameters (these values can be automatically converted to the desired type).

Some conversion examples:

| Input value                              | Input type | Conversion target type |
|------------------------------------------|------------|------------------------|
| `12.34`                                  | Double     | BigDecimal             |
| `12.34f`                                 | Float      | Double                 |
| `42`                                     | Integer    | Long                   |
| `42L`                                    | Long       | BigDecimal             |
| `'Europe/Warsaw'`                        | String     | ZoneId                 |
| `'+01:00'`                               | String     | ZoneOffset             |
| `'09:00'`                                | String     | LocalTime              |
| `'2020-07-01'`                           | String     | LocalDate              |
| `'2020-07-01T'09:00'`                    | String     | LocalDateTime          |
| `'en_GB'`                                | String     | Locale                 |
| `'ISO-8859-1'`                           | String     | Charset                |
| `'USD'`                                  | String     | Currency               |
| `'bf3bb3e0-b359-4e18-95dd-1d89c7dc5135'` | String     | UUID                   |

Usage example:

| Expression                          | Input value       | Input type | Target type |
|-------------------------------------|-------------------|------------|-------------|
| `atZone(now(), 'Europe/Warsaw')`    | `'Europe/Warsaw'` | String     | ZoneId      |
| `'' + 42`                           | `'42'`            | Integer    | String      |

## Built-in functions

For the full list of available functions take a look at [Functions](/reference/functions).
