
# Notification

A Notification definition.

## Properties

Name | Type
------------ | -------------
`id` | number
`createdBy` | string
`createdDate` | Date
`lastModifiedBy` | string
`lastModifiedDate` | Date
`name` | string
`type` | string
`settings` | { [key: string]: object; }
`notificationEvents` | [Array&lt;NotificationEvent&gt;](NotificationEvent.md)
`notificationEventIds` | Array&lt;number&gt;
`version` | number

## Example

```typescript
import type { Notification } from ''

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "createdBy": null,
  "createdDate": null,
  "lastModifiedBy": null,
  "lastModifiedDate": null,
  "name": null,
  "type": null,
  "settings": null,
  "notificationEvents": null,
  "notificationEventIds": null,
  "version": null,
} satisfies Notification

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as Notification
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


