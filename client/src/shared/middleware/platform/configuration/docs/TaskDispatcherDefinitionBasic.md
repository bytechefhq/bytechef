
# TaskDispatcherDefinitionBasic

A task dispatcher defines a strategy for dispatching tasks to be executed.

## Properties

Name | Type
------------ | -------------
`description` | string
`icon` | string
`name` | string
`outputDefined` | boolean
`outputFunctionDefined` | boolean
`outputSchemaDefined` | boolean
`resources` | [Resources](Resources.md)
`title` | string
`variablePropertiesDefined` | boolean
`version` | number

## Example

```typescript
import type { TaskDispatcherDefinitionBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "description": null,
  "icon": null,
  "name": null,
  "outputDefined": null,
  "outputFunctionDefined": null,
  "outputSchemaDefined": null,
  "resources": null,
  "title": null,
  "variablePropertiesDefined": null,
  "version": null,
} satisfies TaskDispatcherDefinitionBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskDispatcherDefinitionBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


