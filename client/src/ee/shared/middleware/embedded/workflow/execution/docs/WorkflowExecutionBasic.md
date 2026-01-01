
# WorkflowExecutionBasic

Contains information about execution of a Integration workflow.

## Properties

Name | Type
------------ | -------------
`id` | number
`integration` | [IntegrationBasic](IntegrationBasic.md)
`integrationInstanceConfiguration` | [IntegrationInstanceConfigurationBasic](IntegrationInstanceConfigurationBasic.md)
`integrationInstance` | [IntegrationInstanceBasic](IntegrationInstanceBasic.md)
`job` | [JobBasic](JobBasic.md)
`workflow` | [WorkflowBasic](WorkflowBasic.md)

## Example

```typescript
import type { WorkflowExecutionBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "integration": null,
  "integrationInstanceConfiguration": null,
  "integrationInstance": null,
  "job": null,
  "workflow": null,
} satisfies WorkflowExecutionBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowExecutionBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


