package com.bytechef.component.clickup.action;

import static com.bytechef.component.clickup.constant.ClickupConstants.COMMENT_TEXT;
import static com.bytechef.component.clickup.constant.ClickupConstants.FOLDER_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.LIST_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.NOTIFY_ALL;
import static com.bytechef.component.clickup.constant.ClickupConstants.SPACE_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.TASK_ID;
import static com.bytechef.component.clickup.constant.ClickupConstants.WORKSPACE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;

/**
 * @author Karlo Čehulić
 */
public class ClickupCreateTaskCommentActionTest {
    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Map<String, Object> responseMap = Map.of("id", "abc", "hist_id", "bcd", "date", 123);
    private final Parameters mockedParameters = MockParametersFactory
            .create(Map.of(TASK_ID, "some task id", COMMENT_TEXT, "some comment text", NOTIFY_ALL, true));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
                .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

        Object result = ClickupCreateTaskCommentAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);
    }
}
