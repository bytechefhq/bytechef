
# IntegrationInstanceConfigurationWorkflow

Contains configuration and connections required for the execution of a particular integration workflow.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`inputs` | { [key: string]: any; }
`connections` | [Array&lt;IntegrationInstanceConfigurationWorkflowConnection&gt;](IntegrationInstanceConfigurationWorkflowConnection.md)
`enabled` | boolean
`id` | number
`integrationInstanceConfigurationId` | number
`lastExecutionDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`workflowId` | string
`workflowUuid` | string
`version` | number

## Example

```typescript
import type { IntegrationInstanceConfigurationWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "inputs": null,
  "connections": null,
  "enabled": null,
  "id": null,
  "integrationInstanceConfigurationId": null,
  "lastExecutionDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "workflowId": null,
  "workflowUuid": null,
  "version": null,
} satisfies IntegrationInstanceConfigurationWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationInstanceConfigurationWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


