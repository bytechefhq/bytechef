---
title: "Ollama"
description: "Get up and running with large language models."
---
## Reference
<hr />

Get up and running with large language models.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Url | STRING | TEXT  |  URL to your Ollama server  |





<hr />





## Actions


### Ask
Ask anything you want.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Model | STRING | SELECT  |  ID of the model to use.  |
| Messages | [{STRING\(content), STRING\(role)}] | ARRAY_BUILDER  |  A list of messages comprising the conversation so far.  |
| Response format | INTEGER | SELECT  |  In which format do you want the response to be in?  |
| Response Schema | STRING | TEXT_AREA  |  Define the JSON schema for the response.  |
| Keep alive for | STRING | TEXT  |  Controls how long the model will stay loaded into memory following the request  |
| Num predict | INTEGER | INTEGER  |  Maximum number of tokens to predict when generating text. (-1 = infinite generation, -2 = fill context)  |
| Temperature | NUMBER | NUMBER  |  Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic.  |
| Top P | NUMBER | NUMBER  |  An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered.  |
| Top K | INTEGER | INTEGER  |  Specify the number of token choices the generative uses to generate the next token.  |
| Frequency Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim.  |
| Presence Penalty | NUMBER | NUMBER  |  Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics.  |
| Stop | [STRING] | ARRAY_BUILDER  |  Up to 4 sequences where the API will stop generating further tokens.  |
| Functions | [STRING] | ARRAY_BUILDER  |  Enter the names of functions you want to use.  |
| Seed | INTEGER | INTEGER  |  Keeping the same seed would output the same response.  |
| Use NUMA | BOOLEAN | SELECT  |  Whether to use NUMA.  |
| Num CTX | INTEGER | INTEGER  |  Sets the size of the context window used to generate the next token.  |
| Num batch | INTEGER | INTEGER  |  Prompt processing maximum batch size.  |
| Num GPU | INTEGER | INTEGER  |  The number of layers to send to the GPU(s). On macOS it defaults to 1 to enable metal support, 0 to disable. 1 here indicates that NumGPU should be set dynamically  |
| Main GPU | INTEGER | INTEGER  |  When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of splitting the computation across all GPUs is not worthwhile. The GPU in question will use slightly more VRAM to store a scratch buffer for temporary results.  |
| Low VRAM | BOOLEAN | SELECT  |  |
| F16 KV | BOOLEAN | SELECT  |  |
| Logits all | BOOLEAN | SELECT  |  Return logits for all the tokens, not just the last one. To enable completions to return logprobs, this must be true.  |
| Vocab only | BOOLEAN | SELECT  |  Load only the vocabulary, not the weights.  |
| Use MMap | BOOLEAN | SELECT  |  By default, models are mapped into memory, which allows the system to load only the necessary parts of the model as needed. However, if the model is larger than your total amount of RAM or if your system is low on available memory, using mmap might increase the risk of pageouts, negatively impacting performance. Disabling mmap results in slower load times but may reduce pageouts if you’re not using mlock. Note that if the model is larger than the total amount of RAM, turning off mmap would prevent the model from loading at all.  |
| Use MLock | BOOLEAN | SELECT  |  Lock the model in memory, preventing it from being swapped out when memory-mapped. This can improve performance but trades away some of the advantages of memory-mapping by requiring more RAM to run and potentially slowing down load times as the model loads into RAM.  |
| Num thread | INTEGER | INTEGER  |  Sets the number of threads to use during computation. By default, Ollama will detect this for optimal performance. It is recommended to set this value to the number of physical CPU cores your system has (as opposed to the logical number of cores). 0 = let the runtime decide  |
| Nul keep | INTEGER | INTEGER  |  |
| Tfs Z | NUMBER | NUMBER  |  Tail-free sampling is used to reduce the impact of less probable tokens from the output. A higher value (e.g., 2.0) will reduce the impact more, while a value of 1.0 disables this setting.  |
| Typical P | NUMBER | NUMBER  |  |
| Repeat last N | INTEGER | INTEGER  |  Sets how far back for the model to look back to prevent repetition. (Default: 64, 0 = disabled, -1 = num_ctx)  |
| Repeat penalty | NUMBER | NUMBER  |  Sets how strongly to penalize repetitions. A higher value (e.g., 1.5) will penalize repetitions more strongly, while a lower value (e.g., 0.9) will be more lenient.  |
| Mirostat | INTEGER | INTEGER  |  Enable Mirostat sampling for controlling perplexity. (default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0)  |
| Mirostat Tau | NUMBER | NUMBER  |  Controls the balance between coherence and diversity of the output. A lower value will result in more focused and coherent text.  |
| Mirostat Eta | NUMBER | NUMBER  |  Influences how quickly the algorithm responds to feedback from the generated text. A lower learning rate will result in slower adjustments, while a higher learning rate will make the algorithm more responsive.  |
| Penalize new line | BOOLEAN | SELECT  |  |
| Truncate | BOOLEAN | SELECT  |  |




