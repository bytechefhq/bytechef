import {type APIRequestContext, type APIResponse, type Browser, type Page, expect, test} from '@playwright/test';

import {DEVELOPMENT_ENVIRONMENT_ID, SERVER_URL, graphqlData} from './apiUtils';

export const APPROVAL_TIMEOUT = 60000;

export interface PendingApprovalI {
    createdDate?: string;
    expiresAt?: string;
    formUrl?: string;
    jobId: string;
    workflowLabel: string;
}

export type ApprovalTaskStatusType = 'COMPLETED' | 'EXPIRED' | 'IN_PROGRESS' | 'OPEN';

export interface ApprovalTaskI {
    description?: string;
    dueDate?: string;
    id: string;
    jobResumeId?: string;
    name: string;
    priority: string;
    status: ApprovalTaskStatusType;
    version: number;
}

/**
 * Takes the signed approval token out of an approval link such as `<public-url>/resume/<token>?approved=true`.
 */
export function extractApprovalToken(approvalUrl: string): string {
    const tokenMatch = approvalUrl.match(/\/resume\/(v1\.[^?"\s)]+)/);

    if (!tokenMatch) {
        throw new Error(`No approval token in ${approvalUrl}`);
    }

    return tokenMatch[1];
}

export function getTokenSegments(token: string): {expiry: string; payload: string; signature: string; version: string} {
    const [version, expiry, payload, signature] = token.split('.');

    return {expiry, payload, signature, version};
}

function flipLastCharacter(value: string): string {
    const lastCharacter = value.slice(-1);

    return `${value.slice(0, -1)}${lastCharacter === 'A' ? 'B' : 'A'}`;
}

export function tamperTokenSignature(token: string): string {
    const {expiry, payload, signature, version} = getTokenSegments(token);

    return [version, expiry, payload, flipLastCharacter(signature)].join('.');
}

export function tamperTokenExpiry(token: string): string {
    const {expiry, payload, signature, version} = getTokenSegments(token);

    return [version, String(Number(expiry) + 3600), payload, signature].join('.');
}

export function tamperTokenPayload(token: string): string {
    const {expiry, payload, signature, version} = getTokenSegments(token);

    return [version, expiry, flipLastCharacter(payload), signature].join('.');
}

/**
 * Decodes the raw (unsigned) resume id that the signed token wraps in its payload segment.
 */
export function decodeRawResumeId(token: string): string {
    return Buffer.from(getTokenSegments(token).payload, 'base64url').toString('utf8');
}

export function getTokenExpiryEpochSeconds(token: string): number {
    return Number(getTokenSegments(token).expiry);
}

interface ResumeJobProps {
    accept?: string;
    baseUrl?: string;
    body?: unknown;
    request: APIRequestContext;
    token: string;
}

/**
 * Resolves an approval exactly as the hosted form does: an anonymous `POST /job/resume/<token>`.
 */
export async function resumeJob({
    accept = 'application/json',
    baseUrl,
    body,
    request,
    token,
}: ResumeJobProps): Promise<APIResponse> {
    const resumeUrl = `${baseUrl ?? ''}/job/resume/${token}`;

    return request.post(resumeUrl, {
        data: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
        headers: {Accept: accept, 'Content-Type': 'application/json'},
    });
}

export async function getApprovalForm(request: APIRequestContext, token: string): Promise<APIResponse> {
    return request.get(`/api/platform/internal/approval-form/${token}`);
}

export async function mintShortToken(
    request: APIRequestContext,
    body: string,
    contentType = 'text/plain'
): Promise<APIResponse> {
    return request.post(`${SERVER_URL}/approval/short-token`, {
        data: body,
        headers: {'Content-Type': contentType},
    });
}

export async function getShortId(request: APIRequestContext, token: string): Promise<string> {
    const response = await mintShortToken(request, token);

    expect(response.status()).toBe(200);

    return (await response.text()).trim();
}

export async function getPendingApprovals(page: Page): Promise<PendingApprovalI[]> {
    const data = await graphqlData<{pendingApprovals: Array<PendingApprovalI | null>}>(
        page,
        'query pendingApprovals($environmentId: Int) { pendingApprovals(environmentId: $environmentId) { jobId workflowLabel formUrl createdDate expiresAt } }',
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID}
    );

    return data.pendingApprovals.filter((pendingApproval): pendingApproval is PendingApprovalI => !!pendingApproval);
}

export async function waitForPendingApproval(page: Page, jobId: string): Promise<PendingApprovalI> {
    let foundPendingApproval: PendingApprovalI | undefined;

    await expect(async () => {
        const pendingApprovals = await getPendingApprovals(page);

        foundPendingApproval = pendingApprovals.find((pendingApproval) => pendingApproval.jobId === jobId);

        expect(foundPendingApproval?.formUrl).toBeTruthy();
    }).toPass({timeout: APPROVAL_TIMEOUT});

    return foundPendingApproval as PendingApprovalI;
}

export async function getApprovalTokenForJob(page: Page, jobId: string): Promise<string> {
    const pendingApproval = await waitForPendingApproval(page, jobId);

    return extractApprovalToken(pendingApproval.formUrl as string);
}

const APPROVAL_TASK_FIELDS = 'id name description status priority dueDate jobResumeId version';

export async function getApprovalTasks(page: Page): Promise<ApprovalTaskI[]> {
    const data = await graphqlData<{approvalTasks: Array<ApprovalTaskI | null>}>(
        page,
        `query approvalTasks($environmentId: Int) { approvalTasks(environmentId: $environmentId) { ${APPROVAL_TASK_FIELDS} } }`,
        {environmentId: DEVELOPMENT_ENVIRONMENT_ID}
    );

    return data.approvalTasks.filter((approvalTask): approvalTask is ApprovalTaskI => !!approvalTask);
}

export async function findApprovalTasksByName(page: Page, name: string): Promise<ApprovalTaskI[]> {
    const approvalTasks = await getApprovalTasks(page);

    return approvalTasks.filter((approvalTask) => approvalTask.name === name);
}

export async function waitForApprovalTaskStatus(
    page: Page,
    name: string,
    status: ApprovalTaskStatusType
): Promise<ApprovalTaskI> {
    let foundApprovalTask: ApprovalTaskI | undefined;

    await expect(async () => {
        const approvalTasks = await findApprovalTasksByName(page, name);

        foundApprovalTask = approvalTasks[0];

        expect(approvalTasks).toHaveLength(1);
        expect(foundApprovalTask.status).toBe(status);
    }).toPass({timeout: APPROVAL_TIMEOUT});

    return foundApprovalTask as ApprovalTaskI;
}

export const UPDATE_APPROVAL_TASK_MUTATION = `mutation updateApprovalTask($approvalTask: ApprovalTaskInput!) { updateApprovalTask(approvalTask: $approvalTask) { ${APPROVAL_TASK_FIELDS} } }`;

/**
 * Opens a page in a fresh browser context with no session, as an approver who follows a link from a mail would.
 */
export async function newAnonymousPage(browser: Browser): Promise<Page> {
    const browserContext = await browser.newContext({baseURL: test.info().project.use.baseURL});

    return browserContext.newPage();
}
