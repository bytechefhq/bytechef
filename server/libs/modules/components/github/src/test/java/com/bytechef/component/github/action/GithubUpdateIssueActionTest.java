package com.bytechef.component.github.action;

import static com.bytechef.component.github.constant.GithubConstants.ISSUE;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class GithubUpdateIssueActionTest extends AbstractGithubActionTest {
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
            REPOSITORY, "mockedRepository", ISSUE, "mockedIssue"));

    @Test
    void testPerform() {
        when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

        Map<String, Object> result = GithubUpdateIssueAction.perform(mockedParameters, mockedParameters,
                mockedContext);

        assertEquals(responseMap, result);
    }

}
