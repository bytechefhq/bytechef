---
title: "Math Helper"
description: "Helper component to perform mathematical operations."
---

Helper component to perform mathematical operations.


Categories: helpers


Type: mathHelper/v1

<hr />




## Actions


### Addition
Name: addition

Add two numbers.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| firstNumber | First Number | NUMBER |  | true |
| secondNumber | Second Number | NUMBER |  | true |


#### Output



Type: NUMBER





#### JSON Example
```json
{
  "label" : "Addition",
  "name" : "addition",
  "parameters" : {
    "firstNumber" : 0.0,
    "secondNumber" : 0.0
  },
  "type" : "mathHelper/v1/addition"
}
```


### Division
Name: division

Divide two numbers.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| firstNumber | First Number | NUMBER | Number to be divided. | true |
| secondNumber | Second Number | NUMBER | Number to divide by. | true |


#### Output



Type: NUMBER





#### JSON Example
```json
{
  "label" : "Division",
  "name" : "division",
  "parameters" : {
    "firstNumber" : 0.0,
    "secondNumber" : 0.0
  },
  "type" : "mathHelper/v1/division"
}
```


### Modulo
Name: modulo

Get the remainder of the division of two numbers.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| firstNumber | First Number | NUMBER | Number to be divided. | true |
| secondNumber | Second Number | NUMBER | Number to divide by. | true |


#### Output



Type: NUMBER





#### JSON Example
```json
{
  "label" : "Modulo",
  "name" : "modulo",
  "parameters" : {
    "firstNumber" : 0.0,
    "secondNumber" : 0.0
  },
  "type" : "mathHelper/v1/modulo"
}
```


### Multiplication
Name: multiplication

Multiply two numbers.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| firstNumber | First Number | NUMBER |  | true |
| secondNumber | Second Number | NUMBER |  | true |


#### Output



Type: NUMBER





#### JSON Example
```json
{
  "label" : "Multiplication",
  "name" : "multiplication",
  "parameters" : {
    "firstNumber" : 0.0,
    "secondNumber" : 0.0
  },
  "type" : "mathHelper/v1/multiplication"
}
```


### Subtraction
Name: subtraction

Subtract two numbers.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| firstNumber | First Number | NUMBER | Number to subtract from. | true |
| secondNumber | Second Number | NUMBER | Number to subtract. | true |


#### Output



Type: NUMBER





#### JSON Example
```json
{
  "label" : "Subtraction",
  "name" : "subtraction",
  "parameters" : {
    "firstNumber" : 0.0,
    "secondNumber" : 0.0
  },
  "type" : "mathHelper/v1/subtraction"
}
```




