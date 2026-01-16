
# ProjectGitConfiguration

The git configuration.

## Properties

Name | Type
------------ | -------------
`projectId` | number
`branch` | string
`enabled` | boolean
`createdBy` | string
`createdDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`version` | number

## Example

```typescript
import type { ProjectGitConfiguration } from ''

// TODO: Update the object below with actual values
const example = {
  "projectId": null,
  "branch": null,
  "enabled": null,
  "createdBy": null,
  "createdDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "version": null,
} satisfies ProjectGitConfiguration

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProjectGitConfiguration
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


