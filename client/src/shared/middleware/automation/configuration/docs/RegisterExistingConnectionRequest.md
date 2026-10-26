
# RegisterExistingConnectionRequest

Contains all required information to register a connection backed by an externally-provisioned credential.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`connectionVersion` | number
`credentialRef` | string
`credentialStoreType` | string
`environmentId` | number
`name` | string
`tags` | [Array&lt;Tag&gt;](Tag.md)
`workspaceId` | number

## Example

```typescript
import type { RegisterExistingConnectionRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "connectionVersion": null,
  "credentialRef": null,
  "credentialStoreType": null,
  "environmentId": null,
  "name": null,
  "tags": null,
  "workspaceId": null,
} satisfies RegisterExistingConnectionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RegisterExistingConnectionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


