package com.bytechef.component.youtube.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.youtube.constant.YoutubeConstants.DESCRIPTION;
import static com.bytechef.component.youtube.constant.YoutubeConstants.FILE;
import static com.bytechef.component.youtube.constant.YoutubeConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Nikolina Spehar
 */
class YoutubeUploadVideoActionTest {
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
            Map.of(FILE, "testTitle", TITLE, "testTitle", DESCRIPTION, "testDescription"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of();

    @Test
    void perform() {

        YoutubeUploadVideoAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertNotNull(List.of());
    }
}
