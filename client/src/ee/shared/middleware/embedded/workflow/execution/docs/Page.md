
# Page

A sublist of a list of objects. It allows gain information about the position of it in the containing entire list.

## Properties

Name | Type
------------ | -------------
`number` | number
`size` | number
`numberOfElements` | number
`totalPages` | number
`totalElements` | number
`content` | Array&lt;object&gt;

## Example

```typescript
import type { Page } from ''

// TODO: Update the object below with actual values
const example = {
  "number": null,
  "size": null,
  "numberOfElements": null,
  "totalPages": null,
  "totalElements": null,
  "content": null,
} satisfies Page

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Page
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


