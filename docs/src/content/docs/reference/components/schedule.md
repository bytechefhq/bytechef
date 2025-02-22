---
title: "Schedule"
description: "With the Scheduled trigger, you can initiate customized workflows at specific time intervals."
---

With the Scheduled trigger, you can initiate customized workflows at specific time intervals.


Categories: helpers


Type: schedule/v1

<hr />






## Triggers


### Every Day
Name: everyDay

Trigger off at a specific time either on a daily basis or selected days of the week.

Type: LISTENER

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| hour | Hour | INTEGER | The hour at which a workflow will be triggered. | true |
| minute | Minute | INTEGER | The minute at which a workflow will be triggered. | true |
| dayOfWeek | Day of Week | ARRAY <details> <summary> Items </summary> [INTEGER] </details> | Days at which a workflow will be triggered. | null |
| timezone | Timezone | STRING <details> <summary> Options </summary> Etc/GMT+0, Etc/GMT-1, Etc/GMT-2, Etc/GMT-3, Etc/GMT-4, Etc/GMT-5, Etc/GMT-6, Etc/GMT-7, Etc/GMT-8, Etc/GMT-9, Etc/GMT-10, Etc/GMT-11, Etc/GMT-12, Etc/GMT-13, Etc/GMT-14, Etc/GMT+1, Etc/GMT+2, Etc/GMT+3, Etc/GMT+4, Etc/GMT+5, Etc/GMT+6, Etc/GMT+7, Etc/GMT+8, Etc/GMT+9, Etc/GMT+10, Etc/GMT+11, Etc/GMT+12 </details> | The timezone at which the cron expression will be scheduled. | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| datetime | STRING |
| hour | INTEGER |
| minute | INTEGER |
| dayOfWeek | ARRAY <details> <summary> Items </summary> [INTEGER] </details> |
| timezone | STRING |




#### JSON Example
```json
{
  "label" : "Every Day",
  "name" : "everyDay",
  "parameters" : {
    "hour" : 1,
    "minute" : 1,
    "dayOfWeek" : [ 1 ],
    "timezone" : ""
  },
  "type" : "schedule/v1/everyDay"
}
```


### Every Week
Name: everyWeek

Trigger off at a specific day of the week.

Type: LISTENER

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| hour | Hour | INTEGER | The hour at which a workflow will be triggered. | true |
| minute | Minute | INTEGER | The minute at which a workflow will be triggered. | true |
| dayOfWeek | Day of Week | INTEGER <details> <summary> Options </summary> 1, 2, 3, 4, 5, 6, 7 </details> | Days at which a workflow will be triggered. | true |
| timezone | Timezone | STRING <details> <summary> Options </summary> Etc/GMT+0, Etc/GMT-1, Etc/GMT-2, Etc/GMT-3, Etc/GMT-4, Etc/GMT-5, Etc/GMT-6, Etc/GMT-7, Etc/GMT-8, Etc/GMT-9, Etc/GMT-10, Etc/GMT-11, Etc/GMT-12, Etc/GMT-13, Etc/GMT-14, Etc/GMT+1, Etc/GMT+2, Etc/GMT+3, Etc/GMT+4, Etc/GMT+5, Etc/GMT+6, Etc/GMT+7, Etc/GMT+8, Etc/GMT+9, Etc/GMT+10, Etc/GMT+11, Etc/GMT+12 </details> | The timezone at which the cron expression will be scheduled. | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| datetime | STRING |
| hour | INTEGER |
| minute | INTEGER |
| dayOfWeek | INTEGER |
| timezone | STRING |




#### JSON Example
```json
{
  "label" : "Every Week",
  "name" : "everyWeek",
  "parameters" : {
    "hour" : 1,
    "minute" : 1,
    "dayOfWeek" : 1,
    "timezone" : ""
  },
  "type" : "schedule/v1/everyWeek"
}
```


### Every Month
Name: everyMonth

Trigger off at a specific time in month.

Type: LISTENER

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| hour | Hour | INTEGER | The hour at which a workflow will be triggered. | true |
| minute | Minute | INTEGER | The minute at which a workflow will be triggered. | true |
| dayOfMonth | Day of Month | INTEGER | The day of the month  at which a workflow will be triggered. | true |
| timezone | Timezone | STRING <details> <summary> Options </summary> Etc/GMT+0, Etc/GMT-1, Etc/GMT-2, Etc/GMT-3, Etc/GMT-4, Etc/GMT-5, Etc/GMT-6, Etc/GMT-7, Etc/GMT-8, Etc/GMT-9, Etc/GMT-10, Etc/GMT-11, Etc/GMT-12, Etc/GMT-13, Etc/GMT-14, Etc/GMT+1, Etc/GMT+2, Etc/GMT+3, Etc/GMT+4, Etc/GMT+5, Etc/GMT+6, Etc/GMT+7, Etc/GMT+8, Etc/GMT+9, Etc/GMT+10, Etc/GMT+11, Etc/GMT+12 </details> | The timezone at which the cron expression will be scheduled. | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| datetime | STRING |
| hour | INTEGER |
| minute | INTEGER |
| dayOfMonth | INTEGER |
| timezone | STRING |




#### JSON Example
```json
{
  "label" : "Every Month",
  "name" : "everyMonth",
  "parameters" : {
    "hour" : 1,
    "minute" : 1,
    "dayOfMonth" : 1,
    "timezone" : ""
  },
  "type" : "schedule/v1/everyMonth"
}
```


### Interval
Name: interval

Trigger off periodically, for example every minute or day, based on a set interval.

Type: LISTENER

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| interval | Interval | INTEGER | The hour at which a workflow will be triggered. | true |
| timeUnit | Day of Week | INTEGER <details> <summary> Options </summary> 1, 2, 3, 4 </details> | Days at which a workflow will be triggered. | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| datetime | STRING |
| interval | INTEGER |
| timeUnit | INTEGER |




#### JSON Example
```json
{
  "label" : "Interval",
  "name" : "interval",
  "parameters" : {
    "interval" : 1,
    "timeUnit" : 1
  },
  "type" : "schedule/v1/interval"
}
```


### Cron
Name: cron

Trigger off based on a custom schedule.

Type: LISTENER

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| expression | Expression | STRING | The chron schedule expression. Format: [Minute] [Hour] [Day of Month] [Month] [Day of Week] | true |
| timezone | Timezone | STRING <details> <summary> Options </summary> Etc/GMT+0, Etc/GMT-1, Etc/GMT-2, Etc/GMT-3, Etc/GMT-4, Etc/GMT-5, Etc/GMT-6, Etc/GMT-7, Etc/GMT-8, Etc/GMT-9, Etc/GMT-10, Etc/GMT-11, Etc/GMT-12, Etc/GMT-13, Etc/GMT-14, Etc/GMT+1, Etc/GMT+2, Etc/GMT+3, Etc/GMT+4, Etc/GMT+5, Etc/GMT+6, Etc/GMT+7, Etc/GMT+8, Etc/GMT+9, Etc/GMT+10, Etc/GMT+11, Etc/GMT+12 </details> | The timezone at which the cron expression will be scheduled. | null |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| datetime | STRING |
| expression | STRING |
| timezone | STRING |




#### JSON Example
```json
{
  "label" : "Cron",
  "name" : "cron",
  "parameters" : {
    "expression" : "",
    "timezone" : ""
  },
  "type" : "schedule/v1/cron"
}
```


<hr />

