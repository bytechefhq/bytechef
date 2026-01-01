
# IntegrationInstanceWorkflow

Contains user configurations for the execution of a particular integration workflow.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`inputs` | { [key: string]: any; }
`enabled` | boolean
`id` | number
`integrationInstanceConfigurationWorkflowId` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`workflowId` | string

## Example

```typescript
import type { IntegrationInstanceWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "inputs": null,
  "enabled": null,
  "id": null,
  "integrationInstanceConfigurationWorkflowId": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "workflowId": null,
} satisfies IntegrationInstanceWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationInstanceWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


