
# FocusPathElement

An element of the path from a top-level task execution to the task execution to focus.

## Properties

Name | Type
------------ | -------------
`childIndex` | number
`iterationIndex` | number
`taskExecutionId` | number

## Example

```typescript
import type { FocusPathElement } from ''

// TODO: Update the object below with actual values
const example = {
  "childIndex": null,
  "iterationIndex": null,
  "taskExecutionId": null,
} satisfies FocusPathElement

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as FocusPathElement
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


