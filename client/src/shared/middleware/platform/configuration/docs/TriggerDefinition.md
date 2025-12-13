
# TriggerDefinition

A trigger definition defines ways to trigger workflows from the outside services.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`componentVersion` | number
`description` | string
`help` | [Help](Help.md)
`name` | string
`outputDefined` | boolean
`outputFunctionDefined` | boolean
`outputSchemaDefined` | boolean
`properties` | [Array&lt;Property&gt;](Property.md)
`title` | string
`type` | [TriggerType](TriggerType.md)
`workflowNodeDescriptionDefined` | boolean

## Example

```typescript
import type { TriggerDefinition } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "componentVersion": null,
  "description": null,
  "help": null,
  "name": null,
  "outputDefined": null,
  "outputFunctionDefined": null,
  "outputSchemaDefined": null,
  "properties": null,
  "title": null,
  "type": null,
  "workflowNodeDescriptionDefined": null,
} satisfies TriggerDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TriggerDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


