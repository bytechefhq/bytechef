package com.integri.atlas.task.handler.httpclient.v1_0.http;

import com.integri.atlas.engine.task.execution.SimpleTaskExecution;
import com.integri.atlas.engine.task.execution.TaskExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.http.HttpClient;

public class HttpClientHelperIntTest {

    @Autowired
    private HttpClientHelper httpClientHelper;

    @Test
    public void testBuildHttpClient() {
        TaskExecution taskExecution = new SimpleTaskExecution();

        HttpClient httpClient = httpClientHelper.buildHttpClient(taskExecution);

        Assertions.assertNotNull(httpClient);
    }
}
