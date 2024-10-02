# Overview

Data Mapper is a component or a tool designed to facilitate and configure the transformation of data from one format to another. Data Mapper typically allows you to map fields from the source data (typically a JSON output from another component) to the fields of the target data format or structure (e.g., the data model of another application). This component is particularly useful when dealing with diverse data sources and targets, or when data structures do not align directly and require transformation, such as renaming fields, renaming keys or reformatting values.
<hr/>

## Rename Keys

### Overview

This action allows you to specify mappings that direct the renaming of keys in an input object to new names according to defined rules.

### Property Description

The process involves two main components:
1. **Input**: This defines your input value. It can be inputted manually or using data pills, which is its intended use case.
2. **Mappings**: Each mapping requires two parameters:
   - **From Path**: This specifies the location of the key within the structured data that needs to be renamed.
     - Dot Notation: The path is defined in a dot notation (for JSON), which allows the action to precisely identify and access the key in a nested or hierarchical data structure.
   - **To**: This specifies the name that the original key should be changed to. This renaming can be necessary for several reasons such as making key names more descriptive, aligning data from different sources with a common schema, or complying with the naming conventions of a target system.

### Use Cases:

By defining these mappings, the Rename Keys action effectively helps in normalizing data, making it easier to integrate and process across various systems and applications. This capability is particularly valuable in data integration, migration projects, or when interfacing with external systems that require a specific data schema.

### Example

<div style="position:relative;height:100%;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.87165775% + 32px)"><iframe src="https://www.guidejar.com/embed/177e078b-541d-44e0-8a5b-f7ef2748d6dd?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

## Replace Value

### Overview:

This action enables the substitution of specified values, based on predefined mappings. This action strictly adheres to type consistency, meaning that the values being replaced and their replacements must be of the same data type.

### Property Description

1. **Value Type**: Initially, you must define the data type of the values you intend to replace. This ensures type consistency, preventing errors that could arise from type mismatches (e.g., replacing a string with an integer).
2. **Value**: This defines your input value. It can be inputted manually or using data pills, which is its intended use case.
3. **Default Value**: If the replacement action encounters a value for which no explicit mapping is defined, it refers to a predefined default value. If no default value is specified either, the action will return null or a similar placeholder to denote the absence of a replacement.
4. **Mappings**: You need to establish a set of mappings that dictate how each value should be replaced. A mapping consists of a pair - the original value (From) and its intended replacement (To).
   - Regex Support for Strings: In special cases where the values to be replaced are strings, you have the flexibility to define replacements using regular expressions (regex). This allows for more dynamic and complex matching and replacing scenarios, such as pattern-based text substitutions.

### Use Cases: 

This action is particularly useful in data cleansing and transformation tasks where consistency and correctness of data types are crucial. It is useful when you need to transform a single data pill output or a value in the output in a multiple iteration process.

[//]: # (### Example)
[//]: # ()
[//]: # ([Guidejar]&#40;https://guidejar.com/guides/b644bd49-59e5-4b1f-b47b-004f7e99cc12&#41; tutorial.)

## Replace All Specified Values

### Overview:

This action automates the batch replacement of specified data within objects or arrays of objects, based on predefined mappings of values. Once initiated, the action iterates through each parameter of the input objects. If a parameter’s value matches a key in the mappings, it is replaced by the mapped value. Parameters not specified in the mappings remain unchanged. Additionally, the action provides the capability to modify string values through the use of regular expressions.

### Property Description

1. **Input Type**: This action accepts either a single object or an array of objects.
2. **Input**: Depends on Input Type. Each object is treated individually, undergoing a comprehensive scan through all of its parameters for potential replacements.
3. **Value Type**: Before performing replacements, it's crucial to ensure that all mappings maintain type consistency. This means that both the original value (From) and the new value (To) within each mapping pair must share the same data type.
4. **Mappings**: You need to establish a set of mappings that dictate how each value should be replaced. A mapping consists of a pair - the original value (From) and its intended replacement (To).
    - Regex Support for Strings: In special cases where the values to be replaced are strings, you have the flexibility to define replacements using regular expressions (regex). This allows for more dynamic and complex matching and replacing scenarios, such as pattern-based text substitutions.

### Use Cases:

This action can be used when you want to replace multiple values, but don't know where they are located or don't have the time to locate all of them. For example:
- Data Sanitization: Replacing or anonymizing sensitive information from objects within a dataset.
- Data Standardization: Ensuring that all data entries adhere to a uniform format or set of terminologies.

### Example

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.87165775% + 32px)"><iframe src="https://www.guidejar.com/embed/b8136d72-51b0-4edc-b3d9-f9d58a49a4c0?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

## Replace Multiple Values by Key

### Overview:

This action facilitates targeted replacements within an object based on a set of predefined mappings between keys. These mappings dictate which values from specified keys ('From Path') should be replaced by values from other keys ('To Path').

### Property Description

1. **Input**: This defines your input value. It can be inputted manually or using data pills, which is its intended use case.
2. **Output**: This defines your output value.
3. **Mappings**: Each mapping requires two parameters:
   - **From Path**: Indicates the key path in the Input object where the current value is located.
   - **To Path**: Points to the key path in the Output object whose value will replace the 'From Path' value.
     - Dot Notation: Paths to both keys within are specified in dot notation. This allows for precise targeting and modification of entries, even within deeply nested structures.

### Use Cases:
This method is especially useful for restructuring or transforming the structure of complex data objects without altering their inherent data format. For example:

- Data Integration: Useful in integrating systems where data from one system needs to be mapped to the schema of another system, hence facilitating smoother data interoperability.
- Configuration Overrides: Allows dynamic adjustments of configurations within software systems where properties from one part of a configuration object need to be replaced with those from another based on varying operational conditions or business rules.

### Example

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.87165775% + 32px)"><iframe src="https://www.guidejar.com/embed/4da6cdd8-d5e1-40ed-b8b6-b54a88369f6d?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

## Map Objects to Object

### Overview:

This action is a transformation technique designed to selectively project and potentially rename properties from input data, creating a new, simplified object. This action is highly configurable, allowing decisions on inclusion based on mappings, and handling of unmapped fields, nulls, and empty strings.

### Property Description

1. **Input Type**: Indicates whether the provided data is a single object or an array of objects.
2. **Input**: An object that contains various properties to be mapped and filtered based on specified conditions.
3. **Mapping**: An array of objects where each object includes properties 'From Path', 'To', and 'Required Field'. This directs how each property in the input is processed and renamed in the output. 
   - **From Path**: Specifies the current location of the property using dot notation, which is useful for locating nested values. 
   - **To**: The parameter allows users to rename properties in the output object, providing flexibility in structuring the output.
   - **Required Field**: The parameter can indicate which fields are mandatory for the function to process. If the field doesn't have a value, an exception is thrown. Default value is false.
4. **Include Unmapped**: Specifies whether properties not included in mappings should nevertheless appear in the output object. Default value is false.
5. **Include Nulls**: Determines if properties with null values should be included in the output. Default value is true.
6. **Include Empty Strings**: Indicates whether to include properties that have empty strings as values in the output object. Default value is true.

### Use Cases:

- Data Cleaning and Structuring: Perfect for restructuring incoming data streams to fit the schema expected by downstream systems or databases.
- API Data Preparation: Useful in preparing data received from external APIs, where only specific information is needed, or names need standardization.
- Configurations and Settings Management: Can be used to selectively extract and rename settings from complex nested configuration objects, making it easier to manage application settings.

[//]: # (### Example)
[//]: # ()
[//]: # (TODO)

## Map Objects to Array

### Overview:
This action is designed to convert a given single object or an array of objects into an array composed of key-value pairs. This transformation facilitates easier manipulation, aggregation, or visualization of nested or complex object data by flattening it into a more accessible, tabular format.

### Property Description

1. **Input Type**: Defines whether the input provided is a single object or an array of objects.
2. **Input**: An input object containing one or more properties that will be transformed into an array of key-value pairs.
3. **Field Key**: Specifies the property key in each newly created object within the output array. The value of this key in the new object will be the name (key) of a property from the input object.
4. **Value Key**: Specifies the property key in each newly created object within the output array that holds the property value from the input object.

### Use Cases:

- Data Transformation: Useful for data processing tasks where object structures need to be simplified or normalized for further processing, such as in analytics or reporting tools that require flat data structures.
- API Response Transformation: Ideal for transforming complex JSON structures received from API calls into a format that is easier to manage or display in user interfaces.
- Database Loading: Assists in the preparation of data for loading into databases that are optimized for handling flat structures, such as relational databases or certain types of NoSQL databases.

### Example

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(63.92358560% + 32px)"><iframe src="https://www.guidejar.com/embed/901fe11a-f79e-474f-9a23-8036fdcfca01?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

## Merge and Pivot Properties by Key

### Overview:
This action transforms an array of objects into a new, consolidated object based on a specified key. This action enables the aggregation and reorganization of data where each unique key becomes a single property in the resultant object, and the associated values are formed into sub-properties based on other shared properties in the input objects.

### Property Description

1. **Input**: This property takes an array of objects. Each object should have key-value pairs that need to be evaluated and merged based on shared keys.
2. **Field Key**: This is the key based on which the input objects are analyzed and merged. For each unique value of this key found across objects, a new property is created in the resultant object.
3. **Field Value**: This is the value that every property in each sub-object of the resultant main object will have. Essentially, it defines the static value assigned to each key derived during the merging process.

### Use Cases:

- Data Aggregation: Useful in scenarios where there is a need to aggregate information that shares common identifiers across multiple records, such as combining different attributes of products listed by the same identifier across various datasets.
- Analytical Reporting: Facilitates the creation of pivoted data structures that are often required in analytical reporting and data visualization to summarize data effectively.
- Configuration Management: In systems configurations, merging different configuration objects based on a common identifier can simplify the management and deployment of configuration settings.

### Example

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.24754902% + 32px)"><iframe src="https://www.guidejar.com/embed/2bda7178-8fa4-48af-9526-b5192d0b8fb1?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>
