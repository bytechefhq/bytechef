# PgVector Vector Store Component Design

**Date:** 2026-01-20
**Status:** Approved

## Overview

Implement PgVectorStore component using `org.springframework.ai.vectorstore.pgvector.PgVectorStore` to enable PostgreSQL-based vector storage for AI workflows.

## Component Structure

```
server/libs/modules/components/ai/vectorstore/pgvector/
├── build.gradle.kts
├── src/main/java/com/bytechef/component/ai/vectorstore/pgvector/
│   ├── PgVectorComponentHandler.java
│   ├── connection/
│   │   └── PgVectorConnection.java
│   ├── constant/
│   │   └── PgVectorConstants.java
│   ├── cluster/
│   │   └── PgVectorVectorStore.java
│   ├── action/
│   │   ├── PgVectorLoadAction.java
│   │   └── PgVectorSearchAction.java
│   └── task/handler/
│       ├── PgVectorLoadTaskHandler.java
│       └── PgVectorSearchTaskHandler.java
├── src/main/resources/assets/
│   └── pgvector.svg
└── src/test/java/.../PgVectorComponentHandlerTest.java
```

## Connection Properties

### Required
| Property | Type | Description |
|----------|------|-------------|
| `url` | string | PostgreSQL JDBC URL |
| `username` | string | Database username |
| `password` | string | Database password |

### Optional Configuration
| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `schemaName` | string | `public` | Database schema |
| `tableName` | string | `vector_store` | Vector table name |
| `dimensions` | integer | auto | Vector dimensions |
| `distanceType` | enum | `COSINE_DISTANCE` | Distance function |
| `indexType` | enum | `HNSW` | Index type |
| `initializeSchema` | boolean | `false` | Create table if not exists |
| `removeExistingTable` | boolean | `false` | Drop and recreate table |

## Dependencies

```kotlin
dependencies {
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    implementation("com.zaxxer:HikariCP")
    implementation("org.postgresql:postgresql")
}
```

## Implementation Notes

- Uses direct JDBC connection properties (URL, username, password)
- Creates HikariDataSource and JdbcTemplate per operation
- Follows existing vector store component patterns (Pinecone, Qdrant, etc.)
- Component name: `pgvector`
