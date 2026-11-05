
# ErrorsInnerMeta

Additional metadata about the error.

## Properties

Name | Type
------------ | -------------
`cause` | object
`origin` | string
`applicationName` | string

## Example

```typescript
import type { ErrorsInnerMeta } from ''

// TODO: Update the object below with actual values
const example = {
  "cause": null,
  "origin": remote-provider,
  "applicationName": MyCompany Production,
} satisfies ErrorsInnerMeta

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ErrorsInnerMeta
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


