/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.auth.web.rest;

import com.bytechef.hermes.auth.domain.Authentication;
import com.bytechef.hermes.auth.service.AuthenticationService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @DeleteMapping(value = "/authentications/{id}")
    public ResponseEntity<?> deleteAuthentication(@PathVariable("id") String id) {
        authenticationService.remove(id);

        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/authentications/{id}")
    public Authentication getAuthentication(@PathVariable("id") String id) {
        return authenticationService.fetchAuthentication(id);
    }

    @GetMapping(value = "/authentications", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Authentication> getAuthentications() {
        return authenticationService.getAuthentications();
    }

    @PostMapping(value = "/authentications", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Authentication postAuthentication(@RequestBody AuthenticationCreateDTO authenticationCreateDTO) {
        return authenticationService.add(
                authenticationCreateDTO.name, authenticationCreateDTO.type, authenticationCreateDTO.properties);
    }

    @PutMapping(value = "/authentications", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Authentication putAuthentication(@RequestBody AuthenticationMergeDTO authenticationMergeDTO) {
        return authenticationService.update(authenticationMergeDTO.id, authenticationMergeDTO.name);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void handleIllegalArgumentException(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendError(HttpStatus.BAD_REQUEST.value());
    }

    public record AuthenticationCreateDTO(String name, Map<String, Object> properties, String type) {}

    public record AuthenticationMergeDTO(String id, String name) {}
}
