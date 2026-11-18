
# OAuth2


## Properties

Name | Type
------------ | -------------
`authorizationUrl` | string
`extraQueryParameters` | { [key: string]: string; }
`clientId` | string
`redirectUri` | string
`scopes` | { [key: string]: string; }

## Example

```typescript
import type { OAuth2 } from ''

// TODO: Update the object below with actual values
const example = {
  "authorizationUrl": null,
  "extraQueryParameters": null,
  "clientId": null,
  "redirectUri": null,
  "scopes": null,
} satisfies OAuth2

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as OAuth2
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


