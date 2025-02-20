---
title: "MistralAI"
description: "Open, efficient, helpful and trustworthy AI models through ground-breaking innovations."
---

Open, efficient, helpful and trustworthy AI models through ground-breaking innovations.


Categories: artificial-intelligence


Type: mistral/v1

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
| model | Model | STRING <details> <summary> Options </summary> mistral-large-latest, mistral-medium-latest, mistral-small-latest, open-mistral-7b, open-mixtral-8x22b, open-mixtral-8x7b, pixtral-12b-2409, pixtral-large-latest </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| maxTokens | Max Tokens | INTEGER | INTEGER | The maximum number of tokens to generate in the chat completion. | null |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |
| seed | Seed | INTEGER | INTEGER | Keeping the same seed would output the same response. | null |
| safePrompt | Safe prompt | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Should the prompt be safe for work? | null |


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
    "response" : {
      "responseFormat" : "",
      "responseSchema" : ""
    },
    "maxTokens" : 1,
    "temperature" : 0.0,
    "topP" : 0.0,
    "stop" : [ "" ],
    "seed" : 1,
    "safePrompt" : false
  },
  "type" : "mistral/v1/ask"
}
```




