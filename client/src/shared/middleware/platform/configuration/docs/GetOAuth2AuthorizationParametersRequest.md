
# GetOAuth2AuthorizationParametersRequest

Contains all required information to open a connection to a service defined by componentName parameter.

## Properties

Name | Type
------------ | -------------
`authorizationType` | [AuthorizationType](AuthorizationType.md)
`componentName` | string
`connectionVersion` | number
`parameters` | { [key: string]: any; }

## Example

```typescript
import type { GetOAuth2AuthorizationParametersRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "authorizationType": null,
  "componentName": null,
  "connectionVersion": null,
  "parameters": null,
} satisfies GetOAuth2AuthorizationParametersRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as GetOAuth2AuthorizationParametersRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


