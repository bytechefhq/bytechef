
# UpdateConnectionRequest

Contains all connection parameters that can be updated.

## Properties

Name | Type
------------ | -------------
`name` | string
`tags` | [Array&lt;Tag&gt;](Tag.md)
`version` | number

## Example

```typescript
import type { UpdateConnectionRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "name": null,
  "tags": null,
  "version": null,
} satisfies UpdateConnectionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateConnectionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


