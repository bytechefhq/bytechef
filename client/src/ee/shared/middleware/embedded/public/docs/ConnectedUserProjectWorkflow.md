
# ConnectedUserProjectWorkflow

A group of tasks that make one logical workflow.

## Properties

Name | Type
------------ | -------------
`createdDate` | Date
`description` | string
`definition` | string
`lastModifiedDate` | Date
`enabled` | boolean
`label` | string
`workflowUuid` | string
`workflowVersion` | number
`kind` | string
`automationUuid` | string
`copiedFromWorkflowUuid` | string
`dangling` | boolean
`components` | [Array&lt;AutomationProjectComponent&gt;](AutomationProjectComponent.md)
`inputs` | [Array&lt;AutomationProjectWorkflowInput&gt;](AutomationProjectWorkflowInput.md)
`inputValues` | { [key: string]: any; }
`connections` | { [key: string]: number; }
`attentionReason` | string

## Example

```typescript
import type { ConnectedUserProjectWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "createdDate": null,
  "description": null,
  "definition": null,
  "lastModifiedDate": null,
  "enabled": null,
  "label": null,
  "workflowUuid": null,
  "workflowVersion": null,
  "kind": null,
  "automationUuid": null,
  "copiedFromWorkflowUuid": null,
  "dangling": null,
  "components": null,
  "inputs": null,
  "inputValues": null,
  "connections": null,
  "attentionReason": null,
} satisfies ConnectedUserProjectWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectedUserProjectWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


