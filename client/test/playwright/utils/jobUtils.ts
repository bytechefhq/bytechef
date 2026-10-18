import {type APIRequestContext, type APIResponse, type Page, expect} from '@playwright/test';

import {AUTOMATION_API_PATH, apiFetch, apiJson} from './apiUtils';

export const JOB_TIMEOUT = 60000;

export type JobStatusType = 'CANCELLED' | 'COMPLETED' | 'CREATED' | 'FAILED' | 'STARTED' | 'STOPPED';

export interface TaskExecutionI {
    error?: {message?: string} | null;
    id: string;
    output?: unknown;
    status: string;
    workflowTask?: {name?: string};
}

export interface JobI {
    error?: {message?: string; stackTrace?: string[]} | null;
    id: string;
    label?: string;
    metadata?: Record<string, unknown>;
    outputs?: unknown;
    status: JobStatusType;
    taskExecutions?: TaskExecutionI[];
}

interface WorkflowExecutionI {
    id: number;
    job: JobI;
}

export async function getProjectJobs(page: Page, workspaceId: number, projectId: string): Promise<JobI[]> {
    const executionsPage = await apiJson<{content: WorkflowExecutionI[]}>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/workspaces/${workspaceId}/workflow-executions?projectId=${projectId}`,
    });

    return executionsPage.content.map((workflowExecution) => workflowExecution.job);
}

export async function getJob(page: Page, jobId: string): Promise<JobI> {
    const workflowExecution = await apiJson<WorkflowExecutionI>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/workflow-executions/${jobId}`,
    });

    return workflowExecution.job;
}

export async function waitForJobStatus(
    page: Page,
    jobId: string,
    expectedStatus: JobStatusType,
    timeout = JOB_TIMEOUT
): Promise<JobI> {
    let job: JobI | undefined;

    await expect(async () => {
        job = await getJob(page, jobId);

        expect(job.status).toBe(expectedStatus);
    }).toPass({intervals: [500, 1000, 2000], timeout});

    return job as JobI;
}

/**
 * Waits for a job of the project that is not among `knownJobIds` and returns its id. Each test deploys its own
 * project, so the new job is the run the test just started.
 */
export async function waitForNewProjectJob({
    knownJobIds,
    page,
    projectId,
    timeout = JOB_TIMEOUT,
    workspaceId,
}: {
    knownJobIds: string[];
    page: Page;
    projectId: string;
    timeout?: number;
    workspaceId: number;
}): Promise<string> {
    let newJobId: string | undefined;

    await expect(async () => {
        const projectJobs = await getProjectJobs(page, workspaceId, projectId);

        newJobId = projectJobs.map((job) => job.id).find((jobId) => !knownJobIds.includes(jobId));

        expect(newJobId).toBeTruthy();
    }).toPass({intervals: [500, 1000], timeout});

    return newJobId as string;
}

export async function getTaskExecution(page: Page, jobId: string, taskName: string): Promise<TaskExecutionI> {
    const job = await getJob(page, jobId);

    const taskExecution = job.taskExecutions?.find(
        (jobTaskExecution) => jobTaskExecution.workflowTask?.name === taskName
    );

    if (!taskExecution) {
        throw new Error(`Job ${jobId} has no execution of ${taskName}`);
    }

    return apiJson<TaskExecutionI>({
        method: 'GET',
        page,
        path: `${AUTOMATION_API_PATH}/workflow-executions/${jobId}/task-executions/${taskExecution.id}`,
    });
}

export async function getTaskOutput<T = unknown>(page: Page, jobId: string, taskName: string): Promise<T> {
    const taskExecution = await getTaskExecution(page, jobId, taskName);

    return taskExecution.output as T;
}

export async function countTaskExecutions(page: Page, jobId: string, taskName?: string): Promise<number> {
    const job = await getJob(page, jobId);

    return (job.taskExecutions ?? []).filter(
        (taskExecution) => !taskName || taskExecution.workflowTask?.name === taskName
    ).length;
}

export function getJobErrorMessage(job: JobI): string {
    return job.error?.message ?? '';
}

export async function getTaskErrorMessage(page: Page, jobId: string, taskName: string): Promise<string> {
    const taskExecution = await getTaskExecution(page, jobId, taskName);

    return taskExecution.error?.message ?? '';
}

export async function postWebhook(
    request: APIRequestContext,
    webhookUrl: string,
    body: unknown = {}
): Promise<APIResponse> {
    return request.post(webhookUrl, {data: body});
}

export async function stopJob(page: Page, jobId: string): Promise<APIResponse> {
    return apiFetch({method: 'PUT', page, path: `/api/platform/internal/jobs/${jobId}/stop`});
}

export async function restartJob(page: Page, jobId: string): Promise<APIResponse> {
    return apiFetch({method: 'PUT', page, path: `/api/platform/internal/jobs/${jobId}/restart`});
}

/**
 * Finds a job created after `afterJobId` by its workflow label. Sub-workflow jobs started by a Subflow task or a
 * synchronous caller are not listed among their project's executions, so they are looked up by id instead.
 */
export async function waitForJobByLabelAfter({
    afterJobId,
    label,
    page,
    timeout = JOB_TIMEOUT,
}: {
    afterJobId: string;
    label: string;
    page: Page;
    timeout?: number;
}): Promise<JobI> {
    let foundJob: JobI | undefined;

    await expect(async () => {
        for (let jobId = Number(afterJobId) + 1; jobId <= Number(afterJobId) + 50 && !foundJob; jobId++) {
            const response = await apiFetch({
                method: 'GET',
                page,
                path: `${AUTOMATION_API_PATH}/workflow-executions/${jobId}`,
            });

            if (response.ok()) {
                const workflowExecution = (await response.json()) as WorkflowExecutionI;

                if (workflowExecution.job.label === label) {
                    foundJob = workflowExecution.job;
                }
            }
        }

        expect(foundJob).toBeTruthy();
    }).toPass({intervals: [1000, 2000], timeout});

    return foundJob as JobI;
}
