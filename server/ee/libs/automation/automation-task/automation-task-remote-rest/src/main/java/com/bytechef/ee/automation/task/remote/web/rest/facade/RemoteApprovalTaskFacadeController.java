/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.task.remote.web.rest.facade;

import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.domain.PendingApproval;
import com.bytechef.automation.task.facade.ApprovalTaskFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves {@link ApprovalTaskFacade} to remote callers so worker-app can create approval-task rows (and read the
 * pending-approvals inbox) even though the real facade lives in configuration-app.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Hidden
@RestController
@RequestMapping("/remote/approval-task-facade")
public class RemoteApprovalTaskFacadeController {

    private final ApprovalTaskFacade approvalTaskFacade;

    @SuppressFBWarnings("EI")
    public RemoteApprovalTaskFacadeController(ApprovalTaskFacade approvalTaskFacade) {
        this.approvalTaskFacade = approvalTaskFacade;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        value = "/create-approval-task",
        produces = {
            "application/json"
        })
    public ResponseEntity<ApprovalTask> createApprovalTask(@RequestBody ApprovalTask approvalTask) {
        return ResponseEntity.ok(approvalTaskFacade.createApprovalTask(approvalTask));
    }

    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get-pending-approvals",
        produces = {
            "application/json"
        })
    public ResponseEntity<List<PendingApproval>> getPendingApprovals(
        @RequestParam(required = false) @Nullable Integer environmentId) {

        return ResponseEntity.ok(approvalTaskFacade.getPendingApprovals(environmentId));
    }
}
