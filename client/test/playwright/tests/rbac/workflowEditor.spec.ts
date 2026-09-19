import {type Page, type Request, expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {RBAC_BASE_URL, type RbacWorkspaceI, openAsRoleUser, requestWorkflowUpdateAs} from '../../utils/rbacUtils';

const SAMPLE_WORKFLOW_NODE_NAME = 'var_1';

function isWriteRequest(request: Request): boolean {
    const url = request.url();

    if (!url.includes('/api/') && !url.includes('/graphql')) {
        return false;
    }

    if (url.includes('/graphql')) {
        return (request.postData() || '').includes('mutation');
    }

    return request.method() !== 'GET';
}

async function openWorkflowEditor(page: Page, rbacWorkspace: RbacWorkspaceI) {
    await page.goto(
        `/automation/projects/${rbacWorkspace.projectId}/project-workflows/${rbacWorkspace.projectWorkflowId}`
    );

    await expect(page.getByLabel(`${SAMPLE_WORKFLOW_NODE_NAME} node`, {exact: true})).toBeVisible({timeout: 30000});
}

test.describe('Workflow editor as a VIEWER', () => {
    test('opens read-only and sends no writes', async ({browser, rbacWorkspace, roleUsers}) => {
        const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUsers.VIEWER, rbacWorkspace);

        const writeRequests: string[] = [];

        page.on('request', (request) => {
            if (isWriteRequest(request)) {
                writeRequests.push(`${request.method()} ${request.url()}`);
            }
        });

        try {
            await openWorkflowEditor(page, rbacWorkspace);

            await test.step('shows the view only badge and hides authoring controls', async () => {
                await expect(page.getByRole('status', {name: 'View only'})).toBeVisible();

                await expect(page.getByRole('button', {exact: true, name: 'Test'})).toHaveCount(0);
                await expect(page.getByRole('button', {name: 'Components & Flow Controls'})).toHaveCount(0);
                await expect(page.getByTitle('Click to add a node')).toHaveCount(0);
            });

            await test.step('offers only Info in the node menu', async () => {
                await page.getByLabel(`${SAMPLE_WORKFLOW_NODE_NAME} node`, {exact: true}).hover();

                await page.getByRole('button', {name: `${SAMPLE_WORKFLOW_NODE_NAME} node actions`}).click();

                await expect(page.getByRole('menuitem', {name: 'Info'})).toBeVisible();
                await expect(page.getByRole('menuitem', {name: /Delete|Cut|Copy|Rename/})).toHaveCount(0);

                await page.keyboard.press('Escape');
            });

            await test.step('shows the node properties disabled', async () => {
                await page.getByLabel(`${SAMPLE_WORKFLOW_NODE_NAME} node`, {exact: true}).click();

                const configurationPanel = page.locator(
                    `[aria-label="${SAMPLE_WORKFLOW_NODE_NAME} component configuration panel"]`
                );

                await expect(configurationPanel).toBeVisible({timeout: 20000});

                const versionSelect = configurationPanel.getByRole('combobox', {name: 'Component version'});

                if ((await versionSelect.count()) > 0) {
                    await expect(versionSelect).toBeDisabled();
                }

                await expect(configurationPanel.locator('fieldset[disabled]').first()).toBeAttached();

                await page.waitForTimeout(1500);
            });

            await test.step('sent no write request while browsing', async () => {
                expect(writeRequests).toEqual([]);
            });

            await test.step('is refused when saving the workflow directly', async () => {
                expect(await requestWorkflowUpdateAs(page, rbacWorkspace.workflowId)).toBe(403);
            });
        } finally {
            await context.close();
        }
    });
});

test.describe('Workflow editor as an EDITOR', () => {
    test('offers authoring controls and may save', async ({browser, rbacWorkspace, roleUsers}) => {
        const {context, page} = await openAsRoleUser(browser, RBAC_BASE_URL, roleUsers.EDITOR, rbacWorkspace);

        try {
            await openWorkflowEditor(page, rbacWorkspace);

            await expect(page.getByRole('status', {name: 'View only'})).toHaveCount(0);
            await expect(page.getByRole('button', {name: 'Components & Flow Controls'})).toBeVisible();

            expect(await requestWorkflowUpdateAs(page, rbacWorkspace.workflowId)).toBe(200);
        } finally {
            await context.close();
        }
    });
});
