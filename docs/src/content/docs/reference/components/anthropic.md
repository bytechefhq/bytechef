---
title: "Anthropic"
description: "Anthropic is an AI safety and research company that's working to build reliable, interpretable, and steerable AI systems."
---

Anthropic is an AI safety and research company that's working to build reliable, interpretable, and steerable AI systems.


Categories: artificial-intelligence


Type: anthropic/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| token | Token | STRING | TEXT |  | true |





<hr />



## Actions


### Ask
Name: ask

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> claude-2.0, claude-2.1, claude-3-5-haiku-latest, claude-3-5-sonnet-latest, claude-3-haiku-20240307, claude-3-opus-latest, claude-3-sonnet-20240229, claude-instant-1.2 </details> | SELECT | ID of the model to use. | true |
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
  "label" : "Ask",
  "name" : "ask",
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
  "type" : "anthropic/v1/ask"
}
```




