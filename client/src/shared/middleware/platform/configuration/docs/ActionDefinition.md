
# ActionDefinition

An action is a portion of reusable code that accomplish a specific task. When building a workflow, each action is represented as a task inside the workflow. The task \'type\' property is defined as [component name]/v[component version]/[action name]. Action properties are used to set properties of the task inside the workflow.

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
`sseStreamResponse` | boolean
`title` | string
`workflowNodeDescriptionDefined` | boolean

## Example

```typescript
import type { ActionDefinition } from ''

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
  "sseStreamResponse": null,
  "title": null,
  "workflowNodeDescriptionDefined": null,
} satisfies ActionDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ActionDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


