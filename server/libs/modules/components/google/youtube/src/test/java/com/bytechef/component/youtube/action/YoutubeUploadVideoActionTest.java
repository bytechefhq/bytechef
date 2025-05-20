package com.bytechef.component.youtube.action;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.swing.text.html.HTMLDocument.HTMLReader.ParagraphAction;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.youtube.constant.YoutubeConstants.CATEGORY_ID;
import static com.bytechef.component.youtube.constant.YoutubeConstants.DESCRIPTION;
import static com.bytechef.component.youtube.constant.YoutubeConstants.FILE;
import static com.bytechef.component.youtube.constant.YoutubeConstants.PRIVACY_STATUS;
import static com.bytechef.component.youtube.constant.YoutubeConstants.TAGS;
import static com.bytechef.component.youtube.constant.YoutubeConstants.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikolina Spehar
 */
class YoutubeUploadVideoActionTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Context mockedContext = mock(Context.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> valueArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        FILE, mockedFileEntry, TITLE, "testTitle", DESCRIPTION, "testDescription", TAGS, List.of(),
        PRIVACY_STATUS, "private", CATEGORY_ID, "2"));

    @Test
    void perform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
//    when(mockedExecutor.headers(Map.of(nameArgumentCaptor.capture(), List.of(valueArgumentCaptor.capture()))))
//        .thenReturn(mockedExecutor);
    when(mockedExecutor.header(nameArgumentCaptor.capture(), valueArgumentCaptor.capture()))
        .thenReturn(mockedExecutor);
    when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
        .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getHeaders())
            .thenReturn(Map.of("location", List.of("url")));
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(null);

        FileEntry result = YoutubeUploadVideoAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedFileEntry, result);

    }
}
