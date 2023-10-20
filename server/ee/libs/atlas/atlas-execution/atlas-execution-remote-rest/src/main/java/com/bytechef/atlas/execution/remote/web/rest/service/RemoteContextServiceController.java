
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.atlas.execution.remote.web.rest.service;

import com.bytechef.atlas.execution.domain.Context.Classname;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.file.storage.domain.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/context-service")
public class RemoteContextServiceController {

    private final ContextService contextService;

    @SuppressFBWarnings("EI")
    public RemoteContextServiceController(ContextService contextService) {
        this.contextService = contextService;
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/peek/{stackId}/{classname}",
        produces = {
            "application/json"
        })
    public ResponseEntity<FileEntry> peek(@PathVariable long stackId, @PathVariable Classname classname) {
        return ResponseEntity.ok(contextService.peek(stackId, classname));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/peek/{stackId}/{subStackId}/{classname}",
        produces = {
            "application/json"
        })
    public ResponseEntity<FileEntry> peek(
        @PathVariable long stackId, @PathVariable int subStackId, @PathVariable Classname classname) {

        return ResponseEntity.ok(contextService.peek(stackId, subStackId, classname));
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/push/{stackId}/{classname}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> push(
        @PathVariable long stackId, @PathVariable Classname classname, @Valid @RequestBody FileEntry value) {

        contextService.push(stackId, classname, value);

        return ResponseEntity.noContent()
            .build();
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/push/{stackId}/{subStackId}/{classname}",
        consumes = {
            "application/json"
        })
    public ResponseEntity<Void> push(
        @PathVariable long stackId, @PathVariable int subStackId, @PathVariable Classname classname,
        @Valid @RequestBody FileEntry value) {

        contextService.push(stackId, subStackId, classname, value);

        return ResponseEntity.noContent()
            .build();
    }
}
