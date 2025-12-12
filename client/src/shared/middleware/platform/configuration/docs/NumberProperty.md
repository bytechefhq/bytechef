
# NumberProperty

A number property type.

## Properties

Name | Type
------------ | -------------
`defaultValue` | number
`exampleValue` | number
`maxNumberPrecision` | number
`maxValue` | number
`minNumberPrecision` | number
`minValue` | number
`numberPrecision` | number
`options` | [Array&lt;Option&gt;](Option.md)
`optionsDataSource` | [OptionsDataSource](OptionsDataSource.md)

## Example

```typescript
import type { NumberProperty } from ''

// TODO: Update the object below with actual values
const example = {
  "defaultValue": null,
  "exampleValue": null,
  "maxNumberPrecision": null,
  "maxValue": null,
  "minNumberPrecision": null,
  "minValue": null,
  "numberPrecision": null,
  "options": null,
  "optionsDataSource": null,
} satisfies NumberProperty

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as NumberProperty
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


