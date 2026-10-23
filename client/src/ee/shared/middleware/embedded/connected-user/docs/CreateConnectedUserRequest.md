
# CreateConnectedUserRequest


## Properties

Name | Type
------------ | -------------
`email` | string
`environmentId` | number
`externalId` | string
`metadata` | { [key: string]: string; }
`name` | string

## Example

```typescript
import type { CreateConnectedUserRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "email": null,
  "environmentId": null,
  "externalId": null,
  "metadata": null,
  "name": null,
} satisfies CreateConnectedUserRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateConnectedUserRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


