
# AutomationWorkflowProjectWorkflowTemplate

A catalog workflow template within an automation workflow project.

## Properties

Name | Type
------------ | -------------
`id` | string
`label` | string
`description` | string
`components` | [Array&lt;AutomationWorkflowProjectComponent&gt;](AutomationWorkflowProjectComponent.md)

## Example

```typescript
import type { AutomationWorkflowProjectWorkflowTemplate } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "label": null,
  "description": null,
  "components": null,
} satisfies AutomationWorkflowProjectWorkflowTemplate

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AutomationWorkflowProjectWorkflowTemplate
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


