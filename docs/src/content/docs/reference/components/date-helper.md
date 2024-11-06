---
title: "Date Helper"
description: "Helper component for date and time manipulation."
---
## Reference
<hr />

Helper component for date and time manipulation.


Categories: [helpers]


Version: 1

<hr />






## Actions


### Add Time
Add time to the date.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Date | DATE_TIME | DATE_TIME  |  |
| Date Format | STRING | SELECT  |  Here's what each part of the format (eg. YYYY) means: yyyy : Year (4 digits) yy : Year (2 digits) MMMM : Month (full name) MMM : Month (short name) MM : Month (2 digits) EEE : Day (short name) dd : Day (2 digits) HH : Hour (2 digits) mm : Minute (2 digits) ss : Second (2 digits).  |
| Year | INTEGER | INTEGER  |  Years to add.  |
| Month | INTEGER | INTEGER  |  Months to add.  |
| Day | INTEGER | INTEGER  |  Days to add.  |
| Hour | INTEGER | INTEGER  |  Hours to add.  |
| Minute | INTEGER | INTEGER  |  Minutes to add.  |
| Second | INTEGER | INTEGER  |  Seconds to add.  |


### Output



Type: STRING







### Convert Date Timestamp
Converts UNIX timestamp to ISO8601 format.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| UNIX Timestamp | NUMBER | NUMBER  |  UNIX Timestamp in seconds (10 digits) or milliseconds (13 digits)  |
| Date Format | STRING | SELECT  |  Formatting that should be applied the text representation of date.  |


### Output



Type: STRING







### Date Difference
Get the difference between two dates.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Start Date | DATE_TIME | DATE_TIME  |  |
| End Date | DATE_TIME | DATE_TIME  |  |
| Unit | STRING | SELECT  |  The unit of difference between the two dates.  |


### Output



Type: NUMBER







### Extract Date Units
Extract date units (year/month/day/hour/minute/second/day of week/month name) from a date.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Date | DATE_TIME | DATE_TIME  |  |
| Unit to Extract | STRING | SELECT  |  Unit to extract from date.  |


### Output



Type: STRING







### Get Current Date
Get current date in the specified format.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Time Zone | STRING | SELECT  |  Time zone to use when formatting date.  |
| Date Format | STRING | SELECT  |  Here's what each part of the format (eg. YYYY) means: yyyy : Year (4 digits) yy : Year (2 digits) MMMM : Month (full name) MMM : Month (short name) MM : Month (2 digits) EEE : Day (short name) dd : Day (2 digits) HH : Hour (2 digits) mm : Minute (2 digits) ss : Second (2 digits).  |


### Output



Type: STRING







### Subtract Time
Subtract time from date

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Date | DATE_TIME | DATE_TIME  |  |
| Date Format | STRING | SELECT  |  Here's what each part of the format (eg. YYYY) means: yyyy : Year (4 digits) yy : Year (2 digits) MMMM : Month (full name) MMM : Month (short name) MM : Month (2 digits) EEE : Day (short name) dd : Day (2 digits) HH : Hour (2 digits) mm : Minute (2 digits) ss : Second (2 digits).  |
| Year | INTEGER | INTEGER  |  Years to subtract.  |
| Month | INTEGER | INTEGER  |  Months to subtract.  |
| Day | INTEGER | INTEGER  |  Days to subtract.  |
| Hour | INTEGER | INTEGER  |  Hours to subtract.  |
| Minute | INTEGER | INTEGER  |  Minutes to subtract.  |
| Second | INTEGER | INTEGER  |  Seconds to subtract.  |


### Output



Type: DATE_TIME







