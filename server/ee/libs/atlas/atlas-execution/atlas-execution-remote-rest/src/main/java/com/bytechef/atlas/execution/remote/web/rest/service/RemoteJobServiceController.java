
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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParameters;
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
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
@RequestMapping("/remote/job-service")
public class RemoteJobServiceController {

    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public RemoteJobServiceController(JobService jobService) {
        this.jobService = jobService;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> create(@RequestBody JobCreateRequest jobCreateRequest) {
        return ResponseEntity.ok(jobService.create(jobCreateRequest.jobParameters, jobCreateRequest.workflow));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-job/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> getJob(@PathVariable long id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-task-execution-job/{taskExecutionId}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> getTaskExecutionJob(@PathVariable long taskExecutionId) {
        return ResponseEntity.ok(jobService.getTaskExecutionJob(taskExecutionId));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/resume-to-status-started/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> resumeToStatusStarted(@PathVariable long id) {
        return ResponseEntity.ok(jobService.resumeToStatusStarted(id));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/set-status-to-started/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> setStatusToStarted(@PathVariable long id) {
        return ResponseEntity.ok(jobService.setStatusToStarted(id));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/set-status-to-stopped/{id}",
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> setStatusToStopped(@PathVariable long id) {
        return ResponseEntity.ok(jobService.setStatusToStopped(id));
    }

    @RequestMapping(
        method = RequestMethod.PUT,
        value = "/update",
        consumes = {
            "application/json"
        },
        produces = {
            "application/json"
        })
    public ResponseEntity<Job> update(@RequestBody Job job) {
        return ResponseEntity.ok(jobService.update(job));
    }

    @SuppressFBWarnings("EI")
    public record JobCreateRequest(JobParameters jobParameters, Workflow workflow) {
    }
}
