---
title: "Beamer"
description: "Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, and boost user engagement through in-app notifications, changelogs, and announcements."
---

Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, and boost user engagement through in-app notifications, changelogs, and announcements.


Categories: productivity-and-collaboration


Type: beamer/v1

<hr />



## Connections

Version: 1


### API Key

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| key | API key | STRING |  | true |





<hr />



## Actions


### Create Feature Request
Name: createFeatureRequest

Creates a new feature request.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| title | Feature Request Title | STRING | The name of the new feature request. | true |
| content | Feature Request Content | STRING | The content of the new feature request. | false |
| userEmail | User Email | STRING | The email of the user that is creating the new feature request. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| date | STRING |
| visible | STRING |
| category | STRING |
| status | STRING |
| translations | ARRAY <details> <summary> Items </summary> [{STRING\(title), STRING\(content), STRING\(contentHtml), STRING\(language), STRING\(permalink), [STRING]\(images)}] </details> |
| votesCount | INTEGER |
| commentsCount | STRING |
| notes | STRING |
| filters | STRING |
| internalUserEmail | STRING |
| internalUserFirstname | STRING |
| internalUserLastname | STRING |
| userId | STRING |
| userEmail | STRING |
| userFirstname | STRING |
| userLastname | STRING |




#### JSON Example
```json
{
  "label" : "Create Feature Request",
  "name" : "createFeatureRequest",
  "parameters" : {
    "title" : "",
    "content" : "",
    "userEmail" : ""
  },
  "type" : "beamer/v1/createFeatureRequest"
}
```


### Create Post
Name: createPost

Creates a new post.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| title | Title | STRING | Title of the new post. | true |
| content | Content | STRING | Content of the new post. | true |
| category | Category | STRING <details> <summary> Options </summary> new, improvement, fix, comingsoon, announcement, other </details> | Category of the new post. | true |
| userEmail | User Email | STRING | Email of the user that is creating the new post. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| date | STRING |
| dueDate | STRING |
| published | STRING |
| category | STRING |
| feedbackEnabled | STRING |
| reactionsEnabled | STRING |
| translations | ARRAY <details> <summary> Items </summary> [{STRING\(title), STRING\(content), STRING\(category), STRING\(contentHtml), STRING\(language), STRING\(postUrl)}] </details> |




#### JSON Example
```json
{
  "label" : "Create Post",
  "name" : "createPost",
  "parameters" : {
    "title" : "",
    "content" : "",
    "category" : "",
    "userEmail" : ""
  },
  "type" : "beamer/v1/createPost"
}
```


### Get Feed
Name: getFeed

Get the URL for your feed.


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| url | STRING |




#### JSON Example
```json
{
  "label" : "Get Feed",
  "name" : "getFeed",
  "type" : "beamer/v1/getFeed"
}
```


### New Comment
Name: newComment

Creates a new comment on selected post.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| postId | Post | STRING | ID of the post that will have the new comment. | true |
| text | Text | STRING | Text of the comment. | false |
| userId | User ID | STRING | ID of the user that is creating the new comment. | false |
| userEmail | User Email | STRING | Email of the user that is creating the new comment. | false |
| userFirstname | User First Name | STRING | First name of the user that is creating the new comment. | false |
| userLastname | User Last Name | STRING | Last name of the user that is creating the new comment. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| date | STRING |
| text | STRING |
| postTitle | STRING |
| userId | STRING |
| userEmail | STRING |
| userFirstname | STRING |
| userLastname | STRING |
| url | STRING |




#### JSON Example
```json
{
  "label" : "New Comment",
  "name" : "newComment",
  "parameters" : {
    "postId" : "",
    "text" : "",
    "userId" : "",
    "userEmail" : "",
    "userFirstname" : "",
    "userLastname" : ""
  },
  "type" : "beamer/v1/newComment"
}
```


### New Vote
Name: newVote

Creates a new vote on selected feature request.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| requestId | Feature Request ID | STRING | ID of the feature request that will have the new vote. | true |
| userId | User ID | STRING | ID of the user that is creating the new vote. | false |
| userEmail | User Email | STRING | Email of the user that is creating the new vote. | false |
| userFirstname | User First Name | STRING | First name of the user that is creating the new vote. | false |
| userLastname | User Last Name | STRING | Last name of the user that is creating the new vote. | false |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |
|:------------:|:------------:|
| id | STRING |
| date | STRING |
| featureRequestTitle | STRING |
| userId | STRING |
| userEmail | STRING |
| userFirstname | STRING |
| userLastname | STRING |
| url | STRING |




#### JSON Example
```json
{
  "label" : "New Vote",
  "name" : "newVote",
  "parameters" : {
    "requestId" : "",
    "userId" : "",
    "userEmail" : "",
    "userFirstname" : "",
    "userLastname" : ""
  },
  "type" : "beamer/v1/newVote"
}
```




