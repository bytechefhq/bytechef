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

    public enum Edition {
        CE, EE
    }

    private Cache cache;
    private Cloud cloud = new Cloud();
    private Component component = new Component();
    private Coordinator coordinator = new Coordinator();
    private Datasource datasource;
    private DataStorage dataStorage;
    private DiscoveryService discoveryService;
    private Edition edition;
    private Email email;
    private Encryption encryption;
    private FileStorage fileStorage;
    private Mail mail;
    private MessageBroker messageBroker;
    private Oauth2 oauth2;
    private String publicUrl;
    private Resources resources;
    private Security security;
    private Tenant tenant;
    private String webhookUrl;
    private Worker worker = new Worker();
    private Workflow workflow;

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

    public Email getEmail() {
        return email;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
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

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public void setFileStorage(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
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

    public static class Cache {

        public enum Provider {
            REDIS, SIMPLE
        }

        private Provider provider;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    public static class Cloud {

        public enum Provider {
            AWS
        }

        private Aws aws = new Aws();
        private Provider provider;

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

        public static class Aws {

            private String accessKeyId;
            private String region;
            private String secretAccessKey;

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

    public static class Component {

        private Registry registry = new Registry();

        public Registry getRegistry() {
            return registry;
        }

        public void setRegistry(Registry registry) {
            this.registry = registry;
        }

        public static class Registry {

            private List<String> exclude = List.of();

            public List<String> getExclude() {
                return exclude;
            }

            public void setExclude(List<String> exclude) {
                this.exclude = exclude;
            }
        }
    }

    public static class Coordinator {

        private Task task = new Task();
        private Trigger trigger = new Trigger();

        public Task getTask() {
            return task;
        }

        public Trigger getTrigger() {
            return trigger;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        public void setTrigger(Trigger trigger) {
            this.trigger = trigger;
        }

        public static class Task {

            private Subscriptions subscriptions = new Subscriptions();

            public Subscriptions getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Subscriptions subscriptions) {
                this.subscriptions = subscriptions;
            }

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

        public class Trigger {

            private Subscriptions subscriptions = new Subscriptions();

            public Subscriptions getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Subscriptions subscriptions) {
                this.subscriptions = subscriptions;
            }

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

    public static class DataStorage {

        public enum Provider {
            JDBC
        }

        private Provider provider;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    public static class DiscoveryService {

        private Provider provider;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public enum Provider {
            REDIS
        }
    }

    public static class FileStorage {

        public enum Provider {
            AWS, BASE64, FILESYSTEM, NOOP
        }

        private Filesystem filesystem;
        private Provider provider;

        public Filesystem getFilesystem() {
            return filesystem;
        }

        public Provider getProvider() {
            return provider;
        }

        public void setFilesystem(Filesystem filesystem) {
            this.filesystem = filesystem;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

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

    public static class Email {

        private String tempDomainListUrl;

        public String getTempDomainListUrl() {
            return tempDomainListUrl;
        }

        public void setTempDomainListUrl(String tempDomainListUrl) {
            this.tempDomainListUrl = tempDomainListUrl;
        }
    }

    public static class Encryption {

        public enum Provider {
            AWS, FILESYSTEM;
        }

        private Provider provider;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }
    }

    public static class Mail {

        private boolean enabled;
        private String from;
        private String baseUrl;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class MessageBroker {

        private Provider provider;

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public enum Provider {
            AMQP, AWS, JMS, KAFKA, REDIS
        }
    }

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

    public static class Resources {

        private String web;

        public String getWeb() {
            return web;
        }

        public void setWeb(String web) {
            this.web = web;
        }
    }

    public static class Security {

        private String contentSecurityPolicy;
        private RememberMe rememberMe;

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

    public static class Tenant {

        private Mode mode;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public enum Mode {
            MULTI, SINGLE
        }
    }

    public static class Worker {

        private Task task = new Task();

        public Task getTask() {
            return task;
        }

        public void setTask(Task task) {
            this.task = task;
        }

        public static class Task {

            private Map<String, Object> subscriptions = new HashMap<>();

            public Map<String, Object> getSubscriptions() {
                return subscriptions;
            }

            public void setSubscriptions(Map<String, Object> subscriptions) {
                this.subscriptions = subscriptions;
            }
        }
    }

    public static class Workflow {

        private OutputStorage outputStorage;
        private Repository repository;

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

        public static class OutputStorage {

            public enum Provider {
                AWS, BASE64, FILESYSTEM, NOOP
            }

            private Provider provider;

            public Provider getProvider() {
                return provider;
            }

            public void setProvider(Provider provider) {
                this.provider = provider;
            }
        }

        public static class Repository {

            private Classpath classpath;
            private Filesystem filesystem;
            private Git git;
            private Jdbc jdbc;

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

            public static class Jdbc {

                private boolean enabled;

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
