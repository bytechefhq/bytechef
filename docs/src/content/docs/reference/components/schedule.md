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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Hour | INTEGER | INTEGER  |
| Minute | INTEGER | INTEGER  |
| Day of week | OBJECT | OBJECT_BUILDER  |
| Timezone | STRING | SELECT  |


### Output



Type: OBJECT

#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| INTEGER | INTEGER  |
| INTEGER | INTEGER  |
| OBJECT | OBJECT_BUILDER  |
| STRING | TEXT  |






### Every week
Trigger off at a specific day of the week.

#### Type: LISTENER
#### Properties

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Hour | INTEGER | INTEGER  |
| Minute | INTEGER | INTEGER  |
| Day of week | INTEGER | SELECT  |
| Timezone | STRING | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Hour | INTEGER | INTEGER  |
| Minute | INTEGER | INTEGER  |
| Day of month | INTEGER | INTEGER  |
| Timezone | STRING | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Interval | INTEGER | INTEGER  |
| Day of week | INTEGER | SELECT  |


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

|      Name      |     Type     |     Control Type     |
|:--------------:|:------------:|:--------------------:|
| Expression | STRING | TEXT  |
| Timezone | STRING | SELECT  |


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



