/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.artifact;

import com.bytechef.automation.assetfile.domain.AssetFileFormat;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Maps {@link AssetFileFormat} → {@link ArtifactGenerator} and dispatches a {@link GenerationRequest} to the right
 * generator. Built from the list of all {@link ArtifactGenerator} beans discovered by Spring; duplicate registrations
 * for the same format throw at wire time so the misconfiguration surfaces on application startup rather than silently
 * shadowing one generator with another at runtime.
 *
 * <p>
 * Mirrors the {@code MutationApplierRegistry} pattern: one bean per format, the registry composes them into an
 * {@link EnumMap} keyed by format, and dispatch is a single map lookup. The {@code apply}-style flow lives on the tool
 * callback, not here — this class is intentionally just a routing table so it can be unit-tested without pulling in
 * Spring or the AssetFileFacade.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ArtifactGeneratorRegistry {

    private final Map<AssetFileFormat, ArtifactGenerator> generatorsByFormat;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ArtifactGeneratorRegistry(List<ArtifactGenerator> generators) {
        EnumMap<AssetFileFormat, ArtifactGenerator> mutable = new EnumMap<>(AssetFileFormat.class);

        for (ArtifactGenerator generator : generators) {
            AssetFileFormat format = generator.format();
            ArtifactGenerator existing = mutable.put(format, generator);

            if (existing != null) {
                throw new IllegalStateException(
                    "Duplicate ArtifactGenerator registration for format '" + format +
                        "': " + existing.getClass()
                            .getName()
                        + " and " + generator.getClass()
                            .getName());
            }
        }

        generatorsByFormat = Collections.unmodifiableMap(mutable);
    }

    /**
     * Dispatches the request to the registered generator for the given format and returns its result.
     *
     * @throws IllegalArgumentException when no generator is registered for {@code format}. The tool callback layer
     *                                  translates this into a structured tool error so the LLM can recover rather than
     *                                  the chat surface seeing an unhandled exception.
     */
    public GenerationResult generate(AssetFileFormat format, GenerationRequest request) {
        ArtifactGenerator generator = generatorsByFormat.get(format);

        if (generator == null) {
            throw new IllegalArgumentException(
                "No ArtifactGenerator registered for format '" + format + "'");
        }

        return generator.generate(request);
    }

    /**
     * Returns the formats for which a generator is registered. Intended for the tool-input JSON schema's enum so the
     * LLM is steered toward formats the running deployment can actually produce — adding the registry to a future
     * health endpoint also gives ops a way to confirm wiring.
     */
    public java.util.Set<AssetFileFormat> supportedFormats() {
        return generatorsByFormat.keySet();
    }
}
