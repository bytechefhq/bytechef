---
title: "Watsonx AI"
description: "IBM watsonx.ai AI studio is part of the IBM watsonx AI and data platform, bringing together new generative AI (gen AI) capabilities powered by foundation models and traditional machine learning (ML) into a powerful studio spanning the AI lifecycle."
---

IBM watsonx.ai AI studio is part of the IBM watsonx AI and data platform, bringing together new generative AI (gen AI) capabilities powered by foundation models and traditional machine learning (ML) into a powerful studio spanning the AI lifecycle.


Categories: artificial-intelligence


Type: watsonx/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| url | Region | STRING | SELECT  |  URL to connect to.  |  true  |
| streamEndpoint | Stream Endpoint | STRING | TEXT  |  The streaming endpoint.  |  true  |
| textEndpoint | Text Endpoint | STRING | TEXT  |  The text endpoint.  |  true  |
| projectId | Project ID | STRING | TEXT  |  The project ID.  |  true  |
| token | IAM Token | STRING | TEXT  |  The IBM Cloud account IAM token.  |  true  |





<hr />



## Actions


### Ask
Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |     Control Type     |     Description     |     Required        |
|:--------------:|:--------------:|:------------:|:--------------------:|:-------------------:|:-------------------:|
| model | Model | STRING | TEXT  |  Model is the identifier of the LLM Model to be used.  |  false  |
| messages | Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |  true  |
| response | Response | {STRING\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |  false  |
| decodingMethod | Decoding Method | STRING | TEXT  |  Decoding is the process that a model uses to choose the tokens in the generated output.  |  null  |
| repetitionPenalty | Repetition Penalty | NUMBER | NUMBER  |  Sets how strongly to penalize repetitions. A higher value (e.g., 1.8) will penalize repetitions more strongly, while a lower value (e.g., 1.1) will be more lenient.  |  null  |
| minTokens | Min Tokens | INTEGER | INTEGER  |  Sets how many tokens must the LLM generate.  |  null  |
| maxTokens | Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |  null  |
| temperature | Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |  null  |
| topP | Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |  null  |
| topK | Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |  null  |
| stop | Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |  null  |
| seed | Seed | INTEGER | INTEGER  |  Keeping the same seed would output the same response.  |  null  |






