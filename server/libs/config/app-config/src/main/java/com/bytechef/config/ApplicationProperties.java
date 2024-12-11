/*
 * Copyright 2023-present ByteChef Inc.
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
    private Loki loki = new Loki();
    private Mail mail = new Mail();
    private MessageBroker messageBroker = new MessageBroker();
    private Oauth2 oauth2 = new Oauth2();
    private String publicUrl;
    private Resources resources = new Resources();
    private Security security;
    private SignUp signUp = new SignUp();
    private Tenant tenant = new Tenant();
    private String webhookUrl;
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

    public Loki getLoki() {
        return loki;
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

    public void setLoki(Loki loki) {
        this.loki = loki;
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

    public static class Loki {
        private Appender appender = new Appender();

        public Appender getAppender() {
            return appender;
        }

        public void setAppender(Appender appender) {
            this.appender = appender;
        }

        public static class Appender {
            private Level level = Level.OFF;

            public enum Level {
                DEBUG, ERROR, FATAL, INFO, OFF, TRACE, WARN
            }

            public Level getLevel() {
                return level;
            }

            public void setLevel(Level level) {
                this.level = level;
            }
        }
    }

    /**
     * Ai properties.
     */
    public static class Ai {

        private Copilot copilot = new Copilot();

        public Copilot getCopilot() {
            return copilot;
        }

        public void setCopilot(Copilot copilot) {
            this.copilot = copilot;
        }

        public static class Copilot {

            public enum Provider {
                OPENAI, ANTHROPIC
            }

            private boolean enabled;
            private OpenAi openAi = new OpenAi();
            private Provider provider = Provider.OPENAI;

            public boolean isEnabled() {
                return enabled;
            }

            public OpenAi getOpenAi() {
                return openAi;
            }

            public Provider getProvider() {
                return provider;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public void setOpenAi(OpenAi openAi) {
                this.openAi = openAi;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
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
     * Analytics properties.
     */
    public static class Analytics {

        private boolean enabled;

        private PostHog postHog = new PostHog();

        public boolean isEnabled() {
            return enabled;
        }

        public PostHog getPostHog() {
            return postHog;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setPostHog(PostHog postHog) {
            this.postHog = postHog;
        }

        /**
         * PostHog properties.
         */
        public static class PostHog {

            private String apiKey;
            private String host;

            public String getApiKey() {
                return apiKey;
            }

            public String getHost() {
                return host;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public void setHost(String host) {
                this.host = host;
            }
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
            REDIS, SIMPLE
        }

        private Provider provider = Provider.SIMPLE;

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

            private Subscriptions subscriptions = new Subscriptions();

            private Scheduler scheduler = new Scheduler();

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

            private String basedir;

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

        private CommandBar commandBar = new CommandBar();
        private boolean enabled;

        public CommandBar getCommandBar() {
            return commandBar;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setCommandBar(CommandBar commandBar) {
            this.commandBar = commandBar;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public static class CommandBar {

            private String orgId;

            public String getOrgId() {
                return orgId;
            }

            public void setOrgId(String orgId) {
                this.orgId = orgId;
            }
        }
    }

    /**
     * Mail properties.
     */
    public static class Mail {

        private String from;
        private String host;
        private String baseUrl;
        private String password;
        private int port = 25;
        private Smtp smtp = new Smtp();
        private String username;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

        public Smtp getSmtp() {
            return smtp;
        }

        public void setSmtp(Smtp smtp) {
            this.smtp = smtp;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public static class Smtp {

            private boolean auth;
            private Starttls starttls = new Starttls();

            public boolean isAuth() {
                return auth;
            }

            public void setAuth(boolean auth) {
                this.auth = auth;
            }

            public Starttls getStarttls() {
                return starttls;
            }

            public void setStarttls(Starttls starttls) {
                this.starttls = starttls;
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
    }

    /**
     * MessageBroker properties.
     */
    public static class MessageBroker {

        public enum Provider {
            AMQP, AWS, JMS, KAFKA, REDIS
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
    }

    /**
     * Sign up properties.
     */
    public static class SignUp {

        private boolean activationRequired;
        private boolean enabled = true;
        private String tempDomainListUrl;

        public boolean isActivationRequired() {
            return activationRequired;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getTempDomainListUrl() {
            return tempDomainListUrl;
        }

        public void setActivationRequired(boolean activationRequired) {
            this.activationRequired = activationRequired;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setTempDomainListUrl(String tempDomainListUrl) {
            this.tempDomainListUrl = tempDomainListUrl;
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
