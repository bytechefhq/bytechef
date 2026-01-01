
# SigningKey

Contains generated public key used for signing JWT tokens.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`environmentId` | number
`id` | number
`keyId` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`lastUsedDate` | Date
`name` | string

## Example

```typescript
import type { SigningKey } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "environmentId": null,
  "id": null,
  "keyId": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "lastUsedDate": null,
  "name": null,
} satisfies SigningKey

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as SigningKey
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


