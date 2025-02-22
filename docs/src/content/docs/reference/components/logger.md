---
title: "Logger"
description: "Logs a value to the system log."
---

Logs a value to the system log.


Categories: helpers


Type: logger/v1

<hr />




## Actions


### Debug
Name: debug

null

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | | STRING |  | null |


#### JSON Example
```json
{
  "label" : "Debug",
  "name" : "debug",
  "parameters" : {
    "text" : ""
  },
  "type" : "logger/v1/debug"
}
```


### Error
Name: error

null

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | | STRING |  | null |


#### JSON Example
```json
{
  "label" : "Error",
  "name" : "error",
  "parameters" : {
    "text" : ""
  },
  "type" : "logger/v1/error"
}
```


### Info
Name: info

null

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | | STRING |  | null |


#### JSON Example
```json
{
  "label" : "Info",
  "name" : "info",
  "parameters" : {
    "text" : ""
  },
  "type" : "logger/v1/info"
}
```


### Warn
Name: warn

null

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| text | | STRING |  | null |


#### JSON Example
```json
{
  "label" : "Warn",
  "name" : "warn",
  "parameters" : {
    "text" : ""
  },
  "type" : "logger/v1/warn"
}
```




