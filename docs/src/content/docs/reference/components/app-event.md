---
title: "App Event"
description: "Use one event from your app tot trigger workflows across any integrations."
---

Use one event from your app tot trigger workflows across any integrations.


Categories: helpers


Type: appEvent/v1

<hr />






## Triggers


### New Event
Name: newEvent

Triggers when new app event is sent.

Type: STATIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| appEventId | App Event Id | INTEGER | SELECT | The Id of an app event. | null |


#### JSON Example
```json
{
  "label" : "New Event",
  "name" : "newEvent",
  "parameters" : {
    "appEventId" : 1
  },
  "type" : "appEvent/v1/newEvent"
}
```


<hr />

