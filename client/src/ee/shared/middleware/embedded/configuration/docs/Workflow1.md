
# Workflow1

The blueprint that describe the execution of a job.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`connectionsCount` | number
`definition` | string
`description` | string
`format` | [WorkflowFormat](WorkflowFormat.md)
`id` | string
`inputs` | [Array&lt;WorkflowInput&gt;](WorkflowInput.md)
`inputsCount` | number
`label` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`outputs` | [Array&lt;WorkflowOutput&gt;](WorkflowOutput.md)
`sourceType` | string
`maxRetries` | number
`workflowTaskComponentNames` | Array&lt;string&gt;
`workflowTriggerComponentNames` | Array&lt;string&gt;
`tasks` | [Array&lt;WorkflowTask&gt;](WorkflowTask.md)
`triggers` | [Array&lt;WorkflowTrigger&gt;](WorkflowTrigger.md)
`version` | number

## Example

```typescript
import type { Workflow1 } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "connectionsCount": null,
  "definition": null,
  "description": null,
  "format": null,
  "id": null,
  "inputs": null,
  "inputsCount": null,
  "label": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "outputs": null,
  "sourceType": null,
  "maxRetries": null,
  "workflowTaskComponentNames": null,
  "workflowTriggerComponentNames": null,
  "tasks": null,
  "triggers": null,
  "version": null,
} satisfies Workflow1

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Workflow1
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


