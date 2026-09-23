
# IntegrationBasic

A group of workflows that make one logical integration for a particular service represented by component.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`description` | string
`icon` | string
`id` | number
`integrationInstances` | [Array&lt;IntegrationInstance&gt;](IntegrationInstance.md)
`integrationVersion` | number
`multipleInstances` | boolean
`name` | string

## Example

```typescript
import type { IntegrationBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "description": null,
  "icon": null,
  "id": null,
  "integrationInstances": null,
  "integrationVersion": null,
  "multipleInstances": null,
  "name": null,
} satisfies IntegrationBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


