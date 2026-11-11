/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum Source {

    WORKFLOW_EDITOR, CODE_EDITOR, CLUSTER_ELEMENT, FILES, AI_HUB;

    public Short toAgentSourceOrdinal() {
        return (short) ordinal();
    }

    public static Source fromAgentSourceOrdinal(Short ordinal) {
        if (ordinal == null) {
            return null;
        }

        return EnumOrdinals.fromOrdinal(ordinal, Source.class);
    }
}
