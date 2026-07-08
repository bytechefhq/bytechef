
# BillingSubscription

A billing subscription.

## Properties

Name | Type
------------ | -------------
`planName` | string
`status` | string
`taskLimit` | number
`currentPeriodEnd` | Date
`cancelAtPeriodEnd` | boolean
`scheduledPlanName` | string
`tasksUsed` | number

## Example

```typescript
import type { BillingSubscription } from ''

// TODO: Update the object below with actual values
const example = {
  "planName": null,
  "status": null,
  "taskLimit": null,
  "currentPeriodEnd": null,
  "cancelAtPeriodEnd": null,
  "scheduledPlanName": null,
  "tasksUsed": null,
} satisfies BillingSubscription

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as BillingSubscription
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


