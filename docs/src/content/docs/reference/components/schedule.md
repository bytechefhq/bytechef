---
title: "Schedule"
description: "With the Scheduled trigger, you can initiate customized workflows at specific time intervals."
---

With the Scheduled trigger, you can initiate customized workflows at specific time intervals.


Categories: helpers


Type: schedule/v1

<hr />




## Actions





## Triggers


### Every Day
Trigger off at a specific time either on a daily basis or selected days of the week.

Type: LISTENER
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| hour | Hour | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |  true  |
| minute | Minute | INTEGER | INTEGER  |  The minute at which a workflow will be triggered.  |  true  |
| dayOfWeek | Day of Week | {BOOLEAN\(1), BOOLEAN\(2), BOOLEAN\(3), BOOLEAN\(4), BOOLEAN\(5), BOOLEAN\(6), BOOLEAN\(7)} | OBJECT_BUILDER  |  Days at which a workflow will be triggered.  |  null  |
| timezone | Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| datetime | STRING | TEXT  |
| hour | INTEGER | INTEGER  |
| minute | INTEGER | INTEGER  |
| dayOfWeek | {BOOLEAN\(1), BOOLEAN\(2), BOOLEAN\(3), BOOLEAN\(4), BOOLEAN\(5), BOOLEAN\(6), BOOLEAN\(7)} | OBJECT_BUILDER  |
| timezone | STRING | TEXT  |







### Every Week
Trigger off at a specific day of the week.

Type: LISTENER
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| hour | Hour | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |  true  |
| minute | Minute | INTEGER | INTEGER  |  The minute at which a workflow will be triggered.  |  true  |
| dayOfWeek | Day of Week | INTEGER | SELECT  |  Days at which a workflow will be triggered.  |  true  |
| timezone | Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| datetime | STRING | TEXT  |
| hour | INTEGER | INTEGER  |
| minute | INTEGER | INTEGER  |
| dayOfWeek | INTEGER | INTEGER  |
| timezone | STRING | TEXT  |







### Every Month
Trigger off at a specific time in month.

Type: LISTENER
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| hour | Hour | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |  true  |
| minute | Minute | INTEGER | INTEGER  |  The minute at which a workflow will be triggered.  |  true  |
| dayOfMonth | Day of Month | INTEGER | INTEGER  |  The day of the month  at which a workflow will be triggered.  |  true  |
| timezone | Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| datetime | STRING | TEXT  |
| hour | INTEGER | INTEGER  |
| minute | INTEGER | INTEGER  |
| dayOfMonth | INTEGER | INTEGER  |
| timezone | STRING | TEXT  |







### Interval
Trigger off periodically, for example every minute or day, based on a set interval.

Type: LISTENER
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| interval | Interval | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |  true  |
| timeUnit | Day of Week | INTEGER | SELECT  |  Days at which a workflow will be triggered.  |  true  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| datetime | STRING | TEXT  |
| interval | INTEGER | INTEGER  |
| timeUnit | INTEGER | INTEGER  |







### Cron
Trigger off based on a custom schedule.

Type: LISTENER
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| expression | Expression | STRING | TEXT  |  The chron schedule expression. Format: [Minute] [Hour] [Day of Month] [Month] [Day of Week]  |  true  |
| timezone | Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |  null  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| datetime | STRING | TEXT  |
| expression | STRING | TEXT  |
| timezone | STRING | TEXT  |







<hr />

