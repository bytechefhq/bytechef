
# WorkflowExecution

Contains information about execution of a project workflow.

## Properties

Name | Type
------------ | -------------
`id` | number
`job` | [Job](Job.md)
`project` | [ProjectBasic](ProjectBasic.md)
`projectDeployment` | [ProjectDeploymentBasic](ProjectDeploymentBasic.md)
`triggerExecution` | [TriggerExecution](TriggerExecution.md)
`workflow` | [WorkflowBasic](WorkflowBasic.md)

## Example

```typescript
import type { WorkflowExecution } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "job": null,
  "project": null,
  "projectDeployment": null,
  "triggerExecution": null,
  "workflow": null,
} satisfies WorkflowExecution

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowExecution
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


