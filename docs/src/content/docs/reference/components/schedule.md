---
title: "Schedule"
description: "With the Scheduled trigger, you can initiate customized workflows at specific time intervals."
---
## Reference
<hr />

With the Scheduled trigger, you can initiate customized workflows at specific time intervals.


Categories: [HELPERS]


Version: 1

<hr />




## Triggers


### Every day
Trigger off at a specific time either on a daily basis or selected days of the week.

#### Type: LISTENER
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Hour | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |
| Minute | INTEGER | INTEGER  |  The minute at which a workflow will be triggered.  |
| Day of week | {BOOLEAN(1), BOOLEAN(2), BOOLEAN(3), BOOLEAN(4), BOOLEAN(5), BOOLEAN(6), BOOLEAN(7)} | OBJECT_BUILDER  |  Days at which a workflow will be triggered.  |
| Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| {BOOLEAN(1), BOOLEAN(2), BOOLEAN(3), BOOLEAN(4), BOOLEAN(5), BOOLEAN(6), BOOLEAN(7)} | OBJECT_BUILDER  |
| STRING | TEXT  |






### Every week
Trigger off at a specific day of the week.

#### Type: LISTENER
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Hour | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |
| Minute | INTEGER | INTEGER  |  The minute at which a workflow will be triggered.  |
| Day of week | INTEGER | SELECT  |  Days at which a workflow will be triggered.  |
| Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |






### Every month
Trigger off at a specific time in month.

#### Type: LISTENER
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Hour | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |
| Minute | INTEGER | INTEGER  |  The minute at which a workflow will be triggered.  |
| Day of month | INTEGER | INTEGER  |  The day of the month  at which a workflow will be triggered.  |
| Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| STRING | TEXT  |






### Interval
Trigger off periodically, for example every minute or day, based on a set interval.

#### Type: LISTENER
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Interval | INTEGER | INTEGER  |  The hour at which a workflow will be triggered.  |
| Day of week | INTEGER | SELECT  |  Days at which a workflow will be triggered.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |






### Cron
Trigger off based on a custom schedule.

#### Type: LISTENER
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Expression | STRING | TEXT  |  The chron schedule expression. Format: [Minute] [Hour] [Day of Month] [Month] [Day of Week]  |
| Timezone | STRING | SELECT  |  The timezone at which the cron expression will be scheduled.  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






<hr />



## Actions



