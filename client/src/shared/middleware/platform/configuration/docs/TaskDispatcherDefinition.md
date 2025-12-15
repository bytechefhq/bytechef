
# TaskDispatcherDefinition

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
`properties` | [Array&lt;Property&gt;](Property.md)
`resources` | [Resources](Resources.md)
`taskProperties` | [Array&lt;Property&gt;](Property.md)
`title` | string
`variablePropertiesDefined` | boolean
`version` | number

## Example

```typescript
import type { TaskDispatcherDefinition } from ''

// TODO: Update the object below with actual values
const example = {
  "description": null,
  "icon": null,
  "name": null,
  "outputDefined": null,
  "outputFunctionDefined": null,
  "outputSchemaDefined": null,
  "properties": null,
  "resources": null,
  "taskProperties": null,
  "title": null,
  "variablePropertiesDefined": null,
  "version": null,
} satisfies TaskDispatcherDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskDispatcherDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


