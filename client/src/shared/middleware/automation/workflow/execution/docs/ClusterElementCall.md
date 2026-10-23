
# ClusterElementCall

The outcome of the calls a task made to one of its cluster elements.

## Properties

Name | Type
------------ | -------------
`endDate` | Date
`error` | string
`name` | string
`startDate` | Date
`status` | string

## Example

```typescript
import type { ClusterElementCall } from ''

// TODO: Update the object below with actual values
const example = {
  "endDate": null,
  "error": null,
  "name": null,
  "startDate": null,
  "status": null,
} satisfies ClusterElementCall

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ClusterElementCall
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


