---
title: "Data Mapper"
description: "The Data Mapper enables you to configure data mappings."
---
## Reference
<hr />

The Data Mapper enables you to configure data mappings.

Categories: [HELPERS]

Version: 1

<hr />






## Actions


### Rename keys
The action renames keys of an input object defined by mappings.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Input | OBJECT | OBJECT_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |




### Replace value
Replaces a given value with the specified value defined in mappings. In case there is no mapping specified for the value, it returns the default value, and if there is no default defined, it returns null. You can also change a string value with regex.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Value type | INTEGER | SELECT  |
| Value | ARRAY | ARRAY_BUILDER  |
| Value | BOOLEAN | SELECT  |
| Value | DATE | DATE  |
| Value | DATE_TIME | DATE_TIME  |
| Value | INTEGER | INTEGER  |
| Value | NUMBER | NUMBER  |
| Value | OBJECT | OBJECT_BUILDER  |
| Value | STRING | TEXT  |
| Value | TIME | TIME  |
| Default value | ARRAY | ARRAY_BUILDER  |
| Default value | BOOLEAN | SELECT  |
| Default value | DATE | DATE  |
| Default value | DATE_TIME | DATE_TIME  |
| Default value | INTEGER | INTEGER  |
| Default value | NULL | NULL  |
| Default value | NUMBER | NUMBER  |
| Default value | OBJECT | OBJECT_BUILDER  |
| Default value | STRING | TEXT  |
| Default value | TIME | TIME  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |




### Replace all specified values
Goes through all object parameters and replaces all specified input parameter values.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Input type | INTEGER | SELECT  |
| Input | OBJECT | OBJECT_BUILDER  |
| Input | ARRAY | ARRAY_BUILDER  |
| Value type | INTEGER | SELECT  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |




### Replace multiple values by key
Replaces all values specified by the keys in the input object with the values specified by keys in the output object.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Input | OBJECT | OBJECT_BUILDER  |
| Output | OBJECT | OBJECT_BUILDER  |
| Mappings | ARRAY | ARRAY_BUILDER  |




### Map objects to object
Creates a new object with the chosen input properties. You can also rename the property keys.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Input type | INTEGER | SELECT  |
| Input | OBJECT | OBJECT_BUILDER  |
| Input | ARRAY | ARRAY_BUILDER  |
| Mapping | ARRAY | ARRAY_BUILDER  |
| Include Unmapped | BOOLEAN | SELECT  |
| Include Nulls | BOOLEAN | SELECT  |
| Include Empty strings | BOOLEAN | SELECT  |




### Map objects to array
Transform an object or array of objects into an array of key-value pairs.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Input type | INTEGER | SELECT  |
| Input | OBJECT | OBJECT_BUILDER  |
| Input | ARRAY | ARRAY_BUILDER  |
| Field key | STRING | TEXT  |
| Value key | STRING | TEXT  |




### Merge and pivot properties by key
Creates a new object out of all objects that have the same key as the specified field kay and an object as value. That value of the new object contains values of all properties that share the specified field key as keys and the they all have the specified field value as a value.

#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Input | ARRAY | ARRAY_BUILDER  |
| Field Key | STRING | TEXT  |
| Field Value | STRING | TEXT  |




<hr />

# Additional instructions
<hr />

# Overview

Data Mapper is a component or a tool designed to facilitate and configure the transformation of data from one format to another. Data Mapper typically allows you to map fields from the source data (typically a JSON output from another component) to the fields of the target data format or structure (e.g., the data model of another application). This component is particularly useful when dealing with diverse data sources and targets, or when data structures do not align directly and require transformation, such as renaming fields, renaming keys or reformatting values.
<hr/>

## Actions:

## Rename Keys

### Overview

This action allows you to specify mappings that direct the renaming of keys in an input object to new names according to defined rules.

The process typically involves two main components:

1. **Path to the Key**: This specifies the location of the key within the structured data that needs to be renamed. The path is defined in a dot notation (for JSON), which allows the action to precisely identify and access the key in a nested or hierarchical data structure.
2. **New Key Name**: This is the name that the original key should be changed to. This renaming can be necessary for several reasons such as making key names more descriptive, aligning data from different sources with a common schema, or complying with the naming conventions of a target system.

By defining these mappings, the Rename Keys action effectively helps in normalizing data, making it easier to integrate and process across various systems and applications. This capability is particularly valuable in data integration, migration projects, or when interfacing with external systems that require a specific data schema.


### Example

