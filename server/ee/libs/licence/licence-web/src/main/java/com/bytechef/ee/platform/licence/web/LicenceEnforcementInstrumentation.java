/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web;

import com.bytechef.platform.licence.LicenceManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.GraphqlErrorException;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import java.util.Map;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class LicenceEnforcementInstrumentation extends SimplePerformantInstrumentation {

    static final Set<String> LICENCE_MANAGEMENT_FIELDS = Set.of("licence", "uploadLicence", "deleteLicence");

    private final EeGraphQlFieldRegistry eeGraphQlFieldRegistry;
    private final LicenceManager licenceManager;

    @SuppressFBWarnings("EI2")
    public LicenceEnforcementInstrumentation(
        EeGraphQlFieldRegistry eeGraphQlFieldRegistry, LicenceManager licenceManager) {

        this.eeGraphQlFieldRegistry = eeGraphQlFieldRegistry;
        this.licenceManager = licenceManager;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(
        InstrumentationExecuteOperationParameters parameters, InstrumentationState state) {

        OperationDefinition operationDefinition = parameters.getExecutionContext()
            .getOperationDefinition();

        if (operationDefinition.getSelectionSet() == null) {
            return SimpleInstrumentationContext.noOp();
        }

        for (Selection<?> selection : operationDefinition.getSelectionSet()
            .getSelections()) {

            if (!(selection instanceof Field field)) {
                continue;
            }

            if (!LICENCE_MANAGEMENT_FIELDS.contains(field.getName())
                && eeGraphQlFieldRegistry.isEeField(field.getName())
                && !licenceManager.getStatus()
                    .isActive()) {

                throw GraphqlErrorException.newErrorException()
                    .message("A valid Enterprise licence is required to access this field")
                    .errorClassification(ErrorType.ExecutionAborted)
                    .extensions(Map.of(
                        "errorKey", "LICENCE_REQUIRED",
                        "licenceStatus", licenceManager.getStatus()
                            .name()))
                    .build();
            }
        }

        return SimpleInstrumentationContext.noOp();
    }
}
