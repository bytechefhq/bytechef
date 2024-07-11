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

