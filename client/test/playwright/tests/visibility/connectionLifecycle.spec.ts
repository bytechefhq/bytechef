import {type APIRequestContext, expect, test} from '@playwright/test';

import {createAdminApiContext, deleteRoleUser} from '../../utils/rbacUtils';
import {
    RBAC_BASE_URL,
    type RoleUserI,
    addWorkspaceMember,
    createConnection,
    createUserApiContext,
    createWorkspace,
    createWorkspaceMember,
    deleteWorkspace,
    deployHttpClientProject,
    expectAccessDenied,
    expectGraphQlError,
    expectNoGraphQlErrors,
    fetchConnectionGrants,
    getConnection,
    grantConnectionAccess,
    markConnectionsPendingReassignment,
    postGraphQl,
    reassignConnection,
    removeWorkspaceMember,
    updateConnectionResponse,
} from '../../utils/visibilityUtils';

test.describe('Connection lifecycle on member removal', () => {
    test.describe.configure({mode: 'serial'});

    let adminApi: APIRequestContext;
    let workspaceId: number;
    let secondWorkspaceId: number;
    let leaver: RoleUserI;
    let leaverApi: APIRequestContext;
    let colleague: RoleUserI;
    let colleagueApi: APIRequestContext;
    let workspaceAdmin: RoleUserI;
    let workspaceAdminApi: APIRequestContext;
    let leaverConnectionId: number;
    let leaverSecondWorkspaceConnectionId: number;
    let colleaguePrivateConnectionId: number;
    let deployedWorkflowId: string;

    test.beforeAll(async () => {
        adminApi = await createAdminApiContext(RBAC_BASE_URL);

        workspaceId = (await createWorkspace(adminApi)).id;
        secondWorkspaceId = (await createWorkspace(adminApi)).id;

        leaver = await createWorkspaceMember(adminApi, workspaceId, 'EDITOR');

        await addWorkspaceMember(adminApi, secondWorkspaceId, leaver, 'EDITOR');

        colleague = await createWorkspaceMember(adminApi, workspaceId, 'EDITOR');
        workspaceAdmin = await createWorkspaceMember(adminApi, workspaceId, 'ADMIN');

        leaverApi = await createUserApiContext(leaver.email, leaver.password);
        colleagueApi = await createUserApiContext(colleague.email, colleague.password);
        workspaceAdminApi = await createUserApiContext(workspaceAdmin.email, workspaceAdmin.password);

        leaverConnectionId = await createConnection(leaverApi, workspaceId);
        leaverSecondWorkspaceConnectionId = await createConnection(leaverApi, secondWorkspaceId);

        colleaguePrivateConnectionId = await createConnection(colleagueApi, workspaceId, {visibility: 'PRIVATE'});

        expectNoGraphQlErrors(
            await grantConnectionAccess(colleagueApi, workspaceId, colleaguePrivateConnectionId, leaver.userId!)
        );

        deployedWorkflowId = (await deployHttpClientProject(adminApi, workspaceId, leaverConnectionId)).workflowId;
    });

    test.afterAll(async () => {
        for (const api of [leaverApi, colleagueApi, workspaceAdminApi]) {
            await api?.dispose();
        }

        for (const user of [leaver, colleague, workspaceAdmin]) {
            if (user) {
                await deleteRoleUser(adminApi, user);
            }
        }

        await deleteWorkspace(adminApi, workspaceId);
        await deleteWorkspace(adminApi, secondWorkspaceId);

        await adminApi.dispose();
    });

    test('removing a member moves the connections they own in that workspace to PENDING_REASSIGNMENT and revokes their grants', async () => {
        expect((await getConnection(adminApi, leaverConnectionId)).status).toBe('ACTIVE');

        await removeWorkspaceMember(adminApi, workspaceId, leaver);

        await expect
            .poll(async () => (await getConnection(adminApi, leaverConnectionId)).status, {timeout: 15000})
            .toBe('PENDING_REASSIGNMENT');

        expect((await getConnection(adminApi, leaverSecondWorkspaceConnectionId)).status).toBe('ACTIVE');

        const grants = await fetchConnectionGrants(colleagueApi, workspaceId, colleaguePrivateConnectionId);

        expectNoGraphQlErrors(grants);
        expect(grants.data!.connectionGrants.map(Number)).not.toContain(Number(leaver.userId));
    });

    test('a former member no longer counts as the owner of a connection left in that workspace', async () => {
        expect((await updateConnectionResponse(leaverApi, leaverConnectionId, 'vis_after_leaving')).status()).toBe(403);
    });

    test("a tenant admin lists the removed member's unresolved connections and the workflows they affect", async () => {
        const unresolved = await postGraphQl<{
            unresolvedConnections: {connectionId: string; dependentWorkflowCount: number; visibility: string}[];
        }>(
            adminApi,
            `query Unresolved($workspaceId: ID!, $userLogin: String!) {
                unresolvedConnections(workspaceId: $workspaceId, userLogin: $userLogin) {
                    connectionId visibility dependentWorkflowCount
                }
            }`,
            {userLogin: leaver.login, workspaceId: String(workspaceId)}
        );

        expectNoGraphQlErrors(unresolved);
        expect(unresolved.data!.unresolvedConnections).toEqual([
            {connectionId: String(leaverConnectionId), dependentWorkflowCount: 1, visibility: 'WORKSPACE'},
        ]);

        const affected = await postGraphQl<{affectedWorkflows: {connectionIds: string[]; workflowId: string}[]}>(
            adminApi,
            `query Affected($workspaceId: ID!, $userLogin: String!) {
                affectedWorkflows(workspaceId: $workspaceId, userLogin: $userLogin) { workflowId connectionIds }
            }`,
            {userLogin: leaver.login, workspaceId: String(workspaceId)}
        );

        expectNoGraphQlErrors(affected);
        expect(affected.data!.affectedWorkflows).toEqual([
            {connectionIds: [String(leaverConnectionId)], workflowId: deployedWorkflowId},
        ]);
    });

    test('a workspace admin who is not a tenant admin cannot read or run the reassignment operations', async () => {
        expectAccessDenied(
            await postGraphQl(
                workspaceAdminApi,
                `query Unresolved($workspaceId: ID!, $userLogin: String!) {
                    unresolvedConnections(workspaceId: $workspaceId, userLogin: $userLogin) { connectionId }
                }`,
                {userLogin: leaver.login, workspaceId: String(workspaceId)}
            )
        );

        expectAccessDenied(await markConnectionsPendingReassignment(workspaceAdminApi, workspaceId, leaver.login));

        expectAccessDenied(
            await reassignConnection(workspaceAdminApi, workspaceId, leaverConnectionId, workspaceAdmin.login)
        );
    });

    test('marking a user whose connections are already pending reports them as skipped, not failed', async () => {
        const response = await markConnectionsPendingReassignment(adminApi, workspaceId, leaver.login);

        expectNoGraphQlErrors(response);
        expect(response.data!.markConnectionsPendingReassignment).toEqual({
            failed: 0,
            skipped: 1,
            total: 1,
            updated: 0,
        });
    });

    test('reassignment refuses a connection outside the named workspace and an unknown new owner', async () => {
        expectGraphQlError(
            await reassignConnection(adminApi, workspaceId, leaverSecondWorkspaceConnectionId, colleague.login),
            /does not belong to workspace/
        );

        expectGraphQlError(
            await reassignConnection(adminApi, workspaceId, leaverConnectionId, 'vis_no_such_user'),
            /does not exist/
        );

        expect((await getConnection(adminApi, leaverConnectionId)).status).toBe('PENDING_REASSIGNMENT');
    });

    test('reassigning a pending connection hands it to the new owner, an EDITOR, who may then rename it', async () => {
        expectNoGraphQlErrors(await reassignConnection(adminApi, workspaceId, leaverConnectionId, colleague.login));

        expect(await getConnection(adminApi, leaverConnectionId)).toMatchObject({
            createdBy: colleague.login,
            status: 'ACTIVE',
        });

        const renameResponse = await updateConnectionResponse(colleagueApi, leaverConnectionId, 'vis_reassigned');

        expect(renameResponse.status(), await renameResponse.text()).toBe(204);
    });
});
