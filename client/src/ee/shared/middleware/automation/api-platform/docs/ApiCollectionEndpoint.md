
# ApiCollectionEndpoint

An API collection endpoint.

## Properties

Name | Type
------------ | -------------
`apiCollectionId` | number
`createdBy` | string
`createdDate` | Date
`enabled` | boolean
`httpMethod` | [HttpMethod](HttpMethod.md)
`id` | number
`name` | string
`lastExecutionDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`path` | string
`projectDeploymentWorkflowId` | number
`workflowUuid` | string
`version` | number

## Example

```typescript
import type { ApiCollectionEndpoint } from ''

// TODO: Update the object below with actual values
const example = {
  "apiCollectionId": null,
  "createdBy": null,
  "createdDate": null,
  "enabled": null,
  "httpMethod": null,
  "id": null,
  "name": null,
  "lastExecutionDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "path": null,
  "projectDeploymentWorkflowId": null,
  "workflowUuid": null,
  "version": null,
} satisfies ApiCollectionEndpoint

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiCollectionEndpoint
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


