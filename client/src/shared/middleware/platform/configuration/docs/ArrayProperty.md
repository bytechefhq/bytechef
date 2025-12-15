
# ArrayProperty

An array property type.

## Properties

Name | Type
------------ | -------------
`defaultValue` | Array&lt;object&gt;
`exampleValue` | Array&lt;object&gt;
`items` | [Array&lt;Property&gt;](Property.md)
`maxItems` | number
`minItems` | number
`multipleValues` | boolean
`options` | [Array&lt;Option&gt;](Option.md)
`optionsDataSource` | [OptionsDataSource](OptionsDataSource.md)

## Example

```typescript
import type { ArrayProperty } from ''

// TODO: Update the object below with actual values
const example = {
  "defaultValue": null,
  "exampleValue": null,
  "items": null,
  "maxItems": null,
  "minItems": null,
  "multipleValues": null,
  "options": null,
  "optionsDataSource": null,
} satisfies ArrayProperty

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ArrayProperty
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


