
# ConnectionDefinition

Definition of a connection to an outside service.

## Properties

Name | Type
------------ | -------------
`authorizationRequired` | boolean
`authorizations` | [Array&lt;Authorization&gt;](Authorization.md)
`baseUri` | string
`componentDescription` | string
`componentName` | string
`properties` | [Array&lt;Property&gt;](Property.md)
`componentTitle` | string
`version` | number

## Example

```typescript
import type { ConnectionDefinition } from ''

// TODO: Update the object below with actual values
const example = {
  "authorizationRequired": null,
  "authorizations": null,
  "baseUri": null,
  "componentDescription": null,
  "componentName": null,
  "properties": null,
  "componentTitle": null,
  "version": null,
} satisfies ConnectionDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectionDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


