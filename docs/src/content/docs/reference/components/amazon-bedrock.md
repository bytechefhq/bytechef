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

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| accessKey | Access Key ID | STRING | TEXT |  | true |
| secretKey | Secret Access Key | STRING | TEXT |  | true |
| region | | STRING <details> <summary> Options </summary> us-east-1, us-west-2, ap-south-1, ap-southeast-1, ap-southeast-2, ap-northeast-1, ca-central-1, eu-central-1, eu-west-1, eu-west-2, eu-west-3, sa-east-1 </details> | SELECT |  | true |





<hr />



## Actions


### Ask Anthropic3
Name: askAnthropic3

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> anthropic.claude-instant-v1, anthropic.claude-v2, anthropic.claude-v2:1 </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| topK | Top K | INTEGER | INTEGER | Specify the number of token choices the generative uses to generate the next token. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |


#### JSON Example
```json
{
  "label" : "Ask Anthropic3",
  "name" : "askAnthropic3",
  "parameters" : {
    "model" : "",
    "messages" : [ {
      "role" : "",
      "content" : "",
      "attachments" : [ {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      } ]
    } ],
    "maxTokens" : 1,
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "temperature" : 0.0,
    "topP" : 0.0,
    "topK" : 1,
    "stop" : [ "" ]
  },
  "type" : "amazonBedrock/v1/askAnthropic3"
}
```


### Ask Anthropic2
Name: askAnthropic2

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> anthropic.claude-instant-v1, anthropic.claude-v2, anthropic.claude-v2:1 </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| topK | Top K | INTEGER | INTEGER | Specify the number of token choices the generative uses to generate the next token. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |


#### JSON Example
```json
{
  "label" : "Ask Anthropic2",
  "name" : "askAnthropic2",
  "parameters" : {
    "model" : "",
    "messages" : [ {
      "role" : "",
      "content" : "",
      "attachments" : [ {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      } ]
    } ],
    "maxTokens" : 1,
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "temperature" : 0.0,
    "topP" : 0.0,
    "topK" : 1,
    "stop" : [ "" ]
  },
  "type" : "amazonBedrock/v1/askAnthropic2"
}
```


### Ask Cohere
Name: askCohere

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> cohere.command-light-text-v14, cohere.command-text-v14 </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | null |
| n | Number of Chat Completion Choices | INTEGER | INTEGER | How many chat completion choices to generate for each input message. | null |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| topK | Top K | INTEGER | INTEGER | Specify the number of token choices the generative uses to generate the next token. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |
| logitBias | Logit Bias | OBJECT <details> <summary> Properties </summary> {STRING\(biasToken), NUMBER\(biasValue)} </details> | OBJECT_BUILDER | Modify the likelihood of a specified token appearing in the completion. | null |
| returnLikelihoods | Return Likelihoods | STRING <details> <summary> Options </summary> ALL, GENERATION, NONE </details> | SELECT | The token likelihoods are returned with the response. | null |
| truncate | Truncate | STRING <details> <summary> Options </summary> END, NONE, START </details> | SELECT | Specifies how the API handles inputs longer than the maximum token length | null |


#### JSON Example
```json
{
  "label" : "Ask Cohere",
  "name" : "askCohere",
  "parameters" : {
    "model" : "",
    "messages" : [ {
      "role" : "",
      "content" : "",
      "attachments" : [ {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      } ]
    } ],
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "maxTokens" : 1,
    "n" : 1,
    "temperature" : 0.0,
    "topP" : 0.0,
    "topK" : 1,
    "stop" : [ "" ],
    "logitBias" : {
      "biasToken" : "",
      "biasValue" : 0.0
    },
    "returnLikelihoods" : "",
    "truncate" : ""
  },
  "type" : "amazonBedrock/v1/askCohere"
}
```


### Ask Jurassic2
Name: askJurassic2

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| truncate | Min Tokens | INTEGER | INTEGER | The minimum number of tokens to generate in the chat completion. | null |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | null |
| prompt | Prompt | STRING | TEXT | The text which the model is requested to continue. | null |
| n | Number of Chat Completion Choices | INTEGER | INTEGER | How many chat completion choices to generate for each input message. | null |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| topK | Top K | INTEGER | INTEGER | Specify the number of token choices the generative uses to generate the next token. | null |
| frequencyPenalty | Frequency Penalty | NUMBER | NUMBER | Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim. | null |
| presencePenalty | Presence Penalty | NUMBER | NUMBER | Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |
| countPenalty | Count Penalty | NUMBER | NUMBER | Penalty object for count. | null |


#### JSON Example
```json
{
  "label" : "Ask Jurassic2",
  "name" : "askJurassic2",
  "parameters" : {
    "model" : "",
    "messages" : [ {
      "role" : "",
      "content" : "",
      "attachments" : [ {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      } ]
    } ],
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "truncate" : 1,
    "maxTokens" : 1,
    "prompt" : "",
    "n" : 1,
    "temperature" : 0.0,
    "topP" : 0.0,
    "topK" : 1,
    "frequencyPenalty" : 0.0,
    "presencePenalty" : 0.0,
    "stop" : [ "" ],
    "countPenalty" : 0.0
  },
  "type" : "amazonBedrock/v1/askJurassic2"
}
```


### Ask Llama
Name: askLlama

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> meta.llama2-13b-chat-v1, meta.llama2-70b-chat-v1, meta.llama3-1-405b-instruct-v1:0, meta.llama3-1-70b-instruct-v1:0, meta.llama3-1-8b-instruct-v1:0, meta.llama3-2-11b-instruct-v1:0, meta.llama3-2-1b-instruct-v1:0, meta.llama3-2-3b-instruct-v1:0, meta.llama3-2-90b-instruct-v1:0, meta.llama3-70b-instruct-v1:0, meta.llama3-8b-instruct-v1:0 </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | null |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |


#### JSON Example
```json
{
  "label" : "Ask Llama",
  "name" : "askLlama",
  "parameters" : {
    "model" : "",
    "messages" : [ {
      "role" : "",
      "content" : "",
      "attachments" : [ {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      } ]
    } ],
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "maxTokens" : 1,
    "temperature" : 0.0,
    "topP" : 0.0
  },
  "type" : "amazonBedrock/v1/askLlama"
}
```


### Ask Titan
Name: askTitan

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> amazon.titan-text-express-v1, amazon.titan-text-lite-v1, amazon.titan-text-premier-v1:0 </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | null |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |


#### JSON Example
```json
{
  "label" : "Ask Titan",
  "name" : "askTitan",
  "parameters" : {
    "model" : "",
    "messages" : [ {
      "role" : "",
      "content" : "",
      "attachments" : [ {
        "extension" : "",
        "mimeType" : "",
        "name" : "",
        "url" : ""
      } ]
    } ],
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "maxTokens" : 1,
    "temperature" : 0.0,
    "topP" : 0.0,
    "stop" : [ "" ]
  },
  "type" : "amazonBedrock/v1/askTitan"
}
```




