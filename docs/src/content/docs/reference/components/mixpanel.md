---
title: "Mixpanel"
description: "Mixpanel is a product analytics tool that helps you track user interactions and behaviors in your app or website to make data-driven decisions."
---

Mixpanel is a product analytics tool that helps you track user interactions and behaviors in your app or website to make data-driven decisions.


Categories: Analytics


Type: mixpanel/v1

<hr />



## Connections

Version: 1


### Basic Auth

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| username | Username | STRING |  | true |





<hr />



## Actions


### Track Events
Name: trackEvents

Send batches of events from your servers to Mixpanel.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| Events | | ARRAY <details> <summary> Items </summary> [{STRING\(event), DATE_TIME\(time), STRING\(distinct_id), STRING\($insert_id)}] </details> |  | null |

#### Example JSON Structure
```json
{
  "label" : "Track Events",
  "name" : "trackEvents",
  "parameters" : {
    "Events" : [ {
      "event" : "",
      "time" : "2021-01-01T00:00:00",
      "distinct_id" : "",
      "$insert_id" : ""
    } ]
  },
  "type" : "mixpanel/v1/trackEvents"
}
```

#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Description     |
|:------------:|:------------:|:-------------------:|
| response | OBJECT <details> <summary> Properties </summary> {INTEGER\(code), INTEGER\(num_records_imported), STRING\(status)} </details> |  |




#### Output Example
```json
{
  "response" : {
    "code" : 1,
    "num_records_imported" : 1,
    "status" : ""
  }
}
```




