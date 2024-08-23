---
title: "ZhiPu AI"
description: "Zhipu AI is an artificial intelligence company with the mission of teaching machines to think like humans."
---
## Reference
<hr />

Zhipu AI is an artificial intelligence company with the mission of teaching machines to think like humans.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Token | STRING | TEXT  |  |





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
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |
| Request Id | STRING | TEXT  |  The parameter is passed by the client and must ensure uniqueness. It is used to distinguish the unique identifier for each request. If the client does not provide it, the platform will generate it by default.  |
| Do sample | BOOLEAN | SELECT  |  When do_sample is set to true, the sampling strategy is enabled. If do_sample is false, the sampling strategy parameters temperature and top_p will not take effect.  |


### Output



Type: STRING







### Create image
Create an image using text-to-image models

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  The model to use for image generation.  |
| Messages | [{STRING\(content), NUMBER\(weight)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |


### Output



Type: OBJECT


#### Properties

|     Type     |     Control Type     |
|:------------:|:--------------------:|
| INTEGER | INTEGER  |
| [{STRING\(url), STRING\(b64Json), STRING\(revisedPrompt)}] | ARRAY_BUILDER  |






