
# ProvidedValueLockedError

Returned when a request sets the connection of a component, or the value of an input, that the configuration provides for every connected user. Exactly one of the properties is present.

## Properties

Name | Type
------------ | -------------
`providedComponentName` | string
`providedInputName` | string

## Example

```typescript
import type { ProvidedValueLockedError } from ''

// TODO: Update the object below with actual values
const example = {
  "providedComponentName": null,
  "providedInputName": null,
} satisfies ProvidedValueLockedError

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProvidedValueLockedError
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


