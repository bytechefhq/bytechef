/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.runner.kubernetes;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.CREDENTIALS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.KILL_GRACE_PERIOD;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PASSWORD;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.REGISTRY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.USERNAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The Kubernetes runner's task properties, read and checked once.
 *
 * @author Ivica Cardic
 * @version ee
 */
record KubernetesRunnerSettings(
    String image, @Nullable String namespace, @Nullable String serviceAccountName,
    Map<String, String> resourceRequests, Map<String, String> resourceLimits, Map<String, String> nodeSelector,
    List<Toleration> tolerations, Map<String, String> labels, Map<String, String> annotations,
    @Nullable RegistryCredentials credentials, String fileSidecarImage, Duration killGracePeriod) {

    static final String ANNOTATIONS = "annotations";
    static final String CPU_LIMIT = "cpuLimit";
    static final String CPU_REQUEST = "cpuRequest";
    static final String FILE_SIDECAR_IMAGE = "fileSidecarImage";
    static final String LABELS = "labels";
    static final String MEMORY_LIMIT = "memoryLimit";
    static final String MEMORY_REQUEST = "memoryRequest";
    static final String NAMESPACE = "namespace";
    static final String NODE_SELECTOR = "nodeSelector";
    static final String RESOURCES = "resources";
    static final String SERVICE_ACCOUNT_NAME = "serviceAccountName";
    static final String TOLERATIONS = "tolerations";

    private static final String DEFAULT_FILE_SIDECAR_IMAGE = "busybox:1.36";
    private static final Map<String, String> DEFAULT_IMAGES = Map.of(
        "javascript", "node:22-alpine", "js", "node:22-alpine", "python", "python:3.12-slim", "shell",
        "ubuntu:24.04");
    private static final int MAX_QUANTITY_LENGTH = 64;
    private static final Set<String> TOLERATION_EFFECTS = Set.of("NoSchedule", "PreferNoSchedule", "NoExecute");
    private static final Set<String> TOLERATION_OPERATORS = Set.of("Equal", "Exists");

    record Toleration(String key, String operator, @Nullable String value, @Nullable String effect) {
    }

    record RegistryCredentials(String registry, String username, String password) {

        @Override
        public String toString() {
            return "RegistryCredentials[registry=%s, username=%s, password=***]".formatted(registry, username);
        }
    }

    @SuppressFBWarnings(
        value = "REDOS",
        justification = "The pattern is unambiguous and the matched input is capped at MAX_QUANTITY_LENGTH characters")
    private static final class QuantityPattern {

        static final Pattern VALUE =
            Pattern.compile("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+|[KMGTPE]i?|[mkMGTPE])?$");

        private QuantityPattern() {
        }
    }

    static KubernetesRunnerSettings of(String languageId, Parameters parameters) {
        String image = text(parameters.getString(IMAGE));

        if (image == null) {
            image = DEFAULT_IMAGES.get(languageId);
        }

        if (image == null) {
            throw new IllegalArgumentException(
                "The kubernetes runner has no default image for language '%s'; name one on the task runner"
                    .formatted(languageId));
        }

        Map<String, String> resources = parameters.getMap(RESOURCES, String.class, Map.of());

        Map<String, String> resourceRequests = new LinkedHashMap<>();
        Map<String, String> resourceLimits = new LinkedHashMap<>();

        putQuantity(resourceRequests, "cpu", resources, CPU_REQUEST);
        putQuantity(resourceRequests, "memory", resources, MEMORY_REQUEST);
        putQuantity(resourceLimits, "cpu", resources, CPU_LIMIT);
        putQuantity(resourceLimits, "memory", resources, MEMORY_LIMIT);

        Map<String, String> labels = parameters.getMap(LABELS, String.class, Map.of());
        Map<String, String> annotations = parameters.getMap(ANNOTATIONS, String.class, Map.of());

        checkNotReserved(labels, LABELS);
        checkNotReserved(annotations, ANNOTATIONS);

        String fileSidecarImage = text(parameters.getString(FILE_SIDECAR_IMAGE));

        return new KubernetesRunnerSettings(
            image, text(parameters.getString(NAMESPACE)), text(parameters.getString(SERVICE_ACCOUNT_NAME)),
            Map.copyOf(resourceRequests), Map.copyOf(resourceLimits),
            Map.copyOf(parameters.getMap(NODE_SELECTOR, String.class, Map.of())), tolerations(parameters),
            Map.copyOf(labels), Map.copyOf(annotations), credentials(parameters),
            fileSidecarImage == null ? DEFAULT_FILE_SIDECAR_IMAGE : fileSidecarImage, killGracePeriod(parameters));
    }

    static List<ModifiableValueProperty<?, ?>> properties() {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            string(IMAGE)
                .label("Image")
                .description("The container image the task runs in. Defaults to the image for the action's language.")
                .required(false));
        properties.add(
            array(ENTRYPOINT)
                .label("Entrypoint")
                .description(
                    "Replaces the interpreter the source is handed to. Defaults to the interpreter for the action's " +
                        "language; the source file is appended either way. The image must provide /bin/sh.")
                .items(string())
                .required(false));
        properties.add(
            string(NAMESPACE)
                .label("Namespace")
                .description("The namespace the pod runs in. Defaults to the connection's namespace.")
                .required(false));
        properties.add(
            string(SERVICE_ACCOUNT_NAME)
                .label("Service Account")
                .description(
                    "The service account the pod runs as. Without one, no service account token is mounted.")
                .required(false));
        properties.add(
            object(RESOURCES)
                .label("Resources")
                .description("CPU and memory requests and limits for the task's container, as Kubernetes quantities.")
                .properties(
                    string(CPU_REQUEST).label("CPU Request")
                        .required(false),
                    string(CPU_LIMIT).label("CPU Limit")
                        .required(false),
                    string(MEMORY_REQUEST).label("Memory Request")
                        .required(false),
                    string(MEMORY_LIMIT).label("Memory Limit")
                        .required(false))
                .required(false));
        properties.add(
            object(NODE_SELECTOR)
                .label("Node Selector")
                .description("Node labels the pod must be scheduled onto.")
                .additionalProperties(string())
                .required(false));
        properties.add(
            array(TOLERATIONS)
                .label("Tolerations")
                .description("Taints the pod tolerates.")
                .items(
                    object().properties(
                        string("key").label("Key")
                            .required(false),
                        string("operator").label("Operator")
                            .options(option("Equal", "Equal"), option("Exists", "Exists"))
                            .required(true),
                        string("value").label("Value")
                            .required(false),
                        string("effect").label("Effect")
                            .options(
                                option("No Schedule", "NoSchedule"), option("Prefer No Schedule", "PreferNoSchedule"),
                                option("No Execute", "NoExecute"))
                            .required(false)))
                .required(false));
        properties.add(
            object(LABELS)
                .label("Labels")
                .description("Extra labels on the pod. Keys under bytechef.io/ are reserved.")
                .additionalProperties(string())
                .required(false));
        properties.add(
            object(ANNOTATIONS)
                .label("Annotations")
                .description("Extra annotations on the pod. Keys under bytechef.io/ are reserved.")
                .additionalProperties(string())
                .required(false));
        properties.add(
            object(CREDENTIALS)
                .label("Registry Credentials")
                .description("How the image is pulled from a private registry; a temporary pull secret is created.")
                .properties(
                    string(REGISTRY).label("Registry")
                        .required(true),
                    string(USERNAME).label("Username")
                        .required(true),
                    string(PASSWORD).label("Password")
                        .controlType(ControlType.PASSWORD)
                        .required(true))
                .required(false));
        properties.add(
            string(FILE_SIDECAR_IMAGE)
                .label("File Sidecar Image")
                .description(
                    "The image that moves files in and out of the pod; it needs sh, head, tar, test, sleep and touch.")
                .defaultValue(DEFAULT_FILE_SIDECAR_IMAGE)
                .required(false));
        properties.add(
            string(KILL_GRACE_PERIOD)
                .label("Kill Grace Period")
                .description(
                    "How long a still-running pod is given to stop on its own when it is killed - on a timeout or a " +
                        "non-resumable shutdown - before it is forced, as an ISO-8601 duration.")
                .defaultValue("PT0S")
                .required(false));

        return properties;
    }

    String namespaceOr(String connectionNamespace) {
        return namespace == null ? connectionNamespace : namespace;
    }

    private static void checkNotReserved(Map<String, String> entries, String propertyName) {
        for (String key : entries.keySet()) {
            if (key.startsWith(KubernetesJobLabels.RESERVED_PREFIX)) {
                throw new IllegalArgumentException(
                    "The kubernetes runner's %s cannot use the reserved key '%s' (keys under %s belong to ByteChef)"
                        .formatted(propertyName, key, KubernetesJobLabels.RESERVED_PREFIX));
            }
        }
    }

    @Nullable
    private static RegistryCredentials credentials(Parameters parameters) {
        Map<String, String> credentials = parameters.getMap(CREDENTIALS, String.class, Map.of());

        if (credentials.isEmpty()) {
            return null;
        }

        String registry = text(credentials.get(REGISTRY));
        String username = text(credentials.get(USERNAME));
        String password = credentials.get(PASSWORD);

        if (registry == null || username == null || password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                "The kubernetes runner's registry credentials need a registry, a username and a password");
        }

        return new RegistryCredentials(registry, username, password);
    }

    private static Duration killGracePeriod(Parameters parameters) {
        String value = text(parameters.getString(KILL_GRACE_PERIOD));

        if (value == null) {
            return Duration.ZERO;
        }

        Duration duration;

        try {
            duration = Duration.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The kubernetes runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    value, KILL_GRACE_PERIOD),
                exception);
        }

        if (duration.isNegative()) {
            throw new IllegalArgumentException(
                "The kubernetes runner's %s must not be negative".formatted(KILL_GRACE_PERIOD));
        }

        return duration;
    }

    private static void putQuantity(
        Map<String, String> target, String resourceName, Map<String, String> resources, String propertyName) {

        String value = text(resources.get(propertyName));

        if (value == null) {
            return;
        }

        if (value.length() > MAX_QUANTITY_LENGTH || !QuantityPattern.VALUE.matcher(value)
            .matches()) {

            String echoed = value.length() > MAX_QUANTITY_LENGTH ? value.substring(0, MAX_QUANTITY_LENGTH) : value;

            throw new IllegalArgumentException(
                "The kubernetes runner cannot read '%s' as the quantity %s expects".formatted(echoed, propertyName));
        }

        target.put(resourceName, value);
    }

    @Nullable
    private static String text(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<Toleration> tolerations(Parameters parameters) {
        List<Toleration> tolerations = new ArrayList<>();

        for (Map<?, ?> toleration : parameters.getList(TOLERATIONS, Map.class, List.of())) {
            String operator = text(stringValue(toleration.get("operator")));
            String effect = text(stringValue(toleration.get("effect")));

            if (operator == null || !TOLERATION_OPERATORS.contains(operator)) {
                throw new IllegalArgumentException(
                    "The kubernetes runner does not know the toleration operator '%s'; use Equal or Exists"
                        .formatted(operator));
            }

            if (effect != null && !TOLERATION_EFFECTS.contains(effect)) {
                throw new IllegalArgumentException(
                    "The kubernetes runner does not know the toleration effect '%s'".formatted(effect));
            }

            tolerations.add(
                new Toleration(
                    String.valueOf(toleration.get("key") == null ? "" : toleration.get("key")), operator,
                    text(stringValue(toleration.get("value"))), effect));
        }

        return List.copyOf(tolerations);
    }

    @Nullable
    private static String stringValue(@Nullable Object value) {
        return value == null ? null : value.toString();
    }
}
