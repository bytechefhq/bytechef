
# ConnectionDefinitionBasic

Definition of a connection to an outside service.

## Properties

Name | Type
------------ | -------------
`componentDescription` | string
`componentName` | string
`componentTitle` | string
`version` | number

## Example

```typescript
import type { ConnectionDefinitionBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "componentDescription": null,
  "componentName": null,
  "componentTitle": null,
  "version": null,
} satisfies ConnectionDefinitionBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectionDefinitionBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


