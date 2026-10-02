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
