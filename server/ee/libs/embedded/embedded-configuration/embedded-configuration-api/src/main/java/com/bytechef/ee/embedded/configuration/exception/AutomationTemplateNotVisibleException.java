/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AutomationTemplateNotVisibleException extends RuntimeException {

    public AutomationTemplateNotVisibleException(String automationUuid) {
        super("Not a published automation workflow template: " + automationUuid);
    }
}
