/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.automation.execution.dto;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record ToolDTO(String name, String description, String parameters, Long connectionId) {
}
