
# StringProperty

A string property.

## Properties

Name | Type
------------ | -------------
`languageId` | string
`defaultValue` | string
`exampleValue` | string
`maxLength` | number
`minLength` | number
`regex` | string
`options` | [Array&lt;Option&gt;](Option.md)
`optionsDataSource` | [OptionsDataSource](OptionsDataSource.md)
`optionsLoadedDynamically` | boolean

## Example

```typescript
import type { StringProperty } from ''

// TODO: Update the object below with actual values
const example = {
  "languageId": null,
  "defaultValue": null,
  "exampleValue": null,
  "maxLength": null,
  "minLength": null,
  "regex": null,
  "options": null,
  "optionsDataSource": null,
  "optionsLoadedDynamically": null,
} satisfies StringProperty

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as StringProperty
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


