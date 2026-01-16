
# ProjectDeploymentWorkflow

Contains configuration and connections required for the execution of a particular project workflow.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`inputs` | { [key: string]: any; }
`connections` | [Array&lt;ProjectDeploymentWorkflowConnection&gt;](ProjectDeploymentWorkflowConnection.md)
`enabled` | boolean
`id` | number
`lastExecutionDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`projectDeploymentId` | number
`staticWebhookUrl` | string
`workflowId` | string
`workflowUuid` | string
`version` | number

## Example

```typescript
import type { ProjectDeploymentWorkflow } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "inputs": null,
  "connections": null,
  "enabled": null,
  "id": null,
  "lastExecutionDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "projectDeploymentId": null,
  "staticWebhookUrl": null,
  "workflowId": null,
  "workflowUuid": null,
  "version": null,
} satisfies ProjectDeploymentWorkflow

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProjectDeploymentWorkflow
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


