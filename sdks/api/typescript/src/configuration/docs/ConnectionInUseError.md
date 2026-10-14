
# ConnectionInUseError

Returned when a connection cannot be deleted because it is still used by an automation.

## Properties

Name | Type
------------ | -------------
`reason` | string

## Example

```typescript
import type { ConnectionInUseError } from ''

// TODO: Update the object below with actual values
const example = {
  "reason": null,
} satisfies ConnectionInUseError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ConnectionInUseError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


