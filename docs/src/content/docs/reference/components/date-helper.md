---
title: "Date Helper"
description: "Helper component for date and time manipulation."
---

Helper component for date and time manipulation.


Categories: helpers


Type: dateHelper/v1

<hr />




## Actions


### Add Time
Add time to the date.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| inputDate | Date | DATE_TIME | DATE_TIME  |  | true  |
| dateFormat | Date Format | STRING | SELECT  |  Here's what each part of the format (eg. YYYY) means: yyyy : Year (4 digits) yy : Year (2 digits) MMMM : Month (full name) MMM : Month (short name) MM : Month (2 digits) EEE : Day (short name) dd : Day (2 digits) HH : Hour (2 digits) mm : Minute (2 digits) ss : Second (2 digits).  |  true  |
| year | Year | INTEGER | INTEGER  |  Years to add.  |  false  |
| month | Month | INTEGER | INTEGER  |  Months to add.  |  false  |
| day | Day | INTEGER | INTEGER  |  Days to add.  |  false  |
| hour | Hour | INTEGER | INTEGER  |  Hours to add.  |  false  |
| minute | Minute | INTEGER | INTEGER  |  Minutes to add.  |  false  |
| second | Second | INTEGER | INTEGER  |  Seconds to add.  |  false  |


#### Output



Type: STRING







### Convert Date Timestamp
Converts UNIX timestamp to ISO8601 format.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| dateTimestamp | UNIX Timestamp | NUMBER | NUMBER  |  UNIX Timestamp in seconds (10 digits) or milliseconds (13 digits)  |  true  |
| dateFormat | Date Format | STRING | SELECT  |  Formatting that should be applied the text representation of date.  |  true  |


#### Output



Type: STRING







### Date Difference
Get the difference between two dates.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| startDate | Start Date | DATE_TIME | DATE_TIME  |  | true  |
| endDate | End Date | DATE_TIME | DATE_TIME  |  | true  |
| unit | Unit | STRING | SELECT  |  The unit of difference between the two dates.  |  true  |


#### Output



Type: NUMBER







### Extract Date Units
Extract date units (year/month/day/hour/minute/second/day of week/month name) from a date.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| inputDate | Date | DATE_TIME | DATE_TIME  |  | true  |
| unit | Unit to Extract | STRING | SELECT  |  Unit to extract from date.  |  true  |


#### Output



Type: STRING







### Get Current Date
Get current date in the specified format.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| timeZone | Time Zone | STRING | SELECT  |  Time zone to use when formatting date.  |  true  |
| dateFormat | Date Format | STRING | SELECT  |  Here's what each part of the format (eg. YYYY) means: yyyy : Year (4 digits) yy : Year (2 digits) MMMM : Month (full name) MMM : Month (short name) MM : Month (2 digits) EEE : Day (short name) dd : Day (2 digits) HH : Hour (2 digits) mm : Minute (2 digits) ss : Second (2 digits).  |  true  |


#### Output



Type: STRING







### Subtract Time
Subtract time from date

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| inputDate | Date | DATE_TIME | DATE_TIME  |  | true  |
| dateFormat | Date Format | STRING | SELECT  |  Here's what each part of the format (eg. YYYY) means: yyyy : Year (4 digits) yy : Year (2 digits) MMMM : Month (full name) MMM : Month (short name) MM : Month (2 digits) EEE : Day (short name) dd : Day (2 digits) HH : Hour (2 digits) mm : Minute (2 digits) ss : Second (2 digits).  |  true  |
| year | Year | INTEGER | INTEGER  |  Years to subtract.  |  false  |
| month | Month | INTEGER | INTEGER  |  Months to subtract.  |  false  |
| day | Day | INTEGER | INTEGER  |  Days to subtract.  |  false  |
| hour | Hour | INTEGER | INTEGER  |  Hours to subtract.  |  false  |
| minute | Minute | INTEGER | INTEGER  |  Minutes to subtract.  |  false  |
| second | Second | INTEGER | INTEGER  |  Seconds to subtract.  |  false  |


#### Output



Type: DATE_TIME









