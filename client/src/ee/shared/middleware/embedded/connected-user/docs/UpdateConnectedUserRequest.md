
# UpdateConnectedUserRequest


## Properties

Name | Type
------------ | -------------
`email` | string
`metadata` | { [key: string]: string; }
`name` | string
`version` | number

## Example

```typescript
import type { UpdateConnectedUserRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "email": null,
  "metadata": null,
  "name": null,
  "version": null,
} satisfies UpdateConnectedUserRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateConnectedUserRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


