
# ProjectBasic

A group of workflows that make one logical project.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`description` | string
`id` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`lastPublishedDate` | Date
`lastStatus` | [ProjectStatus](ProjectStatus.md)
`lastProjectVersion` | number
`uuid` | string

## Example

```typescript
import type { ProjectBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "id": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "lastPublishedDate": null,
  "lastStatus": null,
  "lastProjectVersion": null,
  "uuid": null,
} satisfies ProjectBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProjectBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


