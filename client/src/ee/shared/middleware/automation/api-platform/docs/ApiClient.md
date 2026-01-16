
# ApiClient

Contains generated key required for calling API.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`id` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`lastUsedDate` | Date
`name` | string
`secretKey` | string

## Example

```typescript
import type { ApiClient } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "id": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "lastUsedDate": null,
  "name": null,
  "secretKey": null,
} satisfies ApiClient

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiClient
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


