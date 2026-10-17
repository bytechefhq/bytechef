import {type Page, expect} from '@playwright/test';

import {type WorkflowDeployerI} from '../fixtures/workflowDeployment';
import {getApprovalTokenForJob} from './approvalUtils';
import {type DeployedProjectI, type WorkflowDefinitionType} from './deploymentUtils';
import getRandomString from './getRandomString';
import {type JobI, waitForJobStatus} from './jobUtils';

export interface ApprovalRunI {
    deployedProject: DeployedProjectI;
    job: JobI;
    jobId: string;
    token: string;
}

export function uniqueSuffix(): string {
    return getRandomString().slice(0, 8);
}

export function uniqueRecipient(prefix: string): string {
    return `${prefix}-${uniqueSuffix()}@example.com`;
}

/**
 * Starts one more run of an already deployed approval workflow and waits until it is suspended on its approval.
 */
export async function startApprovalRun({
    body = {},
    deployedProject,
    deployer,
    page,
    workflowLabel,
}: {
    body?: unknown;
    deployedProject: DeployedProjectI;
    deployer: WorkflowDeployerI;
    page: Page;
    workflowLabel?: string;
}): Promise<ApprovalRunI> {
    const startedJob = await deployer.triggerWebhookAndWaitForJob(deployedProject, body, workflowLabel);

    const job = await waitForJobStatus(page, startedJob.id, 'STOPPED');

    const token = await getApprovalTokenForJob(page, job.id);

    return {deployedProject, job, jobId: job.id, token};
}

/**
 * Deploys the workflow and starts its first run, suspended on its approval.
 */
export async function deployAndStartApprovalRun({
    body = {},
    definition,
    deployer,
    page,
}: {
    body?: unknown;
    definition: WorkflowDefinitionType;
    deployer: WorkflowDeployerI;
    page: Page;
}): Promise<ApprovalRunI> {
    const deployedProject = await deployer.deploy([definition]);

    expect(deployedProject.workflows[0].webhookUrl).toBeTruthy();

    return startApprovalRun({body, deployedProject, deployer, page});
}
