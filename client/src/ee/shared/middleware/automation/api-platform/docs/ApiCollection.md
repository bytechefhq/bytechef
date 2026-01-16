
# ApiCollection

An API collection.

## Properties

Name | Type
------------ | -------------
`collectionVersion` | number
`contextPath` | string
`createdBy` | string
`createdDate` | Date
`description` | string
`enabled` | boolean
`endpoints` | [Array&lt;ApiCollectionEndpoint&gt;](ApiCollectionEndpoint.md)
`environmentId` | number
`id` | number
`name` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`projectId` | number
`project` | [ProjectBasic](ProjectBasic.md)
`projectDeploymentId` | number
`projectDeployment` | [ProjectDeploymentBasic](ProjectDeploymentBasic.md)
`projectVersion` | number
`tags` | [Array&lt;Tag&gt;](Tag.md)
`workspaceId` | number
`version` | number

## Example

```typescript
import type { ApiCollection } from ''

// TODO: Update the object below with actual values
const example = {
  "collectionVersion": null,
  "contextPath": null,
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "enabled": null,
  "endpoints": null,
  "environmentId": null,
  "id": null,
  "name": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "projectId": null,
  "project": null,
  "projectDeploymentId": null,
  "projectDeployment": null,
  "projectVersion": null,
  "tags": null,
  "workspaceId": null,
  "version": null,
} satisfies ApiCollection

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiCollection
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


