
# TaskExecutionChildrenPage

A page of the children of a task execution. At most one of children and iterations is non-empty; both are empty on an empty page.

## Properties

Name | Type
------------ | -------------
`children` | [Array&lt;TaskExecution&gt;](TaskExecution.md)
`iterations` | Array&lt;Array&lt;TaskExecution&gt;&gt;
`number` | number
`numberOfElements` | number
`size` | number
`totalElements` | number
`totalPages` | number

## Example

```typescript
import type { TaskExecutionChildrenPage } from ''

// TODO: Update the object below with actual values
const example = {
  "children": null,
  "iterations": null,
  "number": null,
  "numberOfElements": null,
  "size": null,
  "totalElements": null,
  "totalPages": null,
} satisfies TaskExecutionChildrenPage

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskExecutionChildrenPage
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


