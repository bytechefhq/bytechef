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
        /** Community Edition */
        CE,
        /** Enterprise Edition */
        EE
    }

    /** AI and machine learning configuration */
    private Ai ai = new Ai();

    /** Analytics configuration */
    private Analytics analytics = new Analytics();

    /** Cache provider configuration */
    private Cache cache = new Cache();

    /** Cloud provider configuration */
    private Cloud cloud = new Cloud();

    /** Component registry configuration */
    private Component component = new Component();

    /** Workflow coordinator configuration */
    private Coordinator coordinator = new Coordinator();

    /** Database datasource configuration */
    private Datasource datasource = new Datasource();

    /** Data storage provider configuration */
    private DataStorage dataStorage;

    /** Service discovery configuration */
    private DiscoveryService discoveryService = new DiscoveryService();

    /** Application edition (CE or EE) */
    private Edition edition = Edition.EE;

    /** Encryption configuration */
    private Encryption encryption;

    /** List of enabled feature flags */
    private List<String> featureFlags = List.of();

    /** File storage provider configuration */
    private FileStorage fileStorage = new FileStorage();

    /** Help hub configuration */
    private HelpHub helpHub = new HelpHub();

    /** Knowledge base configuration */
    private KnowledgeBase knowledgeBase = new KnowledgeBase();

    /** Observability and logging configuration */
    private Observability observability = new Observability();

    /** Email configuration */
    private Mail mail = new Mail();

    /** Message broker configuration */
    private MessageBroker messageBroker = new MessageBroker();

    /** OAuth2 configuration */
    private Oauth2 oauth2 = new Oauth2();

    /** Public URL for the application */
    private String publicUrl;

    /** Static resources configuration */
    private Resources resources = new Resources();

    /** Security configuration */
    private Security security;

    /** User sign-up configuration */
    private SignUp signUp = new SignUp();

    /** Multi-tenancy configuration */
    private Tenant tenant = new Tenant();

    /** Webhook URL for external integrations */
    private String webhookUrl;
    private Worker worker = new Worker();

    /** Workflow engine configuration */
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

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public Observability getObservability() {
        return observability;
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

    public String getPublicUrl() {
        return publicUrl;
    }

    public Resources getResources() {
        return resources;
    }

    public Security getSecurity() {
        return security;
    }

    public SignUp getSignUp() {
        return signUp;
    }

    public Tenant getTenant() {
        return tenant;
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

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public void setObservability(Observability observability) {
        this.observability = observability;
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

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public void setSignUp(SignUp signUp) {
        this.signUp = signUp;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
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

        /** Whether observability features are enabled */
        private boolean enabled;

        /** Logging observability configuration */
        private Logging logging = new Logging();

        /** Metrics observability configuration */
        private Metrics metrics = new Metrics();

        /** Tracing observability configuration */
        private Tracing tracing = new Tracing();

        public boolean isEnabled() {
            return enabled;
        }

        public Logging getLogging() {
            return logging;
        }

        public Metrics getMetrics() {
            return metrics;
        }

        public Tracing getTracing() {
            return tracing;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

            /** OTLP endpoint for logging export */
            private String endpoint;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }
        }

        public static class Metrics {

            /** OTLP endpoint for metrics export */
            private String endpoint;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }
        }

        public static class Tracing {

            /** OTLP endpoint for tracing export */
            private String endpoint;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }
        }
    }

    /**
     * Ai properties.
     */
    public static class Ai {

        private Anthropic anthropic = new Anthropic();
        private Copilot copilot = new Copilot();
        private KnowledgeBase knowledgeBase = new KnowledgeBase();
        private Mcp mcp = new Mcp();
        private OpenAi openAi = new OpenAi();
        private Provider provider = new Provider();
        private Vectorstore vectorstore = new Vectorstore();

        public Anthropic getAnthropic() {
            return anthropic;
        }

        public Copilot getCopilot() {
            return copilot;
        }

        public KnowledgeBase getKnowledgeBase() {
            return knowledgeBase;
        }

        public Mcp getMcp() {
            return mcp;
        }

        public OpenAi getOpenAi() {
            return openAi;
        }

        public Provider getProvider() {
            return provider;
        }

        public Vectorstore getVectorstore() {
            return vectorstore;
        }

        public void setAnthropic(Anthropic anthropic) {
            this.anthropic = anthropic;
        }

        public void setCopilot(Copilot copilot) {
            this.copilot = copilot;
        }

        public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
            this.knowledgeBase = knowledgeBase;
        }

        public void setMcp(Mcp mcp) {
            this.mcp = mcp;
        }

        public void setOpenAi(OpenAi openAi) {
            this.openAi = openAi;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public void setVectorstore(Vectorstore vectorstore) {
            this.vectorstore = vectorstore;
        }

        /**
         * Anthropic AI provider configuration.
         */
        public static class Anthropic {

            /** Anthropic API key */
            private String apiKey;

            /** Chat model configuration */
            private Chat chat = new Chat();

            /** Embedding model configuration */
            private Embedding embedding = new Embedding();

            public String getApiKey() {
                return apiKey;
            }

            public Chat getChat() {
                return chat;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public void setChat(Chat chat) {
                this.chat = chat;
            }

            public Embedding getEmbedding() {
                return embedding;
            }

            public void setEmbedding(Embedding embedding) {
                this.embedding = embedding;
            }

            /**
             * Anthropic chat model configuration.
             */
            public static class Chat {

                /** Chat model options */
                private Options options = new Options();

                public Options getOptions() {
                    return options;
                }

                public void setOptions(Options options) {
                    this.options = options;
                }

                /**
                 * Chat model configuration options.
                 */
                public static class Options {

                    /** AI model name (e.g., claude-3-opus-20240229) */
                    private String model;

                    /** Temperature for response randomness (0.0-1.0) */
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
             * Anthropic embedding configuration (using external provider).
             */
            public static class Embedding {

                /**
                 * Embedding provider type for Anthropic.
                 */
                public enum Provider {
                    /** OpenAI embedding provider */
                    OPENAI
                }

                /** OpenAI embedding configuration */
                private OpenAi openAi = new OpenAi();

                /** Embedding provider */
                private Provider provider = Provider.OPENAI;

                public OpenAi getOpenAi() {
                    return openAi;
                }

                public void setOpenAi(OpenAi openAi) {
                    this.openAi = openAi;
                }

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }

                /**
                 * OpenAI embedding configuration for Anthropic.
                 */
                public static class OpenAi {

                    /** OpenAI embedding options */
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

                        /** Embedding model name */
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
         * AI Copilot configuration for workflow assistance.
         */
        public static class Copilot {

            /**
             * AI provider for Copilot functionality.
             */
            public enum Provider {
                /** OpenAI provider */
                OPENAI,
                /** Anthropic provider */
                ANTHROPIC
            }

            /** Whether Copilot is enabled */
            private boolean enabled;

            /** AI provider for Copilot */
            private Provider provider = Provider.OPENAI;

            /** Vector store configuration for Copilot context */
            private Vectorstore vectorstore = new Vectorstore();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }

            public Vectorstore getVectorstore() {
                return vectorstore;
            }

            public void setVectorstore(Vectorstore vectorstore) {
                this.vectorstore = vectorstore;
            }

            /**
             * Vector store configuration for Copilot.
             */
            public static class Vectorstore {

                /** Vector store provider */
                private Ai.Vectorstore.Provider provider = Ai.Vectorstore.Provider.PGVECTOR;

                public Ai.Vectorstore.Provider getProvider() {
                    return provider;
                }

                public void setProvider(Ai.Vectorstore.Provider provider) {
                    this.provider = provider;
                }
            }
        }

        /**
         * Knowledge base AI configuration.
         */
        public static class KnowledgeBase {

            /** Whether knowledge base AI features are enabled */
            private boolean enabled;

            /** Embedding model configuration for knowledge base */
            private Embedding embedding = new Embedding();

            /** Vector store configuration for knowledge base */
            private Vectorstore vectorstore = new Vectorstore();

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public Embedding getEmbedding() {
                return embedding;
            }

            public void setEmbedding(Embedding embedding) {
                this.embedding = embedding;
            }

            public Vectorstore getVectorstore() {
                return vectorstore;
            }

            public void setVectorstore(Vectorstore vectorstore) {
                this.vectorstore = vectorstore;
            }

            /**
             * Knowledge base embedding configuration.
             */
            public static class Embedding {

                /**
                 * Embedding provider for knowledge base.
                 */
                public enum Provider {
                    /** OpenAI embedding provider */
                    OPENAI
                }

                /** OpenAI embedding configuration */
                private OpenAi openAi = new OpenAi();

                /** Embedding provider */
                private Provider provider = Provider.OPENAI;

                public OpenAi getOpenAi() {
                    return openAi;
                }

                public void setOpenAi(OpenAi openAi) {
                    this.openAi = openAi;
                }

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }

                /**
                 * OpenAI embedding configuration for knowledge base.
                 */
                public static class OpenAi {

                    /** OpenAI embedding options */
                    private Options options = new Options();

                    public Options getOptions() {
                        return options;
                    }

                    public void setOptions(Options options) {
                        this.options = options;
                    }

                    /**
                     * OpenAI embedding model options for knowledge base.
                     */
                    public static class Options {

                        /** Embedding model name */
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
             * Knowledge base vector store configuration.
             */
            public static class Vectorstore {

                /** Vector store provider */
                private Ai.Vectorstore.Provider provider = Ai.Vectorstore.Provider.PGVECTOR;

                public Ai.Vectorstore.Provider getProvider() {
                    return provider;
                }

                public void setProvider(Ai.Vectorstore.Provider provider) {
                    this.provider = provider;
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

                /** Whether the MCP server is enabled */
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
         * OpenAI provider configuration.
         */
        public static class OpenAi {

            /** OpenAI API key */
            private String apiKey;

            /** Chat model configuration */
            private Chat chat = new Chat();

            /** Embedding model configuration */
            private Embedding embedding = new Embedding();

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public Chat getChat() {
                return chat;
            }

            public Embedding getEmbedding() {
                return embedding;
            }

            public void setChat(Chat chat) {
                this.chat = chat;
            }

            public void setEmbedding(Embedding embedding) {
                this.embedding = embedding;
            }

            /**
             * OpenAI chat model configuration.
             */
            public static class Chat {

                /** Chat model options */
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

                    /** Chat model name (e.g., gpt-4, gpt-3.5-turbo) */
                    private String model;

                    /** Temperature for response randomness (0.0-2.0) */
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
             * OpenAI embedding model configuration.
             */
            public static class Embedding {

                /** Embedding model options */
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

                    /** Embedding model name (e.g., text-embedding-ada-002) */
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
         * AI provider API key configuration for various AI services.
         */
        public static class Provider {

            /** Amazon Bedrock Anthropic Claude 2 configuration */
            private AmazonBedrockAnthropic2 amazonBedrockAnthropic2 = new AmazonBedrockAnthropic2();

            /** Amazon Bedrock Anthropic Claude 3 configuration */
            private AmazonBedrockAnthropic3 amazonBedrockAnthropic3 = new AmazonBedrockAnthropic3();

            /** Amazon Bedrock Cohere configuration */
            private AmazonBedrockCohere amazonBedrockCohere = new AmazonBedrockCohere();

            /** Amazon Bedrock Jurassic-2 configuration */
            private AmazonBedrockJurassic2 amazonBedrockJurassic2 = new AmazonBedrockJurassic2();

            /** Amazon Bedrock Llama configuration */
            private AmazonBedrockLlama amazonBedrockLlama = new AmazonBedrockLlama();

            /** Amazon Bedrock Titan configuration */
            private AmazonBedrockTitan amazonBedrockTitan = new AmazonBedrockTitan();

            /** Anthropic Claude configuration */
            private Anthropic anthropic = new Anthropic();

            /** Azure OpenAI configuration */
            private AzureOpenAi azureOpenAi = new AzureOpenAi();

            /** DeepSeek configuration */
            private DeepSeek deepSeek = new DeepSeek();

            /** Groq configuration */
            private Groq groq = new Groq();

            /** Mistral AI configuration */
            private Mistral mistral = new Mistral();

            /** NVIDIA AI configuration */
            private Nvidia nvidia = new Nvidia();

            /** OpenAI configuration */
            private OpenAi openAi = new OpenAi();

            /** Perplexity AI configuration */
            private Perplexity perplexity = new Perplexity();

            /** Stability AI configuration */
            private Stability stability = new Stability();

            /** Google Vertex AI Gemini configuration */
            private VertexGemini vertexGemini = new VertexGemini();

            public AmazonBedrockAnthropic2 getAmazonBedrockAnthropic2() {
                return amazonBedrockAnthropic2;
            }

            public AmazonBedrockAnthropic3 getAmazonBedrockAnthropic3() {
                return amazonBedrockAnthropic3;
            }

            public AmazonBedrockCohere getAmazonBedrockCohere() {
                return amazonBedrockCohere;
            }

            public AmazonBedrockJurassic2 getAmazonBedrockJurassic2() {
                return amazonBedrockJurassic2;
            }

            public AmazonBedrockLlama getAmazonBedrockLlama() {
                return amazonBedrockLlama;
            }

            public AmazonBedrockTitan getAmazonBedrockTitan() {
                return amazonBedrockTitan;
            }

            public Anthropic getAnthropic() {
                return anthropic;
            }

            public AzureOpenAi getAzureOpenAi() {
                return azureOpenAi;
            }

            public DeepSeek getDeepSeek() {
                return deepSeek;
            }

            public Groq getGroq() {
                return groq;
            }

            public Nvidia getNvidia() {
                return nvidia;
            }

            /** HuggingFace configuration */
            private HuggingFace huggingFace = new HuggingFace();

            public HuggingFace getHuggingFace() {
                return huggingFace;
            }

            public Mistral getMistral() {
                return mistral;
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

            public void setAmazonBedrockAnthropic2(AmazonBedrockAnthropic2 amazonBedrockAnthropic2) {
                this.amazonBedrockAnthropic2 = amazonBedrockAnthropic2;
            }

            public void setAmazonBedrockAnthropic3(AmazonBedrockAnthropic3 amazonBedrockAnthropic3) {
                this.amazonBedrockAnthropic3 = amazonBedrockAnthropic3;
            }

            public void setAmazonBedrockCohere(AmazonBedrockCohere amazonBedrockCohere) {
                this.amazonBedrockCohere = amazonBedrockCohere;
            }

            public void setAmazonBedrockJurassic2(AmazonBedrockJurassic2 amazonBedrockJurassic2) {
                this.amazonBedrockJurassic2 = amazonBedrockJurassic2;
            }

            public void setAmazonBedrockLlama(AmazonBedrockLlama amazonBedrockLlama) {
                this.amazonBedrockLlama = amazonBedrockLlama;
            }

            public void setAmazonBedrockTitan(AmazonBedrockTitan amazonBedrockTitan) {
                this.amazonBedrockTitan = amazonBedrockTitan;
            }

            public void setAnthropic(Anthropic anthropic) {
                this.anthropic = anthropic;
            }

            public void setAzureOpenAi(AzureOpenAi azureOpenAi) {
                this.azureOpenAi = azureOpenAi;
            }

            public void setDeepSeek(DeepSeek deepSeek) {
                this.deepSeek = deepSeek;
            }

            public void setGroq(Groq groq) {
                this.groq = groq;
            }

            public void setNvidia(Nvidia nvidia) {
                this.nvidia = nvidia;
            }

            public void setHuggingFace(HuggingFace huggingFace) {
                this.huggingFace = huggingFace;
            }

            public void setMistral(Mistral mistral) {
                this.mistral = mistral;
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

            /** Amazon Bedrock Anthropic Claude 2 API configuration. */
            public static class AmazonBedrockAnthropic2 {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Amazon Bedrock Anthropic Claude 3 API configuration. */
            public static class AmazonBedrockAnthropic3 {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Amazon Bedrock Cohere API configuration. */
            public static class AmazonBedrockCohere {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Amazon Bedrock Jurassic-2 API configuration. */
            public static class AmazonBedrockJurassic2 {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Amazon Bedrock Llama API configuration. */
            public static class AmazonBedrockLlama {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Amazon Bedrock Titan API configuration. */
            public static class AmazonBedrockTitan {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Anthropic Claude API configuration. */
            public static class Anthropic {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Azure OpenAI API configuration. */
            public static class AzureOpenAi {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** DeepSeek API configuration. */
            public static class DeepSeek {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Groq API configuration. */
            public static class Groq {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** NVIDIA AI API configuration. */
            public static class Nvidia {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** HuggingFace API configuration. */
            public static class HuggingFace {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Mistral AI API configuration. */
            public static class Mistral {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** OpenAI API configuration. */
            public static class OpenAi {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Perplexity AI API configuration. */
            public static class Perplexity {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Stability AI API configuration. */
            public static class Stability {
                /** API key */
                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            /** Google Vertex AI Gemini API configuration. */
            public static class VertexGemini {
                /** API key */
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
         * Vector store configuration for AI embeddings storage.
         */
        public static class Vectorstore {

            /**
             * Vector store provider type.
             */
            public enum Provider {
                /** PostgreSQL with pgvector extension */
                PGVECTOR
            }

            /** PgVector configuration */
            private PgVector pgVector = new PgVector();

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

                /** Database password */
                private String password;

                /** JDBC URL for PostgreSQL database */
                private String url;

                /** Database username */
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

        /** Whether analytics features are enabled */
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
            /** Redis cache */
            REDIS,
            /** Caffeine in-memory cache */
            CAFFEINE
        }

        /** Cache provider */
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
            /** Amazon Web Services */
            AWS,
            /** No cloud provider */
            NONE
        }

        /** AWS configuration */
        private Aws aws;

        /** Cloud provider */
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

            /** AWS access key ID */
            private String accessKeyId;

            /** AWS region */
            private String region;

            /** AWS secret access key */
            private String secretAccessKey;

            /** AWS account ID */
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

        /** Component registry configuration */
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

            /** List of component names to exclude from registration */
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

        /** Whether coordinator is enabled */
        private boolean enabled = true;

        /** Task coordination configuration */
        private Task task = new Task();

        /** Trigger coordination configuration */
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

            /** Event subscription configuration */
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

                /** Number of subscribers for application events */
                private int applicationEvents = 1;

                /** Number of subscribers for resume job events */
                private int resumeJobEvents = 1;

                /** Number of subscribers for start job events */
                private int startJobEvents = 1;

                /** Number of subscribers for stop job events */
                private int stopJobEvents = 1;

                /** Number of subscribers for task execution complete events */
                private int taskExecutionCompleteEvents = 1;

                /** Number of subscribers for task execution error events */
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

            /** Polling configuration for trigger checks */
            private Polling polling = new Polling();

            /** Event subscription configuration */
            private Subscriptions subscriptions = new Subscriptions();

            /** Scheduler configuration */
            private Scheduler scheduler = new Scheduler();

            public Polling getPolling() {
                return polling;
            }

            public void setPolling(Polling polling) {
                this.polling = polling;
            }

            public Scheduler getScheduler() {
                return scheduler;
            }

            public void setScheduler(Scheduler scheduler) {
                this.scheduler = scheduler;
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

                /** Check period in seconds */
                private int checkPeriod = 5;

                public int getCheckPeriod() {
                    return checkPeriod;
                }

                public void setCheckPeriod(int checkPeriod) {
                    this.checkPeriod = checkPeriod;
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
                    /** AWS EventBridge Scheduler */
                    AWS,
                    /** Quartz Scheduler */
                    QUARTZ
                }

                /** Scheduler provider */
                private Provider provider = Provider.QUARTZ;

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }
            }

            /**
             * Event subscription configuration for trigger events.
             */
            public static class Subscriptions {

                /** Number of subscribers for application events */
                private int applicationEvents = 1;

                /** Number of subscribers for trigger execution complete events */
                private int triggerExecutionCompleteEvents = 1;

                /** Number of subscribers for trigger execution error events */
                private int triggerExecutionErrorEvents = 1;

                /** Number of subscribers for trigger listener events */
                private int triggerListenerEvents = 1;

                /** Number of subscribers for trigger poll events */
                private int triggerPollEvents = 1;

                /** Number of subscribers for trigger webhook events */
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

        /** Database password */
        private String password;

        /** JDBC URL for database connection */
        private String url;

        /** Database username */
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
            /** AWS-based storage */
            AWS,
            /** Filesystem-based storage */
            FILESYSTEM,
            /** JDBC database storage */
            JDBC
        }

        /** Data storage provider */
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
            /** Redis-based service discovery */
            REDIS
        }

        /** Service discovery provider */
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
            /** AWS S3 storage */
            AWS,
            /** Local filesystem storage */
            FILESYSTEM,
            /** JDBC database storage */
            JDBC
        }

        /** AWS S3 configuration */
        private Aws aws = new Aws();

        /** Filesystem storage configuration */
        private Filesystem filesystem = new Filesystem();

        /** File storage provider */
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

            /** S3 bucket name */
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

            /** Base directory for file storage */
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
            /** Filesystem-based key storage */
            FILESYSTEM,
            /** Property-based key storage */
            PROPERTY;
        }

        /** Encryption provider */
        private Provider provider = Provider.FILESYSTEM;

        /** Property-based encryption configuration */
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

            /** Encryption key */
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

        /** Whether help hub is enabled */
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Knowledge base configuration for document storage and retrieval.
     */
    public static class KnowledgeBase {

        /** Whether knowledge base is enabled */
        private boolean enabled;

        /** OCR configuration */
        private Ocr ocr = new Ocr();

        /** Event subscription configuration */
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
         * Embedding configuration for knowledge base document processing.
         */
        public static class Embedding {

            /**
             * Embedding provider type.
             */
            public enum Provider {
                /** OpenAI embedding provider */
                OPENAI,
                /** Ollama embedding provider */
                OLLAMA,
                /** Azure embedding provider */
                AZURE
            }

            /** Embedding model name */
            private String model = "text-embedding-3-small";

            /** Embedding provider */
            private Provider provider = Provider.OPENAI;

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }
        }

        /**
         * OCR configuration for knowledge base document processing.
         */
        public static class Ocr {

            /**
             * OCR provider type.
             */
            public enum Provider {
                /** No OCR provider */
                NONE,
                /** Azure OCR provider */
                AZURE,
                /** Mistral OCR provider */
                MISTRAL
            }

            /** OCR provider */
            private Provider provider = Provider.NONE;

            /** Mistral OCR configuration */
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

                /** Mistral API key */
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

            /** Number of subscribers for document process events */
            private int documentProcessEvents = 1;

            /** Number of subscribers for document chunk update events */
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
     * Email configuration for sending notifications and alerts.
     */
    public static class Mail {

        /** Whether SMTP authentication is required */
        private boolean auth;

        /** Whether to enable debug logging for mail */
        private boolean debug;

        /** Email address to use as sender */
        private String from;

        /** SMTP server hostname */
        private String host;

        /** Base URL for email links */
        private String baseUrl;

        /** SMTP server password */
        private String password;

        /** Email protocol (e.g., smtp, smtps) */
        private String protocol;

        /** SMTP server port */
        private int port = 25;

        /** STARTTLS configuration */
        private Starttls starttls = new Starttls();

        /** SSL configuration */
        private Ssl ssl = new Ssl();

        /** SMTP server username */
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

            /** Whether SSL is enabled */
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

            /** Whether STARTTLS is enabled */
            private boolean enable;

            /** Whether STARTTLS is required */
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
            /** AMQP (e.g., RabbitMQ) */
            AMQP,
            /** AWS SQS */
            AWS,
            /** JMS */
            JMS,
            /** Apache Kafka */
            KAFKA,
            /** In-memory message broker */
            MEMORY,
            /** Redis-based message broker */
            REDIS
        }

        /** Message broker provider */
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

        /** Predefined OAuth2 applications mapped by component name */
        private Map<String, OAuth2App> predefinedApps = new HashMap<>();

        /** OAuth2 redirect URI */
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

            /** OAuth2 client ID */
            private String clientId;

            /** OAuth2 client secret */
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
     * Static resources configuration.
     */
    public static class Resources {

        /** Web resources path */
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

        /** Content Security Policy header value */
        private String contentSecurityPolicy;

        /** Remember-me authentication configuration */
        private RememberMe rememberMe = new RememberMe();

        /** System user credentials configuration */
        private System system = new System();

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

        public System getSystem() {
            return system;
        }

        public void setSystem(System system) {
            this.system = system;
        }

        /**
         * Remember-me authentication configuration.
         */
        public static class RememberMe {

            /** Remember-me token encryption key */
            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }

        /**
         * System user credentials for internal operations.
         */
        public static class System {

            /** System user username */
            private String username;

            /** System user password */
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
    }

    /**
     * User sign-up configuration.
     */
    public static class SignUp {

        /** Whether email activation is required after sign-up */
        private boolean activationRequired;

        /** Whether user sign-up is enabled */
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
            /** Multi-tenant mode */
            MULTI,
            /** Single-tenant mode */
            SINGLE
        }

        /** Tenancy mode */
        private Mode mode = Mode.SINGLE;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }
    }

    /**
     * Worker node configuration for task execution.
     */
    public static class Worker {

        /** Whether worker is enabled */
        private boolean enabled = true;

        /** Task execution configuration */
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

            /** Default timeout for task execution in milliseconds */
            private Long defaultTimeout;

            /** Event subscriptions mapped by task type to number of subscribers */
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

        /** Output storage configuration for workflow results */
        private OutputStorage outputStorage = new OutputStorage();

        /** Workflow repository configuration */
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
                /** AWS S3 storage */
                AWS,
                /** Filesystem storage */
                FILESYSTEM,
                /** JDBC database storage */
                JDBC
            }

            /** Output storage provider */
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

            /** Classpath-based workflow repository */
            private Classpath classpath = new Classpath();

            /** Filesystem-based workflow repository */
            private Filesystem filesystem = new Filesystem();

            /** Git-based workflow repository */
            private Git git = new Git();

            /** JDBC-based workflow repository */
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

                /** Whether classpath repository is enabled */
                private boolean enabled;

                /** Ant-style pattern for locating workflow files */
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

                /** Whether filesystem repository is enabled */
                private boolean enabled;

                /** Ant-style pattern for locating workflow files */
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

                /** Git branch to use */
                private String branch;

                /** Whether Git repository is enabled */
                private boolean enabled;

                /** Git repository password */
                private String password;

                /** Paths within repository to search for workflows */
                private String[] searchPaths;

                /** Git repository URL */
                private String url;

                /** Git repository username */
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

                /** Whether JDBC repository is enabled */
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
