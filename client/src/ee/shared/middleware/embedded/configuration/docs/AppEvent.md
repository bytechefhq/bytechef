
# AppEvent

An app event.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`id` | number
`name` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`schema` | string
`version` | number

## Example

```typescript
import type { AppEvent } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "id": null,
  "name": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "schema": null,
  "version": null,
} satisfies AppEvent

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as AppEvent
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


