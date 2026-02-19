---
description: "ByteChef Environment Variables. Complete reference for configuring AI, cloud, database, and application settings."
title: Environment Variables
---

ByteChef can be configured using environment variables. This page documents all available environment variables, organized by category.

## AI Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_COPILOT_ENABLED` | Enable or disable the AI copilot feature | `false` |
| `BYTECHEF_AI_COPILOT_PROVIDER` | The AI provider to use for copilot (OPENAI, ANTHROPIC) | `OPENAI` |
| `BYTECHEF_AI_COPILOT_OPENAI_API_KEY` | OpenAI API key for copilot (sensitive) | - |
| `BYTECHEF_AI_COPILOT_OPENAI_CHAT_OPTIONS_MODEL` | OpenAI model to use for chat | `chatgpt-4o-latest` |
| `BYTECHEF_AI_COPILOT_OPENAI_CHAT_OPTIONS_TEMPERATURE` | Temperature setting for OpenAI chat | `0.4` |

## AI Providers

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_AI_PROVIDER_ANTHROPIC_API_KEY` | Anthropic API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_AZURE_OPENAI_API_KEY` | Azure OpenAI API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_DEEP_SEEK_API_KEY` | DeepSeek API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_GROQ_API_KEY` | Groq API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_NVIDIA_API_KEY` | NVIDIA API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_HUGGING_FACE_API_KEY` | HuggingFace API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_MISTRAL_API_KEY` | Mistral API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_OPENAI_API_KEY` | OpenAI API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_PERPLEXITY_API_KEY` | Perplexity API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_STABILITY_API_KEY` | Stability API key (sensitive) | - |
| `BYTECHEF_AI_PROVIDER_VERTEX_GEMINI_API_KEY` | Vertex Gemini API key (sensitive) | - |

## Analytics Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_ANALYTICS_ENABLED` | Enable or disable analytics | `true` |

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
| `BYTECHEF_COORDINATOR_TRIGGER_SCHEDULER_PROVIDER` | Scheduler provider (AWS, QUARTZ) | `QUARTZ` |
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
| `BYTECHEF_DATA_STORAGE_PROVIDER` | Data storage provider (AWS, FILESYSTEM, JDBC) | `JDBC` |

## Discovery Service Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_DISCOVERY_SERVICE_PROVIDER` | Discovery service provider (REDIS) | `REDIS` |

## Edition Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_EDITION` | ByteChef edition (CE, EE) | `EE` |

## Encryption Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_ENCRYPTION_PROVIDER` | Encryption provider (FILESYSTEM, PROPERTY) | `FILESYSTEM` |
| `BYTECHEF_ENCRYPTION_PROPERTY_KEY` | Encryption key (sensitive) | - |

## Feature Flags

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_FEATURE_FLAGS` | List of enabled feature flags | - |

## File Storage Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_FILE_STORAGE_PROVIDER` | File storage provider (AWS, FILESYSTEM, JDBC) | `FILESYSTEM` |
| `BYTECHEF_FILE_STORAGE_FILESYSTEM_BASEDIR` | Base directory for filesystem storage | - |
| `BYTECHEF_FILE_STORAGE_AWS_BUCKET` | AWS S3 bucket name | - |

## Help Hub Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_HELP_HUB_ENABLED` | Enable or disable the help hub | `true` |

## Observability Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_OBSERVABILITY_LOKI_APPENDER_LEVEL` | Log level for Loki appender | `ALL` |
| `BYTECHEF_OBSERVABILITY_LOKI_APPENDER_HTTP_URL` | Loki HTTP URL | `http://localhost:3100/loki/api/v1/push` |

## Mail Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_MAIL_AUTH` | Enable mail authentication | `false` |
| `BYTECHEF_MAIL_BASE_URL` | Base URL for mail links | `BYTECHEF_PUBLIC_URL` |
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
| `BYTECHEF_MESSAGE_BROKER_PROVIDER` | Message broker provider (AMQP, AWS, JMS, KAFKA, LOCAL, REDIS) | `JMS` |

## OAuth2 Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_OAUTH2_REDIRECT_URI` | OAuth2 redirect URI | `BYTECHEF_PUBLIC_URL/callback` |
| `BYTECHEF_OAUTH2_PREDEFINED_APPS_<APP_NAME>_CLIENT_ID` | Client ID for predefined OAuth2 app (sensitive) | - |
| `BYTECHEF_OAUTH2_PREDEFINED_APPS_<APP_NAME>_CLIENT_SECRET` | Client secret for predefined OAuth2 app (sensitive) | - |

## Public URL Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_PUBLIC_URL` | Public URL of the ByteChef instance | `http://127.0.0.1:8080` |

## Resources Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_RESOURCES_WEB` | Web resources location | `file:///opt/bytechef/client/` |

## Security Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_SECURITY_CONTENT_SECURITY_POLICY` | Content Security Policy | `"default-src 'self'; frame-src 'self' https://.command.ai data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://.command.ai https://.commandbar.com https://.i.posthog.com https://cdn.jsdelivr.net https://storage.googleapis.com https://unpkg.com; style-src 'self' 'unsafe-inline' https://.commandbar.com https://cdn.jsdelivr.net https://.command.ai https://unpkg.com; img-src 'self' https://.command.ai data:; font-src 'self' data:; media-src 'self' https://.command.ai; connect-src 'self' https://.command.ai https://.i.posthog.com https://*.commandbar.com; worker-src blob: 'self';"` |
| `BYTECHEF_SECURITY_REMEMBER_ME_KEY` | Remember Me key (sensitive) | - |
| `BYTECHEF_SECURITY_SYSTEM_USERNAME` | System administrator username | `system_admin` |
| `BYTECHEF_SECURITY_SYSTEM_PASSWORD` | System administrator password (sensitive) | - |

System administrator is used for accessing protected data reachable through /actuator/** endpoints. For example /actuator/env returns all environment properties.

## Sign Up Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_SIGNUP_ACTIVATION_REQUIRED` | Require account activation | `false` |
| `BYTECHEF_SIGNUP_ENABLED` | Enable sign up | `true` |

## Tenant Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_TENANT_MODE` | Tenant mode (MULTI, SINGLE) | `SINGLE` |

## Webhook URL Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WEBHOOK_URL` | Webhook URL | `BYTECHEF_PUBLIC_URL/webhooks/{id}` |

## Tracing Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_TRACING_OTLP_ENDPOINT` | OpenTelemetry endpoint | `http://localhost:4318/v1/traces` |

## Worker Configuration

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WORKER_ENABLED` | Enable or disable the worker | `true` |
| `BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_DEFAULT` | Number of subscribers for default worker queue | `10` |
| `BYTECHEF_WORKER_TASK_SUBSCRIPTIONS_<EVENT_TYPE>` | Number of subscribers for specific worker queue | - |

## Workflow Configuration

### Output Storage

| Environment Variable | Description | Default Value |
|---|---|---|
| `BYTECHEF_WORKFLOW_OUTPUT_STORAGE_PROVIDER` | Output storage provider (AWS, FILESYSTEM, JDBC) | `JDBC` |

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