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
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef", ignoreUnknownFields = false)
@SuppressFBWarnings("EI")
public class ApplicationProperties {

    /**
     * Edition.
     */
    public enum Edition {
        CE, EE
    }

    private Ai ai = new Ai();
    private Analytics analytics = new Analytics();
    private Cache cache = new Cache();
    private Cloud cloud = new Cloud();
    private Component component = new Component();
    private Coordinator coordinator = new Coordinator();
    private Datasource datasource = new Datasource();
    private DataStorage dataStorage;
    private DiscoveryService discoveryService = new DiscoveryService();
    private Edition edition = Edition.EE;
    private Encryption encryption;
    private List<String> featureFlags = List.of();
    private FileStorage fileStorage = new FileStorage();
    private HelpHub helpHub = new HelpHub();
    private Observability observability = new Observability();
    private Mail mail = new Mail();
    private Mcp mcp = new Mcp();
    private MessageBroker messageBroker = new MessageBroker();
    private Oauth2 oauth2 = new Oauth2();
    private String publicUrl;
    private Resources resources = new Resources();
    private Security security;
    private SignUp signUp = new SignUp();
    private Tenant tenant = new Tenant();
    private String webhookUrl;
    private Tracing tracing = new Tracing();
    private Worker worker = new Worker();
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

    public Observability getObservability() {
        return observability;
    }

    public Mail getMail() {
        return mail;
    }

    public Mcp getMcp() {
        return mcp;
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

    public Tracing getTracing() {
        return tracing;
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

    public void setObservability(Observability observability) {
        this.observability = observability;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public void setMcp(Mcp mcp) {
        this.mcp = mcp;
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

    public void setTracing(Tracing tracing) {
        this.tracing = tracing;
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

    public static class Observability {
        private boolean enabled;
        private Loki loki;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Loki getLoki() {
            return loki;
        }

        public void setLoki(Loki loki) {
            this.loki = loki;
        }

        public static class Loki {
            private Appender appender = new Appender();

            public Appender getAppender() {
                return appender;
            }

            public void setAppender(Appender appender) {
                this.appender = appender;
            }

            public static class Appender {
                private Http http;
                private Level level = Level.OFF;

                public Http getHttp() {
                    return http;
                }

                public void setHttp(Http http) {
                    this.http = http;
                }

                public enum Level {
                    OFF, ERROR, WARN, INFO, DEBUG, TRACE, ALL
                }

                public Level getLevel() {
                    return level;
                }

                public void setLevel(Level level) {
                    this.level = level;
                }

                public static class Http {
                    private String url;

                    public String getUrl() {
                        return url;
                    }

                    public void setUrl(String url) {
                        this.url = url;
                    }
                }
            }
        }
    }

    /**
     * Ai properties.
     */
    public static class Ai {

        private Provider provider = new Provider();
        private Copilot copilot = new Copilot();

        public Copilot getCopilot() {
            return copilot;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setCopilot(Copilot copilot) {
            this.copilot = copilot;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public static class Anthropic {

            private String apiKey;
            private Chat chat = new Chat();

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

            public static class Chat {

                private Options options = new Options();

                public Options getOptions() {
                    return options;
                }

                public void setOptions(Options options) {
                    this.options = options;
                }

                public static class Options {

                    private String model;

                    public String getModel() {
                        return model;
                    }

                    public void setModel(String model) {
                        this.model = model;
                    }

                    private Double temperature;

                    public Double getTemperature() {
                        return temperature;
                    }

                    public void setTemperature(Double temperature) {
                        this.temperature = temperature;
                    }
                }
            }
        }

        public static class Copilot {

            public enum Provider {
                OPENAI, ANTHROPIC
            }

            private Anthropic anthropic = new Anthropic();
            private Chat chat = new Chat();
            private Embedding embedding = new Embedding();
            private boolean enabled;
            private OpenAi openAi = new OpenAi();
            private Vectorstore vectorstore = new Vectorstore();

            public Anthropic getAnthropic() {
                return anthropic;
            }

            public void setAnthropic(Anthropic anthropic) {
                this.anthropic = anthropic;
            }

            public Chat getChat() {
                return chat;
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

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public OpenAi getOpenAi() {
                return openAi;
            }

            public void setOpenAi(OpenAi openAi) {
                this.openAi = openAi;
            }

            public Vectorstore getVectorstore() {
                return vectorstore;
            }

            public void setVectorstore(Vectorstore vectorstore) {
                this.vectorstore = vectorstore;
            }

            public static class Chat {

                private Provider provider = Provider.OPENAI;

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }
            }

            public static class Embedding {

                private Provider provider = Provider.OPENAI;

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }
            }

            public static class Vectorstore {

                private PgVector pgVector = new PgVector();

                public PgVector getPgVector() {
                    return pgVector;
                }

                public void setPgVector(PgVector pgVector) {
                    this.pgVector = pgVector;
                }

                public static class PgVector {

                    private String url;
                    private String username;
                    private String password;

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

                    public String getPassword() {
                        return password;
                    }

                    public void setPassword(String password) {
                        this.password = password;
                    }
                }
            }
        }

        public static class OpenAi {

            private String apiKey;
            private Chat chat = new Chat();

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

            public static class Chat {

                private Options options = new Options();

                public Options getOptions() {
                    return options;
                }

                public void setOptions(Options options) {
                    this.options = options;
                }

                public static class Options {

                    private String model;
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
        }

        public static class Provider {

            private AmazonBedrockAnthropic2 amazonBedrockAnthropic2 = new AmazonBedrockAnthropic2();
            private AmazonBedrockAnthropic3 amazonBedrockAnthropic3 = new AmazonBedrockAnthropic3();
            private AmazonBedrockCohere amazonBedrockCohere = new AmazonBedrockCohere();
            private AmazonBedrockJurassic2 amazonBedrockJurassic2 = new AmazonBedrockJurassic2();
            private AmazonBedrockLlama amazonBedrockLlama = new AmazonBedrockLlama();
            private AmazonBedrockTitan amazonBedrockTitan = new AmazonBedrockTitan();
            private Anthropic anthropic = new Anthropic();
            private AzureOpenAi azureOpenAi = new AzureOpenAi();
            private DeepSeek deepSeek = new DeepSeek();
            private Groq groq = new Groq();
            private Mistral mistral = new Mistral();
            private Nvidia nvidia = new Nvidia();
            private OpenAi openAi = new OpenAi();
            private Perplexity perplexity = new Perplexity();
            private Stability stability = new Stability();
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

            public static class AmazonBedrockAnthropic2 {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class AmazonBedrockAnthropic3 {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class AmazonBedrockCohere {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class AmazonBedrockJurassic2 {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class AmazonBedrockLlama {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class AmazonBedrockTitan {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class Anthropic {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class AzureOpenAi {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class DeepSeek {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class Groq {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class Nvidia {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class HuggingFace {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class Mistral {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class OpenAi {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class Perplexity {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class Stability {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }

            public static class VertexGemini {

                private String apiKey;

                public String getApiKey() {
                    return apiKey;
                }

                public void setApiKey(String apiKey) {
                    this.apiKey = apiKey;
                }
            }
        }
    }

    /**
     * Analytics properties.
     */
    public static class Analytics {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Cache properties.
     */
    public static class Cache {

        /**
         * Cache provider.
         */
        public enum Provider {
            REDIS, CAFFEINE
        }

        private Provider provider = Provider.CAFFEINE;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * Cloud properties.
     */
    public static class Cloud {

        /**
         * Cloud provider.
         */
        public enum Provider {
            AWS, NONE
        }

        private Aws aws;
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
         * AWS properties.
         */
        public static class Aws {

            private String accessKeyId;
            private String region;
            private String secretAccessKey;
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
     * Component properties.
     */
    public static class Component {

        private Registry registry = new Registry();

        public Registry getRegistry() {
            return registry;
        }

        public void setRegistry(Registry registry) {
            this.registry = registry;
        }

        /**
         * Registry properties.
         */
        public static class Registry {

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
     * Coordinator properties.
     */
    public static class Coordinator {

        private boolean enabled = true;
        private Task task = new Task();
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
         * Task properties.
         */
        public static class Task {

            private Subscriptions subscriptions = new Subscriptions();

            public Subscriptions getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Subscriptions subscriptions) {
                this.subscriptions = subscriptions;
            }

            /**
             * Subscriptions properties.
             */
            public static class Subscriptions {

                private int applicationEvents = 1;
                private int resumeJobEvents = 1;
                private int startJobEvents = 1;
                private int stopJobEvents = 1;
                private int taskExecutionCompleteEvents = 1;
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
         * Trigger properties.
         */
        public static class Trigger {

            private Polling polling = new Polling();
            private Subscriptions subscriptions = new Subscriptions();
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

            public static class Polling {

                private int checkPeriod = 5;

                public int getCheckPeriod() {
                    return checkPeriod;
                }

                public void setCheckPeriod(int checkPeriod) {
                    this.checkPeriod = checkPeriod;
                }
            }

            /**
             * Scheduler properties.
             */
            public static class Scheduler {

                /**
                 * Scheduler provider.
                 */
                public enum Provider {
                    AWS, QUARTZ
                }

                private Provider provider = Provider.QUARTZ;

                public Provider getProvider() {
                    return provider;
                }

                public void setProvider(Provider provider) {
                    this.provider = provider;
                }
            }

            /**
             * Subscriptions properties.
             */
            public static class Subscriptions {

                private int applicationEvents = 1;
                private int triggerExecutionCompleteEvents = 1;
                private int triggerExecutionErrorEvents = 1;
                private int triggerListenerEvents = 1;
                private int triggerPollEvents = 1;
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
     * Datasource properties.
     */
    public static class Datasource {

        private String password;
        private String url;
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
     *
     */
    public static class DataStorage {

        public enum Provider {
            AWS, FILESYSTEM, JDBC
        }

        private Provider provider = Provider.JDBC;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * Discovery service properties.
     */
    public static class DiscoveryService {

        private Provider provider = Provider.REDIS;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        /**
         * Discovery service provider.
         */
        public enum Provider {
            REDIS
        }
    }

    /**
     * FileStorage properties.
     */
    public static class FileStorage {

        /**
         * FileStorage provider.
         */
        public enum Provider {
            AWS, FILESYSTEM, JDBC
        }

        private Aws aws = new Aws();
        private Filesystem filesystem = new Filesystem();
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
         * AWS properties.
         */
        public static class Aws {

            private String bucket;

            public String getBucket() {
                return bucket;
            }

            public void setBucket(String bucket) {
                this.bucket = bucket;
            }
        }

        /**
         * Filesystem properties.
         */
        public static class Filesystem {

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
     * Encryption properties.
     */
    public static class Encryption {

        /**
         * Encryption provider.
         */
        public enum Provider {
            FILESYSTEM, PROPERTY;
        }

        private Provider provider = Provider.FILESYSTEM;
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

        public static class Property {

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
     * HelpHub properties.
     */
    public static class HelpHub {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Mail properties.
     */
    public static class Mail {

        private boolean auth;
        private boolean debug;
        private String from;
        private String host;
        private String baseUrl;
        private String password;
        private String protocol;
        private int port = 25;
        private Starttls starttls = new Starttls();
        private Ssl ssl = new Ssl();
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

        public static class Ssl {

            private boolean enabled;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        public static class Starttls {

            private boolean enable;
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
     * MessageBroker properties.
     */
    public static class MessageBroker {

        public enum Provider {
            AMQP, AWS, JMS, KAFKA, LOCAL, REDIS
        }

        private Provider provider = Provider.JMS;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    /**
     * Represents the MCP configuration structure. Provides access to the server configuration settings.
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
         * Represents the server configuration settings. This configuration determines whether the server is enabled or
         * disabled.
         */
        public static class Server {

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
     * Oauth2 properties.
     */
    public static class Oauth2 {

        private Map<String, OAuth2App> predefinedApps = new HashMap<>();
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

        public static class OAuth2App {
            private String clientId;
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
     * Resources properties.
     */
    public static class Resources {

        private String web;

        public String getWeb() {
            return web;
        }

        public void setWeb(String web) {
            this.web = web;
        }
    }

    /**
     * Security properties.
     */
    public static class Security {

        private String contentSecurityPolicy;
        private RememberMe rememberMe = new RememberMe();
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
         * RememberMe properties.
         */
        public static class RememberMe {

            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }

        /**
         * System properties.
         */
        public static class System {

            private String username;
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
     * Sign up properties.
     */
    public static class SignUp {

        private boolean activationRequired;
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
     * Tenant properties.
     */
    public static class Tenant {

        /**
         * Tenant provider.
         */
        public enum Mode {
            MULTI, SINGLE
        }

        private Mode mode = Mode.SINGLE;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }
    }

    public static class Tracing {

        private Otlp otlp = new Otlp();

        public Otlp getOtlp() {
            return otlp;
        }

        public void setOtlp(Otlp otlp) {
            this.otlp = otlp;
        }
    }

    public static class Otlp {

        private String endpoint;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
    }

    /**
     * Worker properties.
     */
    public static class Worker {

        private boolean enabled = true;
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
         * Task properties.
         */
        public static class Task {

            private Map<String, Integer> subscriptions = new HashMap<>();

            public Map<String, Integer> getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Map<String, Integer> subscriptions) {
                this.subscriptions = subscriptions;
            }
        }
    }

    /**
     * Workflow properties.
     */
    public static class Workflow {

        private OutputStorage outputStorage = new OutputStorage();
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
         * OutputStorage properties.
         */
        public static class OutputStorage {

            /**
             * OutputStorage provider.
             */
            public enum Provider {
                AWS, FILESYSTEM, JDBC
            }

            private Provider provider = Provider.JDBC;

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }
        }

        /**
         * Repository properties.
         */
        public static class Repository {

            private Classpath classpath = new Classpath();
            private Filesystem filesystem = new Filesystem();
            private Git git = new Git();
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
             * Filesystem properties.
             */
            public static class Classpath {

                private boolean enabled;
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
             * Filesystem properties.
             */
            public static class Filesystem {

                private boolean enabled;
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
             * Git properties.
             */
            public static class Git {

                private String branch;
                private boolean enabled;
                private String password;
                private String[] searchPaths;
                private String url;
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
             * Jdbc properties.
             */
            public static class Jdbc {

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
