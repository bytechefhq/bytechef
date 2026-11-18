
# ExternalInvocationPage

A page of a connected user\'s external invocations.

## Properties

Name | Type
------------ | -------------
`content` | [Array&lt;ExternalInvocation&gt;](ExternalInvocation.md)
`number` | number
`numberOfElements` | number
`size` | number
`totalElements` | number
`totalPages` | number

## Example

```typescript
import type { ExternalInvocationPage } from ''

// TODO: Update the object below with actual values
const example = {
  "content": null,
  "number": null,
  "numberOfElements": null,
  "size": null,
  "totalElements": null,
  "totalPages": null,
} satisfies ExternalInvocationPage

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ExternalInvocationPage
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


