
# ApiConnectorEndpoint

An API connector.

## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`description` | string
`httpMethod` | [HttpMethod](HttpMethod.md)
`id` | number
`lastExecutionDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`path` | string
`version` | number

## Example

```typescript
import type { ApiConnectorEndpoint } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "description": null,
  "httpMethod": null,
  "id": null,
  "lastExecutionDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "path": null,
  "version": null,
} satisfies ApiConnectorEndpoint

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ApiConnectorEndpoint
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


