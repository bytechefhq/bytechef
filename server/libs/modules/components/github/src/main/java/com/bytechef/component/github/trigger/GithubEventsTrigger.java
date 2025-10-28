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

package com.bytechef.component.github.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.EVENTS;
import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GithubEventsTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("eventsTrigger")
        .title("Events Trigger")
        .description("Triggers on specified events.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(REPOSITORY)
                .label("Repository")
                .options((OptionsFunction<String>) GithubUtils::getRepositoryOptions)
                .required(true),
            array(EVENTS)
                .label("Events")
                .description("Determines what events the hook is triggered for.")
                .items(
                    string()
                        .options(
                            option("Branch Protection Configuration", "branch_protection_configuration",
                                "This event occurs when there is a change to branch protection configurations for a repository."),
                            option("Branch Protection Rule", "branch_protection_rule",
                                "This event occurs when there is activity relating to branch protection rules."),
                            option("Check Run", "check_run",
                                "This event occurs when there is activity relating to a check run."),
                            option("Check Suite", "check_suite",
                                "This event occurs when there is activity relating to a check suite."),
                            option("Code Scanning Alert", "code_scanning_alert",
                                "This event occurs when there is activity relating to code scanning alerts in a repository."),
                            option("Commit Comment", "commit_comment",
                                "This event occurs when there is activity relating to commit comments."),
                            option("Create", "create", "This event occurs when a Git branch or tag is created."),
                            option("Custom Property", "custom_property",
                                "This event occurs when there is activity relating to a custom property."),
                            option("Custom Property Values", "custom_property_values",
                                "This event occurs when there is activity relating to custom property values for a repository."),
                            option("Delete", "delete", "This event occurs when a Git branch or tag is deleted."),
                            option("Dependabot Alert", "dependabot_alert",
                                "This event occurs when there is activity relating to Dependabot alerts."),
                            option("Deploy Key", "deploy_key",
                                "This event occurs when there is activity relating to deploy keys."),
                            option("Deployment", "deployment",
                                "This event occurs when there is activity relating to deployments."),
                            option("Deployment Protection Rule", "deployment_protection_rule",
                                "This event occurs when there is activity relating to deployment protection rules."),
                            option("Deployment Review", "deployment_review",
                                "This event occurs when there is activity relating to deployment reviews. "),
                            option("Deployment Status", "deployment_status",
                                "This event occurs when there is activity relating to deployment statuses."),
                            option("Discussion", "discussion",
                                "This event occurs when there is activity relating to a discussion."),
                            option("Discussion Comment", "discussion_comment",
                                "This event occurs when there is activity relating to a comment on a discussion."),
                            option("Fork", "fork", "This event occurs when someone forks a repository. "),
                            option("Github App Authorization", "github_app_authorization",
                                "This event occurs when a user revokes their authorization of a GitHub App."),
                            option("Gollum", "gollum",
                                "This event occurs when someone creates or updates a wiki page. "),
                            option("Installation", "installation",
                                "This event occurs when there is activity relating to a GitHub App installation."),
                            option("Installation Repositories", "installation_repositories",
                                "This event occurs when there is activity relating to which repositories a GitHub App installation can access."),
                            option("Installation Target", "installation_target",
                                "This event occurs when there is activity relating to the user or organization account that a GitHub App is installed on."),
                            option("Issue Comment", "issue_comment",
                                "This event occurs when there is activity relating to a comment on an issue or pull request. "),
                            option("Issue Dependencies", "issue_dependencies",
                                "This event occurs when there is activity relating to issue dependencies, such as blocking or blocked-by relationships."),
                            option("Issues", "issue", "This event occurs when there is activity relating to an issue."),
                            option("Label", "label", "This event occurs when there is activity relating to labels."),
                            option("Marketplace Purchase", "marketplace_purchase",
                                "This event occurs when there is activity relating to a GitHub Marketplace purchase."),
                            option("Member", "member",
                                "This event occurs when there is activity relating to collaborators in a repository."),
                            option("Membership", "membership",
                                "This event occurs when there is activity relating to team membership."),
                            option("Merge Group", "merge_group",
                                "This event occurs when there is activity relating to a merge group in a merge queue."),
                            option("Meta", "meta",
                                "This event occurs when there is activity relating to a webhook itself."),
                            option("Milestone", "milestone",
                                "This event occurs when there is activity relating to milestones. "),
                            option("Org Block", "org_block",
                                "This event occurs when organization owners or moderators block or unblock a non-member from collaborating on the organization's repositories."),
                            option("Organization", "organization",
                                "This event occurs when there is activity relating to an organization and its members. "),
                            option("Package", "package",
                                "This event occurs when there is activity relating to GitHub Packages."),
                            option("Page Build", "page_build",
                                "This event occurs when there is an attempted build of a GitHub Pages site."),
                            option("Personal Access Token Request", "personal_access_token_request",
                                "This event occurs when there is activity relating to a request for a fine-grained personal access token to access resources that belong to a resource owner that requires approval for token access. "),
                            option("Ping", "ping", "This event occurs when you create a new webhook."),
                            option("Project Card", "project_card",
                                "This event occurs when there is activity relating to a card on a project (classic)."),
                            option("Project", "project",
                                "This event occurs when there is activity relating to a project (classic)."),
                            option("Project Column", "project_column",
                                "This event occurs when there is activity relating to a column on a project (classic)."),
                            option("Projects V2", "projects_v2",
                                "This event occurs when there is activity relating to an organization-level project."),
                            option("Projects V2 Item", "projects_v2_item",
                                "This event occurs when there is activity relating to an item on an organization-level project. "),
                            option("Projects V2 Status Update", "projects_v2_status_update",
                                "This event occurs when there is activity relating to a status update on an organization-level project."),
                            option("Public", "public",
                                "This event occurs when repository visibility changes from private to public. "),
                            option("Pull request", "pull_request",
                                "This event occurs when there is activity on a pull request."),
                            option("Pull Request Review Comment", "pull_request_review_comment",
                                "This event occurs when there is activity relating to a pull request review comment."),
                            option("Pull Request Review", "pull_request_review",
                                "This event occurs when there is activity relating to a pull request review."),
                            option("Pull Request Review Thread", "pull_request_review_thread",
                                "This event occurs when there is activity relating to a comment thread on a pull request."),
                            option("Push", "push", "This event occurs when there is a push to a repository branch."),
                            option("Registry Package", "registry_package",
                                "This event occurs when there is activity relating to GitHub Packages."),
                            option("Release", "release",
                                "This event occurs when there is activity relating to releases."),
                            option("Repository Advisory", "repository_advisory",
                                "This event occurs when there is activity relating to a repository security advisory."),
                            option("Repository", "repository",
                                "This event occurs when there is activity relating to repositories."),
                            option("Repository Dispatch", "repository_dispatch",
                                "This event occurs when a GitHub App sends a POST request to /repos/{owner}/{repo}/dispatches."),
                            option("Repository Import", "repository_import",
                                "This event occurs when a repository is imported to GitHub."),
                            option("Repository Ruleset", "repository_ruleset",
                                "This event occurs when there is activity relating to repository rulesets. "),
                            option("Repository Vulnerability Alert", "repository_vulnerability_alert",
                                "This event occurs when there is activity relating to a security vulnerability alert in a repository."),
                            option("Secret Scanning Alert", "secret_scanning_alert",
                                "This event occurs when there is activity relating to a secret scanning alert."),
                            option("Secret Scanning Alert Location", "secret_scanning_alert_location",
                                "This event occurs when there is activity relating to the locations of a secret in a secret scanning alert."),
                            option("Secret Scanning Scan", "secret_scanning_scan",
                                "This event occurs when secret scanning completes certain scans on a repository."),
                            option("Security Advisory", "security_advisory",
                                "This event occurs when there is activity relating to a global security advisory that was reviewed by GitHub."),
                            option("Security And Analysis", "security_and_analysis",
                                "This event occurs when code security and analysis features are enabled or disabled for a repository."),
                            option("Sponsorship", "sponsorship",
                                "This event occurs when there is activity relating to a sponsorship listing."),
                            option("Star", "star",
                                "This event occurs when there is activity relating to repository stars."),
                            option("Status", "status", "This event occurs when the status of a Git commit changes. "),
                            option("Sub Issues", "sub_issues",
                                "This event occurs when there is activity relating to sub-issues."),
                            option("Team Add", "team_add", "This event occurs when a team is added to a repository. "),
                            option("Team", "team",
                                "This event occurs when there is activity relating to teams in an organization."),
                            option("Watch", "watch",
                                "This event occurs when there is activity relating to watching, or subscribing to, a repository."),
                            option("Workflow Dispatch", "workflow_dispatch",
                                "This event occurs when a GitHub Actions workflow is manually triggered."),
                            option("Workflow Job", "workflow_job",
                                "This event occurs when there is activity relating to a job in a GitHub Actions workflow."),
                            option("Workflow Run", "workflow_run",
                                "This event occurs when there is activity relating to a run of a GitHub Actions workflow.")))
                .required(true))
        .output()
        .webhookEnable(GithubEventsTrigger::webhookEnable)
        .webhookDisable(GithubEventsTrigger::webhookDisable)
        .webhookRequest(GithubEventsTrigger::webhookRequest);

    private GithubEventsTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Map<String, Object> body = context
            .http(http -> http.post(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/hooks"))
            .body(
                Http.Body.of(
                    EVENTS, inputParameters.getRequiredList(EVENTS),
                    "config", Map.of("url", webhookUrl, "content_type", "json")))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new WebhookEnableOutput(Map.of(ID, (Integer) body.get(ID)), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        context.http(
            http -> http.delete(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/hooks/" +
                    outputParameters.getInteger(ID)))
            .execute();
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, WebhookEnableOutput output, TriggerContext context) {

        return body.getContent(new TypeReference<>() {});
    }
}
