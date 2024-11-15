---
title: "Property"
---

### Array Property

The `ModifiableArrayProperty` class is a customizable property type designed to handle array values within a component.

- `array(String name)` - Initializes a new `ModifiableArrayProperty` with the specified name.
- `defaultValue(T... defaultValue)` - Sets the default value for the property using various data types such as Boolean, Integer, Long, Float, Double, String, or Map.
- `exampleValue(T... exampleValue)` - Provides an example value for illustrative purposes using various data types.
- `items(P.. properties)` - Specifies the properties that define the items in the array.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)` - Defines dependencies for option lookups.
- `maxItems(long maxItems)` - Sets the maximum number of items allowed in the array.
- `minItems(long minItems)` - Sets the minimum number of items required in the array.
- `multipleValues(boolean multipleValues)` - Indicates whether the array can contain multiple values.
- `options(Option<Object>... options)` - Specifies a list of options for the property.
- `options(OptionsFunction optionsFunction)` - Defines a function to dynamically generate options.

### Boolean Property

The `ModifiableBooleanProperty` class is a customizable property type designed to handle boolean values within a component.

- `bool(String name)` - Initializes a new `ModifiableBooleanProperty` with the specified name.
- `defaultValue(boolean defaultValue)` - Sets the default value for the property.
- `exampleValue(boolean exampleValue)` - Provides an example value for illustrative purposes.

### Date Property

The `ModifiableDateProperty` class is a customizable property type designed to handle date values within a component.

- `date(String name)` - Initializes a new `ModifiableDateProperty` with the specified name.
- `defaultValue(LocalDate defaultValue)` - Sets the default value for the property.
- `exampleValue(LocalDate exampleValue)` - Provides an example value for illustrative purposes.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)` - Defines dependencies for option lookups.
- `options(Option<LocalDate>... options)` - Specifies a list of options for the property.
- `options(OptionsFunction optionsFunction)` - Defines a function to dynamically generate options.

### Date Time Property

The `ModifiableDateTimeProperty` class is a customizable property type designed to handle date-time values within a component.

- `dateTime(String name)` - Initializes a new `ModifiableDateTimeProperty` with the specified name.
- `defaultValue(LocalDateTime defaultValue)` - Sets the default value for the property.
- `exampleValue(LocalDateTime exampleValue)` - Provides an example value for illustrative purposes.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)` - Defines dependencies for option lookups.
- `options(Option<LocalDateTime>... options)` - Specifies a list of options for the property.
- `options(OptionsFunction optionsFunction)` - Defines a function to dynamically generate options.

### File Entry Property

The `ModifiableFileEntryProperty` class is a customizable property type designed to handle file entry values within a component.

- `fileEntry(String name)` - Initializes a new `ModifiableFileEntryProperty` with the specified name.

### Integer Property

The `ModifiableIntegerProperty` class is a customizable property type designed to handle integer values within a component.

- `integer(String name)` - Initializes a new `ModifiableTimeProperty` with the specified name.
- `defaultValue(long value)` - Sets the default value for the property.
- `exampleValue(long exampleValue)` - Provides an example value for illustrative purposes.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)` - Defines dependencies for option lookups.
- `maxValue(long maxValue)` - Sets the maximum allowable value for the property.
- `minValue(long minValue)` - Sets the minimum allowable value for the property.
- `options(Option<Long>... options)` - Specifies a list of options for the property.
- `options(List<Option<Long>> options)` - Sets a list of options for the property.
- `options(OptionsFunction optionsFunction)` - Defines a function to dynamically generate options.

### Number Property

The `ModifiableNumberProperty` class is a customizable property type designed to handle numeric values within a component.

- `number(String name)` - Initializes a new `ModifiableNumberProperty` with the specified name.
- `defaultValue(...)` - Sets the default value for the property using various numeric types such as int, long, float, or double.
- `exampleValue(...)` - Provides an example value for illustrative purposes using various numeric types.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)` - Defines dependencies for option lookups.
- `maxNumberPrecision(Integer maxNumberPrecision)` - Sets the maximum precision for the number.
- `maxValue(double maxValue)` - Sets the maximum allowable value for the property.
- `minNumberPrecision(Integer minNumberPrecision)` - Sets the minimum precision for the number.
- `minValue(double minValue)` - Sets the minimum allowable value for the property.
- `numberPrecision(Integer numberPrecision)` - Specifies the precision for the number.
- `options(Option<Double>... options)` - Specifies a list of options for the property.
- `options(OptionsFunction optionsFunction)` - Defines a function to dynamically generate options.

### Object Property

The `ModifiableObjectProperty` class is a customizable property type designed to handle object values within a component.

- `object(String name)` - Initializes a new `ModifiableObjectProperty` with the specified name.
- `defaultValue(Map<String, Object> defaultValue)` - Sets the default value for the property.
- `exampleValue(Map<String, Object> exampleValue)` - Provides an example value for illustrative purposes.
- `additionalProperties(...)` - Specifies additional properties that can be included in the object.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)` - Defines dependencies for option lookups.
- `multipleValues(boolean multipleValues)` - Indicates whether the object can contain multiple values.
- `options(Option<Object>... options)` - Specifies a list of options for the property.
- `options(OptionsFunction optionsFunction)` - Defines a function to dynamically generate options.
- `properties(...)` - Specifies the properties that define the structure of the object.

### String Property

The `ModifiableStringProperty` class is a customizable property type designed to handle string values within a component.

- `string(String name)` - Initializes a new `ModifiableStringProperty` with the specified name.
- `controlType(ControlType controlType)`- Sets the control type for the property (e.g., TEXT, SELECT).
- `defaultValue(String value)`- Specifies the default value for the property.
- `exampleValue(String exampleValue)`- Provides an example value for illustrative purposes.
- `languageId(String languageId)`- Sets the language identifier for the property.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)`- Defines dependencies for option lookups.
- `maxLength(int maxLength)`- Sets the maximum length allowed for the string value.
- `minLength(int minLength)`- Sets the minimum length required for the string value.
- `options(Option<String>... options)`- Specifies a list of options for the property.
- `options(List<? extends Option<String>> options)`- Sets a list of options for the property.
- `options(OptionsFunction optionsFunction)`- Defines a function to dynamically generate options.

### Time Property

The `ModifiableTimeProperty` class is a customizable property type designed to handle time values within a component.

- `time(String name)` - Initializes a new `ModifiableTiimeProperty` with the specified name.
- `defaultValue(LocalTime defaultValue)`- Specifies the default value for the property.
- `exampleValue(LocalTime exampleValue)`- Provides an example value for illustrative purposes.
- `optionsLookupDependsOn(String... optionsLookupDependsOn)`- Defines dependencies for option lookups.
- `options(Option<LocalTime>... options)`- Specifies a list of options for the property.
- `options(List<? extends Option<LocalTime>> options)`- Sets a list of options for the property.
- `options(OptionsFunction optionsFunction)`- Defines a function to dynamically generate options.
