
# NotificationTestResult

The outcome of a test send of a notification.

## Properties

Name | Type
------------ | -------------
`success` | boolean
`statusCode` | number
`responseBody` | string
`error` | string
`signed` | boolean
`unsignedReason` | string

## Example

```typescript
import type { NotificationTestResult } from ''

// TODO: Update the object below with actual values
const example = {
  "success": null,
  "statusCode": null,
  "responseBody": null,
  "error": null,
  "signed": null,
  "unsignedReason": null,
} satisfies NotificationTestResult

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as NotificationTestResult
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


