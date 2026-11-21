
# ComponentOperationPropertyRequest


## Properties

Name | Type
------------ | -------------
`clusterElementType` | string
`clusterElementWorkflowNodeName` | string
`componentName` | string
`componentVersion` | number
`connectionKey` | string
`lookupDependsOnPaths` | Array&lt;string&gt;
`operationName` | string
`operationType` | string
`parameters` | { [key: string]: any; }

## Example

```typescript
import type { ComponentOperationPropertyRequest } from ''

// TODO: Update the object below with actual values
const example = {
  "clusterElementType": null,
  "clusterElementWorkflowNodeName": null,
  "componentName": null,
  "componentVersion": null,
  "connectionKey": null,
  "lookupDependsOnPaths": null,
  "operationName": null,
  "operationType": null,
  "parameters": null,
} satisfies ComponentOperationPropertyRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ComponentOperationPropertyRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


