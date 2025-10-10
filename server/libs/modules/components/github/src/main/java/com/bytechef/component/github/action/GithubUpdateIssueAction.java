package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.ASSIGNEES;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.ISSUE_OUTPUT_PROPERTY;
import static com.bytechef.component.github.constant.GithubConstants.LABELS;
import static com.bytechef.component.github.constant.GithubConstants.MILESTONE;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.STATE;
import static com.bytechef.component.github.constant.GithubConstants.TITLE;
import static com.bytechef.component.github.util.GithubUtils.getOwnerName;

import java.util.Map;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.github.util.GithubUtils;

public class GithubUpdateIssueAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateIssue").title("Update Issue")
            .description("Update specific repository issue")
            .properties(
                    string(REPOSITORY).label("Repository")
                            .description("Repository of issue you want to update")
                            .options((ActionOptionsFunction<String>) GithubUtils::getRepositoryOptions)
                            .required(true),
                    string(ISSUE).label("Issue").description("Issue to update")
                            .options((ActionOptionsFunction<String>) GithubUtils::getIssueOptions)
                            .optionsLookupDependsOn(REPOSITORY)
                            .required(true),
                    string(TITLE).label("Title").description("New issue title"),
                    string(STATE).label("State").description("New issue state")
                            .options(
                                    option("Open", "open"),
                                    option("Closed", "closed")),
                    string(MILESTONE).label("Milestone")
                            .description("Change issue milestone or remove it by passing in the null")
                            .options((ActionOptionsFunction<String>) GithubUtils::getMilestones),
                    array(LABELS).label("Labels").description("Change issue labels")
                            .items(string())
                            .options((ActionOptionsFunction<String>) GithubUtils::getLabels),
                    array(ASSIGNEES).label("Assignees").description("Change issue assignees")
                            .items(string())
                            .options((ActionOptionsFunction<String>) GithubUtils::getCollaborators))
            .output(outputSchema(ISSUE_OUTPUT_PROPERTY))
            .perform(GithubUpdateIssueAction::perform);

    public GithubUpdateIssueAction() {

    }

    public static Map<String, Object> perform(Parameters inputParameters, Parameters connectionParameters,
            Context context) {
        return context.http(http -> http.patch(
                "/repos/" + getOwnerName(context) + "/" + inputParameters.getRequiredString(REPOSITORY) + "/issues/"
                        + inputParameters.getRequiredString(ISSUE)))
                .body(Body.of(
                        TITLE, inputParameters.getString(TITLE),
                        STATE, inputParameters.getString(STATE),
                        MILESTONE, inputParameters.getString(MILESTONE),
                        LABELS, inputParameters.getArray(LABELS),
                        ASSIGNEES, inputParameters.getArray(ASSIGNEES)

                ))
                .configuration(responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {
                });
    }
}
