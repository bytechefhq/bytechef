/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.service;

import com.bytechef.ee.platform.variable.domain.Variable;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import java.util.List;
import java.util.Map;

/**
 * CRUD over the variables of one scope + environment. Not authorization-aware: the GraphQL controllers guard the admin
 * surface, and the runtime resolver calls it with no security context.
 *
 * @version ee
 */
public interface VariableService {

    Variable create(VariableScope scope, long environmentId, String name, String value);

    void delete(VariableScope scope, long environmentId, long id);

    /**
     * Name to value, for seeding the {@code vars} job input. Never null.
     */
    Map<String, String> getVariableMap(VariableScope scope, long environmentId);

    List<Variable> getVariables(VariableScope scope, long environmentId);

    Variable update(VariableScope scope, long environmentId, long id, String name, String value);
}
