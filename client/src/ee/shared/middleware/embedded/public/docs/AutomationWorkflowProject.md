
# AutomationWorkflowProject

An automation workflow catalog project.

## Properties

Name | Type
------------ | -------------
`id` | number
`name` | string
`description` | string
`workflowTemplates` | [Array&lt;AutomationWorkflowProjectWorkflowTemplate&gt;](AutomationWorkflowProjectWorkflowTemplate.md)
`kind` | string

## Example

```typescript
import type { AutomationWorkflowProject } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "name": null,
  "description": null,
  "workflowTemplates": null,
  "kind": null,
} satisfies AutomationWorkflowProject

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AutomationWorkflowProject
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


