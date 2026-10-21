/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.web.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.facade.AppFacade;
import com.bytechef.ee.automation.app.facade.AppPortalFacade;
import com.bytechef.ee.automation.app.web.rest.config.AppRestTestConfiguration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 */
@ContextConfiguration(classes = AppRestTestConfiguration.class)
@TestPropertySource(properties = "bytechef.edition=ee")
@WebMvcTest(AppPortalApiController.class)
class AppPortalApiControllerIntTest {

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
    void testDownloadAnswersTheStoredTypeForbidsSniffingAndAllowsPrivateCaching() throws Exception {
        when(appPortalFacade.getPortalAppIcon(4L)).thenReturn(Optional.of(new AppIconDTO(PNG, "image/png")));

        mockMvc.perform(get("/internal/portal/apps/{id}/icon", 4L).accept("text/html"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Cache-Control", "max-age=300, private"))
            .andExpect(content().bytes(PNG));
    }

    @Test
    void testDownloadOfAnAppWithoutAnIconOrOutsideThePortalAnswersNotFound() throws Exception {
        when(appPortalFacade.getPortalAppIcon(4L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/portal/apps/{id}/icon", 4L))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));

        mockMvc.perform(get("/internal/portal/apps/{id}/icon", 404L))
            .andExpect(status().isNotFound())
            .andExpect(content().string(""));
    }
}
