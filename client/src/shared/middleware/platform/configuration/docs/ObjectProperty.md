
# ObjectProperty

An object property type.

## Properties

Name | Type
------------ | -------------
`additionalProperties` | [Array&lt;Property&gt;](Property.md)
`defaultValue` | { [key: string]: any; }
`exampleValue` | { [key: string]: any; }
`multipleValues` | boolean
`options` | [Array&lt;Option&gt;](Option.md)
`optionsDataSource` | [OptionsDataSource](OptionsDataSource.md)
`properties` | [Array&lt;Property&gt;](Property.md)

## Example

```typescript
import type { ObjectProperty } from ''

// TODO: Update the object below with actual values
const example = {
  "additionalProperties": null,
  "defaultValue": null,
  "exampleValue": null,
  "multipleValues": null,
  "options": null,
  "optionsDataSource": null,
  "properties": null,
} satisfies ObjectProperty

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ObjectProperty
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


