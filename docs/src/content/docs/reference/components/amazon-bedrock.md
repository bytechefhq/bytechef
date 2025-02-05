---
title: "Amazon Bedrock"
description: "Amazon Bedrock is a fully managed service that offers a choice of high-performing foundation models (FMs) from leading AI companies."
---

Amazon Bedrock is a fully managed service that offers a choice of high-performing foundation models (FMs) from leading AI companies.


Categories: artificial-intelligence


Type: amazonBedrock/v1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| accessKey | Access Key ID | STRING | TEXT  |  | true  |
| secretKey | Secret Access Key | STRING | TEXT  |  | true  |
| region | STRING | SELECT  |





<hr />



## Actions


### Ask Anthropic3
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| topK | Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |




### Ask Anthropic2
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| topK | Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |




### Ask Cohere
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| n | Number of Chat Completion Choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| topK | Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |
| logitBias | Logit Bias | {STRING\(biasToken), NUMBER\(biasValue)} | OBJECT_BUILDER  |  Modify the likelihood of a specified token appearing in the completion.  |  null  |
| returnLikelihoods | Return Likelihoods | STRING | SELECT  |  The token likelihoods are returned with the response.  |  null  |
| truncate | Truncate | STRING | SELECT  |  Specifies how the API handles inputs longer than the maximum token length  |  null  |




### Ask Jurassic2
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| truncate | Min Tokens | INTEGER | INTEGER  |  The minimum number of tokens to generate in the chat completion.  |  null  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| prompt | Prompt | STRING | TEXT  |  The text which the model is requested to continue.  |  null  |
| n | Number of Chat Completion Choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| topK | Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |  null  |
| frequencyPenalty | Frequency Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |  null  |
| presencePenalty | Presence Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |
| countPenalty | Count Penalty | NUMBER | NUMBER  |  Penalty object for count.  |  null  |




### Ask Llama
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |




### Ask Titan
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | SELECT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |






