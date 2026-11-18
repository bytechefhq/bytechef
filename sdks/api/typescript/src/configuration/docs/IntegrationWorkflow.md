
# IntegrationWorkflow

A group of tasks that make one logical workflow.

## Properties

Name | Type
------------ | -------------
`description` | string
`inputs` | [Array&lt;Input&gt;](Input.md)
`label` | string
`workflowUuid` | string

## Example

```typescript
import type { IntegrationWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "description": null,
  "inputs": null,
  "label": null,
  "workflowUuid": null,
} satisfies IntegrationWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


