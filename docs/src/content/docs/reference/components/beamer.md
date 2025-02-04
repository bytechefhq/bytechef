---
title: "Beamer"
description: "Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, and boost user engagement through in-app notifications, changelogs, and announcements."
---

Beamer is a customer engagement platform that helps businesses communicate updates, collect feedback, and boost user engagement through in-app notifications, changelogs, and announcements.


Categories: helpers


Type: beamer/v1

<hr />



## Connections

Version: 1


### OAuth2 Authorization Code

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| api_token | API key | STRING | TEXT  |  | true  |





<hr />



## Actions


### Create Feature Request
Name: createFeatureRequest

Create a new Feature Request

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Feature Request Title | STRING | TEXT  |  The name of the new feature request  |  true  |
| content | Feature Request Content | STRING | TEXT  |  The content of the new feature request  |  false  |
| userEmail | User Email | STRING | TEXT  |  The email of the user that is creating the new feature request  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| visible | STRING | TEXT  |
| category | STRING | TEXT  |
| status | STRING | TEXT  |
| translations | [STRING\($title), STRING\($content), STRING\($contentHtml), STRING\($language), STRING\($permalink), STRING\($images)] | ARRAY_BUILDER  |
| votesCount | STRING | TEXT  |
| commentsCount | STRING | TEXT  |
| notes | STRING | TEXT  |
| filters | STRING | TEXT  |
| internalUserEmail | STRING | TEXT  |
| internalUserFirstname | STRING | TEXT  |
| internalUserLastname | STRING | TEXT  |
| userId | STRING | TEXT  |
| userEmail | STRING | TEXT  |
| userFirstname | STRING | TEXT  |
| userLastname | STRING | TEXT  |
| userCustomAttributes | STRING | TEXT  |






### Create New Post
Name: createPost

Create a new Post

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| title | Title | STRING | TEXT  |  Title of the new Post  |  true  |
| content | Content | STRING | TEXT  |  Content of the new Post  |  true  |
| category | Category | STRING | SELECT  |  Category of the new Post  |  true  |
| userEmail | User Email | STRING | TEXT  |  Email of the user that is creating the new Post  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| root | [STRING\($autoOpen), STRING\($category), STRING\($date), STRING\($feedbackEnabled), STRING\($id), STRING\($published), STRING\($reactionsEnabled), [STRING\($category), STRING\($content), STRING\($contentHtml), STRING\($language), STRING\($postUrl), STRING\($title)]\($translations)] | ARRAY_BUILDER  |






### Get Feed
Name: getFeed

Get the URL for your feed.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|



#### Output



Type: STRING







### New Comment
Name: newComment

Create a new comment on selected post.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| postId | Post | STRING | SELECT  |  ID of the post that will have the new comment.  |  true  |
| text | Text | STRING | TEXT  |  Text of the comment.  |  false  |
| userId | User id | STRING | TEXT  |  ID of the user that is creating the new comment.  |  false  |
| userEmail | User email | STRING | TEXT  |  Email of the user that is creating the new comment.  |  false  |
| userFirstname | User first name | STRING | TEXT  |  First name of the user that is creating the new comment.  |  false  |
| userLastname | User last name | STRING | TEXT  |  Last name of the user that is creating the new comment.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| text | STRING | TEXT  |
| postTitle | STRING | TEXT  |
| userId | STRING | TEXT  |
| userEmail | STRING | TEXT  |
| userFirstname | STRING | TEXT  |
| userLastname | STRING | TEXT  |
| url | STRING | TEXT  |
| userCustomAttributes | STRING | TEXT  |






### New Vote
Name: newVote

Create a new vote on selected feature request.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| requestId | Feature Request | STRING | SELECT  |  The id of the feature request that will have the new vote.  |  true  |
| userId | User ID | STRING | TEXT  |  The id of the user that is creating the new vote.  |  false  |
| userEmail | User Email | STRING | TEXT  |  Email of the user that is creating the new vote.  |  false  |
| userFirstname | User First Name | STRING | TEXT  |  First name of the user that is creating the new vote.  |  false  |
| userLastname | User Last Name | STRING | TEXT  |  Last name of the user that is creating the new vote.  |  false  |


#### Output



Type: OBJECT


#### Properties

|     Name     |     Type     |     Control Type     |
|:------------:|:------------:|:--------------------:|
| id | STRING | TEXT  |
| date | STRING | TEXT  |
| featureRequestTitle | STRING | TEXT  |
| userId | STRING | TEXT  |
| userEmail | STRING | TEXT  |
| userFirstname | STRING | TEXT  |
| userLastname | STRING | TEXT  |
| userCustomAttributes | STRING | TEXT  |
| url | STRING | TEXT  |








## Triggers


### Updated Issue
Triggers when an issue is updated.

Type: DYNAMIC_WEBHOOK
#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|



#### Output



Type: STRING








<hr />

<hr />

# Additional instructions
<hr />

## Example

This is an example of an example. 

1. Step 1
2. Step 2
3. ![An illustration of planets and stars featuring the word “astro”](https://raw.githubusercontent.com/withastro/docs/main/public/default-og-image.png)
