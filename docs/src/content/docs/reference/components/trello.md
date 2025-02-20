---
title: "Trello"
description: "Trello is a project management tool that uses boards, lists, and cards to help users organize tasks and collaborate with teams."
---

Trello is a project management tool that uses boards, lists, and cards to help users organize tasks and collaborate with teams.


Categories: productivity-and-collaboration


Type: trello/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| key | Key | STRING | TEXT |  | true |
| token | Token | STRING | TEXT |  | true |





<hr />



## Actions


### Create Board
Name: createBoard

Creates a new board.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| name | Name | STRING | TEXT | The new name for the board. | true |
| desc | Description | STRING | TEXT | A new description for the board. | false |


#### JSON Example
```json
{
  "label" : "Create Board",
  "name" : "createBoard",
  "parameters" : {
    "name" : "",
    "desc" : ""
  },
  "type" : "trello/v1/createBoard"
}
```


### Create Card
Name: createCard

Creates a new card.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| idBoard | Board ID | STRING | SELECT | ID of the board. | true |
| idList | List ID | STRING <details> <summary> Depends On </summary> idBoard </details> | SELECT | ID of the list where the card should be created in. | true |
| name | Name | STRING | TEXT | The name for the card. | false |
| desc | Description | STRING | TEXT | The description for the card. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| desc | STRING | TEXT |
| idBoard | STRING | TEXT |
| idList | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Create Card",
  "name" : "createCard",
  "parameters" : {
    "idBoard" : "",
    "idList" : "",
    "name" : "",
    "desc" : ""
  },
  "type" : "trello/v1/createCard"
}
```


### Get Card
Name: getCard

Gets a card details.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| idBoard | Board ID | STRING | SELECT | ID of the board where card is located. | true |
| id | Card ID | STRING <details> <summary> Depends On </summary> idBoard </details> | SELECT |  | true |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| desc | STRING | TEXT |
| idBoard | STRING | TEXT |
| idList | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "Get Card",
  "name" : "getCard",
  "parameters" : {
    "idBoard" : "",
    "id" : ""
  },
  "type" : "trello/v1/getCard"
}
```




## Triggers


### New Card
Name: newCard

Triggers when a new card is created on specified board or list.

Type: DYNAMIC_WEBHOOK

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| idBoard | Board ID | STRING | SELECT |  | true |
| idList | List ID | STRING <details> <summary> Depends On </summary> idBoard </details> | SELECT |  | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |    Control Type     |
|:------------:|:------------:|:-------------------:|
| id | STRING | TEXT |
| desc | STRING | TEXT |
| idBoard | STRING | TEXT |
| idList | STRING | TEXT |
| name | STRING | TEXT |




#### JSON Example
```json
{
  "label" : "New Card",
  "name" : "newCard",
  "parameters" : {
    "idBoard" : "",
    "idList" : ""
  },
  "type" : "trello/v1/newCard"
}
```


<hr />

