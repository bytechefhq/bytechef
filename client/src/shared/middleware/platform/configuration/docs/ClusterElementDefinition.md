
# ClusterElementDefinition

A cluster element definition.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`componentVersion` | number
`description` | string
`help` | [Help](Help.md)
`name` | string
`icon` | string
`outputDefined` | boolean
`outputFunctionDefined` | boolean
`outputSchemaDefined` | boolean
`title` | string
`type` | string
`properties` | [Array&lt;Property&gt;](Property.md)

## Example

```typescript
import type { ClusterElementDefinition } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "componentVersion": null,
  "description": null,
  "help": null,
  "name": null,
  "icon": null,
  "outputDefined": null,
  "outputFunctionDefined": null,
  "outputSchemaDefined": null,
  "title": null,
  "type": null,
  "properties": null,
} satisfies ClusterElementDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ClusterElementDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


