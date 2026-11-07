
# EmbeddedWorkflowExecution

An integration or an automation workflow execution of a connected user.

## Properties

Name | Type
------------ | -------------
`automationWorkflowExecution` | object
`integrationWorkflowExecution` | [WorkflowExecutionBasic](WorkflowExecutionBasic.md)
`source` | string

## Example

```typescript
import type { EmbeddedWorkflowExecution } from ''

// TODO: Update the object below with actual values
const example = {
  "automationWorkflowExecution": null,
  "integrationWorkflowExecution": null,
  "source": null,
} satisfies EmbeddedWorkflowExecution

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as EmbeddedWorkflowExecution
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


