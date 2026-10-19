import {type Page, type Route, expect, mergeTests} from '@playwright/test';

import {aiHubChatsTest, loginTest} from '../../fixtures';
import {AiHubPage, PENDING_APPROVAL_PLACEHOLDER, SEND_A_MESSAGE_PLACEHOLDER} from '../../pages/aiHubPage';
import {AI_HUB_CHAT_URL, completedTurn, stubAgUiRun, textMessage, toolCall} from '../../utils/agUiStreamStub';
import {
    type ToolApprovalRequestI,
    createAiHubChatThroughApi,
    deleteAiHubChatThroughApi,
    postGraphQl,
    prepareAiHub,
    readAppEnvironmentId,
    stubAiHubAttach,
    toolApprovalRequestResult,
    waitForHubWorkspaceId,
} from '../../utils/aiHubUtils';
import {readGraphQlOperationName, stubGraphQlOperation} from '../../utils/copilotUtils';

export const test = mergeTests(loginTest(), aiHubChatsTest);

const TOOL_APPROVALS_SETTINGS_PATH = '/automation/settings/ai-hub/tool-approvals';

const DELETE_PROJECT_APPROVAL: ToolApprovalRequestI = {
    approvalId: 201,
    arguments: {projectId: 5},
    toolName: 'deleteProject',
};

const deleteProjectApprovalTurn = () =>
    completedTurn(
        toolCall('deleteProject', DELETE_PROJECT_APPROVAL.arguments, toolApprovalRequestResult(DELETE_PROJECT_APPROVAL))
    );

interface ResolveCallI {
    approvalId: string;
    approved: boolean;
    comment?: string;
}

async function stubResolveToolApproval(
    page: Page,
    {continuationStarted}: {continuationStarted: boolean}
): Promise<ResolveCallI[]> {
    const resolveCalls: ResolveCallI[] = [];

    await page.route('**/graphql', async (route: Route) => {
        const request = route.request();

        if (request.method() !== 'POST' || readGraphQlOperationName(request) !== 'resolveAiHubToolApproval') {
            await route.fallback();

            return;
        }

        const {variables} = request.postDataJSON() as {variables: ResolveCallI};

        resolveCalls.push(variables);

        await route.fulfill({
            json: {
                data: {
                    resolveAiHubToolApproval: {
                        approval: {
                            executionError: null,
                            id: variables.approvalId,
                            status: variables.approved ? 'APPROVED' : 'REJECTED',
                        },
                        continuationStarted,
                        runId: continuationStarted ? 'continuation-run' : null,
                    },
                },
            },
        });
    });

    return resolveCalls;
}

test.describe('AI Hub tool approvals', () => {
    test('a gated call renders an approval card and nothing runs until it is decided', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [deleteProjectApprovalTurn()]);

        const resolveCalls = await stubResolveToolApproval(page, {continuationStarted: true});

        const {attachRequests} = await stubAiHubAttach(page, []);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

        await expect(page.getByText('Tool approval required')).toBeVisible();

        await expect(page.getByText('deleteProject', {exact: true})).toBeVisible();

        await expect(page.getByRole('button', {name: 'Approve'})).toBeEnabled();

        await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', PENDING_APPROVAL_PLACEHOLDER);

        expect(resolveCalls).toEqual([]);

        expect(attachRequests).toEqual([]);
    });

    test('approving runs the tool and streams the continuation into the chat', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [deleteProjectApprovalTurn()]);

        const resolveCalls = await stubResolveToolApproval(page, {continuationStarted: true});

        const {attachRequests} = await stubAiHubAttach(page, [
            completedTurn(textMessage('The Demo project is deleted.')),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

        await page.getByLabel('Comment (optional)').fill('Go ahead');

        await page.getByRole('button', {name: 'Approve'}).click();

        await expect(page.getByText('Approved - the tool ran.')).toBeVisible();

        await expect(page.getByText('The Demo project is deleted.')).toBeVisible();

        await aiHubPage.waitForTurnEnd();

        expect(resolveCalls).toEqual([
            expect.objectContaining({
                approvalId: String(DELETE_PROJECT_APPROVAL.approvalId),
                approved: true,
                comment: 'Go ahead',
            }),
        ]);

        expect(attachRequests).toHaveLength(1);
    });

    test('rejecting runs nothing and the agent is told', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [deleteProjectApprovalTurn()]);

        const resolveCalls = await stubResolveToolApproval(page, {continuationStarted: true});

        const {attachRequests} = await stubAiHubAttach(page, [
            completedTurn(textMessage('Understood, I left the Demo project alone.')),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

        await page.getByRole('button', {name: 'Reject'}).click();

        await expect(page.getByText('Rejected.')).toBeVisible();

        await expect(page.getByText('Understood, I left the Demo project alone.')).toBeVisible();

        expect(resolveCalls).toEqual([expect.objectContaining({approved: false})]);

        expect(attachRequests).toHaveLength(1);
    });

    test('double-clicking Approve resolves the approval once', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        await stubAgUiRun(page, AI_HUB_CHAT_URL, [deleteProjectApprovalTurn()]);

        const resolveCalls = await stubResolveToolApproval(page, {continuationStarted: false});

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

        await page.getByRole('button', {name: 'Approve'}).dblclick();

        await expect(page.getByText('Approved - the tool ran.')).toBeVisible();

        expect(resolveCalls).toHaveLength(1);
    });

    test('sending a new message withdraws the pending approval', async ({authenticatedPage: page}) => {
        await prepareAiHub(page);

        const hubRun = await stubAgUiRun(page, AI_HUB_CHAT_URL, [
            deleteProjectApprovalTurn(),
            completedTurn(textMessage('Fine, I will not delete it.')),
        ]);

        const aiHubPage = new AiHubPage(page);

        await aiHubPage.goto();

        await aiHubPage.sendAndWaitForTurnEnd('Delete the Demo project');

        await expect(page.getByRole('button', {name: 'Approve'})).toBeVisible();

        await test.step('Send a message instead of deciding', async () => {
            await aiHubPage.sendAndWaitForTurnEnd('Actually, keep it');

            await hubRun.waitForRequest(1);

            await expect(page.getByText('Fine, I will not delete it.')).toBeVisible();
        });

        await test.step('Assert the approval can no longer be decided', async () => {
            await expect(page.getByRole('button', {name: 'Approve'})).toHaveCount(0);

            await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', SEND_A_MESSAGE_PLACEHOLDER);
        });
    });

    test('an approval older than its expiry shows expired after a reload and cannot run', async ({
        authenticatedPage: page,
    }) => {
        await prepareAiHub(page);

        const aiHubPage = new AiHubPage(page);

        const hubWorkspaceId = waitForHubWorkspaceId(page);

        await aiHubPage.goto();

        const chat = await createAiHubChatThroughApi(page, {
            environment: await readAppEnvironmentId(page),
            workspaceId: await hubWorkspaceId,
        });

        const resolveCalls = await stubResolveToolApproval(page, {continuationStarted: false});

        await stubGraphQlOperation(page, 'aiHubChatMessages', () => ({
            aiHubChatMessages: [
                {
                    content: 'Delete the Demo project',
                    id: 'row-delete-request',
                    role: 'user',
                    timestamp: Date.now() - 2 * 86400000,
                    toolEventsJson: JSON.stringify([
                        {
                            arguments: JSON.stringify(DELETE_PROJECT_APPROVAL.arguments),
                            id: 'call-delete-project',
                            kind: 'call',
                            name: 'deleteProject',
                        },
                        {
                            id: 'call-delete-project',
                            kind: 'result',
                            name: 'deleteProject',
                            response: toolApprovalRequestResult(DELETE_PROJECT_APPROVAL),
                        },
                    ]),
                },
            ],
        }));

        await stubGraphQlOperation(page, 'aiHubToolApprovals', () => ({
            aiHubToolApprovals: [
                {
                    arguments: JSON.stringify(DELETE_PROJECT_APPROVAL.arguments),
                    chatId: String(chat.id),
                    comment: null,
                    componentName: null,
                    decidedAt: null,
                    decidedByUserId: null,
                    executionError: null,
                    expiresAt: Date.now() - 86400000,
                    id: String(DELETE_PROJECT_APPROVAL.approvalId),
                    status: 'PENDING',
                    toolName: 'deleteProject',
                },
            ],
        }));

        try {
            await aiHubPage.gotoChat(chat.id);

            await expect(page.getByText('Tool approval required')).toBeVisible();

            await expect(page.getByText('Expired.')).toBeVisible();

            await expect(page.getByRole('button', {name: 'Approve'})).toHaveCount(0);

            await expect(aiHubPage.composerInput).toHaveAttribute('placeholder', SEND_A_MESSAGE_PLACEHOLDER);

            expect(resolveCalls).toEqual([]);
        } finally {
            await deleteAiHubChatThroughApi(page, chat);
        }
    });

    test.describe('Settings > AI Hub > Tool approvals', () => {
        interface ToolApprovalRuleI {
            id: string;
            mode: string;
            toolKind: string;
            toolName: string;
        }

        const readRules = async (page: Page, workspaceId: number) =>
            (
                await postGraphQl<{aiHubToolApprovalRules: ToolApprovalRuleI[]}>(
                    page,
                    'query aiHubToolApprovalRules($workspaceId: ID!) { aiHubToolApprovalRules(workspaceId: $workspaceId) { id toolKind toolName mode } }',
                    {workspaceId: String(workspaceId)}
                )
            ).aiHubToolApprovalRules;

        const openToolApprovalsSettings = async (page: Page) => {
            const rulesRequest = page.waitForRequest(
                (request) =>
                    request.method() === 'POST' && readGraphQlOperationName(request) === 'aiHubToolApprovalRules'
            );

            await page.goto(TOOL_APPROVALS_SETTINGS_PATH);

            const workspaceId = Number(
                ((await rulesRequest).postDataJSON() as {variables: {workspaceId: string}}).variables.workspaceId
            );

            await expect(page.getByText('Workspace rules')).toBeVisible();

            return workspaceId;
        };

        test('an admin exempts deleteProject and requires publishProject, then removes both rules', async ({
            authenticatedPage: page,
        }) => {
            await prepareAiHub(page);

            const workspaceId = await openToolApprovalsSettings(page);

            const existingRules = await readRules(page, workspaceId);

            test.skip(
                existingRules.some((rule) => ['deleteProject', 'publishProject'].includes(rule.toolName)),
                'The workspace already holds a deleteProject or publishProject rule; this test leaves existing rules alone'
            );

            const deleteProjectRow = page
                .getByRole('row')
                .filter({has: page.getByText('deleteProject', {exact: true})});
            const workspaceRulesCard = page.locator('[data-slot="card"]').filter({hasText: 'Workspace rules'});

            await test.step('Exempt the built-in deleteProject gate', async () => {
                await deleteProjectRow.getByRole('switch', {name: 'Exempt'}).click();

                await expect(deleteProjectRow.getByRole('switch', {name: 'Exempt'})).toBeChecked();

                await expect
                    .poll(async () => (await readRules(page, workspaceId)).map((rule) => [rule.toolName, rule.mode]))
                    .toContainEqual(['deleteProject', 'EXEMPT']);
            });

            await test.step('Require approval for publishProject', async () => {
                await page.getByRole('button', {name: 'Add rule'}).click();

                const addRuleDialog = page.getByRole('dialog', {name: 'Add rule'});

                await addRuleDialog.getByLabel('Tool name').fill('publishProject');

                await addRuleDialog.getByRole('combobox', {name: 'Mode'}).click();

                await page.getByRole('option', {name: 'Require approval'}).click();

                await addRuleDialog.getByRole('button', {name: 'Save'}).click();

                const publishProjectRow = workspaceRulesCard
                    .getByRole('row')
                    .filter({has: page.getByText('publishProject', {exact: true})});

                await expect(publishProjectRow).toContainText('Require');
            });

            await test.step('Remove both rules', async () => {
                await workspaceRulesCard
                    .getByRole('row')
                    .filter({has: page.getByText('publishProject', {exact: true})})
                    .getByRole('button', {name: 'Delete'})
                    .click();

                await deleteProjectRow.getByRole('switch', {name: 'Exempt'}).click();

                await expect(deleteProjectRow.getByRole('switch', {name: 'Exempt'})).not.toBeChecked();

                await expect
                    .poll(async () =>
                        (await readRules(page, workspaceId)).filter((rule) =>
                            ['deleteProject', 'publishProject'].includes(rule.toolName)
                        )
                    )
                    .toEqual([]);
            });
        });

        test('a member who is not a tenant admin sees the rules read-only', async ({authenticatedPage: page}) => {
            await prepareAiHub(page);

            await page.route('**/api/account', async (route: Route) => {
                const response = await route.fetch();

                const account = (await response.json()) as {authorities?: string[]};

                await route.fulfill({
                    json: {
                        ...account,
                        authorities: (account.authorities ?? []).filter((authority) => authority !== 'ROLE_ADMIN'),
                    },
                    response,
                });
            });

            await openToolApprovalsSettings(page);

            await expect(page.getByText('Built-in').first()).toBeVisible();

            await expect(page.getByRole('button', {name: 'Add rule'})).toHaveCount(0);

            await expect(page.getByRole('button', {exact: true, name: 'Delete'})).toHaveCount(0);

            const exemptSwitches = page.getByRole('switch', {name: 'Exempt'});

            await expect(exemptSwitches.first()).toBeVisible();

            for (const exemptSwitch of await exemptSwitches.all()) {
                await expect(exemptSwitch).toBeDisabled();
            }
        });
    });
});
