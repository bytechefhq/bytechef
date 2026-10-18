import {type BrowserContext, type Page, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {
    type AuditEventI,
    auditEventData,
    filterAuditEvents,
    openAuditEventRowWithDataEntry,
    openAuditEventsPage,
    queryAuditEvents,
    selectAuditEventType,
} from '../../utils/auditUtils';
import {
    RBAC_BASE_URL,
    type RbacWorkspaceI,
    type RoleUserI,
    createAccount,
    createRbacWorkspace,
    deleteRbacWorkspace,
    deleteRoleUser,
    fetchUserId,
    openAsTenantAdmin,
    postGraphQl,
} from '../../utils/rbacUtils';

const ADD_WORKSPACE_USER = `
    mutation AddWorkspaceUser($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole) {
        addWorkspaceUser(workspaceId: $workspaceId, userId: $userId, role: $role) {
            userId
        }
    }
`;

const UPDATE_WORKSPACE_USER_ROLE = `
    mutation UpdateWorkspaceUserRole($workspaceId: ID!, $userId: ID!, $role: WorkspaceRole!) {
        updateWorkspaceUserRole(workspaceId: $workspaceId, userId: $userId, role: $role) {
            userId
        }
    }
`;

const REMOVE_WORKSPACE_USER = `
    mutation RemoveWorkspaceUser($workspaceId: ID!, $userId: ID!) {
        removeWorkspaceUser(workspaceId: $workspaceId, userId: $userId)
    }
`;

async function findMembershipEvent(
    page: Page,
    eventType: string,
    workspace: RbacWorkspaceI,
    userId: string,
    outcome = 'SUCCESS'
): Promise<AuditEventI> {
    let found: AuditEventI | undefined;

    await expect(async () => {
        const auditEvents = await queryAuditEvents(page, {eventType, principal: 'admin', size: 200});

        found = auditEvents.content.find((auditEvent) => {
            const data = auditEventData(auditEvent);

            return (
                data.workspaceId === String(workspace.id) && data.userId === userId && auditEvent.outcome === outcome
            );
        });

        expect(found, `no ${eventType} (${outcome}) for user ${userId} in ${workspace.id}`).toBeTruthy();
    }).toPass({timeout: 30000});

    return found!;
}

async function countMembershipEvents(
    page: Page,
    eventType: string,
    workspace: RbacWorkspaceI,
    userId: string
): Promise<number> {
    const auditEvents = await queryAuditEvents(page, {eventType, principal: 'admin', size: 200});

    return auditEvents.content.filter((auditEvent) => {
        const data = auditEventData(auditEvent);

        return data.workspaceId === String(workspace.id) && data.userId === userId;
    }).length;
}

async function expectRowOnAuditEventsPage(page: Page, eventType: string, workspace: RbacWorkspaceI, outcome: string) {
    await openAuditEventsPage(page);

    await selectAuditEventType(page, eventType);
    await filterAuditEvents(page, {principal: 'admin'});

    await openAuditEventRowWithDataEntry(page, 'workspaceId', String(workspace.id));

    const dialog = page.getByRole('dialog');

    await expect(dialog).toContainText(eventType);
    await expect(dialog).toContainText('admin');
    await expect(dialog).toContainText(outcome);

    await page.keyboard.press('Escape');
}

test.describe('Audit Events workspace membership', () => {
    test.describe.configure({mode: 'serial'});

    let account: RoleUserI;
    let context: BrowserContext;
    let page: Page;
    let workspace: RbacWorkspaceI;

    test.beforeAll(async ({adminApi, browser}) => {
        workspace = await createRbacWorkspace(adminApi);
        account = await createAccount(adminApi, RBAC_BASE_URL);

        ({context, page} = await openAsTenantAdmin(browser, RBAC_BASE_URL, workspace));
    });

    test.afterAll(async ({adminApi}) => {
        await context?.close();

        await deleteRoleUser(adminApi, account);
        await deleteRbacWorkspace(adminApi, workspace);
    });

    test('adding a member records WORKSPACE_USER_ADDED with the actor, workspace, user and role', async ({
        adminApi,
    }) => {
        const response = await postGraphQl(adminApi, ADD_WORKSPACE_USER, {
            role: 'EDITOR',
            userId: account.userId,
            workspaceId: String(workspace.id),
        });

        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();

        const auditEvent = await findMembershipEvent(page, 'WORKSPACE_USER_ADDED', workspace, account.userId!);

        expect(auditEvent.principal).toBe('admin');
        expect(auditEventData(auditEvent)).toMatchObject({
            role: 'EDITOR',
            userId: account.userId,
            workspaceId: String(workspace.id),
        });
        expect(auditEventData(auditEvent).previousRole).toBeUndefined();

        expect(await countMembershipEvents(page, 'WORKSPACE_USER_ADDED', workspace, account.userId!)).toBe(1);

        await expectRowOnAuditEventsPage(page, 'WORKSPACE_USER_ADDED', workspace, 'SUCCESS');
    });

    test('adding someone who is already a member is refused and recorded as ERROR', async ({adminApi}) => {
        const response = await postGraphQl(adminApi, ADD_WORKSPACE_USER, {
            role: 'VIEWER',
            userId: account.userId,
            workspaceId: String(workspace.id),
        });

        expect(response.errors?.[0]?.message).toContain('is already a member of workspace');

        const auditEvent = await findMembershipEvent(page, 'WORKSPACE_USER_ADDED', workspace, account.userId!, 'ERROR');

        expect(auditEvent.principal).toBe('admin');
        expect(auditEventData(auditEvent)).toMatchObject({
            errorClass: expect.stringContaining('ConfigurationException'),
            role: 'VIEWER',
        });
    });

    test('changing a role records WORKSPACE_USER_ROLE_UPDATED with the new and previous role', async ({adminApi}) => {
        const response = await postGraphQl(adminApi, UPDATE_WORKSPACE_USER_ROLE, {
            role: 'VIEWER',
            userId: account.userId,
            workspaceId: String(workspace.id),
        });

        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();

        const auditEvent = await findMembershipEvent(page, 'WORKSPACE_USER_ROLE_UPDATED', workspace, account.userId!);

        expect(auditEvent.principal).toBe('admin');
        expect(auditEventData(auditEvent)).toMatchObject({
            previousRole: 'EDITOR',
            role: 'VIEWER',
            userId: account.userId,
            workspaceId: String(workspace.id),
        });

        await expectRowOnAuditEventsPage(page, 'WORKSPACE_USER_ROLE_UPDATED', workspace, 'SUCCESS');
    });

    test('removing a member records WORKSPACE_USER_REMOVED with their previous role', async ({adminApi}) => {
        const response = await postGraphQl(adminApi, REMOVE_WORKSPACE_USER, {
            userId: account.userId,
            workspaceId: String(workspace.id),
        });

        expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();

        const auditEvent = await findMembershipEvent(page, 'WORKSPACE_USER_REMOVED', workspace, account.userId!);

        expect(auditEvent.principal).toBe('admin');
        expect(auditEventData(auditEvent)).toMatchObject({
            previousRole: 'VIEWER',
            userId: account.userId,
            workspaceId: String(workspace.id),
        });

        await expectRowOnAuditEventsPage(page, 'WORKSPACE_USER_REMOVED', workspace, 'SUCCESS');
    });

    test('removing the last admin is refused and recorded as ERROR with the previous role', async ({adminApi}) => {
        const adminUserId = await fetchUserId(adminApi, 'admin');

        const response = await postGraphQl(adminApi, REMOVE_WORKSPACE_USER, {
            userId: adminUserId,
            workspaceId: String(workspace.id),
        });

        expect(response.errors?.[0]?.message).toContain('Cannot remove or demote the last admin');

        const auditEvent = await findMembershipEvent(page, 'WORKSPACE_USER_REMOVED', workspace, adminUserId, 'ERROR');

        expect(auditEventData(auditEvent)).toMatchObject({
            errorClass: expect.stringContaining('ConfigurationException'),
            previousRole: 'ADMIN',
        });

        await expectRowOnAuditEventsPage(page, 'WORKSPACE_USER_REMOVED', workspace, 'ERROR');
    });
});
