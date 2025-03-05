---
title: "Weaviate"
description: "Weaviate is an open-source vector search engine and database that enables efficient storage, retrieval, and management of high-dimensional data, often used in machine learning and AI applications."
---

Weaviate is an open-source vector search engine and database that enables efficient storage, retrieval, and management of high-dimensional data, often used in machine learning and AI applications.


Categories: artificial-intelligence


Type: weaviate/v1

<hr />



## Connections

Version: 1


### custom

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| embeddingApiKey | Open AI API Key | STRING | The API key for the OpenAI API which is used to generate embeddings. | true |
| url | Weaviate Url | STRING | The URL of the Weaviate instance. | true |
| apiKey | Weaviate API Key | STRING | The API key for the Weaviate API. | true |





<hr />



## Actions


### Data Query
Name: dataQuery

Query data from a Weaviate vector store using OpenAI embeddings.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| query | Query | STRING | The query to be executed. | true |

#### Example JSON Structure
```json
{
  "label" : "Data Query",
  "name" : "dataQuery",
  "parameters" : {
    "query" : ""
  },
  "type" : "weaviate/v1/dataQuery"
}
```

#### Output

The output for this action is dynamic and may vary depending on the input parameters. To determine the exact structure of the output, you need to execute the action.




### Load Data
Name: loadData

Loads data into a Pinecone vector store using OpenAI embeddings.

#### Properties

|      Name       |      Label     |     Type     |     Description     | Required |
|:---------------:|:--------------:|:------------:|:-------------------:|:--------:|
| documentType | Document Type | STRING <details> <summary> Options </summary> JSON, MD, PDF, TXT, TIKA </details> | The type of the document. | true |
| jsonKeysToUse | JSON Keys to Use | ARRAY <details> <summary> Items </summary> [STRING] </details> | Json keys on which extraction of content is based. If no keys are specified, it uses the entire JSON object as content. | false |
| document | | FILE_ENTRY |  | true |
| useTokenTextSplitter | Use Token Text Splitter | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether to use the token text splitter. | true |
| tokenTextSplitter | Token Text Splitter | OBJECT <details> <summary> Properties </summary> {INTEGER\(defaultChunkSize), INTEGER\(minChunkSizeChars), INTEGER\(minChunkLengthToEmbed), INTEGER\(maxNumChunks), BOOLEAN\(keepSeparator)} </details> | Splits text into chunks based on token count, using the CL100K_BASE encoding. | true |
| useKeywordEnricher | Use Keyword Metadata Enricher | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether to use the keyword metadata enricher. | true |
| keywordMetadataEnricher | Keyword Metadata Enricher | OBJECT <details> <summary> Properties </summary> {INTEGER\(keywordCount)} </details> | Extract keywords from document content and add them as metadata. | true |
| useSummaryEnricher | Use Summary Metadata Enricher | BOOLEAN <details> <summary> Options </summary> true, false </details> | Whether to use the summary enricher. | true |
| summaryMetadataEnricher | Summary Metadata Enricher | OBJECT <details> <summary> Properties </summary> {[STRING]\(summaryTypes)} </details> | Summarize the document content and add the summaries as metadata. | true |

#### Example JSON Structure
```json
{
  "label" : "Load Data",
  "name" : "loadData",
  "parameters" : {
    "documentType" : "",
    "jsonKeysToUse" : [ "" ],
    "document" : {
      "extension" : "",
      "mimeType" : "",
      "name" : "",
      "url" : ""
    },
    "useTokenTextSplitter" : false,
    "tokenTextSplitter" : {
      "defaultChunkSize" : 1,
      "minChunkSizeChars" : 1,
      "minChunkLengthToEmbed" : 1,
      "maxNumChunks" : 1,
      "keepSeparator" : false
    },
    "useKeywordEnricher" : false,
    "keywordMetadataEnricher" : {
      "keywordCount" : 1
    },
    "useSummaryEnricher" : false,
    "summaryMetadataEnricher" : {
      "summaryTypes" : [ "" ]
    }
  },
  "type" : "weaviate/v1/loadData"
}
```

#### Output

This action does not produce any output.






