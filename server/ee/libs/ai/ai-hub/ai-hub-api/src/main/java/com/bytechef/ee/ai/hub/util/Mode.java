/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

/**
 * Identifies which agent mode the AI Hub should run in for a given turn — {@link #ASK} for read-only answer-building or
 * {@link #BUILD} for state-mutating workflow construction. Agent selection in {@code AiHubConfiguration} branches on
 * this value, and the bare {@code Mode} name (string form) crosses the REST boundary in the {@code state.mode} field of
 * the SSE chat request.
 *
 * <p>
 * <strong>Append-only contract.</strong> The enum-name string form is part of the wire contract with the client.
 * Reordering or renaming values silently breaks both the client/server contract (a stale client posts {@code "ASK"} and
 * the server may now resolve to a different mode) and any on-disk telemetry tagged by mode. When adding a value, append
 * it at the end; to remove or rename, coordinate a versioned migration with the client.
 * </p>
 *
 * <p>
 * Dispatching code MUST use exhaustive {@code switch} expressions over {@code Mode} so adding a new mode is a
 * compile-time error at every dispatch site. Branching on {@code mode.ordinal()} is forbidden — it silently maps a new
 * mode to whichever existing branch happens to share an ordinal.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum Mode {

    // append-only
    ASK, BUILD
}
