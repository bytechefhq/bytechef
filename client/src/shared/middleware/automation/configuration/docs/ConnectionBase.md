
# ConnectionBase

Contains all required information to open a connection to a service defined by componentName parameter.

## Properties

Name | Type
------------ | -------------
`active` | boolean
`authorizationType` | [AuthorizationType](AuthorizationType.md)
`authorizationParameters` | { [key: string]: any; }
`baseUri` | string
`componentName` | string
`connectionParameters` | { [key: string]: any; }
`connectionVersion` | number
`createdBy` | string
`createdDate` | Date
`credentialStatus` | [CredentialStatus](CredentialStatus.md)
`environmentId` | number
`id` | number
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`parameters` | { [key: string]: any; }
`tags` | [Array&lt;Tag&gt;](Tag.md)
`version` | number

## Example

```typescript
import type { ConnectionBase } from ''

// TODO: Update the object below with actual values
const example = {
  "active": null,
  "authorizationType": null,
  "authorizationParameters": null,
  "baseUri": null,
  "componentName": null,
  "connectionParameters": null,
  "connectionVersion": null,
  "createdBy": null,
  "createdDate": null,
  "credentialStatus": null,
  "environmentId": null,
  "id": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "parameters": null,
  "tags": null,
  "version": null,
} satisfies ConnectionBase

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectionBase
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


