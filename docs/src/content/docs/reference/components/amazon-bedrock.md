---
title: "Amazon Bedrock"
description: "Amazon Bedrock is a fully managed service that offers a choice of high-performing foundation models (FMs) from leading AI companies."
---
## Reference
<hr />

Amazon Bedrock is a fully managed service that offers a choice of high-performing foundation models (FMs) from leading AI companies.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Access Key ID | STRING | TEXT  |  |
| Secret Access Key | STRING | TEXT  |  |
| STRING | SELECT  |





<hr />





## Actions


### Ask Anthropic3
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |




### Ask Anthropic2
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |




### Ask Cohere
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Number of chat completion choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Logit Bias | {STRING\(biasToken), NUMBER\(biasValue)} | OBJECT_BUILDER  |  Modify the likelihood of a specified token appearing in the completion.  |
| Return Likelihoods | {} | SELECT  |  The token likelihoods are returned with the response.  |
| Truncate | {} | SELECT  |  Specifies how the API handles inputs longer than the maximum token length  |




### Ask Jurassic2
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Min tokens | INTEGER | INTEGER  |  The minimum number of tokens to generate in the chat completion.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Prompt | STRING | TEXT  |  The text which the model is requested to continue.  |
| Number of chat completion choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Frequency penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |
| Presence penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Count penalty | NUMBER | NUMBER  |  Penalty object for count.  |




### Ask Llama
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |




### Ask Titan
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |




