
# IntegrationInstanceBasic

The integration instance represents a configured integration for a specific user, containing connection and status information

## Properties

Name | Type
------------ | -------------
`id` | number
`credentialStatus` | [CredentialStatus](CredentialStatus.md)
`enabled` | boolean

## Example

```typescript
import type { IntegrationInstanceBasic } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "credentialStatus": null,
  "enabled": null,
} satisfies IntegrationInstanceBasic

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as IntegrationInstanceBasic
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


