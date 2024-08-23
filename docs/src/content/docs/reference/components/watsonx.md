---
title: "Watsonx AI"
description: "IBM watsonx.ai AI studio is part of the IBM watsonx AI and data platform, bringing together new generative AI (gen AI) capabilities powered by foundation models and traditional machine learning (ML) into a powerful studio spanning the AI lifecycle."
---
## Reference
<hr />

IBM watsonx.ai AI studio is part of the IBM watsonx AI and data platform, bringing together new generative AI (gen AI) capabilities powered by foundation models and traditional machine learning (ML) into a powerful studio spanning the AI lifecycle.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Url | STRING | TEXT  |  URL to connect to.  |
| Stream Endpoint | STRING | TEXT  |  The streaming endpoint.  |
| Text Endpoint | STRING | TEXT  |  The text endpoint.  |
| Project ID | STRING | TEXT  |  The project ID.  |
| IAM Token | STRING | TEXT  |  The IBM Cloud account IAM token.  |





<hr />





## Actions


### Ask
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | TEXT  |  Model is the identifier of the LLM Model to be used.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Decoding method | STRING | TEXT  |  Decoding is the process that a model uses to choose the tokens in the generated output.  |
| Repetition penalty | NUMBER | NUMBER  |  Sets how strongly to penalize repetitions. A higher value (e.g., 1.8) will penalize repetitions more strongly, while a lower value (e.g., 1.1) will be more lenient.  |
| Min tokens | INTEGER | INTEGER  |  Sets how many tokens must the LLM generate.  |
| Max tokens | INTEGER | INTEGER  |  The maximum number of tokens to generate in the chat completion.  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Seed | INTEGER | INTEGER  |  Keeping the same seed would output the same response.  |


### Output



Type: STRING







