---
title: "ClickUp"
description: "ClickUp is a cloud-based collaboration tool that offers task management, document sharing, goal tracking, and other productivity features for teams."
---
## Reference
<hr />

ClickUp is a cloud-based collaboration tool that offers task management, document sharing, goal tracking, and other productivity features for teams.


Categories: [project-management]


Version: 1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Client Id | STRING | TEXT  |  |
| Client Secret | STRING | TEXT  |  |





<hr />



## Triggers


### New List
Triggers when new list is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workspace ID | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |
| {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







### New Task
Triggers when new task is created.

#### Type: DYNAMIC_WEBHOOK
#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workspace ID | STRING | SELECT  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| STRING | TEXT  |
| {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |
| {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |
| {STRING\(id), STRING\(name)} | OBJECT_BUILDER  |







<hr />



## Actions


### Create List
Creates a new List in specified Folder.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workspace ID | NUMBER | SELECT  |  |
| Space ID | NUMBER | SELECT  |  |
| Folder ID | NUMBER | SELECT  |  ID of the folder where new list will be created.  |
| List | {STRING\(name)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name), {STRING\(id), STRING\(name)}\(folder), {STRING\(id), STRING\(name)}\(space)} | OBJECT_BUILDER  |






### Create Task
Create a new task in a ClickUp workspace and list.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workspace ID | NUMBER | SELECT  |  |
| Space ID | NUMBER | SELECT  |  |
| Folder ID | NUMBER | SELECT  |  |
| List ID | NUMBER | SELECT  |  ID of the list where new task will be created.  |
| Task | {STRING\(name), STRING\(description)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name), STRING\(description), STRING\(url), {STRING\(id), STRING\(name)}\(list), {STRING\(id), STRING\(name)}\(folder), {STRING\(id)}\(space)} | OBJECT_BUILDER  |






### Create Folder
Creates a new folder in a ClickUp workspace.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Workspace ID | NUMBER | SELECT  |  |
| Space ID | NUMBER | SELECT  |  ID of the space where new folder will be created.  |
| Folder | {STRING\(name)} | OBJECT_BUILDER  |  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| {STRING\(id), STRING\(name), {STRING\(id), STRING\(name)}\(space)} | OBJECT_BUILDER  |






