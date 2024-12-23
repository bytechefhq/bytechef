---
title: "Pinecone"
description: "Pinecone is a vector database designed for efficient similarity search and storage of high-dimensional data, commonly used in machine learning and AI applications."
---
## Reference
<hr />

Pinecone is a vector database designed for efficient similarity search and storage of high-dimensional data, commonly used in machine learning and AI applications.


Categories: [artificial-intelligence]


Version: 1

<hr />



## Connections

Version: 1


### null

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Open AI API Key | STRING | TEXT  |  The API key for the OpenAI API which is used to generate embeddings.  |
| Pinecone API Key | STRING | TEXT  |  The API key for the Pinecone API.  |
| Environment | STRING | TEXT  |  Pinecone environment.  |
| Project ID | STRING | TEXT  |  Pinecone project ID.  |
| Index Name | STRING | TEXT  |  Pinecone index name.  |





<hr />





## Actions


### Data Query
Query data from a Pinecone vector store using OpenAI embeddings.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Query | STRING | TEXT  |  The query to be executed.  |




### Load Data
Loads data into a Pinecone vector store using OpenAI embeddings.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Document Type | STRING | SELECT  |  The type of the document.  |
| JSON Keys to Use | [STRING] | ARRAY_BUILDER  |  Json keys on which extraction of content is based. If no keys are specified, it uses the entire JSON object as content.  |
| FILE_ENTRY | FILE_ENTRY  |
| Use Token Text Splitter | BOOLEAN | SELECT  |  Whether to use the token text splitter.  |
| Token Text Splitter | {INTEGER\(defaultChunkSize), INTEGER\(minChunkSizeChars), INTEGER\(minChunkLengthToEmbed), INTEGER\(maxNumChunks), BOOLEAN\(keepSeparator)} | OBJECT_BUILDER  |  Splits text into chunks based on token count, using the CL100K_BASE encoding.  |
| Use Keyword Metadata Enricher | BOOLEAN | SELECT  |  Whether to use the keyword metadata enricher.  |
| Keyword Metadata Enricher | {INTEGER\(keywordCount)} | OBJECT_BUILDER  |  Extract keywords from document content and add them as metadata.  |
| Use Summary Metadata Enricher | BOOLEAN | SELECT  |  Whether to use the summary enricher.  |
| Summary Metadata Enricher | {[STRING]\(summaryTypes)} | OBJECT_BUILDER  |  Summarize the document content and add the summaries as metadata.  |




