/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * An app together with its plaintext secret key, returned once at create and rotate and never again.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record AppSecretKeyDTO(AppDTO app, String secretKey) {

    @Override
    public String toString() {
        return "AppSecretKeyDTO[app=" + app + ", secretKey=[REDACTED]]";
    }
}
