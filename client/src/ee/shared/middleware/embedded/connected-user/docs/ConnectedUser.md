
# ConnectedUser


## Properties

Name | Type
------------ | -------------
`createdBy` | string
`createdDate` | Date
`email` | string
`enabled` | boolean
`environmentId` | number
`externalId` | string
`id` | number
`integrationInstances` | [Array&lt;ConnectedUserIntegrationInstance&gt;](ConnectedUserIntegrationInstance.md)
`metadata` | { [key: string]: any; }
`name` | string
`lastModifiedBy` | string
`lastModifiedDate` | Date
`version` | number

## Example

```typescript
import type { ConnectedUser } from ''

// TODO: Update the object below with actual values
const example = {
  "createdBy": null,
  "createdDate": null,
  "email": null,
  "enabled": null,
  "environmentId": null,
  "externalId": null,
  "id": null,
  "integrationInstances": null,
  "metadata": null,
  "name": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "version": null,
} satisfies ConnectedUser

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectedUser
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


