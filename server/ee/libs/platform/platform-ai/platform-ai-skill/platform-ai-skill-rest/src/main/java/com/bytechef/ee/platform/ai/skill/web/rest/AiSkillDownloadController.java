/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.web.rest;

import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.ee.platform.ai.skill.facade.AiSkillFacade.AiSkillDownload;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/ai/agent-skills")
@ConditionalOnEEVersion
class AiSkillDownloadController {

    private final AiSkillFacade aiSkillFacade;

    AiSkillDownloadController(AiSkillFacade aiSkillFacade) {
        this.aiSkillFacade = aiSkillFacade;
    }

    @GetMapping("/{id}/download")
    ResponseEntity<Resource> downloadAiSkill(@PathVariable long id) {
        AiSkillDownload download = aiSkillFacade.getAiSkillWithDownload(id);

        String safeName = download.aiSkill()
            .getName()
            .replaceAll("[^a-zA-Z0-9._\\- ]", "_");

        ContentDisposition contentDisposition = ContentDisposition.attachment()
            .filename(safeName + ".skill")
            .build();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
            .body(new ByteArrayResource(download.bytes()));
    }
}
