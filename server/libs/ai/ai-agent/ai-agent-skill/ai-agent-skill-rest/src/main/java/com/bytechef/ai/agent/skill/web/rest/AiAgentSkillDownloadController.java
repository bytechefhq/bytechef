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

package com.bytechef.ai.agent.skill.web.rest;

import com.bytechef.ai.agent.skill.facade.AiAgentSkillFacade;
import com.bytechef.ai.agent.skill.facade.AiAgentSkillFacade.AiAgentSkillDownload;
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
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api/ai/agent-skills")
class AiAgentSkillDownloadController {

    private final AiAgentSkillFacade aiAgentSkillFacade;

    AiAgentSkillDownloadController(AiAgentSkillFacade aiAgentSkillFacade) {
        this.aiAgentSkillFacade = aiAgentSkillFacade;
    }

    @GetMapping("/{id}/download")
    ResponseEntity<Resource> downloadAiAgentSkill(@PathVariable long id) {
        AiAgentSkillDownload download = aiAgentSkillFacade.getAiAgentSkillWithDownload(id);

        String safeName = download.aiAgentSkill()
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
