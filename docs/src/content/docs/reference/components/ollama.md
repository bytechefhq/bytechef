---
title: "Ollama"
description: "Get up and running with large language models."
---

Get up and running with large language models.


Categories: artificial-intelligence


Type: ollama/v1

<hr />



## Connections

Version: 1


### Bearer Token

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| url | Url | STRING | TEXT | URL to your Ollama server | null |





<hr />



## Actions


### Ask
Name: ask

Ask anything you want.

#### Properties

|      Name       |      Label     |     Type     |    Control Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:-------------------:|:--------:|
| model | Model | STRING <details> <summary> Options </summary> codellama, dolphin-phi, gemma, llama2, llama2-uncensored, llama3, llama3.1, llama3.2, llama3.2-vision, llama3.2-vision:90b, llama3.2:1b, llava, mistral, mistral-nemo, moondream, mxbai-embed-large, neural-chat, nomic-embed-text, orca-mini, phi, phi3, qwen2.5, starling-lm </details> | SELECT | ID of the model to use. | true |
| messages | Messages | ARRAY <details> <summary> Items </summary> [{STRING\(role), STRING\(content), [FILE_ENTRY]\(attachments)}] </details> | ARRAY_BUILDER | A list of messages comprising the conversation so far. | true |
| response | Response | OBJECT <details> <summary> Properties </summary> {STRING\(responseFormat), STRING\(responseSchema)} </details> | OBJECT_BUILDER | The response from the API. | false |
| keepAlive | Keep alive for | STRING | TEXT | Controls how long the model will stay loaded into memory following the request | null |
| maxTokens | Num predict | INTEGER | INTEGER | Maximum number of tokens to predict when generating text. (-1 = infinite generation, -2 = fill context) | null |
| temperature | Temperature | NUMBER | NUMBER | Controls randomness:  Higher values will make the output more random, while lower values like will make it more focused and deterministic. | null |
| topP | Top P | NUMBER | NUMBER | An alternative to sampling with temperature, called nucleus sampling,  where the model considers the results of the tokens with top_p probability mass. So 0.1 means only the tokens comprising the top 10% probability mass are considered. | null |
| topK | Top K | INTEGER | INTEGER | Specify the number of token choices the generative uses to generate the next token. | null |
| frequencyPenalty | Frequency Penalty | NUMBER | NUMBER | Number between -2.0 and 2.0. Positive values penalize new tokens based on their existing frequency in the text so far, decreasing the model's likelihood to repeat the same line verbatim. | null |
| presencePenalty | Presence Penalty | NUMBER | NUMBER | Number between -2.0 and 2.0. Positive values penalize new tokens based on whether they appear in the text so far, increasing the model's likelihood to talk about new topics. | null |
| stop | Stop | ARRAY <details> <summary> Items </summary> [STRING] </details> | ARRAY_BUILDER | Up to 4 sequences where the API will stop generating further tokens. | null |
| seed | Seed | INTEGER | INTEGER | Keeping the same seed would output the same response. | null |
| useNuma | Use NUMA | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Whether to use NUMA. | null |
| numCtx | Num CTX | INTEGER | INTEGER | Sets the size of the context window used to generate the next token. | null |
| numBatch | Num batch | INTEGER | INTEGER | Prompt processing maximum batch size. | null |
| numGpu | Num GPU | INTEGER | INTEGER | The number of layers to send to the GPU(s). On macOS it defaults to 1 to enable metal support, 0 to disable. 1 here indicates that NumGPU should be set dynamically | null |
| mainGpu | Main GPU | INTEGER | INTEGER | When using multiple GPUs this option controls which GPU is used for small tensors for which the overhead of splitting the computation across all GPUs is not worthwhile. The GPU in question will use slightly more VRAM to store a scratch buffer for temporary results. | null |
| lowVram | Low VRAM | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |  | null |
| f16kv | F16 KV | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |  | null |
| logitsAll | Logits all | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Return logits for all the tokens, not just the last one. To enable completions to return logprobs, this must be true. | null |
| vocabOnly | Vocab only | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Load only the vocabulary, not the weights. | null |
| useMmap | Use MMap | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | By default, models are mapped into memory, which allows the system to load only the necessary parts of the model as needed. However, if the model is larger than your total amount of RAM or if your system is low on available memory, using mmap might increase the risk of pageouts, negatively impacting performance. Disabling mmap results in slower load times but may reduce pageouts if you’re not using mlock. Note that if the model is larger than the total amount of RAM, turning off mmap would prevent the model from loading at all. | null |
| useMlock | Use MLock | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT | Lock the model in memory, preventing it from being swapped out when memory-mapped. This can improve performance but trades away some of the advantages of memory-mapping by requiring more RAM to run and potentially slowing down load times as the model loads into RAM. | null |
| numThread | Num thread | INTEGER | INTEGER | Sets the number of threads to use during computation. By default, Ollama will detect this for optimal performance. It is recommended to set this value to the number of physical CPU cores your system has (as opposed to the logical number of cores). 0 = let the runtime decide | null |
| numKeep | Nul keep | INTEGER | INTEGER |  | null |
| tfsz | Tfs Z | NUMBER | NUMBER | Tail-free sampling is used to reduce the impact of less probable tokens from the output. A higher value (e.g., 2.0) will reduce the impact more, while a value of 1.0 disables this setting. | null |
| typicalP | Typical P | NUMBER | NUMBER |  | null |
| repeatLastN | Repeat last N | INTEGER | INTEGER | Sets how far back for the model to look back to prevent repetition. (Default: 64, 0 = disabled, -1 = num_ctx) | null |
| repeatPenalty | Repeat penalty | NUMBER | NUMBER | Sets how strongly to penalize repetitions. A higher value (e.g., 1.5) will penalize repetitions more strongly, while a lower value (e.g., 0.9) will be more lenient. | null |
| mirostat | Mirostat | INTEGER | INTEGER | Enable Mirostat sampling for controlling perplexity. (default: 0, 0 = disabled, 1 = Mirostat, 2 = Mirostat 2.0) | null |
| mirostatTau | Mirostat Tau | NUMBER | NUMBER | Controls the balance between coherence and diversity of the output. A lower value will result in more focused and coherent text. | null |
| mirostatEta | Mirostat Eta | NUMBER | NUMBER | Influences how quickly the algorithm responds to feedback from the generated text. A lower learning rate will result in slower adjustments, while a higher learning rate will make the algorithm more responsive. | null |
| penalizeNewLine | Penalize new line | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |  | null |
| truncate | Truncate | BOOLEAN <details> <summary> Options </summary> true, false </details> | SELECT |  | null |


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
    "keepAlive" : "",
    "maxTokens" : 1,
    "temperature" : 0.0,
    "topP" : 0.0,
    "topK" : 1,
    "frequencyPenalty" : 0.0,
    "presencePenalty" : 0.0,
    "stop" : [ "" ],
    "seed" : 1,
    "useNuma" : false,
    "numCtx" : 1,
    "numBatch" : 1,
    "numGpu" : 1,
    "mainGpu" : 1,
    "lowVram" : false,
    "f16kv" : false,
    "logitsAll" : false,
    "vocabOnly" : false,
    "useMmap" : false,
    "useMlock" : false,
    "numThread" : 1,
    "numKeep" : 1,
    "tfsz" : 0.0,
    "typicalP" : 0.0,
    "repeatLastN" : 1,
    "repeatPenalty" : 0.0,
    "mirostat" : 1,
    "mirostatTau" : 0.0,
    "mirostatEta" : 0.0,
    "penalizeNewLine" : false,
    "truncate" : false
  },
  "type" : "ollama/v1/ask"
}
```




