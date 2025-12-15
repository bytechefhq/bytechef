
# ClusterElementType

A type of a cluster element.

## Properties

Name | Type
------------ | -------------
`name` | string
`label` | string
`required` | boolean
`multipleElements` | boolean

## Example

```typescript
import type { ClusterElementType } from ''

// TODO: Update the object below with actual values
const example = {
  "name": null,
  "label": null,
  "required": null,
  "multipleElements": null,
} satisfies ClusterElementType

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ClusterElementType
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


