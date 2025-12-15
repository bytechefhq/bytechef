
# ScriptTestExecution

Contains information about test execution of a script.

## Properties

Name | Type
------------ | -------------
`error` | [ExecutionError](ExecutionError.md)
`output` | object

## Example

```typescript
import type { ScriptTestExecution } from ''

// TODO: Update the object below with actual values
const example = {
  "error": null,
  "output": null,
} satisfies ScriptTestExecution

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ScriptTestExecution
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


