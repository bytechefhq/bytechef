/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.listener;

/**
 * Callback invoked at the very start of {@code ProjectDeploymentFacade#deleteProjectDeployment(long)}, before anything
 * else touches the deployment's rows.
 * <p>
 * A feature that owns rows referencing a project deployment, or referencing that deployment's project deployment
 * workflows, implements this listener to remove those rows itself. None of the foreign keys pointing at
 * {@code project_deployment} or {@code project_deployment_workflow} cascade, so without an implementation the delete
 * fails on a foreign key constraint.
 * <p>
 * An implementation must satisfy the following contract.
 * <ul>
 * <li>Delete grandchildren before parents. Rows referencing {@code project_deployment_workflow} - such as
 * {@code mcp_project_workflow}, {@code a2a_project_workflow} and {@code api_collection_endpoint} - must go before the
 * rows referencing {@code project_deployment} that own them. This ordering is also why the hook exists here rather than
 * as a Spring Data {@code BeforeDeleteEvent<ProjectDeployment>} listener: by the time such an event fires, the
 * deployment's {@code project_deployment_workflow} rows have already been deleted, and the constraint has already been
 * violated.</li>
 * <li>Delete every row for the given deployment, both grandchildren and parents. A listener that removes only its
 * parent rows leaves grandchildren dangling and the delete still fails.</li>
 * <li>Expect to run inside the caller's transaction, on the caller's thread and under the caller's security context.
 * Authorization for the operation as a whole is enforced upstream, by the caller of
 * {@code deleteProjectDeployment}.</li>
 * <li>Throw rather than swallow. An exception aborts the enclosing transaction and therefore the whole project
 * deployment - and, when the delete cascades from a project, the whole project delete. That fail-fast behaviour is
 * intended: a half-deleted deployment is worse than a failed one.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public interface ProjectDeploymentDeleteEventListener {

    void onBeforeDeleteProjectDeployment(long projectDeploymentId);
}
