
# WorkflowNodeOutput

The workflow node output

## Properties

Name | Type
------------ | -------------
`actionDefinition` | [ActionDefinitionBasic](ActionDefinitionBasic.md)
`clusterElementDefinition` | [ClusterElementDefinitionBasic](ClusterElementDefinitionBasic.md)
`outputResponse` | [OutputResponse](OutputResponse.md)
`taskDispatcherDefinition` | [TaskDispatcherDefinitionBasic](TaskDispatcherDefinitionBasic.md)
`testOutputResponse` | boolean
`triggerDefinition` | [TriggerDefinitionBasic](TriggerDefinitionBasic.md)
`variableOutputResponse` | [OutputResponse](OutputResponse.md)
`workflowNodeName` | string

## Example

```typescript
import type { WorkflowNodeOutput } from ''

// TODO: Update the object below with actual values
const example = {
  "actionDefinition": null,
  "clusterElementDefinition": null,
  "outputResponse": null,
  "taskDispatcherDefinition": null,
  "testOutputResponse": null,
  "triggerDefinition": null,
  "variableOutputResponse": null,
  "workflowNodeName": null,
} satisfies WorkflowNodeOutput

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as WorkflowNodeOutput
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


