import {APIResponse, Page, expect} from '@playwright/test';

export const AUDIT_EVENTS_ROUTE = '/automation/settings/audit-events';

export const NON_ADMIN_USER = {
    email: 'user@localhost.com',
    password: 'user',
} as const;

async function getXsrfToken(page: Page): Promise<string> {
    const cookies = await page.context().cookies();

    return cookies.find((cookie) => cookie.name === 'XSRF-TOKEN')?.value || '';
}

export async function postGraphQl(
    page: Page,
    query: string,
    variables: Record<string, unknown> = {}
): Promise<APIResponse> {
    return page.request.post('/graphql', {
        data: {query, variables},
        headers: {'X-XSRF-TOKEN': await getXsrfToken(page)},
    });
}

export async function postApi(page: Page, path: string, data?: unknown): Promise<APIResponse> {
    const response = await page.request.post(path, {
        data,
        headers: {'X-XSRF-TOKEN': await getXsrfToken(page)},
    });

    expect(response.ok(), `POST ${path} returned ${response.status()}: ${await response.text()}`).toBeTruthy();

    return response;
}

export async function putApi(page: Page, path: string, data?: unknown): Promise<APIResponse> {
    const response = await page.request.put(path, {
        data,
        headers: {'X-XSRF-TOKEN': await getXsrfToken(page)},
    });

    expect(response.ok(), `PUT ${path} returned ${response.status()}: ${await response.text()}`).toBeTruthy();

    return response;
}

export async function getApi(page: Page, path: string): Promise<APIResponse> {
    const response = await page.request.get(path);

    expect(response.ok(), `GET ${path} returned ${response.status()}: ${await response.text()}`).toBeTruthy();

    return response;
}

export function auditEventsDataSearchInput(page: Page) {
    return page.getByRole('textbox', {exact: true, name: 'Search data'});
}

export function auditEventsFilterButton(page: Page) {
    return page.getByRole('button', {exact: true, name: 'Filter options'});
}

export async function openAuditEventsPage(page: Page): Promise<void> {
    await page.goto(AUDIT_EVENTS_ROUTE);

    await page.addStyleTag({content: '.tsqd-parent-container { display: none !important; }'});

    await expect(auditEventsDataSearchInput(page)).toBeVisible();
}

async function expectFilterApplied(page: Page, label: string, value: string): Promise<void> {
    if (value) {
        await expect(page.getByText(`${label} ${value}`, {exact: true})).toBeVisible();
    } else {
        await expect(page.getByText(new RegExp(`^${label} `))).toHaveCount(0);
    }

    await expect(page.getByText('Loading…')).toHaveCount(0);
}

export async function filterAuditEvents(
    page: Page,
    {dataSearch, principal}: {dataSearch?: string; principal?: string}
): Promise<void> {
    if (principal !== undefined) {
        await page.getByRole('textbox', {exact: true, name: 'Principal'}).fill(principal);

        await expectFilterApplied(page, 'Principal:', principal);
    }

    if (dataSearch !== undefined) {
        await auditEventsDataSearchInput(page).fill(dataSearch);

        await expectFilterApplied(page, 'Data:', dataSearch);
    }
}

export async function openAuditEventsFilterMenu(page: Page) {
    await auditEventsFilterButton(page).click();

    const filterMenu = page.getByRole('menu');

    await expect(filterMenu.getByText('Filter Audit Events', {exact: true})).toBeVisible();

    return filterMenu;
}

export async function selectAuditEventType(page: Page, eventType: string): Promise<void> {
    const filterMenu = await openAuditEventsFilterMenu(page);

    await filterMenu.getByRole('menuitem', {exact: true, name: eventType}).click();

    await expectFilterApplied(page, 'Event type:', eventType);
}

export function auditEventRows(page: Page) {
    return page.locator('tbody tr');
}

export async function openAuditEventRowWithDataEntry(page: Page, key: string, value: string): Promise<void> {
    await filterAuditEvents(page, {dataSearch: value});

    const rows = auditEventRows(page);

    await expect(rows.first()).toBeVisible();

    const dialog = page.getByRole('dialog');

    const matchingEntry = dialog
        .locator('div.contents')
        .filter({has: page.locator('dt', {hasText: new RegExp(`^${key}$`)})})
        .locator('dd', {hasText: new RegExp(`^${value}$`)});

    const rowCount = await rows.count();

    for (let index = 0; index < rowCount; index++) {
        await rows.nth(index).click();

        await expect(dialog).toBeVisible();

        if ((await matchingEntry.count()) > 0) {
            return;
        }

        await page.keyboard.press('Escape');

        await expect(dialog).toBeHidden();
    }

    throw new Error(`no audit event row carries ${key} = ${value}`);
}

export interface AuditEventDataEntryI {
    key: string;
    value: string;
}

export interface AuditEventI {
    data: AuditEventDataEntryI[];
    eventType: string;
    outcome: string | null;
    principal: string;
}

export async function queryAuditEvents(
    page: Page,
    variables: Record<string, unknown> = {}
): Promise<{content: AuditEventI[]; totalElements: number}> {
    const response = await postGraphQl(
        page,
        `query AuditEvents($principal: String, $eventType: String, $fromDate: Long, $toDate: Long, $dataSearch: String, $size: Int) {
            auditEvents(principal: $principal, eventType: $eventType, fromDate: $fromDate, toDate: $toDate, dataSearch: $dataSearch, size: $size) {
                content {
                    data {
                        key
                        value
                    }
                    eventType
                    outcome
                    principal
                }
                totalElements
            }
        }`,
        variables
    );

    const body = await response.json();

    expect(body.errors, JSON.stringify(body.errors)).toBeUndefined();

    return body.data.auditEvents;
}

export function auditEventData(auditEvent: AuditEventI): Record<string, string> {
    return Object.fromEntries(auditEvent.data.map((entry) => [entry.key, entry.value]));
}

const MANUAL_TRIGGER = [{label: 'Manual', name: 'trigger_1', type: 'manual/v1/manual'}];

export const COMPLETING_WORKFLOW = {
    inputs: [],
    label: 'audit completing',
    tasks: [{label: 'Var', name: 'var_1', parameters: {type: 'STRING', value: 'hello'}, type: 'var/v1/set'}],
    triggers: MANUAL_TRIGGER,
};

export const FAILING_WORKFLOW = {
    inputs: [],
    label: 'audit failing',
    tasks: [{label: 'Var', name: 'var_1', parameters: {type: 'STRING', value: 'hello'}, type: 'var/v1/missingAction'}],
    triggers: MANUAL_TRIGGER,
};

export const WAITING_WORKFLOW = {
    inputs: [],
    label: 'audit waiting',
    tasks: [
        {label: 'Wait', name: 'wait_1', parameters: {amount: 10, unit: 'MINUTES'}, type: 'wait/v1/afterTimeInterval'},
    ],
    triggers: MANUAL_TRIGGER,
};

export interface DeployedWorkflowsI {
    projectDeploymentId: number;
    workflowIds: Record<string, string>;
}

export async function deployWorkflows(
    page: Page,
    projectName: string,
    workflowDefinitions: {label: string}[]
): Promise<DeployedWorkflowsI> {
    const workspaces = await (await getApi(page, '/api/automation/internal/workspaces')).json();

    const projectId = await (
        await postApi(page, '/api/automation/internal/projects', {name: projectName, workspaceId: workspaces[0].id})
    ).json();

    for (const workflowDefinition of workflowDefinitions) {
        await postApi(page, `/api/automation/internal/projects/${projectId}/workflows`, {
            definition: JSON.stringify(workflowDefinition),
        });
    }

    await postApi(page, `/api/automation/internal/projects/${projectId}/publish`, {description: 'audit e2e'});

    const workflows: {id: string; label: string}[] = await (
        await getApi(page, `/api/automation/internal/projects/${projectId}/versions/1/workflows`)
    ).json();

    const workflowIds = Object.fromEntries(workflows.map((workflow) => [workflow.label, workflow.id]));

    const projectDeploymentId = await (
        await postApi(page, '/api/automation/internal/project-deployments', {
            environmentId: 0,
            name: `${projectName}_deployment`,
            projectDeploymentWorkflows: workflows.map((workflow) => ({enabled: true, workflowId: workflow.id})),
            projectId,
            projectVersion: 1,
        })
    ).json();

    const enableResponse = await page.request.patch(
        `/api/automation/internal/project-deployments/${projectDeploymentId}/enable/true`,
        {headers: {'X-XSRF-TOKEN': await getXsrfToken(page)}}
    );

    expect(enableResponse.ok()).toBeTruthy();

    return {projectDeploymentId, workflowIds};
}

export async function runDeployedWorkflow(
    page: Page,
    deployedWorkflows: DeployedWorkflowsI,
    workflowLabel: string
): Promise<number> {
    const response = await postApi(
        page,
        `/api/automation/internal/project-deployments/${deployedWorkflows.projectDeploymentId}/workflows/${deployedWorkflows.workflowIds[workflowLabel]}/jobs`
    );

    return (await response.json()).jobId;
}

export async function expectJobAuditEvent(page: Page, jobId: number, eventType: string): Promise<void> {
    await expect(async () => {
        const auditEvents = await queryAuditEvents(page, {eventType, principal: 'SYSTEM', size: 200});

        expect(auditEvents.content.map((auditEvent) => auditEventData(auditEvent).jobId)).toContain(String(jobId));
    }).toPass({timeout: 30000});
}
