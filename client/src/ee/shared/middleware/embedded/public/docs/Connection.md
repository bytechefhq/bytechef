
# Connection

Contains all required information to open a connection to a service defined by componentName parameter

## Properties

Name | Type
------------ | -------------
`id` | number
`name` | string
`environment` | [Environment](Environment.md)
`componentName` | string
`connectionVersion` | number
`authorizationType` | string
`createdDate` | Date

## Example

```typescript
import type { Connection } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "name": null,
  "environment": null,
  "componentName": null,
  "connectionVersion": null,
  "authorizationType": null,
  "createdDate": null,
} satisfies Connection

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Connection
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


