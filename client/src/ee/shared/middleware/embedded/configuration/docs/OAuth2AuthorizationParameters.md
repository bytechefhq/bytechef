
# OAuth2AuthorizationParameters

The parameters needed to start an OAuth2 authorization flow.

## Properties

Name | Type
------------ | -------------
`authorizationUrl` | string
`clientId` | string
`extraQueryParameters` | { [key: string]: string; }
`scopes` | { [key: string]: boolean; }

## Example

```typescript
import type { OAuth2AuthorizationParameters } from ''

// TODO: Update the object below with actual values
const example = {
  "authorizationUrl": null,
  "clientId": null,
  "extraQueryParameters": null,
  "scopes": null,
} satisfies OAuth2AuthorizationParameters

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as OAuth2AuthorizationParameters
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


