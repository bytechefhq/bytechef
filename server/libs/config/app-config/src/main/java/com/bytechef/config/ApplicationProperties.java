/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.config;

import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application configuration properties. Contains all configurable properties for the platform, including AI, messaging,
 * storage, security, and workflow settings.
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef", ignoreUnknownFields = false)
@SuppressFBWarnings("EI")
public class ApplicationProperties {

    /**
     * Application edition type.
     */
    public enum Edition {
        /**
         * Community Edition
         */
        CE,
        /**
         * Enterprise Edition
         */
        EE
    }

    /**
     * AI and machine learning configuration
     */
    private Ai ai = new Ai();

    /**
     * Analytics configuration
     */
    private Analytics analytics = new Analytics();

    /**
     * Cache provider configuration
     */
    private Cache cache = new Cache();

    /**
     * Cloud provider configuration
     */
    private Cloud cloud = new Cloud();

    /**
     * Component registry configuration
     */
    private Component component = new Component();

    /**
     * Workflow coordinator configuration
     */
    private Coordinator coordinator = new Coordinator();

    /**
     * Database datasource configuration
     */
    private Datasource datasource = new Datasource();

    /**
     * Data storage provider configuration
     */
    private DataStorage dataStorage;

    /**
     * Service discovery configuration
     */
    private DiscoveryService discoveryService = new DiscoveryService();

    /**
     * Application edition (CE or EE)
     */
    private Edition edition = Edition.EE;

    /**
     * Optional environment override (DEVELOPMENT, STAGING, PRODUCTION)
     */
    private Environment environment;

    /**
     * Encryption configuration
     */
    private Encryption encryption;

    /**
     * List of enabled feature flags
     */
    private List<String> featureFlags = List.of();

    /**
     * File storage provider configuration
     */
    private FileStorage fileStorage = new FileStorage();

    /**
     * Help hub configuration
     */
    private HelpHub helpHub = new HelpHub();

    /**
     * Kafka connection configuration
     */
    private Kafka kafka = new Kafka();

    /**
     * Email configuration
     */
    private Mail mail = new Mail();

    /**
     * Message broker configuration
     */
    private MessageBroker messageBroker = new MessageBroker();

    /**
     * OAuth2 configuration
     */
    private Oauth2 oauth2 = new Oauth2();

    /**
     * Observability and logging configuration
     */
    private Observability observability = new Observability();

    /**
     * Public URL for the application
     */
    private String publicUrl;

    /**
     * RabbitMQ connection configuration
     */
    private Rabbitmq rabbitmq = new Rabbitmq();

    /**
     * Redis connection configuration
     */
    private Redis redis = new Redis();

    /**
     * Static resources configuration
     */
    private Resources resources = new Resources();

    /**
     * Security configuration
     */
    private Security security;

    /**
     * User sign-up configuration
     */
    private SignUp signUp = new SignUp();

    /**
     * Scheduler configuration
     */
    private Scheduler scheduler = new Scheduler();

    /**
     * Multi-tenancy configuration
     */
    private Tenant tenant = new Tenant();

    /**
     * Database upgrade (Liquibase) configuration
     */
    private Upgrade upgrade = new Upgrade();

    /**
     * User guiding configuration
     */
    private UserGuiding userGuiding = new UserGuiding();

    /**
     * Webhook URL for external integrations
     */
    private String webhookUrl;
    private Worker worker = new Worker();

    /**
     * Workflow engine configuration
     */
    private Workflow workflow = new Workflow();

    public Ai getAi() {
        return ai;
    }

    public Analytics getAnalytics() {
        return analytics;
    }

    public Cache getCache() {
        return cache;
    }

    public Cloud getCloud() {
        return cloud;
    }

    public Component getComponent() {
        return component;
    }

    public Coordinator getCoordinator() {
        return coordinator;
    }

    public Datasource getDatasource() {
        return datasource;
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    public DiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public Edition getEdition() {
        return edition;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public List<String> getFeatureFlags() {
        return featureFlags;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
    }

    public HelpHub getHelpHub() {
        return helpHub;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public Mail getMail() {
        return mail;
    }

    public MessageBroker getMessageBroker() {
        return messageBroker;
    }

    public Oauth2 getOauth2() {
        return oauth2;
    }

    public Observability getObservability() {
        return observability;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public Rabbitmq getRabbitmq() {
        return rabbitmq;
    }

    public Redis getRedis() {
        return redis;
    }

    public Resources getResources() {
        return resources;
    }

    public Security getSecurity() {
        return security;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public SignUp getSignUp() {
        return signUp;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Upgrade getUpgrade() {
        return upgrade;
    }

    public UserGuiding getUserGuiding() {
        return userGuiding;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public Worker getWorker() {
        return worker;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public void setAnalytics(Analytics analytics) {
        this.analytics = analytics;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public void setCloud(Cloud cloud) {
        this.cloud = cloud;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public void setCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    public void setDatasource(Datasource datasource) {
        this.datasource = datasource;
    }

    public void setDataStorage(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    public void setDiscoveryService(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public void setEdition(Edition edition) {
        this.edition = edition;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public void setFeatureFlags(List<String> featureFlags) {
        this.featureFlags = featureFlags;
    }

    public void setFileStorage(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    public void setHelpHub(HelpHub helpHub) {
        this.helpHub = helpHub;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public void setMessageBroker(MessageBroker messageBroker) {
        this.messageBroker = messageBroker;
    }

    public void setOauth2(Oauth2 oauth2) {
        this.oauth2 = oauth2;
    }

    public void setObservability(Observability observability) {
        this.observability = observability;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public void setRabbitmq(Rabbitmq rabbitmq) {
        this.rabbitmq = rabbitmq;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setSignUp(SignUp signUp) {
        this.signUp = signUp;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public void setUpgrade(Upgrade upgrade) {
        this.upgrade = upgrade;
    }

    public void setUserGuiding(UserGuiding userGuiding) {
        this.userGuiding = userGuiding;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setWorker(Worker worker) {
        this.worker = worker;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    /**
     * Observability and logging configuration for monitoring and diagnostics.
     */
    public static class Observability {

        /**
         * Logging observability configuration
         */
        private Logging logging = new Logging();

        /**
         * Metrics observability configuration
         */
        private Metrics metrics = new Metrics();

        /**
         * Tracing observability configuration
         */
        private Tracing tracing = new Tracing();

        public Logging getLogging() {
            return logging;
        }

        public Metrics getMetrics() {
            return metrics;
        }

        public Tracing getTracing() {
            return tracing;
        }

        public void setLogging(Logging logging) {
            this.logging = logging;
        }

        public void setMetrics(Metrics metrics) {
            this.metrics = metrics;
        }

        public void setTracing(Tracing tracing) {
            this.tracing = tracing;
        }

        public static class Logging {

            /**
             * Whether observability logging features are enabled
             */
            private boolean enabled;

            /**
             * OTLP endpoint for logging export
             */
            private String endpoint;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        public static class Metrics {

            /**
             * Whether observability features are enabled
             */
            private boolean enabled;

            /**
             * OTLP endpoint for metrics export
             */
            private String endpoint;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        public static class Tracing {
            /**
             * Whether observability features are enabled
             */
            private boolean enabled;
            /**
             * OTLP endpoint for tracing export
             */
            private String endpoint;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    /**
     * Ai properties.
     */
    public static class Ai {

        private Copilot copilot = new Copilot();
        private Firecrawl firecrawl = new Firecrawl();
        private KnowledgeBase knowledgeBase = new KnowledgeBase();
        private Mcp mcp = new Mcp();
        private Memory memory = new Memory();
        private Provider provider = new Provider();
        private Vectorstore vectorstore = new Vectorstore();

        public Copilot getCopilot() {
            return copilot;
        }

        public Firecrawl getFirecrawl() {
            return firecrawl;
        }

        public KnowledgeBase getKnowledgeBase() {
            return knowledgeBase;
        }

        public Mcp getMcp() {
            return mcp;
        }

        public Memory getMemory() {
            return memory;
        }

        public Provider getProvider() {
            return provider;
        }

        public Vectorstore getVectorstore() {
            return vectorstore;
        }

        public void setCopilot(Copilot copilot) {
            this.copilot = copilot;
        }

        public void setFirecrawl(Firecrawl firecrawl) {
            this.firecrawl = firecrawl;
        }

        public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
            this.knowledgeBase = knowledgeBase;
        }

        public void setMcp(Mcp mcp) {
            this.mcp = mcp;
        }

        public void setMemory(Memory memory) {
            this.memory = memory;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public void setVectorstore(Vectorstore vectorstore) {
            this.vectorstore = vectorstore;
        }

        /**
         * AI memory configuration. Cross-cutting concern shared by copilot and agents — not owned by any single product
         * surface. Stores conversation history for chat-style interactions.
         */
        public static class Memory {

            /**
             * Memory storage provider type.
             */
            public enum Provider {
                /**
                 * AWS-based memory storage
                 */
                AWS,
                /**
                 * In-memory storage (non-persistent)
                 */
                IN_MEMORY,
                /**
                 * JDBC-based memory storage
                 */
                JDBC,
                /**
                 * Redis-based memory storage
                 */
                REDIS
            }

            private Aws aws = new Aws();

            /**
             * Memory storage provider
             */
            private Provider provider = Provider.JDBC;

            public Aws getAws() {
                return aws;
            }

            public Provider getProvider() {
                return provider;
            }

            public void setAws(Aws aws) {
                this.aws = aws;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }

            /**
             * AWS S3-backed chat memory provider configuration. Active when {@code provider} is {@code AWS}.
             */
            public static class Aws {

                /**
                 * Prefix used to derive the per-tenant S3 bucket name.
                 */
                private String bucketPrefix = "bytechef-chat-memory";

                /**
                 * AWS region
                 */
                private String region;

                /**
                 * AWS access key ID
                 */
                private String accessKeyId;

                /**
                 * AWS secret access key
                 */
                private String secretAccessKey;

                /**
                 * Key prefix prepended to every stored object key.
                 */
                private String keyPrefix = "";

                public String getBucketPrefix() {
                    return bucketPrefix;
                }

                public String getRegion() {
                    return region;
                }

                public String getAccessKeyId() {
                    return accessKeyId;
                }

                public String getSecretAccessKey() {
                    return secretAccessKey;
                }

                public String getKeyPrefix() {
                    return keyPrefix;
                }

                public void setBucketPrefix(String bucketPrefix) {
                    this.bucketPrefix = bucketPrefix;
                }

                public void setRegion(String region) {
                    this.region = region;
                }

                public void setAccessKeyId(String accessKeyId) {
                    this.accessKeyId = accessKeyId;
                }

                public void setSecretAccessKey(String secretAccessKey) {
                    this.secretAccessKey = secretAccessKey;
                }

                public void setKeyPrefix(String keyPrefix) {
                    this.keyPrefix = keyPrefix;
                }
            }
        }

        public static class Copilot {

            /**
             * Whether Copilot is enabled
             */
            private boolean enabled;

            /**
             * Copilot documentation configuration
             */
            private Docs docs = new Docs();

            /**
             * Explicit CE chat-model provider key (e.g. openAi) to use for Copilot, overriding auto-detection from the
             * configured provider API keys/endpoints. Ignored when the EE AI Providers catalog is active.
             */
            private String provider;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Docs getDocs() {
                return docs;
            }

            public void setDocs(Docs docs) {
                this.docs = docs;
            }

            public String getProvider() {
                return provider;
            }

            public void setProvider(String provider) {
                this.provider = provider;
            }

            /**
             * Copilot documentation configuration.
             */
            public static class Docs {

                /**
                 * Copilot documentation embedding configuration
                 */
                private Embedding embedding = new Embedding();

                public Embedding getEmbedding() {
                    return embedding;
                }

                public void setEmbedding(Embedding embedding) {
                    this.embedding = embedding;
                }

                /**
                 * Copilot documentation embedding configuration.
                 */
                public static class Embedding {

                    public enum Provider {
                        OLLAMA, OPENAI
                    }

                    /**
                     * The embedding provider key (e.g. openAi).
                     */
                    private Provider provider;

                    /**
                     * The API key used to authenticate with the Copilot documentation embedding provider. Applies only
                     * to the OpenAI provider; Ollama runs locally and does not require a key.
                     */
                    private String apiKey;

                    public Provider getProvider() {
                        return provider;
                    }

                    public String getApiKey() {
                        return apiKey;
                    }

                    public void setProvider(Provider provider) {
                        this.provider = provider;
                    }

                    public void setApiKey(String apiKey) {
                        this.apiKey = apiKey;
                    }
                }
            }
        }

        /**
         * Firecrawl web scraping service configuration.
         */
        public static class Firecrawl {

            /**
             * Firecrawl API key
             */
            private String apiKey;

            /**
             * Firecrawl API base URL
             */
            private String baseUrl = "https://api.firecrawl.dev/v2";

            /**
             * Whether Firecrawl is enabled
             */
            private boolean enabled;

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        /**
         * Knowledge base configuration.
         */
        public static class KnowledgeBase {

            /**
             * Whether knowledge base AI features are enabled
             */
            private boolean enabled;

            /**
             * OCR configuration
             */
            private Ocr ocr = new Ocr();

            /**
             * Event subscription configuration
             */
            private Subscriptions subscriptions = new Subscriptions();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Ocr getOcr() {
                return ocr;
            }

            public void setOcr(Ocr ocr) {
                this.ocr = ocr;
            }

            public Subscriptions getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Subscriptions subscriptions) {
                this.subscriptions = subscriptions;
            }

            /**
             * OCR configuration for knowledge base document processing.
             */
            public static class Ocr {

                /**
                 * OCR provider type.
                 */
                public enum Provider {
                    /**
                     * No OCR provider
                     */
                    NONE,
                    /**
                     * Azure OCR provider
                     */
                    AZURE,
                    /**
                     * Mistral OCR provider
                     */
                    MISTRAL
                }

                /**
                 * OCR provider
                 */
                private Provider provider = Provider.NONE;

                /**
                 * Mistral OCR configuration
                 */
                private Mistral mistral = new Mistral();

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }

                public Mistral getMistral() {
                    return mistral;
                }

                public void setMistral(Mistral mistral) {
                    this.mistral = mistral;
                }

                /**
                 * Mistral OCR configuration.
                 */
                public static class Mistral {

                    /**
                     * Mistral API key
                     */
                    private String apiKey;

                    public String getApiKey() {
                        return apiKey;
                    }

                    public void setApiKey(String apiKey) {
                        this.apiKey = apiKey;
                    }
                }
            }

            /**
             * Event subscription configuration for knowledge base events.
             */
            public static class Subscriptions {

                /**
                 * Number of subscribers for document process events
                 */
                private int documentProcessEvents = 1;

                /**
                 * Number of subscribers for document chunk update events
                 */
                private int documentChunkUpdateEvents = 1;

                public int getDocumentProcessEvents() {
                    return documentProcessEvents;
                }

                public void setDocumentProcessEvents(int documentProcessEvents) {
                    this.documentProcessEvents = documentProcessEvents;
                }

                public int getDocumentChunkUpdateEvents() {
                    return documentChunkUpdateEvents;
                }

                public void setDocumentChunkUpdateEvents(int documentChunkUpdateEvents) {
                    this.documentChunkUpdateEvents = documentChunkUpdateEvents;
                }
            }
        }

        /**
         * MCP (Model Context Protocol) server configuration.
         */
        public static class Mcp {

            private Server server = new Server();

            public Server getServer() {
                return server;
            }

            public void setServer(Server server) {
                this.server = server;
            }

            /**
             * MCP server settings.
             */
            public static class Server {

                /**
                 * Whether the MCP server is enabled
                 */
                private boolean enabled;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
            }
        }

        /**
         * AI provider API key configuration for various AI services.
         */
        public static class Provider {

//            /** Amazon Bedrock Anthropic Claude 2 configuration */
//            private AmazonBedrockAnthropic2 amazonBedrockAnthropic2 = new AmazonBedrockAnthropic2();
//
//            /** Amazon Bedrock Anthropic Claude 3 configuration */
//            private AmazonBedrockAnthropic3 amazonBedrockAnthropic3 = new AmazonBedrockAnthropic3();
//
//            /** Amazon Bedrock Cohere configuration */
//            private AmazonBedrockCohere amazonBedrockCohere = new AmazonBedrockCohere();
//
//            /** Amazon Bedrock Jurassic-2 configuration */
//            private AmazonBedrockJurassic2 amazonBedrockJurassic2 = new AmazonBedrockJurassic2();
//
//            /** Amazon Bedrock Llama configuration */
//            private AmazonBedrockLlama amazonBedrockLlama = new AmazonBedrockLlama();
//
//            /** Amazon Bedrock Titan configuration */
//            private AmazonBedrockTitan amazonBedrockTitan = new AmazonBedrockTitan();

            /**
             * Anthropic Claude configuration
             */
            private Anthropic anthropic = new Anthropic();

            /**
             * Azure OpenAI configuration
             */
            private AzureOpenAi azureOpenAi = new AzureOpenAi();

            /**
             * Chat model configuration grouped by provider
             */
            private Chat chat = new Chat();

            /**
             * DeepSeek configuration
             */
            private DeepSeek deepSeek = new DeepSeek();

            /**
             * Embedding model configuration grouped by provider
             */
            private Embedding embedding = new Embedding();

            /**
             * Groq configuration
             */
            private Groq groq = new Groq();

            /**
             * Mistral AI configuration
             */
            private Mistral mistral = new Mistral();

            /**
             * NVIDIA AI configuration
             */
            private Nvidia nvidia = new Nvidia();

            /**
             * Ollama configuration
             */
            private Ollama ollama = new Ollama();

            /**
             * OpenAI configuration
             */
            private OpenAi openAi = new OpenAi();

            /**
             * Perplexity AI configuration
             */
            private Perplexity perplexity = new Perplexity();

            /**
             * Stability AI configuration
             */
            private Stability stability = new Stability();

            /**
             * Google Vertex AI Gemini configuration
             */
            private VertexGemini vertexGemini = new VertexGemini();

//            public AmazonBedrockAnthropic2 getAmazonBedrockAnthropic2() {
//                return amazonBedrockAnthropic2;
//            }
//
//            public AmazonBedrockAnthropic3 getAmazonBedrockAnthropic3() {
//                return amazonBedrockAnthropic3;
//            }
//
//            public AmazonBedrockCohere getAmazonBedrockCohere() {
//                return amazonBedrockCohere;
//            }
//
//            public AmazonBedrockJurassic2 getAmazonBedrockJurassic2() {
//                return amazonBedrockJurassic2;
//            }
//
//            public AmazonBedrockLlama getAmazonBedrockLlama() {
//                return amazonBedrockLlama;
//            }
//
//            public AmazonBedrockTitan getAmazonBedrockTitan() {
//                return amazonBedrockTitan;
//            }

            public Anthropic getAnthropic() {
                return anthropic;
            }

            public AzureOpenAi getAzureOpenAi() {
                return azureOpenAi;
            }

            public Chat getChat() {
                return chat;
            }

            public DeepSeek getDeepSeek() {
                return deepSeek;
            }

            public Embedding getEmbedding() {
                return embedding;
            }

            public Groq getGroq() {
                return groq;
            }

            public Nvidia getNvidia() {
                return nvidia;
            }

            public Mistral getMistral() {
                return mistral;
            }

            public Ollama getOllama() {
                return ollama;
            }

            public OpenAi getOpenAi() {
                return openAi;
            }

            public Perplexity getPerplexity() {
                return perplexity;
            }

            public Stability getStability() {
                return stability;
            }

            public VertexGemini getVertexGemini() {
                return vertexGemini;
            }

//            public void setAmazonBedrockAnthropic2(AmazonBedrockAnthropic2 amazonBedrockAnthropic2) {
//                this.amazonBedrockAnthropic2 = amazonBedrockAnthropic2;
//            }
//
//            public void setAmazonBedrockAnthropic3(AmazonBedrockAnthropic3 amazonBedrockAnthropic3) {
//                this.amazonBedrockAnthropic3 = amazonBedrockAnthropic3;
//            }
//
//            public void setAmazonBedrockCohere(AmazonBedrockCohere amazonBedrockCohere) {
//                this.amazonBedrockCohere = amazonBedrockCohere;
//            }
//
//            public void setAmazonBedrockJurassic2(AmazonBedrockJurassic2 amazonBedrockJurassic2) {
//                this.amazonBedrockJurassic2 = amazonBedrockJurassic2;
//            }
//
//            public void setAmazonBedrockLlama(AmazonBedrockLlama amazonBedrockLlama) {
//                this.amazonBedrockLlama = amazonBedrockLlama;
//            }
//
//            public void setAmazonBedrockTitan(AmazonBedrockTitan amazonBedrockTitan) {
//                this.amazonBedrockTitan = amazonBedrockTitan;
//            }

            public void setAnthropic(Anthropic anthropic) {
                this.anthropic = anthropic;
            }

            public void setAzureOpenAi(AzureOpenAi azureOpenAi) {
                this.azureOpenAi = azureOpenAi;
            }

            public void setChat(Chat chat) {
                this.chat = chat;
            }

            public void setDeepSeek(DeepSeek deepSeek) {
                this.deepSeek = deepSeek;
            }

            public void setEmbedding(Embedding embedding) {
                this.embedding = embedding;
            }

            public void setGroq(Groq groq) {
                this.groq = groq;
            }

            public void setNvidia(Nvidia nvidia) {
                this.nvidia = nvidia;
            }

            public void setMistral(Mistral mistral) {
                this.mistral = mistral;
            }

            public void setOllama(Ollama ollama) {
                this.ollama = ollama;
            }

            public void setOpenAi(OpenAi openAi) {
                this.openAi = openAi;
            }

            public void setPerplexity(Perplexity perplexity) {
                this.perplexity = perplexity;
            }

            public void setStability(Stability stability) {
                this.stability = stability;
            }

            public void setVertexGemini(VertexGemini vertexGemini) {
                this.vertexGemini = vertexGemini;
            }

            /**
             * Amazon Bedrock Anthropic Claude 2 API configuration.
             */
            public static class AmazonBedrockAnthropic2 {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Amazon Bedrock Anthropic Claude 3 API configuration.
             */
            public static class AmazonBedrockAnthropic3 {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Amazon Bedrock Cohere API configuration.
             */
            public static class AmazonBedrockCohere {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Amazon Bedrock Jurassic-2 API configuration.
             */
            public static class AmazonBedrockJurassic2 {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Amazon Bedrock Llama API configuration.
             */
            public static class AmazonBedrockLlama {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Amazon Bedrock Titan API configuration.
             */
            public static class AmazonBedrockTitan {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Anthropic Claude API configuration.
             */
            public static class Anthropic {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Azure OpenAI API configuration.
             */
            public static class AzureOpenAi {
                /**
                 * API key
                 */
                private String apiKey;

                /**
                 * Per-deployment Azure OpenAI resource endpoint (e.g. https://my-resource.openai.azure.com).
                 */
                private String endpoint;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }

                public String getEndpoint() {
                    return endpoint;
                }

                public void setEndpoint(String endpoint) {
                    this.endpoint = endpoint;
                }
            }

            /**
             * DeepSeek API configuration.
             */
            public static class DeepSeek {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Groq API configuration.
             */
            public static class Groq {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * NVIDIA AI API configuration.
             */
            public static class Nvidia {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Mistral AI API configuration.
             */
            public static class Mistral {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Ollama API configuration.
             */
            public static class Ollama {
                /**
                 * API key
                 */
                private String apiKey;

                /**
                 * Base URL of the Ollama server (e.g. http://localhost:11434). Serves as the fallback for both chat and
                 * embedding models when no per-environment URL is configured in the AI Providers catalog. When blank,
                 * the Ollama client defaults to http://localhost:11434.
                 */
                private String url;

                public String getApiKey() {
                    return apiKey;
                }

                public String getUrl() {
                    return url;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }

                public void setUrl(String url) {
                    this.url = url;
                }
            }

            /**
             * OpenAI API configuration.
             */
            public static class OpenAi {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Perplexity AI API configuration.
             */
            public static class Perplexity {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Stability AI API configuration.
             */
            public static class Stability {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Google Vertex AI Gemini API configuration.
             */
            public static class VertexGemini {
                /**
                 * API key
                 */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /**
             * Chat model configuration grouped by provider.
             */
            public static class Chat {

                /**
                 * Anthropic chat model configuration
                 */
                private Anthropic anthropic = new Anthropic();

                /**
                 * Azure OpenAI chat model configuration
                 */
                private AzureOpenAi azureOpenAi = new AzureOpenAi();

                /**
                 * DeepSeek chat model configuration
                 */
                private DeepSeek deepSeek = new DeepSeek();

                /**
                 * Groq chat model configuration
                 */
                private Groq groq = new Groq();

                /**
                 * Mistral AI chat model configuration
                 */
                private Mistral mistral = new Mistral();

                /**
                 * NVIDIA AI chat model configuration
                 */
                private Nvidia nvidia = new Nvidia();

                /**
                 * Ollama chat model configuration
                 */
                private Ollama ollama = new Ollama();

                /**
                 * OpenAI chat model configuration
                 */
                private OpenAi openAi = new OpenAi();

                /**
                 * Perplexity AI chat model configuration
                 */
                private Perplexity perplexity = new Perplexity();

                /**
                 * Google Vertex AI Gemini chat model configuration
                 */
                private VertexGemini vertexGemini = new VertexGemini();

                public Anthropic getAnthropic() {
                    return anthropic;
                }

                public void setAnthropic(Anthropic anthropic) {
                    this.anthropic = anthropic;
                }

                public AzureOpenAi getAzureOpenAi() {
                    return azureOpenAi;
                }

                public void setAzureOpenAi(AzureOpenAi azureOpenAi) {
                    this.azureOpenAi = azureOpenAi;
                }

                public DeepSeek getDeepSeek() {
                    return deepSeek;
                }

                public void setDeepSeek(DeepSeek deepSeek) {
                    this.deepSeek = deepSeek;
                }

                public Groq getGroq() {
                    return groq;
                }

                public void setGroq(Groq groq) {
                    this.groq = groq;
                }

                public Mistral getMistral() {
                    return mistral;
                }

                public void setMistral(Mistral mistral) {
                    this.mistral = mistral;
                }

                public Nvidia getNvidia() {
                    return nvidia;
                }

                public void setNvidia(Nvidia nvidia) {
                    this.nvidia = nvidia;
                }

                public Ollama getOllama() {
                    return ollama;
                }

                public void setOllama(Ollama ollama) {
                    this.ollama = ollama;
                }

                public OpenAi getOpenAi() {
                    return openAi;
                }

                public void setOpenAi(OpenAi openAi) {
                    this.openAi = openAi;
                }

                public Perplexity getPerplexity() {
                    return perplexity;
                }

                public void setPerplexity(Perplexity perplexity) {
                    this.perplexity = perplexity;
                }

                public VertexGemini getVertexGemini() {
                    return vertexGemini;
                }

                public void setVertexGemini(VertexGemini vertexGemini) {
                    this.vertexGemini = vertexGemini;
                }

                /**
                 * Anthropic chat model configuration.
                 */
                public static class Anthropic {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Anthropic chat model options.
                     */
                    public static class Options {

                        /**
                         * AI model name (e.g., claude-3-opus-20240229)
                         */
                        private String model;

                        /**
                         * Temperature for response randomness (0.0-1.0)
                         */
                        private Double temperature;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }

                        public Double getTemperature() {
                            return temperature;
                        }

                        public void setTemperature(Double temperature) {
                            this.temperature = temperature;
                        }
                    }
                }

                /**
                 * Azure OpenAI chat model configuration.
                 */
                public static class AzureOpenAi {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Azure OpenAI chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., gpt-4o)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * DeepSeek chat model configuration.
                 */
                public static class DeepSeek {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * DeepSeek chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., deepseek-chat)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * Groq chat model configuration.
                 */
                public static class Groq {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Groq chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., llama-3.3-70b-versatile)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * Mistral AI chat model configuration.
                 */
                public static class Mistral {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Mistral AI chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., mistral-large-latest)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * NVIDIA AI chat model configuration.
                 */
                public static class Nvidia {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * NVIDIA AI chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., meta/llama-3.1-70b-instruct)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * Ollama chat model configuration.
                 */
                public static class Ollama {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Ollama chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., llama3.1)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * OpenAI chat model configuration.
                 */
                public static class OpenAi {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * OpenAI chat model options.
                     */
                    public static class Options {

                        public enum Setting {
                            NONE,
                            LOW,
                            MEDIUM,
                            HIGH
                        }

                        /**
                         * Chat model name (e.g., gpt-4, gpt-3.5-turbo)
                         */
                        private String model;

                        /**
                         * Temperature for response randomness (0.0-2.0)
                         */
                        private Double temperature;

                        private Setting reasoningEffect = Setting.NONE;

                        private Setting verbosity = Setting.LOW;

                        public String getModel() {
                            return model;
                        }

                        public Double getTemperature() {
                            return temperature;
                        }

                        public Setting getReasoningEffect() {
                            return reasoningEffect;
                        }

                        public Setting getVerbosity() {
                            return verbosity;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }

                        public void setTemperature(Double temperature) {
                            this.temperature = temperature;
                        }

                        public void setReasoningEffect(Setting setting) {
                            this.reasoningEffect = setting;
                        }

                        public void setVerbosity(Setting setting) {
                            this.verbosity = setting;
                        }
                    }
                }

                /**
                 * Perplexity AI chat model configuration.
                 */
                public static class Perplexity {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Perplexity AI chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., sonar)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * Google Vertex AI Gemini chat model configuration.
                 */
                public static class VertexGemini {

                    /**
                     * Chat model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Google Vertex AI Gemini chat model options.
                     */
                    public static class Options {

                        /**
                         * Chat model name (e.g., gemini-1.5-pro)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }
            }

            /**
             * Embedding model configuration grouped by provider.
             */
            public static class Embedding {

                /**
                 * Mistral embedding model configuration
                 */
                private Mistral mistral = new Mistral();

                /**
                 * Ollama embedding model configuration
                 */
                private Ollama ollama = new Ollama();

                /**
                 * OpenAI embedding model configuration
                 */
                private OpenAi openAi = new OpenAi();

                public Mistral getMistral() {
                    return mistral;
                }

                public void setMistral(Mistral mistral) {
                    this.mistral = mistral;
                }

                public Ollama getOllama() {
                    return ollama;
                }

                public void setOllama(Ollama ollama) {
                    this.ollama = ollama;
                }

                public OpenAi getOpenAi() {
                    return openAi;
                }

                public void setOpenAi(OpenAi openAi) {
                    this.openAi = openAi;
                }

                /**
                 * Mistral embedding model configuration.
                 */
                public static class Mistral {

                    /**
                     * Embedding model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Mistral embedding model options.
                     */
                    public static class Options {

                        /**
                         * Embedding model name (e.g., mistral-embed)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * Ollama embedding model configuration.
                 */
                public static class Ollama {

                    /**
                     * Embedding model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * Ollama embedding model options.
                     */
                    public static class Options {

                        /**
                         * Embedding model name (e.g., qwen3-embedding:8b)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }

                /**
                 * OpenAI embedding model configuration.
                 */
                public static class OpenAi {

                    /**
                     * Embedding model options
                     */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * OpenAI embedding model options.
                     */
                    public static class Options {

                        /**
                         * Embedding model name (e.g., text-embedding-ada-002)
                         */
                        private String model;

                        public String getModel() {
                            return model;
                        }

                        public void setModel(String model) {
                            this.model = model;
                        }
                    }
                }
            }
        }

        /**
         * Vector store configuration for AI embeddings storage.
         */
        public static class Vectorstore {

            /**
             * Vector store provider type.
             */
            public enum Provider {
                /**
                 * PostgreSQL with pgvector extension
                 */
                PGVECTOR
            }

            /**
             * Vector store provider
             */
            private Provider provider;

            /**
             * PgVector configuration
             */
            private PgVector pgVector = new PgVector();

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }

            public PgVector getPgVector() {
                return pgVector;
            }

            public void setPgVector(PgVector pgVector) {
                this.pgVector = pgVector;
            }

            /**
             * PostgreSQL pgvector extension configuration for vector storage.
             */
            public static class PgVector {

                /**
                 * Database password
                 */
                private String password;

                /**
                 * JDBC URL for PostgreSQL database
                 */
                private String url;

                /**
                 * Database username
                 */
                private String username;

                public String getPassword() {
                    return password;
                }

                public void setPassword(String password) {
                    this.password = password;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public String getUsername() {
                    return username;
                }

                public void setUsername(String username) {
                    this.username = username;
                }
            }
        }
    }

    /**
     * Analytics configuration for usage tracking and metrics.
     */
    public static class Analytics {

        /**
         * Whether analytics features are enabled
         */
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Cache provider configuration.
     */
    public static class Cache {

        /**
         * Available cache providers.
         */
        public enum Provider {
            /**
             * Redis cache
             */
            REDIS,
            /**
             * Caffeine in-memory cache
             */
            CAFFEINE
        }

        /**
         * Cache provider
         */
        private Provider provider = Provider.CAFFEINE;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * Cloud provider configuration for cloud-specific integrations.
     */
    public static class Cloud {

        /**
         * Supported cloud providers.
         */
        public enum Provider {
            /**
             * Amazon Web Services
             */
            AWS,
            /**
             * No cloud provider
             */
            NONE
        }

        /**
         * AWS configuration
         */
        private Aws aws;

        /**
         * Cloud provider
         */
        private Provider provider = Provider.NONE;

        public Aws getAws() {
            return aws;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setAws(Aws aws) {
            this.aws = aws;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        /**
         * Amazon Web Services configuration.
         */
        public static class Aws {

            /**
             * AWS access key ID
             */
            private String accessKeyId;

            /**
             * AWS region
             */
            private String region;

            /**
             * AWS secret access key
             */
            private String secretAccessKey;

            /**
             * AWS account ID
             */
            private String accountId;

            public String getAccountId() {
                return accountId;
            }

            public void setAccountId(String accountId) {
                this.accountId = accountId;
            }

            public String getAccessKeyId() {
                return accessKeyId;
            }

            public String getRegion() {
                return region;
            }

            public String getSecretAccessKey() {
                return secretAccessKey;
            }

            public void setAccessKeyId(String accessKeyId) {
                this.accessKeyId = accessKeyId;
            }

            public void setRegion(String region) {
                this.region = region;
            }

            public void setSecretAccessKey(String secretAccessKey) {
                this.secretAccessKey = secretAccessKey;
            }
        }
    }

    /**
     * Component registry configuration for managing available components.
     */
    public static class Component {

        /**
         * Component registry configuration
         */
        private Registry registry = new Registry();

        public Registry getRegistry() {
            return registry;
        }

        public void setRegistry(Registry registry) {
            this.registry = registry;
        }

        /**
         * Component registry settings.
         */
        public static class Registry {

            /**
             * List of component names to exclude from registration
             */
            private List<String> exclude;

            public List<String> getExclude() {
                return exclude;
            }

            public void setExclude(List<String> exclude) {
                this.exclude = exclude;
            }
        }
    }

    /**
     * Workflow coordinator configuration for orchestrating workflow execution.
     */
    public static class Coordinator {

        /**
         * Whether coordinator is enabled
         */
        private boolean enabled = true;

        /**
         * Task coordination configuration
         */
        private Task task = new Task();

        /**
         * Trigger coordination configuration
         */
        private Trigger trigger = new Trigger();

        public boolean isEnabled() {
            return enabled;
        }

        public Task getTask() {
            return task;
        }

        public Trigger getTrigger() {
            return trigger;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        public void setTrigger(Trigger trigger) {
            this.trigger = trigger;
        }

        /**
         * Task coordination configuration for workflow tasks.
         */
        public static class Task {

            /**
             * Event subscription configuration
             */
            private Subscriptions subscriptions = new Subscriptions();

            public Subscriptions getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Subscriptions subscriptions) {
                this.subscriptions = subscriptions;
            }

            /**
             * Event subscription configuration for task events.
             */
            public static class Subscriptions {

                /**
                 * Number of subscribers for application events
                 */
                private int applicationEvents = 1;

                /**
                 * Number of subscribers for resume job events
                 */
                private int resumeJobEvents = 1;

                /**
                 * Number of subscribers for start job events
                 */
                private int startJobEvents = 1;

                /**
                 * Number of subscribers for stop job events
                 */
                private int stopJobEvents = 1;

                /**
                 * Number of subscribers for task execution complete events
                 */
                private int taskExecutionCompleteEvents = 1;

                /**
                 * Number of subscribers for task execution error events
                 */
                private int taskExecutionErrorEvents = 1;

                public int getApplicationEvents() {
                    return applicationEvents;
                }

                public int getResumeJobEvents() {
                    return resumeJobEvents;
                }

                public int getStartJobEvents() {
                    return startJobEvents;
                }

                public int getStopJobEvents() {
                    return stopJobEvents;
                }

                public int getTaskExecutionCompleteEvents() {
                    return taskExecutionCompleteEvents;
                }

                public int getTaskExecutionErrorEvents() {
                    return taskExecutionErrorEvents;
                }

                public void setApplicationEvents(int applicationEvents) {
                    this.applicationEvents = applicationEvents;
                }

                public void setResumeJobEvents(int resumeJobEvents) {
                    this.resumeJobEvents = resumeJobEvents;
                }

                public void setStartJobEvents(int startJobEvents) {
                    this.startJobEvents = startJobEvents;
                }

                public void setStopJobEvents(int stopJobEvents) {
                    this.stopJobEvents = stopJobEvents;
                }

                public void setTaskExecutionCompleteEvents(int taskExecutionCompleteEvents) {
                    this.taskExecutionCompleteEvents = taskExecutionCompleteEvents;
                }

                public void setTaskExecutionErrorEvents(int taskExecutionErrorEvents) {
                    this.taskExecutionErrorEvents = taskExecutionErrorEvents;
                }
            }
        }

        /**
         * Trigger coordination configuration for workflow triggers.
         */
        public static class Trigger {

            /**
             * Polling configuration for trigger checks
             */
            private Polling polling = new Polling();

            /**
             * Event subscription configuration
             */
            private Subscriptions subscriptions = new Subscriptions();

            public Polling getPolling() {
                return polling;
            }

            public void setPolling(Polling polling) {
                this.polling = polling;
            }

            public Subscriptions getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Subscriptions subscriptions) {
                this.subscriptions = subscriptions;
            }

            /**
             * Polling configuration for periodic trigger checks.
             */
            public static class Polling {

                /**
                 * Check period in seconds
                 */
                private int checkPeriod = 5;

                public int getCheckPeriod() {
                    return checkPeriod;
                }

                public void setCheckPeriod(int checkPeriod) {
                    this.checkPeriod = checkPeriod;
                }
            }

            /**
             * Event subscription configuration for trigger events.
             */
            public static class Subscriptions {

                /**
                 * Number of subscribers for application events
                 */
                private int applicationEvents = 1;

                /**
                 * Number of subscribers for trigger execution complete events
                 */
                private int triggerExecutionCompleteEvents = 1;

                /**
                 * Number of subscribers for trigger execution error events
                 */
                private int triggerExecutionErrorEvents = 1;

                /**
                 * Number of subscribers for trigger listener events
                 */
                private int triggerListenerEvents = 1;

                /**
                 * Number of subscribers for trigger poll events
                 */
                private int triggerPollEvents = 1;

                /**
                 * Number of subscribers for trigger webhook events
                 */
                private int triggerWebhookEvents = 1;

                public int getApplicationEvents() {
                    return applicationEvents;
                }

                public int getTriggerExecutionCompleteEvents() {
                    return triggerExecutionCompleteEvents;
                }

                public int getTriggerExecutionErrorEvents() {
                    return triggerExecutionErrorEvents;
                }

                public int getTriggerListenerEvents() {
                    return triggerListenerEvents;
                }

                public int getTriggerPollEvents() {
                    return triggerPollEvents;
                }

                public int getTriggerWebhookEvents() {
                    return triggerWebhookEvents;
                }

                public void setApplicationEvents(int applicationEvents) {
                    this.applicationEvents = applicationEvents;
                }

                public void setTriggerExecutionCompleteEvents(int triggerExecutionCompleteEvents) {
                    this.triggerExecutionCompleteEvents = triggerExecutionCompleteEvents;
                }

                public void setTriggerExecutionErrorEvents(int triggerExecutionErrorEvents) {
                    this.triggerExecutionErrorEvents = triggerExecutionErrorEvents;
                }

                public void setTriggerListenerEvents(int triggerListenerEvents) {
                    this.triggerListenerEvents = triggerListenerEvents;
                }

                public void setTriggerPollEvents(int triggerPollEvents) {
                    this.triggerPollEvents = triggerPollEvents;
                }

                public void setTriggerWebhookEvents(int triggerWebhookEvents) {
                    this.triggerWebhookEvents = triggerWebhookEvents;
                }
            }
        }
    }

    /**
     * Database datasource configuration for the main application database.
     */
    public static class Datasource {

        /**
         * Database password
         */
        private String password;

        /**
         * JDBC URL for database connection
         */
        private String url;

        /**
         * Database username
         */
        private String username;

        public String getPassword() {
            return password;
        }

        public String getUrl() {
            return url;
        }

        public String getUsername() {
            return username;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * Data storage provider configuration for structured data persistence.
     */
    public static class DataStorage {

        /**
         * Available data storage providers.
         */
        public enum Provider {
            /**
             * AWS-based storage
             */
            AWS,
            /**
             * Filesystem-based storage
             */
            FILESYSTEM,
            /**
             * JDBC database storage
             */
            JDBC
        }

        /**
         * Data storage provider
         */
        private Provider provider = Provider.JDBC;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * Service discovery configuration for microservices architecture.
     */
    public static class DiscoveryService {

        /**
         * Available service discovery providers.
         */
        public enum Provider {
            /**
             * Redis-based service discovery
             */
            REDIS
        }

        /**
         * Service discovery provider
         */
        private Provider provider = Provider.REDIS;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * File storage configuration for storing files and attachments.
     */
    public static class FileStorage {

        /**
         * Available file storage providers.
         */
        public enum Provider {
            /**
             * AWS S3 storage
             */
            AWS,
            /**
             * Local filesystem storage
             */
            FILESYSTEM,
            /**
             * JDBC database storage
             */
            JDBC
        }

        /**
         * AWS S3 configuration
         */
        private Aws aws = new Aws();

        /**
         * Filesystem storage configuration
         */
        private Filesystem filesystem = new Filesystem();

        /**
         * File storage provider
         */
        private Provider provider = Provider.FILESYSTEM;

        public Aws getAws() {
            return aws;
        }

        public Filesystem getFilesystem() {
            return filesystem;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setAws(Aws aws) {
            this.aws = aws;
        }

        public void setFilesystem(Filesystem filesystem) {
            this.filesystem = filesystem;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        /**
         * AWS S3 configuration for file storage.
         */
        public static class Aws {

            /**
             * S3 bucket name
             */
            private String bucket;

            public String getBucket() {
                return bucket;
            }

            public void setBucket(String bucket) {
                this.bucket = bucket;
            }
        }

        /**
         * Local filesystem configuration for file storage.
         */
        public static class Filesystem {

            /**
             * Base directory for file storage
             */
            private String basedir = "";

            public String getBasedir() {
                return basedir;
            }

            public void setBasedir(String basedir) {
                this.basedir = basedir;
            }
        }
    }

    /**
     * Encryption configuration for sensitive data protection.
     */
    public static class Encryption {

        /**
         * Available encryption key providers.
         */
        public enum Provider {
            /**
             * Filesystem-based key storage
             */
            FILESYSTEM,
            /**
             * Property-based key storage
             */
            PROPERTY;
        }

        /**
         * Encryption provider
         */
        private Provider provider = Provider.FILESYSTEM;

        /**
         * Property-based encryption configuration
         */
        private Property property = new Property();

        public Provider getProvider() {
            return provider;
        }

        public Property getProperty() {
            return property;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public void setProperty(Property property) {
            this.property = property;
        }

        /**
         * Property-based encryption key configuration.
         */
        public static class Property {

            /**
             * Encryption key
             */
            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }
    }

    /**
     * Help hub configuration for in-application help features.
     */
    public static class HelpHub {

        /**
         * Whether help hub is enabled
         */
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Kafka connection configuration.
     */
    public static class Kafka {

        /**
         * Comma-separated list of Kafka bootstrap servers (host:port)
         */
        private String bootstrapServers;

        /**
         * Kafka consumer configuration
         */
        private Consumer consumer = new Consumer();

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public Consumer getConsumer() {
            return consumer;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public void setConsumer(Consumer consumer) {
            this.consumer = consumer;
        }

        /**
         * Kafka consumer configuration.
         */
        public static class Consumer {

            /**
             * Consumer group identifier
             */
            private String groupId;

            public String getGroupId() {
                return groupId;
            }

            public void setGroupId(String groupId) {
                this.groupId = groupId;
            }
        }
    }

    /**
     * Email configuration for sending notifications and alerts.
     */
    public static class Mail {

        /**
         * Whether SMTP authentication is required
         */
        private boolean auth;

        /**
         * Whether to enable debug logging for mail
         */
        private boolean debug;

        /**
         * Email address to use as sender
         */
        private String from;

        /**
         * SMTP server hostname
         */
        private String host;

        /**
         * Base URL for email links
         */
        private String baseUrl;

        /**
         * SMTP server password
         */
        private String password;

        /**
         * Email protocol (e.g., smtp, smtps)
         */
        private String protocol;

        /**
         * SMTP server port
         */
        private int port = 25;

        /**
         * STARTTLS configuration
         */
        private Starttls starttls = new Starttls();

        /**
         * SSL configuration
         */
        private Ssl ssl = new Ssl();

        /**
         * SMTP server username
         */
        private String username;

        public boolean isAuth() {
            return auth;
        }

        public void setAuth(boolean auth) {
            this.auth = auth;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isDebug() {
            return debug;
        }

        public void setDebug(boolean debug) {
            this.debug = debug;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Ssl getSsl() {
            return ssl;
        }

        public void setSsl(Ssl ssl) {
            this.ssl = ssl;
        }

        public Starttls getStarttls() {
            return starttls;
        }

        public void setStarttls(Starttls starttls) {
            this.starttls = starttls;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * SSL configuration for email.
         */
        public static class Ssl {

            /**
             * Whether SSL is enabled
             */
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        /**
         * STARTTLS configuration for email.
         */
        public static class Starttls {

            /**
             * Whether STARTTLS is enabled
             */
            private boolean enable;

            /**
             * Whether STARTTLS is required
             */
            private boolean required;

            public boolean isEnable() {
                return enable;
            }

            public void setEnable(boolean enable) {
                this.enable = enable;
            }

            public boolean isRequired() {
                return required;
            }

            public void setRequired(boolean required) {
                this.required = required;
            }
        }
    }

    /**
     * Message broker configuration for asynchronous messaging.
     */
    public static class MessageBroker {

        /**
         * Available message broker providers.
         */
        public enum Provider {
            /**
             * AMQP (e.g., RabbitMQ)
             */
            AMQP,
            /**
             * AWS SQS
             */
            AWS,
            /**
             * JMS
             */
            JMS,
            /**
             * Apache Kafka
             */
            KAFKA,
            /**
             * In-memory message broker
             */
            MEMORY,
            /**
             * Redis-based message broker
             */
            REDIS
        }

        /**
         * Message broker provider
         */
        private Provider provider = Provider.JMS;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * OAuth2 configuration for third-party authentication.
     */
    public static class Oauth2 {

        /**
         * Predefined OAuth2 applications mapped by component name
         */
        private Map<String, OAuth2App> predefinedApps = new HashMap<>();

        /**
         * OAuth2 redirect URI
         */
        private String redirectUri;

        public Map<String, OAuth2App> getPredefinedApps() {
            return predefinedApps;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setPredefinedApps(Map<String, OAuth2App> predefinedApps) {
            this.predefinedApps = predefinedApps;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        /**
         * OAuth2 application credentials.
         */
        public static class OAuth2App {

            /**
             * OAuth2 client ID
             */
            private String clientId;

            /**
             * OAuth2 client secret
             */
            private String clientSecret;

            public String getClientId() {
                return clientId;
            }

            public void setClientId(String clientId) {
                this.clientId = clientId;
            }

            public String getClientSecret() {
                return clientSecret;
            }

            public void setClientSecret(String clientSecret) {
                this.clientSecret = clientSecret;
            }
        }
    }

    /**
     * RabbitMQ connection configuration.
     */
    public static class Rabbitmq {

        /**
         * RabbitMQ server hostname
         */
        private String host;

        /**
         * RabbitMQ server password
         */
        private String password;

        /**
         * RabbitMQ server port
         */
        private int port = 5672;

        /**
         * RabbitMQ server username
         */
        private String username;

        public String getHost() {
            return host;
        }

        public String getPassword() {
            return password;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }

    /**
     * Redis connection configuration.
     */
    public static class Redis {

        /**
         * Redis database index
         */
        private int database;

        /**
         * Redis server hostname
         */
        private String host;

        /**
         * Redis server password
         */
        private String password;

        /**
         * Redis server port
         */
        private int port = 6379;

        /**
         * Connection timeout in milliseconds
         */
        private long timeout;

        public int getDatabase() {
            return database;
        }

        public String getHost() {
            return host;
        }

        public String getPassword() {
            return password;
        }

        public int getPort() {
            return port;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Static resources configuration.
     */
    public static class Resources {

        /**
         * Web resources path
         */
        private String web;

        public String getWeb() {
            return web;
        }

        public void setWeb(String web) {
            this.web = web;
        }
    }

    /**
     * Security configuration for authentication and authorization.
     */
    public static class Security {

        /**
         * Content Security Policy header value
         */
        private String contentSecurityPolicy;

        /**
         * Remember-me authentication configuration
         */
        private RememberMe rememberMe = new RememberMe();

        /**
         * Enterprise SSO (OIDC + SAML) configuration
         */
        private Sso sso = new Sso();

        /**
         * Social login (OAuth2) configuration
         */
        private SocialLogin socialLogin = new SocialLogin();

        /**
         * System user credentials configuration
         */
        private System system = new System();

        /**
         * Two-factor authentication configuration
         */
        private TwoFactorAuthentication twoFactorAuthentication = new TwoFactorAuthentication();

        public String getContentSecurityPolicy() {
            return contentSecurityPolicy;
        }

        public RememberMe getRememberMe() {
            return rememberMe;
        }

        public void setContentSecurityPolicy(String contentSecurityPolicy) {
            this.contentSecurityPolicy = contentSecurityPolicy;
        }

        public void setRememberMe(RememberMe rememberMe) {
            this.rememberMe = rememberMe;
        }

        public Sso getSso() {
            return sso;
        }

        public void setSso(Sso sso) {
            this.sso = sso;
        }

        public SocialLogin getSocialLogin() {
            return socialLogin;
        }

        public void setSocialLogin(SocialLogin socialLogin) {
            this.socialLogin = socialLogin;
        }

        public System getSystem() {
            return system;
        }

        public void setSystem(System system) {
            this.system = system;
        }

        public TwoFactorAuthentication getTwoFactorAuthentication() {
            return twoFactorAuthentication;
        }

        public void setTwoFactorAuthentication(TwoFactorAuthentication twoFactorAuthentication) {
            this.twoFactorAuthentication = twoFactorAuthentication;
        }

        /**
         * Remember-me authentication configuration.
         */
        public static class RememberMe {

            /**
             * Remember-me token encryption key
             */
            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }

        /**
         * Enterprise SSO (OIDC + SAML) configuration.
         */
        public static class Sso {

            /**
             * Whether enterprise SSO is enabled
             */
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        /**
         * System user credentials for internal operations.
         */
        public static class System {

            /**
             * System user username
             */
            private String username;

            /**
             * System user password
             */
            private String password;

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }

        /**
         * Social login (OAuth2) configuration for Google and GitHub providers.
         */
        public static class SocialLogin {

            /**
             * Whether social login is enabled
             */
            private boolean enabled;

            /**
             * Google OAuth2 configuration
             */
            private Provider google = new Provider();

            /**
             * GitHub OAuth2 configuration
             */
            private Provider github = new Provider();

            public boolean isEnabled() {
                return enabled;
            }

            public Provider getGoogle() {
                return google;
            }

            public Provider getGithub() {
                return github;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public void setGoogle(Provider google) {
                this.google = google;
            }

            public void setGithub(Provider github) {
                this.github = github;
            }

            /**
             * OAuth2 provider credentials.
             */
            public static class Provider {

                /**
                 * OAuth2 client ID
                 */
                private String clientId;

                /**
                 * OAuth2 client secret
                 */
                private String clientSecret;

                public String getClientId() {
                    return clientId;
                }

                public String getClientSecret() {
                    return clientSecret;
                }

                public void setClientId(String clientId) {
                    this.clientId = clientId;
                }

                public void setClientSecret(String clientSecret) {
                    this.clientSecret = clientSecret;
                }
            }
        }

        /**
         * Two-factor authentication configuration.
         */
        public static class TwoFactorAuthentication {

            /**
             * Whether two-factor authentication is enabled
             */
            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }
    }

    /**
     * Scheduler configuration for scheduled triggers.
     */
    public static class Scheduler {

        /**
         * Available scheduler providers.
         */
        public enum Provider {
            /**
             * AWS EventBridge Scheduler
             */
            AWS,
            /**
             * Quartz Scheduler
             */
            QUARTZ
        }

        /**
         * Scheduler provider
         */
        private Provider provider = Provider.QUARTZ;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * User sign-up configuration.
     */
    public static class SignUp {

        /**
         * Whether email activation is required after sign-up
         */
        private boolean activationRequired;

        /**
         * Whether user sign-up is enabled
         */
        private boolean enabled = true;

        public boolean isActivationRequired() {
            return activationRequired;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setActivationRequired(boolean activationRequired) {
            this.activationRequired = activationRequired;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Multi-tenancy configuration.
     */
    public static class Tenant {

        /**
         * Tenancy mode.
         */
        public enum Mode {
            /**
             * Multi-tenant mode
             */
            MULTI,
            /**
             * Single-tenant mode
             */
            SINGLE
        }

        /**
         * Tenancy mode
         */
        private Mode mode = Mode.SINGLE;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }
    }

    /**
     * Database upgrade configuration. Controls whether Liquibase migrations run at application startup. Useful for
     * disabling migrations on read-only replicas or when only a designated instance should apply schema changes in a
     * multi-instance deployment.
     */
    public static class Upgrade {

        /**
         * Whether database upgrades (Liquibase migrations) run at startup
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * User guiding configuration for in-application tutorials.
     */
    public static class UserGuiding {

        /**
         * Container ID for the UserGuiding SDK
         */
        private String containerId;

        /**
         * Whether user guiding is enabled
         */
        private boolean enabled;

        public String getContainerId() {
            return containerId;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setContainerId(String containerId) {
            this.containerId = containerId;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Worker node configuration for task execution.
     */
    public static class Worker {

        /**
         * Whether worker is enabled
         */
        private boolean enabled = true;

        /**
         * Task execution configuration
         */
        private Task task = new Task();

        public boolean isEnabled() {
            return enabled;
        }

        public Task getTask() {
            return task;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        /**
         * Task execution configuration for workers.
         */
        public static class Task {

            /**
             * Default timeout for task execution in milliseconds
             */
            private Long defaultTimeout;

            /**
             * Event subscriptions mapped by task type to number of subscribers
             */
            private Map<String, Integer> subscriptions = new HashMap<>();

            public Long getDefaultTimeout() {
                return defaultTimeout;
            }

            public Map<String, Integer> getSubscriptions() {
                return subscriptions;
            }

            public void setDefaultTimeout(Long defaultTimeout) {
                this.defaultTimeout = defaultTimeout;
            }

            public void setSubscriptions(Map<String, Integer> subscriptions) {
                this.subscriptions = subscriptions;
            }
        }
    }

    /**
     * Workflow engine configuration.
     */
    public static class Workflow {

        /**
         * Output storage configuration for workflow results
         */
        private OutputStorage outputStorage = new OutputStorage();

        /**
         * Workflow repository configuration
         */
        private Repository repository = new Repository();

        public OutputStorage getOutputStorage() {
            return outputStorage;
        }

        public Repository getRepository() {
            return repository;
        }

        public void setOutputStorage(OutputStorage outputStorage) {
            this.outputStorage = outputStorage;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }

        /**
         * Workflow output storage configuration for persisting execution results.
         */
        public static class OutputStorage {

            /**
             * Available output storage providers.
             */
            public enum Provider {
                /**
                 * AWS S3 storage
                 */
                AWS,
                /**
                 * Filesystem storage
                 */
                FILESYSTEM,
                /**
                 * JDBC database storage
                 */
                JDBC
            }

            /**
             * Output storage provider
             */
            private Provider provider = Provider.JDBC;

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }
        }

        /**
         * Workflow repository configuration for storing workflow definitions.
         */
        public static class Repository {

            /**
             * Classpath-based workflow repository
             */
            private Classpath classpath = new Classpath();

            /**
             * Filesystem-based workflow repository
             */
            private Filesystem filesystem = new Filesystem();

            /**
             * Git-based workflow repository
             */
            private Git git = new Git();

            /**
             * JDBC-based workflow repository
             */
            private Jdbc jdbc = new Jdbc();

            public Classpath getClasspath() {
                return classpath;
            }

            public Filesystem getFilesystem() {
                return filesystem;
            }

            public Git getGit() {
                return git;
            }

            public Jdbc getJdbc() {
                return jdbc;
            }

            public void setClasspath(Classpath classpath) {
                this.classpath = classpath;
            }

            public void setFilesystem(Filesystem filesystem) {
                this.filesystem = filesystem;
            }

            public void setGit(Git git) {
                this.git = git;
            }

            public void setJdbc(Jdbc jdbc) {
                this.jdbc = jdbc;
            }

            /**
             * Classpath-based workflow repository configuration.
             */
            public static class Classpath {

                /**
                 * Whether classpath repository is enabled
                 */
                private boolean enabled;

                /**
                 * Ant-style pattern for locating workflow files
                 */
                private String locationPattern;

                public String getLocationPattern() {
                    return locationPattern;
                }

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public void setLocationPattern(String locationPattern) {
                    this.locationPattern = locationPattern;
                }
            }

            /**
             * Filesystem-based workflow repository configuration.
             */
            public static class Filesystem {

                /**
                 * Whether filesystem repository is enabled
                 */
                private boolean enabled;

                /**
                 * Ant-style pattern for locating workflow files
                 */
                private String locationPattern;

                public String getLocationPattern() {
                    return locationPattern;
                }

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public void setLocationPattern(String locationPattern) {
                    this.locationPattern = locationPattern;
                }
            }

            /**
             * Git-based workflow repository configuration.
             */
            public static class Git {

                /**
                 * Git branch to use
                 */
                private String branch;

                /**
                 * Whether Git repository is enabled
                 */
                private boolean enabled;

                /**
                 * Git repository password
                 */
                private String password;

                /**
                 * Paths within repository to search for workflows
                 */
                private String[] searchPaths;

                /**
                 * Git repository URL
                 */
                private String url;

                /**
                 * Git repository username
                 */
                private String username;

                public String getBranch() {
                    return branch;
                }

                public String getPassword() {
                    return password;
                }

                public String[] getSearchPaths() {
                    return searchPaths;
                }

                public String getUrl() {
                    return url;
                }

                public String getUsername() {
                    return username;
                }

                public boolean isEnabled() {
                    return enabled;
                }

                public void setBranch(String branch) {
                    this.branch = branch;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }

                public void setPassword(String password) {
                    this.password = password;
                }

                public void setSearchPaths(String[] searchPaths) {
                    this.searchPaths = searchPaths;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public void setUsername(String username) {
                    this.username = username;
                }
            }

            /**
             * JDBC-based workflow repository configuration for database storage.
             */
            public static class Jdbc {

                /**
                 * Whether JDBC repository is enabled
                 */
                private boolean enabled = true;

                public boolean isEnabled() {
                    return enabled;
                }

                public void setEnabled(boolean enabled) {
                    this.enabled = enabled;
                }
            }
        }
    }
}
