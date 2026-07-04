/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

/**
 * Guest-side view of the host callback object the loader places into the polyglot bindings. Method-for-method it must
 * match the {@code @HostAccess.Export} methods of the host's {@code HostContextBridge} — that exported set is the
 * entire sandbox boundary between guest component code and the host JVM.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface HostBridge {

    String httpExecute(String requestJson);

    boolean isEditorEnvironment();

    void log(String level, String message, String exceptionMessage);
}
