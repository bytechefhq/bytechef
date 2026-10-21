/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppPortalFacade;
import com.bytechef.ee.automation.app.web.rest.config.AppRestTestConfiguration;
import com.bytechef.exception.ConfigurationException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 */
@ContextConfiguration(classes = AppRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppIconApiController.class)
class AppIconApiControllerIntTest {

    private static final byte[] PNG = {
        (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    @MockitoBean
    private AppFacade appFacade;

    @MockitoBean
    private AppPortalFacade appPortalFacade;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUploadPassesTheFileContentToTheFacade() throws Exception {
        AtomicReference<byte[]> uploadedContent = new AtomicReference<>();

        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgument(1);

            uploadedContent.set(inputStream.readAllBytes());

            return null;
        }).when(appFacade)
            .updateAppIcon(eq(4L), any(InputStream.class));

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);

        mockMvc.perform(multipart("/internal/apps/{id}/icon", 4L).file(file))
            .andExpect(status().isNoContent());

        assertThat(uploadedContent.get()).isEqualTo(PNG);
    }

    @Test
    void testUploadOfARejectedIconAnswersBadRequest() throws Exception {
        doThrow(new ConfigurationException("App icon must be PNG, JPEG or WebP", AppErrorType.APP_ICON_INVALID))
            .when(appFacade)
            .updateAppIcon(eq(4L), any(InputStream.class));

        MockMultipartFile file =
            new MockMultipartFile("file", "logo.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/internal/apps/{id}/icon", 4L).file(file))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testUploadDeniedByTheFacadeAnswersForbidden() throws Exception {
        doThrow(new AccessDeniedException("Access Denied")).when(appFacade)
            .updateAppIcon(eq(4L), any(InputStream.class));

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", PNG);

        mockMvc.perform(multipart("/internal/apps/{id}/icon", 4L).file(file))
            .andExpect(status().isForbidden());
    }

    @Test
    void testUploadWithoutAFileAnswersBadRequest() throws Exception {
        mockMvc.perform(multipart("/internal/apps/{id}/icon", 4L))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(appFacade);
    }

    @Test
    void testDownloadAnswersTheStoredTypeAndForbidsSniffing() throws Exception {
        when(appFacade.getAppIcon(4L)).thenReturn(Optional.of(new AppIconDTO(PNG, "image/png")));

        mockMvc.perform(get("/internal/apps/{id}/icon", 4L).accept("text/html"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes(PNG));
    }

    @Test
    void testDownloadOfAnAppWithoutIconAnswersNotFound() throws Exception {
        when(appFacade.getAppIcon(4L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/apps/{id}/icon", 4L))
            .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDelegatesToTheFacade() throws Exception {
        mockMvc.perform(delete("/internal/apps/{id}/icon", 4L))
            .andExpect(status().isNoContent());

        verify(appFacade).deleteAppIcon(4L);
    }
}
