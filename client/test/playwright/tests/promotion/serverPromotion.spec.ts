import {type Page, expect} from '@playwright/test';

import {loginTest, mergeTests, projectTest} from '../../fixtures';
import {DEVELOPMENT_ENVIRONMENT_ID} from '../../utils/apiUtils';
import {getProjectWorkspaceId} from '../../utils/deploymentUtils';
import getRandomString from '../../utils/getRandomString';
import {
    type PromotionResourceType,
    STAGING_ENVIRONMENT_ID,
    STAGING_LABEL,
    createA2aServer,
    createMcpComponentWithTools,
    createWorkspaceMcpServer,
    deleteA2aServerQuietly,
    deleteWorkspaceMcpServerQuietly,
    getCurrentEnvironmentId,
    getMcpServerComponent,
    getPromotionPreview,
    goToNextPromotionStep,
    openPromotionDialog,
    promoteAndWaitForToast,
    promoteViaApi,
    setCurrentEnvironment,
    setMcpComponentTools,
    skipUnlessPromotionIsAvailable,
} from '../../utils/promotionUtils';

const NEW_URL_WARNING = 'A new server URL and secret key will be generated for the promoted server.';

const test = mergeTests(loginTest(), projectTest);

interface ServerCaseI {
    create: (page: Page, workspaceId: number, name: string) => Promise<string>;
    delete: (page: Page, serverId: string) => Promise<void>;
    listPath: string;
    resourceType: PromotionResourceType;
    title: string;
}

const SERVER_CASES: ServerCaseI[] = [
    {
        create: createWorkspaceMcpServer,
        delete: deleteWorkspaceMcpServerQuietly,
        listPath: '/automation/mcp-servers',
        resourceType: 'MCP_SERVER',
        title: 'MCP server',
    },
    {
        create: createA2aServer,
        delete: deleteA2aServerQuietly,
        listPath: '/automation/a2a-servers',
        resourceType: 'A2A_SERVER',
        title: 'A2A server',
    },
];

for (const serverCase of SERVER_CASES) {
    test.describe(`${serverCase.title} promotion`, () => {
        const serverIds: string[] = [];

        test.beforeEach(async ({authenticatedPage}) => {
            await skipUnlessPromotionIsAvailable(authenticatedPage);

            await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
        });

        test.afterEach(async ({authenticatedPage}) => {
            for (const serverId of serverIds.splice(0)) {
                await serverCase.delete(authenticatedPage, serverId);
            }

            await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
        });

        test('mints a new URL on the first promotion and keeps it on the next', async ({
            authenticatedPage: page,
            project,
        }) => {
            const workspaceId = await getProjectWorkspaceId(page, project.id);

            const serverName = `server_${getRandomString()}`;

            const developmentServerId = await serverCase.create(page, workspaceId, serverName);

            serverIds.push(developmentServerId);

            await page.goto(serverCase.listPath);

            const createDialog = await openPromotionDialog(page, serverName);

            await expect(createDialog.getByText('Will create')).toBeVisible();

            await goToNextPromotionStep(createDialog);

            await expect(createDialog.getByText(NEW_URL_WARNING)).toBeVisible();

            const createToast = await promoteAndWaitForToast(page, createDialog);

            const preview = await getPromotionPreview(page, serverCase.resourceType, developmentServerId);

            const stagingServerId = preview.existingTargetId!;

            serverIds.unshift(stagingServerId);

            await expect(createToast).toContainText(`was created disabled in ${STAGING_LABEL}`);
            await expect(createToast).toContainText('New URL: ');

            await page.reload();

            const updateDialog = await openPromotionDialog(page, serverName);

            await expect(updateDialog.getByText(`Will update ${preview.existingTargetName}`)).toBeVisible();

            await goToNextPromotionStep(updateDialog);

            await expect(updateDialog.getByText(NEW_URL_WARNING)).toBeHidden();

            const updateToast = await promoteAndWaitForToast(page, updateDialog);

            await expect(updateToast).toContainText('was updated');
            await expect(updateToast).not.toContainText('New URL');

            expect(
                (await getPromotionPreview(page, serverCase.resourceType, developmentServerId)).existingTargetId
            ).toBe(stagingServerId);

            await updateToast.getByRole('button', {name: `View in ${STAGING_LABEL}`}).click();

            await expect(page).toHaveURL(new RegExp(`${serverCase.listPath}$`));

            expect(await getCurrentEnvironmentId(page)).toBe(STAGING_ENVIRONMENT_ID);

            await expect(page.getByText(serverName, {exact: true})).toBeVisible();
        });
    });
}

test.describe('MCP server promotion deletes', () => {
    const serverIds: string[] = [];

    test.beforeEach(async ({authenticatedPage}) => {
        await skipUnlessPromotionIsAvailable(authenticatedPage);

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test.afterEach(async ({authenticatedPage}) => {
        for (const serverId of serverIds.splice(0)) {
            await deleteWorkspaceMcpServerQuietly(authenticatedPage, serverId);
        }

        await setCurrentEnvironment(authenticatedPage, DEVELOPMENT_ENVIRONMENT_ID);
    });

    test('shows the deleted tool and requires acknowledgement', async ({authenticatedPage: page, project}) => {
        const workspaceId = await getProjectWorkspaceId(page, project.id);

        const serverName = `server_${getRandomString()}`;

        const keptToolName = 'keptTool';
        const extraToolName = 'extraTool';

        const developmentServerId = await createWorkspaceMcpServer(page, workspaceId, serverName);

        serverIds.push(developmentServerId);

        await createMcpComponentWithTools(page, developmentServerId, [keptToolName]);

        await promoteViaApi(page, 'MCP_SERVER', developmentServerId);

        const preview = await getPromotionPreview(page, 'MCP_SERVER', developmentServerId);

        const stagingServerId = preview.existingTargetId!;

        serverIds.unshift(stagingServerId);

        const stagingComponent = await getMcpServerComponent(page, stagingServerId);

        await setMcpComponentTools(page, stagingServerId, stagingComponent, [keptToolName, extraToolName]);

        await page.goto('/automation/mcp-servers');

        const dialog = await openPromotionDialog(page, serverName);

        await goToNextPromotionStep(dialog);

        await expect(dialog.getByText('Will delete')).toBeVisible();
        await expect(dialog.getByText(extraToolName, {exact: true})).toBeVisible();

        await goToNextPromotionStep(dialog);

        const promoteButton = dialog.getByRole('button', {exact: true, name: 'Promote'});

        await expect(promoteButton).toBeDisabled();

        await dialog.getByRole('checkbox', {name: /I understand \d+ change\(s\) delete or overwrite/}).check();

        await expect(promoteButton).toBeEnabled();

        await promoteButton.click();

        await expect(dialog).toBeHidden({timeout: 30000});

        const refreshedComponent = await getMcpServerComponent(page, stagingServerId);

        expect(refreshedComponent.mcpTools.map((mcpTool) => mcpTool.name)).toEqual([keptToolName]);
    });
});
