
# ProjectDeployment

Contains configurations and connections required for the execution of project workflows.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`description` | string
`enabled` | boolean
`environmentId` | number
`id` | number
`lastExecutionDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`projectId` | number
`projectVersion` | number
`project` | object
`projectDeploymentWorkflows` | [Array&lt;ProjectDeploymentWorkflow&gt;](ProjectDeploymentWorkflow.md)
`tags` | [Array&lt;Tag&gt;](Tag.md)
`version` | number

## Example

```typescript
import type { ProjectDeployment } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "enabled": null,
  "environmentId": null,
  "id": null,
  "lastExecutionDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "projectId": null,
  "projectVersion": null,
  "project": null,
  "projectDeploymentWorkflows": null,
  "tags": null,
  "version": null,
} satisfies ProjectDeployment

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProjectDeployment
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


