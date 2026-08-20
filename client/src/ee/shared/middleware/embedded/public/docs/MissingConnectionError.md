
# MissingConnectionError

Returned when a required connection could not be auto-wired for a catalog code workflow reference.

## Properties

Name | Type
------------ | -------------
`missingConnectionComponentName` | string

## Example

```typescript
import type { MissingConnectionError } from ''

// TODO: Update the object below with actual values
const example = {
  "missingConnectionComponentName": null,
} satisfies MissingConnectionError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MissingConnectionError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


