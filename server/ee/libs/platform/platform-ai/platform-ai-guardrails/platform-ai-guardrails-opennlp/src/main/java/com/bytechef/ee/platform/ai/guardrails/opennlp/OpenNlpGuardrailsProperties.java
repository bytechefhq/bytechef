/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.opennlp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code bytechef.ai.guardrails.opennlp.*}.
 *
 * <p>
 * Model locations are Spring resource strings, so {@code file:} and {@code classpath:} both work without this module
 * implementing path handling of its own. Entity-model keys are {@code SensitiveSpan} categories, not a separate
 * vocabulary — a key of {@code PERSON} produces {@code [REDACTED_PERSON]} with no mapping table anywhere.
 * </p>
 *
 * <p>
 * The same keys are mirrored on {@code ApplicationProperties.Ai.Guardrails.OpenNlp}. That is not redundancy: the
 * central binder declares the whole {@code bytechef} tree with {@code ignoreUnknownFields = false}, so an operator-set
 * key with no field there fails every app's context, including apps that do not carry this module.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai.guardrails.opennlp")
@SuppressFBWarnings("EI")
public class OpenNlpGuardrailsProperties {

    private boolean enabled;

    private Map<String, String> entityModels = new LinkedHashMap<>();

    private double minConfidence = 0.85;

    private String tokenizerModel;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Returns the live map, not a defensive copy: Spring's relaxed binder needs the actual field reference to
    // populate map-valued @ConfigurationProperties, and a copy here would silently discard bound entries.
    public Map<String, String> getEntityModels() {
        return entityModels;
    }

    public void setEntityModels(Map<String, String> entityModels) {
        this.entityModels = entityModels;
    }

    public double getMinConfidence() {
        return minConfidence;
    }

    public void setMinConfidence(double minConfidence) {
        this.minConfidence = minConfidence;
    }

    public String getTokenizerModel() {
        return tokenizerModel;
    }

    public void setTokenizerModel(String tokenizerModel) {
        this.tokenizerModel = tokenizerModel;
    }
}
