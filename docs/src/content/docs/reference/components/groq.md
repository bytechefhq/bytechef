---
title: "Groq"
description: "The LPU Inference Engine by Groq is a hardware and software platform that delivers exceptional compute speed, quality, and energy efficiency."
---

The LPU Inference Engine by Groq is a hardware and software platform that delivers exceptional compute speed, quality, and energy efficiency.


Categories: artificial-intelligence


Type: groq/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| token | Token | STRING | TEXT  |  | true  |





<hr />



## Actions


### Ask
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | TEXT  |  ID of the model to use.  |  true  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| n | Number of Chat Completion Choices | INTEGER | INTEGER  |  How many chat completion choices to generate for each input message.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| frequencyPenalty | Frequency Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |  null  |
| presencePenalty | Presence Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |  null  |
| logitBias | Logit Bias | {} | OBJECT_BUILDER  |  Modify the likelihood of specified tokens appearing in the completion.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |
| user | User | STRING | TEXT  |  A unique identifier representing your end-user, which can help admins to monitor and detect abuse.  |  false  |






