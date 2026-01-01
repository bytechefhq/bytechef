
# UpdateTagsRequest

The request object that contains the array of tags.

## Properties

Name | Type
------------ | -------------
`tags` | [Array&lt;Tag&gt;](Tag.md)

## Example

```typescript
import type { UpdateTagsRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "tags": null,
} satisfies UpdateTagsRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateTagsRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


