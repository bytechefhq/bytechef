
# MissingInputError

Returned when a reference cannot be enabled because a required workflow input has no value yet.

## Properties

Name | Type
------------ | -------------
`missingInputName` | string

## Example

```typescript
import type { MissingInputError } from ''

// TODO: Update the object below with actual values
const example = {
  "missingInputName": null,
} satisfies MissingInputError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MissingInputError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


