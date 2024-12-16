---
title: "MistralAI"
description: "Open, efficient, helpful and trustworthy AI models through ground-breaking innovations."
---
## Reference
<hr />

Open, efficient, helpful and trustworthy AI models through ground-breaking innovations.


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
| Messages | [{STRING\(content), FILE_ENTRY\(image), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response Format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Response Schema | STRING | JSON_SCHEMA_BUILDER  |  Define the JSON schema for the response.  |
| Max Tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Seed | INTEGER | INTEGER  |  Keeping the same seed would output the same response.  |
| Safe prompt | BOOLEAN | SELECT  |  Should the prompt be safe for work?  |




