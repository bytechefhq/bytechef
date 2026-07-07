---
title: Environment Variables
description: Configuration options for ByteChef through environment variables
---

ByteChef can be configured using environment variables. This page documents all available environment variables, organized by category.

## AI Copilot Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_COPILOT_ENABLED` | Enable or disable the AI copilot feature | `false` |
| `BYTECHEF_AI_COPILOT_PROVIDER` | Explicit CE chat-model provider key (e.g. `openai`) to use for Copilot, overriding auto-detection from the configured provider API keys/endpoints. Ignored when the EE AI Providers catalog is active. | - |
| `BYTECHEF_AI_COPILOT_DOCS_EMBEDDING_PROVIDER` | Embedding provider for the Copilot documentation index (OLLAMA, OPENAI) | - |
| `BYTECHEF_AI_COPILOT_DOCS_EMBEDDING_APIKEY` | API key for the Copilot documentation embedding provider — OpenAI only; Ollama runs locally and needs none (sensitive) | - |

## AI Firecrawl Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_FIRECRAWL_APIKEY` | Firecrawl API key (sensitive) | - |
| `BYTECHEF_AI_FIRECRAWL_BASEURL` | Firecrawl API base URL | `https://api.firecrawl.dev/v2` |
| `BYTECHEF_AI_FIRECRAWL_ENABLED` | Enable or disable Firecrawl | `false` |

## AI Knowledge Base Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_KNOWLEDGEBASE_ENABLED` | Enable or disable knowledge base AI features | `false` |
| `BYTECHEF_AI_KNOWLEDGEBASE_OCR_PROVIDER` | OCR provider for knowledge base documents (NONE, AZURE, MISTRAL) | `NONE` |
| `BYTECHEF_AI_KNOWLEDGEBASE_OCR_MISTRAL_APIKEY` | Mistral OCR API key (sensitive) | - |
| `BYTECHEF_AI_KNOWLEDGEBASE_SUBSCRIPTIONS_DOCUMENT_PROCESS_EVENTS` | Number of subscribers for document process events | `1` |
| `BYTECHEF_AI_KNOWLEDGEBASE_SUBSCRIPTIONS_DOCUMENT_CHUNK_UPDATE_EVENTS` | Number of subscribers for document chunk update events | `1` |

## AI MCP Server Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_MCP_SERVER_ENABLED` | Enable or disable the MCP (Model Context Protocol) server | `false` |

## AI Memory Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_MEMORY_PROVIDER` | Memory storage provider for chat-style interactions (AWS, IN_MEMORY, JDBC, REDIS) | `JDBC` |
| `BYTECHEF_AI_MEMORY_AWS_BUCKETPREFIX` | Prefix used to derive the per-tenant S3 bucket name (provider `AWS`) | `bytechef-chat-memory` |
| `BYTECHEF_AI_MEMORY_AWS_REGION` | AWS region for S3-backed chat memory (provider `AWS`) | - |
| `BYTECHEF_AI_MEMORY_AWS_ACCESSKEYID` | AWS access key ID for S3-backed chat memory (sensitive) | - |
| `BYTECHEF_AI_MEMORY_AWS_SECRETACCESSKEY` | AWS secret access key for S3-backed chat memory (sensitive) | - |
| `BYTECHEF_AI_MEMORY_AWS_KEYPREFIX` | Key prefix prepended to every stored object key (provider `AWS`) | - |

## AI Provider API Keys

| Environment Variable                        | Description | Default Value |
|---------------------------------------------|---|---|
| `BYTECHEF_AI_PROVIDER_ANTHROPIC_APIKEY`     | Anthropic API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_AZURE_OPENAI_APIKEY`  | Azure OpenAI API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_AZURE_OPENAI_ENDPOINT` | Azure OpenAI resource endpoint, e.g. `https://my-resource.openai.azure.com` | - |
| `BYTECHEF_AI_PROVIDER_DEEP_SEEK_APIKEY`     | DeepSeek API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_GROQ_APIKEY`          | Groq API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_MISTRAL_APIKEY`       | Mistral API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_NVIDIA_APIKEY`        | NVIDIA API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_OLLAMA_APIKEY`        | Ollama API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_OLLAMA_URL`           | Ollama server base URL; fallback for chat and embedding models (defaults to `http://localhost:11434` when blank) | - |
| `BYTECHEF_AI_PROVIDER_OPENAI_APIKEY`        | OpenAI API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_PERPLEXITY_APIKEY`    | Perplexity API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_STABILITY_APIKEY`     | Stability API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_VERTEX_GEMINI_APIKEY` | Vertex Gemini API key (sensitive) | - |

## AI Chat Model Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_PROVIDER_CHAT_ANTHROPIC_OPTIONS_MODEL` | Anthropic chat model name | `claude-sonnet-4-6` |
| `BYTECHEF_AI_PROVIDER_CHAT_ANTHROPIC_OPTIONS_TEMPERATURE` | Anthropic chat temperature (0.0-1.0) | `0.5` |
| `BYTECHEF_AI_PROVIDER_CHAT_AZURE_OPENAI_OPTIONS_MODEL` | Azure OpenAI chat model name (e.g., `gpt-4o`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_DEEP_SEEK_OPTIONS_MODEL` | DeepSeek chat model name (e.g., `deepseek-chat`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_GROQ_OPTIONS_MODEL` | Groq chat model name (e.g., `llama-3.3-70b-versatile`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_MISTRAL_OPTIONS_MODEL` | Mistral chat model name (e.g., `mistral-large-latest`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_NVIDIA_OPTIONS_MODEL` | NVIDIA chat model name (e.g., `meta/llama-3.1-70b-instruct`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_OLLAMA_OPTIONS_MODEL` | Ollama chat model name (e.g., `llama3.1`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_OPENAI_OPTIONS_MODEL` | OpenAI chat model name | `gpt-5.1` |
| `BYTECHEF_AI_PROVIDER_CHAT_OPENAI_OPTIONS_TEMPERATURE` | OpenAI chat temperature (0.0-2.0) | `1` |
| `BYTECHEF_AI_PROVIDER_CHAT_OPENAI_OPTIONS_REASONINGEFFECT` | OpenAI reasoning effect (NONE, LOW, MEDIUM, HIGH) | `MEDIUM` |
| `BYTECHEF_AI_PROVIDER_CHAT_OPENAI_OPTIONS_VERBOSITY` | OpenAI response verbosity (NONE, LOW, MEDIUM, HIGH) | `LOW` |
| `BYTECHEF_AI_PROVIDER_CHAT_PERPLEXITY_OPTIONS_MODEL` | Perplexity chat model name (e.g., `sonar`) | - |
| `BYTECHEF_AI_PROVIDER_CHAT_VERTEX_GEMINI_OPTIONS_MODEL` | Vertex Gemini chat model name (e.g., `gemini-1.5-pro`) | - |

## AI Embedding Model Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_PROVIDER_EMBEDDING_MISTRAL_OPTIONS_MODEL` | Mistral embedding model name (e.g., `mistral-embed`) | - |
| `BYTECHEF_AI_PROVIDER_EMBEDDING_OLLAMA_OPTIONS_MODEL` | Ollama embedding model name | `qwen3-embedding:8b` |
| `BYTECHEF_AI_PROVIDER_EMBEDDING_OPENAI_OPTIONS_MODEL` | OpenAI embedding model name | `text-embedding-3-small` |

## AI Vectorstore Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_VECTORSTORE_PROVIDER` | Vector store provider (PGVECTOR) | `PGVECTOR` |
| `BYTECHEF_AI_VECTORSTORE_PGVECTOR_URL` | JDBC URL for the pgvector PostgreSQL database | - |
| `BYTECHEF_AI_VECTORSTORE_PGVECTOR_USERNAME` | pgvector database username (sensitive) | - |
| `BYTECHEF_AI_VECTORSTORE_PGVECTOR_PASSWORD` | pgvector database password (sensitive) | - |

## Analytics Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_ANALYTICS_ENABLED` | Enable or disable analytics | `false` |

## Cache Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_CACHE_PROVIDER` | Cache provider (REDIS, CAFFEINE) | `CAFFEINE` |

## Cloud Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_CLOUD_PROVIDER` | Cloud provider (AWS, NONE) | `NONE` |
| `BYTECHEF_CLOUD_AWS_ACCESS_KEY_ID` | AWS access key ID (sensitive) | - |
| `BYTECHEF_CLOUD_AWS_SECRET_ACCESS_KEY` | AWS secret access key (sensitive) | - |
| `BYTECHEF_CLOUD_AWS_REGION` | AWS region | - |
| `BYTECHEF_CLOUD_AWS_ACCOUNT_ID` | AWS account ID | - |

## Component Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_COMPONENT_REGISTRY_EXCLUDE` | List of components to exclude from registry | - |

## Coordinator Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_COORDINATOR_ENABLED` | Enable or disable the coordinator | `true` |

## Coordinator Task Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_COORDINATOR_TASK_SUBSCRIPTIONS_APPLICATION_EVENTS` | Number of application event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TASK_SUBSCRIPTIONS_RESUME_JOB_EVENTS` | Number of resume job event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TASK_SUBSCRIPTIONS_START_JOB_EVENTS` | Number of start job event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TASK_SUBSCRIPTIONS_STOP_JOB_EVENTS` | Number of stop job event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TASK_SUBSCRIPTIONS_TASK_EXECUTION_COMPLETE_EVENTS` | Number of task execution complete event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TASK_SUBSCRIPTIONS_TASK_EXECUTION_ERROR_EVENTS` | Number of task execution error event subscribers | `1` |

## Coordinator Trigger Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_COORDINATOR_TRIGGER_POLLING_CHECKPERIOD` | Trigger polling interval in minutes | `5` |
| `BYTECHEF_COORDINATOR_TRIGGER_SUBSCRIPTIONS_APPLICATION_EVENTS` | Number of application event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TRIGGER_SUBSCRIPTIONS_TRIGGER_EXECUTION_COMPLETE_EVENTS` | Number of trigger execution complete event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TRIGGER_SUBSCRIPTIONS_TRIGGER_EXECUTION_ERROR_EVENTS` | Number of trigger execution error event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TRIGGER_SUBSCRIPTIONS_TRIGGER_LISTENER_EVENTS` | Number of trigger listener event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TRIGGER_SUBSCRIPTIONS_TRIGGER_POLL_EVENTS` | Number of trigger poll event subscribers | `1` |
| `BYTECHEF_COORDINATOR_TRIGGER_SUBSCRIPTIONS_TRIGGER_WEBHOOK_EVENTS` | Number of trigger webhook event subscribers | `1` |

## Datasource Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_DATASOURCE_URL` | Database URL | - |
| `BYTECHEF_DATASOURCE_USERNAME` | Database username (sensitive) | - |
| `BYTECHEF_DATASOURCE_PASSWORD` | Database password (sensitive) | - |

## Data Storage Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_DATASTORAGE_PROVIDER` | Data storage provider (AWS, FILESYSTEM, JDBC) | `JDBC` |

## Discovery Service Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_DISCOVERY_SERVICE_PROVIDER` | Discovery service provider (REDIS) | `REDIS` |

## Edition Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_EDITION` | ByteChef edition (CE, EE) | `EE` |

## Environment Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_ENVIRONMENT` | Restrict the instance to a single environment (DEVELOPMENT, STAGING, PRODUCTION) | - |

## Encryption Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_ENCRYPTION_PROVIDER` | Encryption provider (FILESYSTEM, PROPERTY) | `FILESYSTEM` |
| `BYTECHEF_ENCRYPTION_PROPERTY_KEY` | Encryption key (sensitive) | - |

## Feature Flags

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_FEATUREFLAGS` | List of enabled feature flags | - |

## File Storage Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_FILESTORAGE_PROVIDER` | File storage provider (AWS, FILESYSTEM, JDBC) | `FILESYSTEM` |
| `BYTECHEF_FILESTORAGE_FILESYSTEM_BASEDIR` | Base directory for filesystem storage | `${user.home}/bytechef/data/file-storage` |
| `BYTECHEF_FILESTORAGE_AWS_BUCKET` | AWS S3 bucket name | - |

## Help Hub Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_HELPHUB_ENABLED` | Enable or disable the help hub | `false` |

## Kafka Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_KAFKA_BOOTSTRAP_SERVERS` | Comma-separated list of Kafka bootstrap servers (`host:port`) | - |
| `BYTECHEF_KAFKA_CONSUMER_GROUP_ID` | Kafka consumer group identifier | - |

## Mail Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_MAIL_AUTH` | Enable mail authentication | `false` |
| `BYTECHEF_MAIL_BASEURL` | Base URL for mail links | `BYTECHEF_PUBLICURL` |
| `BYTECHEF_MAIL_DEBUG` | Enable mail debug | `false` |
| `BYTECHEF_MAIL_FROM` | From email address | `noreply@app.bytechef.io` |
| `BYTECHEF_MAIL_HOST` | Mail server host | `localhost` |
| `BYTECHEF_MAIL_PASSWORD` | Mail password (sensitive) | - |
| `BYTECHEF_MAIL_PORT` | Mail server port | `25` |
| `BYTECHEF_MAIL_PROTOCOL` | Mail protocol | `smtp` |
| `BYTECHEF_MAIL_SSL_ENABLED` | Enable SSL for mail | `false` |
| `BYTECHEF_MAIL_STARTTLS_ENABLE` | Enable STARTTLS | `false` |
| `BYTECHEF_MAIL_STARTTLS_REQUIRED` | Require STARTTLS | `false` |
| `BYTECHEF_MAIL_USERNAME` | Mail username (sensitive) | - |

## Message Broker Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_MESSAGEBROKER_PROVIDER` | Message broker provider (AMQP, AWS, JMS, KAFKA, MEMORY, REDIS) | `MEMORY` |

## OAuth2 Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_OAUTH2_REDIRECT_URI` | OAuth2 redirect URI | `BYTECHEF_PUBLICURL/callback` |
| `BYTECHEF_OAUTH2_PREDEFINED_APPS_<APP_NAME>_CLIENT_ID` | Client ID for predefined OAuth2 app (sensitive) | - |
| `BYTECHEF_OAUTH2_PREDEFINED_APPS_<APP_NAME>_CLIENT_SECRET` | Client secret for predefined OAuth2 app (sensitive) | - |

## Public URL Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_PUBLICURL` | Public URL of the ByteChef instance | `http://127.0.0.1:8080` |

## RabbitMQ Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_RABBITMQ_HOST` | RabbitMQ server hostname | - |
| `BYTECHEF_RABBITMQ_PORT` | RabbitMQ server port | `5672` |
| `BYTECHEF_RABBITMQ_USERNAME` | RabbitMQ username (sensitive) | - |
| `BYTECHEF_RABBITMQ_PASSWORD` | RabbitMQ password (sensitive) | - |

## Redis Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_REDIS_HOST` | Redis server hostname | - |
| `BYTECHEF_REDIS_PORT` | Redis server port | `6379` |
| `BYTECHEF_REDIS_PASSWORD` | Redis password (sensitive) | - |
| `BYTECHEF_REDIS_DATABASE` | Redis database index | `0` |
| `BYTECHEF_REDIS_TIMEOUT` | Connection timeout in milliseconds | `0` |

## Resources Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_RESOURCES_WEB` | Web resources location | `file:///opt/bytechef/client/` |

## Security Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_SECURITY_CONTENT_SECURITY_POLICY` | Content Security Policy | `"default-src 'self'; frame-src 'self' https://*.command.ai data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://*.command.ai https://*.commandbar.com https://*.i.posthog.com https://cdn.jsdelivr.net https://storage.googleapis.com https://unpkg.com; style-src 'self' 'unsafe-inline' https://*.commandbar.com https://cdn.jsdelivr.net https://*.command.ai https://unpkg.com; img-src 'self' https://*.command.ai data:; font-src 'self' data:; media-src 'self' https://*.command.ai; connect-src 'self' https://*.command.ai https://*.i.posthog.com https://*.commandbar.com; worker-src blob: 'self';"` |
| `BYTECHEF_SECURITY_REMEMBER_ME_KEY` | Remember Me key (sensitive) | - |
| `BYTECHEF_SECURITY_SOCIALLOGIN_ENABLED` | Enable social login | `false` |
| `BYTECHEF_SECURITY_SOCIALLOGIN_GOOGLE_CLIENTID` | Google OAuth2 client ID (sensitive) | - |
| `BYTECHEF_SECURITY_SOCIALLOGIN_GOOGLE_CLIENTSECRET` | Google OAuth2 client secret (sensitive) | - |
| `BYTECHEF_SECURITY_SOCIALLOGIN_GITHUB_CLIENTID` | GitHub OAuth2 client ID (sensitive) | - |
| `BYTECHEF_SECURITY_SOCIALLOGIN_GITHUB_CLIENTSECRET` | GitHub OAuth2 client secret (sensitive) | - |
| `BYTECHEF_SECURITY_SSO_ENABLED` | Enable Single Sign-On | `false` |
| `BYTECHEF_SECURITY_TWOFACTORAUTHENTICATION_ENABLED` | Enable two-factor authentication | `false` |
| `BYTECHEF_SECURITY_SYSTEM_USERNAME` | System administrator username | `system_admin` |
| `BYTECHEF_SECURITY_SYSTEM_PASSWORD` | System administrator password (sensitive) | - |

System administrator is used for accessing protected data reachable through /actuator/** endpoints. For example /actuator/env returns all environment properties.

## Scheduler Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_SCHEDULER_PROVIDER` | Scheduler provider (AWS, QUARTZ) | `QUARTZ` |

## Sign Up Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_SIGNUP_ACTIVATIONREQUIRED` | Require account activation | `false` |
| `BYTECHEF_SIGNUP_ENABLED` | Enable sign up | `true` |

## Tenant Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_TENANT_MODE` | Tenant mode (MULTI, SINGLE) | `SINGLE` |

## Upgrade Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_UPGRADE_ENABLED` | Run database upgrades (Liquibase migrations) at startup. Disable on read-only replicas or when only a designated instance should apply schema changes in a multi-instance deployment. | `true` |

## User Guiding Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_USERGUIDING_ENABLED` | Enable or disable user guiding (in-application tutorials) | `false` |
| `BYTECHEF_USERGUIDING_CONTAINER_ID` | Container ID for the UserGuiding SDK | - |

## Webhook URL Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WEBHOOKURL` | Webhook URL | `BYTECHEF_PUBLICURL/webhooks/{id}` |

## Observability Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_OBSERVABILITY_LOGGING_ENABLED` | Enable or disable OTel logging | `false` |
| `BYTECHEF_OBSERVABILITY_LOGGING_ENDPOINT` | OTel logging endpoint URL | `http://localhost:4318/v1/logs` |
| `BYTECHEF_OBSERVABILITY_METRICS_ENABLED` | Enable or disable OTel metrics | `false` |
| `BYTECHEF_OBSERVABILITY_METRICS_ENDPOINT` | OTel metrics endpoint URL | `http://localhost:4318/v1/metrics` |
| `BYTECHEF_OBSERVABILITY_TRACING_ENABLED` | Enable or disable OTel tracing | `false` |
| `BYTECHEF_OBSERVABILITY_TRACING_ENDPOINT` | OTel tracing endpoint URL | `http://localhost:4318/v1/traces` |

## Worker Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WORKER_ENABLED` | Enable or disable the worker | `true` |
| `BYTECHEF_WORKER_TASK_DEFAULT_TIMEOUT` | Default timeout for task execution in milliseconds | - |
| `BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_DEFAULT` | Number of concurrent consumers for the `default` worker queue | `10` |
| `BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_<QUEUE_NAME>` | Number of concurrent consumers for an additional worker queue (e.g., `captions` for tasks routed via `node: captions`). The queue must be created before tasks can be routed to it; ByteChef creates the queue automatically when the worker bootstraps if it doesn't already exist. | - |

## Workflow Configuration

### Output Storage

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WORKFLOW_OUTPUTSTORAGE_PROVIDER` | Output storage provider (AWS, FILESYSTEM, JDBC) | `JDBC` |

### Repository Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WORKFLOW_REPOSITORY_CLASSPATH_ENABLED` | Enable classpath repository | `false` |
| `BYTECHEF_WORKFLOW_REPOSITORY_CLASSPATH_LOCATION_PATTERN` | Classpath location pattern | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_ENABLED` | Enable filesystem repository | `false` |
| `BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_LOCATION_PATTERN` | Filesystem location pattern | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_GIT_ENABLED` | Enable Git repository | `false` |
| `BYTECHEF_WORKFLOW_REPOSITORY_GIT_URL` | Git repository URL | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_GIT_BRANCH` | Git repository branch | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_GIT_USERNAME` | Git username (sensitive) | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_GIT_PASSWORD` | Git password (sensitive) | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_GIT_SEARCH_PATHS` | Git search paths | - |
| `BYTECHEF_WORKFLOW_REPOSITORY_JDBC_ENABLED` | Enable JDBC repository | `true` |
