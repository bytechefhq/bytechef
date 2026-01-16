
# WorkflowTestExecution

Contains information about test execution of a workflow.

## Properties

Name | Type
------------ | -------------
`job` | [Job](Job.md)
`triggerExecution` | [TriggerExecution](TriggerExecution.md)

## Example

```typescript
import type { WorkflowTestExecution } from ''

// TODO: Update the object below with actual values
const example = {
  "job": null,
  "triggerExecution": null,
} satisfies WorkflowTestExecution

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowTestExecution
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


