---
title: "QIanFan"
description: "百度智能云 云智一体深入产业. 全栈自研的AI大底座，满足产业对智算基础设施的需求."
---
## Reference
<hr />

百度智能云 云智一体深入产业. 全栈自研的AI大底座，满足产业对智算基础设施的需求.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| API Key | STRING | TEXT  |  |
| Secret Key | STRING | TEXT  |  |





<hr />





## Actions


### Ask
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Presence penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |
| Frequency penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |


### Output



Type: STRING







### Create image
Create an image using text-to-image models

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  The model to use for image generation.  |
| Messages | [{STRING\(content), NUMBER\(weight)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Size | {} | SELECT  |  The size of the generated images.  |
| Number of responses | INTEGER | INTEGER  |  The number of images to generate. Must be between 1 and 4.  |
| Style | STRING | SELECT  |  The style of the generated images. The default style is Base.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| [{STRING\(url), STRING\(b64Json), STRING\(revisedPrompt)}] | ARRAY_BUILDER  |






