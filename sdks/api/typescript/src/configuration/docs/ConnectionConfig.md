
# ConnectionConfig

The connection configuration

## Properties

Name | Type
------------ | -------------
`authorizationType` | [AuthorizationType](AuthorizationType.md)
`inputs` | [Array&lt;Input&gt;](Input.md)
`oauth2` | [OAuth2](OAuth2.md)

## Example

```typescript
import type { ConnectionConfig } from ''

// TODO: Update the object below with actual values
const example = {
  "authorizationType": null,
  "inputs": null,
  "oauth2": null,
} satisfies ConnectionConfig

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectionConfig
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


