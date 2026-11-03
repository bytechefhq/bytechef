
# ConfigurationMoveImpactWorkflow

A workflow of the target configuration that needs new required inputs or connections.

## Properties

Name | Type
------------ | -------------
`affectedConnectedUserCount` | number
`label` | string
`newRequiredConnections` | Array&lt;string&gt;
`newRequiredInputs` | Array&lt;string&gt;
`workflowUuid` | string

## Example

```typescript
import type { ConfigurationMoveImpactWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "affectedConnectedUserCount": null,
  "label": null,
  "newRequiredConnections": null,
  "newRequiredInputs": null,
  "workflowUuid": null,
} satisfies ConfigurationMoveImpactWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConfigurationMoveImpactWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


