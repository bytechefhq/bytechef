import {type APIRequestContext, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {auditEventRows, filterAuditEvents, openAuditEventsPage, selectAuditEventType} from '../../utils/auditUtils';
import {type BuiltInRoleType, RBAC_BASE_URL, deleteRoleUser, openAsTenantAdmin} from '../../utils/rbacUtils';
import {
    createConnection,
    createOrganizationConnectionId,
    createProjectResponse,
    createUserApiContext,
    createWorkspaceMember,
    deleteConnectionResponse,
    deleteOrganizationConnection,
    deployHttpClientProject,
    expectNoGraphQlErrors,
    fetchUserId,
    getConnection,
    getConnectionResponse,
    grantConnectionAccess,
    grantProjectAccess,
    postGraphQl,
    reassignConnection,
    removeWorkspaceMember,
    revokeConnectionAccess,
    revokeProjectAccess,
    runDeployedWorkflowResponse,
    setConnectionVisibility,
    setProjectVisibility,
    updateConnectionCredentials,
} from '../../utils/visibilityUtils';

interface AuditRecordI {
    data: Record<string, string>;
    eventType: string;
    outcome: string | null;
    principal: string;
}

async function findAuditRecords(
    adminApi: APIRequestContext,
    eventType: string,
    dataSearch: string
): Promise<AuditRecordI[]> {
    const response = await postGraphQl<{
        auditEvents: {
            content: {
                data: {key: string; value: string}[];
                eventType: string;
                outcome: string | null;
                principal: string;
            }[];
        };
    }>(
        adminApi,
        `query AuditEvents($eventType: String, $dataSearch: String) {
            auditEvents(eventType: $eventType, dataSearch: $dataSearch, size: 100) {
                content { data { key value } eventType outcome principal }
            }
        }`,
        {dataSearch, eventType}
    );

    expectNoGraphQlErrors(response);

    return response.data!.auditEvents.content.map((auditEvent) => ({
        data: Object.fromEntries(auditEvent.data.map((entry) => [entry.key, entry.value])),
        eventType: auditEvent.eventType,
        outcome: auditEvent.outcome,
        principal: auditEvent.principal,
    }));
}

async function expectAuditRecord(
    adminApi: APIRequestContext,
    eventType: string,
    matcher: (auditRecord: AuditRecordI) => boolean,
    dataSearch: string
): Promise<AuditRecordI> {
    let match: AuditRecordI | undefined;

    await expect(async () => {
        match = (await findAuditRecords(adminApi, eventType, dataSearch)).find(matcher);

        expect(match, `no ${eventType} record matching ${dataSearch}`).toBeDefined();
    }).toPass({timeout: 30000});

    return match!;
}

test.describe('Connection and project audit events', () => {
    test.describe.configure({mode: 'default'});

    const apis: Partial<Record<BuiltInRoleType, APIRequestContext>> = {};

    let viewerId: string;

    test.beforeAll(async ({adminApi, workerRoleUsers}) => {
        for (const role of ['ADMIN', 'EDITOR', 'VIEWER'] as BuiltInRoleType[]) {
            apis[role] = await createUserApiContext(workerRoleUsers[role].email, workerRoleUsers[role].password);
        }

        viewerId = await fetchUserId(adminApi, workerRoleUsers.VIEWER.login);
    });

    test.afterAll(async () => {
        for (const api of Object.values(apis)) {
            await api.dispose();
        }
    });

    test('the connection lifecycle and sharing are recorded with their principal and data', async ({
        adminApi,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const editorPrincipals = [workerRoleUsers.EDITOR.email, workerRoleUsers.EDITOR.login];
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);
        const id = String(connectionId);

        const created = await expectAuditRecord(
            adminApi,
            'CONNECTION_CREATED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );

        expect(editorPrincipals).toContain(created.principal);
        expect(created.outcome).toBe('SUCCESS');
        expect(created.data).toMatchObject({visibility: 'WORKSPACE', workspaceId: String(rbacWorkspace.id)});

        expectNoGraphQlErrors(await setConnectionVisibility(apis.EDITOR!, rbacWorkspace.id, connectionId, 'PRIVATE'));

        const visibilityChanged = await expectAuditRecord(
            adminApi,
            'CONNECTION_VISIBILITY_CHANGED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );

        expect(visibilityChanged.data).toMatchObject({fromVisibility: 'WORKSPACE', toVisibility: 'PRIVATE'});

        expectNoGraphQlErrors(await grantConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));
        expectNoGraphQlErrors(await revokeConnectionAccess(apis.EDITOR!, rbacWorkspace.id, connectionId, viewerId));

        for (const eventType of ['CONNECTION_ACCESS_GRANTED', 'CONNECTION_ACCESS_REVOKED']) {
            const auditRecord = await expectAuditRecord(
                adminApi,
                eventType,
                (candidate) => candidate.data.connectionId === id,
                id
            );

            expect(editorPrincipals).toContain(auditRecord.principal);
            expect(auditRecord.data.targetUserId).toBe(viewerId);
        }

        const version = (await (await getConnectionResponse(adminApi, connectionId)).json()).__version;

        expectNoGraphQlErrors(await updateConnectionCredentials(apis.EDITOR!, connectionId, {}, version));

        const credentialsUpdated = await expectAuditRecord(
            adminApi,
            'CONNECTION_CREDENTIALS_UPDATED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );

        expect(editorPrincipals).toContain(credentialsUpdated.principal);
        expect(Object.keys(credentialsUpdated.data).sort()).toEqual(['connectionId', 'method', 'result']);

        const deleteResponse = await deleteConnectionResponse(apis.EDITOR!, connectionId);

        expect(deleteResponse.ok(), await deleteResponse.text()).toBeTruthy();

        const deleted = await expectAuditRecord(
            adminApi,
            'CONNECTION_DELETED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );

        expect(editorPrincipals).toContain(deleted.principal);
    });

    test('a refused sharing change is recorded as DENIED', async ({adminApi, rbacWorkspace, workerRoleUsers}) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);
        const id = String(connectionId);

        await setConnectionVisibility(apis.VIEWER!, rbacWorkspace.id, connectionId, 'PRIVATE');

        const denied = await expectAuditRecord(
            adminApi,
            'CONNECTION_VISIBILITY_CHANGED',
            (auditRecord) => auditRecord.data.connectionId === id && auditRecord.outcome === 'DENIED',
            id
        );

        expect([workerRoleUsers.VIEWER.email, workerRoleUsers.VIEWER.login]).toContain(denied.principal);
    });

    test('organization connections are recorded on create and delete', async ({adminApi}) => {
        const connectionId = await createOrganizationConnectionId(adminApi);
        const id = String(connectionId);

        const created = await expectAuditRecord(
            adminApi,
            'CONNECTION_CREATED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );

        expect(created.data.visibility).toBe('ORGANIZATION');

        expectNoGraphQlErrors(await deleteOrganizationConnection(adminApi, connectionId));

        await expectAuditRecord(
            adminApi,
            'CONNECTION_DELETED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );
    });

    test('a reassignment and a run blocked by a pending connection are recorded, the latter as WORKFLOW_PAUSED', async ({
        adminApi,
        rbacWorkspace,
        workerRoleUsers,
    }) => {
        const leaver = await createWorkspaceMember(adminApi, rbacWorkspace.id, 'EDITOR');

        try {
            const leaverApi = await createUserApiContext(leaver.email, leaver.password);

            let connectionId: number;

            try {
                connectionId = await createConnection(leaverApi, rbacWorkspace.id);
            } finally {
                await leaverApi.dispose();
            }

            const deployment = await deployHttpClientProject(adminApi, rbacWorkspace.id, connectionId);

            await removeWorkspaceMember(adminApi, rbacWorkspace.id, leaver);

            await expect
                .poll(async () => (await getConnection(adminApi, connectionId)).status, {timeout: 15000})
                .toBe('PENDING_REASSIGNMENT');

            const runResponse = await runDeployedWorkflowResponse(adminApi, deployment);

            expect(runResponse.ok()).toBeFalsy();

            const id = String(connectionId);

            const paused = await expectAuditRecord(
                adminApi,
                'WORKFLOW_PAUSED',
                (auditRecord) => auditRecord.data.connectionIds === id,
                id
            );

            expect(paused.outcome).toBe('ERROR');
            expect(paused.data).toMatchObject({
                connectionStatuses: 'PENDING_REASSIGNMENT',
                jobPrincipalId: String(deployment.projectDeploymentId),
                workflowId: deployment.workflowId,
            });

            expectNoGraphQlErrors(
                await reassignConnection(adminApi, rbacWorkspace.id, connectionId, workerRoleUsers.EDITOR.login)
            );

            const reassigned = await expectAuditRecord(
                adminApi,
                'CONNECTION_REASSIGNED',
                (auditRecord) => auditRecord.data.connectionId === id,
                id
            );

            expect(reassigned.data.newOwnerUserId).toBe(await fetchUserId(adminApi, workerRoleUsers.EDITOR.login));
            expect(JSON.stringify(reassigned.data)).not.toContain('@');
        } finally {
            await deleteRoleUser(adminApi, leaver);
        }
    });

    test('project visibility changes and grants are recorded', async ({adminApi, rbacWorkspace}) => {
        const projectId = Number(await (await createProjectResponse(apis.EDITOR!, rbacWorkspace.id)).text());
        const id = String(projectId);

        expectNoGraphQlErrors(await setProjectVisibility(apis.EDITOR!, rbacWorkspace.id, projectId, 'PRIVATE'));
        expectNoGraphQlErrors(await grantProjectAccess(apis.EDITOR!, rbacWorkspace.id, projectId, viewerId));
        expectNoGraphQlErrors(await revokeProjectAccess(apis.EDITOR!, rbacWorkspace.id, projectId, viewerId));

        const visibilityChanged = await expectAuditRecord(
            adminApi,
            'PROJECT_VISIBILITY_CHANGED',
            (auditRecord) => auditRecord.data.projectId === id,
            id
        );

        expect(visibilityChanged.data).toMatchObject({fromVisibility: 'WORKSPACE', toVisibility: 'PRIVATE'});

        for (const eventType of ['PROJECT_ACCESS_GRANTED', 'PROJECT_ACCESS_REVOKED']) {
            const auditRecord = await expectAuditRecord(
                adminApi,
                eventType,
                (candidate) => candidate.data.projectId === id,
                id
            );

            expect(auditRecord.data.targetUserId).toBe(viewerId);
        }
    });

    test('every connection event type is listed on the audit events page', async ({
        adminApi,
        browser,
        rbacWorkspace,
    }) => {
        const connectionId = await createConnection(apis.EDITOR!, rbacWorkspace.id);
        const id = String(connectionId);

        expectNoGraphQlErrors(await setConnectionVisibility(apis.EDITOR!, rbacWorkspace.id, connectionId, 'PRIVATE'));

        await expectAuditRecord(
            adminApi,
            'CONNECTION_VISIBILITY_CHANGED',
            (auditRecord) => auditRecord.data.connectionId === id,
            id
        );

        const {context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, rbacWorkspace);

        try {
            await openAuditEventsPage(page);

            for (const eventType of ['CONNECTION_CREATED', 'CONNECTION_VISIBILITY_CHANGED']) {
                await page.reload();

                await openAuditEventsPage(page);

                await selectAuditEventType(page, eventType);

                await filterAuditEvents(page, {dataSearch: id});

                await expect(auditEventRows(page).filter({hasText: eventType}).first()).toBeVisible();
            }

            await page.reload();

            await openAuditEventsPage(page);

            await page.getByRole('combobox').click();

            for (const eventType of [
                'CONNECTION_ACCESS_GRANTED',
                'CONNECTION_ACCESS_REVOKED',
                'CONNECTION_CREATED',
                'CONNECTION_CREDENTIALS_UPDATED',
                'CONNECTION_DELETED',
                'CONNECTION_REASSIGNED',
                'CONNECTION_VISIBILITY_CHANGED',
                'PROJECT_ACCESS_GRANTED',
                'PROJECT_ACCESS_REVOKED',
                'PROJECT_VISIBILITY_CHANGED',
                'WORKFLOW_PAUSED',
            ]) {
                await expect(page.getByRole('option', {exact: true, name: eventType})).toBeAttached();
            }
        } finally {
            await context.close();
        }
    });
});
