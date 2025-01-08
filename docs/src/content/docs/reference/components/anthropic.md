---
title: "Anthropic"
description: "Anthropic is an AI safety and research company that's working to build reliable, interpretable, and steerable AI systems."
---
## Reference
<hr />

Anthropic is an AI safety and research company that's working to build reliable, interpretable, and steerable AI systems.


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
| Messages | [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Response | {INTEGER\(responseFormat), STRING\(responseSchema)} | OBJECT_BUILDER  |  The response from the API.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |




