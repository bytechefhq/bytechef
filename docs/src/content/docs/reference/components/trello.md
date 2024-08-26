---
title: "Trello"
description: "Trello is a project management tool that uses boards, lists, and cards to help users organize tasks and collaborate with teams."
---
## Reference
<hr />

Trello is a project management tool that uses boards, lists, and cards to help users organize tasks and collaborate with teams.


Categories: [productivity-and-collaboration]


Version: 1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Key | STRING | TEXT  |  |
| Token | STRING | TEXT  |  |





<hr />



## Triggers


### New Card
Triggers when a new card is created on specified board or list.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Board | STRING | SELECT  |  |
| List | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |







<hr />



## Actions


### Create Board
Creates a new board.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Name | STRING | TEXT  |  The new name for the board.  |
| Description | STRING | TEXT  |  A new description for the board.  |




### Create Card
Creates a new card.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Board | STRING | SELECT  |  |
| List | STRING | SELECT  |  List the card should be created in.  |
| Name | STRING | TEXT  |  The name for the card.  |
| Description | STRING | TEXT  |  The description for the card.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |






