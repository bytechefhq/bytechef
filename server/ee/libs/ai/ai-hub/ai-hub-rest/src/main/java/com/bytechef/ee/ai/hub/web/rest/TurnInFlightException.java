/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.web.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class TurnInFlightException extends RuntimeException {

    public TurnInFlightException() {
        super("A turn is already in flight for this chat");
    }
}
