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
 * graphql-java {@link graphql.execution.instrumentation.Instrumentation} that blocks execution of EE-only GraphQL
 * fields when no active Enterprise licence is present.
 *
 * <p>
 * The check runs in {@code beginExecuteOperation}, which fires after parsing and validation but before field execution.
 * Any top-level selection-set field that is registered in {@link EeGraphQlFieldRegistry} is rejected with a
 * {@link GraphqlErrorException} carrying {@link ErrorType#ExecutionAborted} when the licence is inactive. The error
 * carries a stable {@code LICENCE_REQUIRED} key in its {@code extensions} map so clients can discriminate it from
 * generic execution failures.
 *
 * <p>
 * Error surfacing: {@code GraphqlErrorException} implements {@link graphql.GraphQLError} and is handled natively by
 * graphql-java — it surfaces as a top-level GraphQL error with a {@code FORBIDDEN}-equivalent classification without
 * needing a Spring GraphQL {@code DataFetcherExceptionResolverAdapter}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class LicenceEnforcementInstrumentation extends SimplePerformantInstrumentation {

    /**
     * GraphQL field names that manage the licence itself. These fields must remain accessible even when the licence is
     * inactive so that an administrator can upload a new licence or check the current status. They are still protected
     * by {@code @PreAuthorize("hasAuthority('ROLE_ADMIN')")} on the controller, so exempting them here does not open a
     * security hole.
     */
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
