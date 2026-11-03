/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * The outcome of moving connected users from one configuration to another.
 *
 * @param movedConnectedUserIds          the connected users now assigned to the target
 * @param skipped                        the connected users left where they were, with the reason
 * @param needsAttentionConnectedUserIds the moved connected users whose turned-on workflows need attention on the
 *                                       target
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConfigurationMoveResultDTO(
    List<Long> movedConnectedUserIds, List<Skipped> skipped, List<Long> needsAttentionConnectedUserIds) {

    /**
     * A connected user the move left in place.
     *
     * @param connectedUserId the connected user id
     * @param reason          {@link #NOT_ON_SOURCE}, {@link #TARGET_LACKS_REFERENCED_WORKFLOWS},
     *                        {@link #TARGET_LACKS_ENABLED_WORKFLOWS}, {@link #CONNECTION_MISMATCH} or
     *                        {@link #MOVE_FAILED}
     * @param workflowLabels  the labels of the referenced workflows the target lacks; empty for the other reasons
     *
     * @version ee
     *
     * @author Ivica Cardic
     */
    @SuppressFBWarnings("EI")
    public record Skipped(long connectedUserId, String reason, List<String> workflowLabels) {

        public static final String CONNECTION_MISMATCH = "CONNECTION_MISMATCH";
        public static final String MOVE_FAILED = "MOVE_FAILED";
        public static final String NOT_ON_SOURCE = "NOT_ON_SOURCE";
        public static final String TARGET_LACKS_ENABLED_WORKFLOWS = "TARGET_LACKS_ENABLED_WORKFLOWS";
        public static final String TARGET_LACKS_REFERENCED_WORKFLOWS = "TARGET_LACKS_REFERENCED_WORKFLOWS";
    }
}
