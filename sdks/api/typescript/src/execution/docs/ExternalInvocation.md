
# ExternalInvocation

A record of a single external invocation.

## Properties

Name | Type
------------ | -------------
`componentName` | string
`componentVersion` | number
`createdDate` | Date
`durationMs` | number
`errorMessage` | string
`errorType` | string
`id` | number
`integrationInstanceId` | number
`jobId` | number
`kind` | [ExternalInvocationKind](ExternalInvocationKind.md)
`operationName` | string
`outcome` | [ExternalInvocationOutcome](ExternalInvocationOutcome.md)
`source` | [ExternalInvocationSource](ExternalInvocationSource.md)
`toolName` | string

## Example

```typescript
import type { ExternalInvocation } from ''

// TODO: Update the object below with actual values
const example = {
  "componentName": null,
  "componentVersion": null,
  "createdDate": null,
  "durationMs": null,
  "errorMessage": null,
  "errorType": null,
  "id": null,
  "integrationInstanceId": null,
  "jobId": null,
  "kind": null,
  "operationName": null,
  "outcome": null,
  "source": null,
  "toolName": null,
} satisfies ExternalInvocation

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ExternalInvocation
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


