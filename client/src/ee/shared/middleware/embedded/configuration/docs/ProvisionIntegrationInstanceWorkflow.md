
# ProvisionIntegrationInstanceWorkflow

A workflow to enable on a provisioned integration instance.

## Properties

Name | Type
------------ | -------------
`workflowUuid` | string
`inputs` | { [key: string]: any; }

## Example

```typescript
import type { ProvisionIntegrationInstanceWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "workflowUuid": null,
  "inputs": null,
} satisfies ProvisionIntegrationInstanceWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProvisionIntegrationInstanceWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


