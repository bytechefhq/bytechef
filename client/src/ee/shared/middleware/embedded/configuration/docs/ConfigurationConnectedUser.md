
# ConfigurationConnectedUser

A connected user with an integration instance on a configuration.

## Properties

Name | Type
------------ | -------------
`connectedUserId` | number
`email` | string
`externalId` | string
`name` | string

## Example

```typescript
import type { ConfigurationConnectedUser } from ''

// TODO: Update the object below with actual values
const example = {
  "connectedUserId": null,
  "email": null,
  "externalId": null,
  "name": null,
} satisfies ConfigurationConnectedUser

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConfigurationConnectedUser
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


