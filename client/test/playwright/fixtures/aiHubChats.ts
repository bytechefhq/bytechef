import {type Page, type Route, test as base} from '@playwright/test';

import {type AiHubChatRefI, deleteAiHubChatThroughApi, trackCreatedAiHubChats} from '../utils/aiHubUtils';
import {readGraphQlOperationName, stubGraphQlOperation} from '../utils/copilotUtils';

export type AiHubChatsFixturesType = {
    aiHubChatCleanup: void;
};

/**
 * Deletes every AI Hub chat the test's page created through `createAiHubChat`, once the test ends.
 */
export const aiHubChatsTest = base.extend<AiHubChatsFixturesType>({
    aiHubChatCleanup: [
        async ({page}, use) => {
            const {chats} = trackCreatedAiHubChats(page);

            await use();

            for (const chat of chats) {
                await deleteAiHubChatThroughApi(page, chat);
            }
        },
        {auto: true},
    ],
});

export interface ChatWorkflowI {
    projectDeploymentId: string;
    projectId: string;
    projectName: string;
    projectWorkflowId: string;
    workflowExecutionId: string;
    workflowId: string;
    workflowLabel: string;
}

export const workflowChatLabel = (chatWorkflow: ChatWorkflowI) =>
    `${chatWorkflow.projectName} — ${chatWorkflow.workflowLabel}`;

interface GraphQlChatI {
    id: string;
    kind?: string;
    projectDeploymentId?: string | null;
    title?: string | null;
    workflowExecutionId?: string | null;
}

interface StubWorkflowChatsOptionsI {
    chatWorkflows: ChatWorkflowI[];
    workflowChat: AiHubChatRefI;
}

/**
 * Serves workflow chats without a deployed chat workflow. `workspaceChatWorkflows` lists `chatWorkflows`, and
 * `createWorkflowChatAiHubChat` answers with `workflowChat`, a real chat created beforehand: the request is re-sent as
 * an `updateAiHubChat` that stores the picked label as its title, and the answer carries the picked workflow. From
 * then on `aiHubChats` reports that chat as a `WORKFLOW_CHAT`; before it, the chat is left out of the list.
 */
export async function stubWorkflowChats(
    page: Page,
    {chatWorkflows, workflowChat}: StubWorkflowChatsOptionsI
): Promise<{createCalls: Array<Record<string, unknown>>}> {
    const createCalls: Array<Record<string, unknown>> = [];

    const pickedWorkflow = () =>
        chatWorkflows.find((chatWorkflow) => chatWorkflow.workflowExecutionId === createCalls[0]?.workflowExecutionId);

    const toWorkflowChat = (chat: GraphQlChatI): GraphQlChatI => ({
        ...chat,
        kind: 'WORKFLOW_CHAT',
        projectDeploymentId: pickedWorkflow()?.projectDeploymentId ?? null,
        title: createCalls[0]?.title as string,
        workflowExecutionId: pickedWorkflow()?.workflowExecutionId ?? null,
    });

    await stubGraphQlOperation(page, 'workspaceChatWorkflows', () => ({workspaceChatWorkflows: chatWorkflows}));

    await page.route('**/graphql', async (route: Route) => {
        const request = route.request();

        const operationName = request.method() === 'POST' ? readGraphQlOperationName(request) : undefined;

        if (operationName === 'createWorkflowChatAiHubChat') {
            const {variables} = request.postDataJSON() as {variables: Record<string, unknown>};

            createCalls.push(variables);

            const response = await route.fetch({
                postData: JSON.stringify({
                    query: `mutation createWorkflowChatAiHubChat($input: AiHubChatPatchInput!) {
                        createWorkflowChatAiHubChat: updateAiHubChat(input: $input) {
                            id workspaceId userId threadId title lastPreview messageCount status environmentId
                            createdAt updatedAt kind workflowExecutionId projectDeploymentId autoTitled ownerUserId
                            isOwner
                        }
                    }`,
                    variables: {
                        input: {
                            id: String(workflowChat.id),
                            title: variables.title,
                            workspaceId: String(workflowChat.workspaceId),
                        },
                    },
                }),
            });

            const responseBody = (await response.json()) as {data: {createWorkflowChatAiHubChat: GraphQlChatI}};

            await route.fulfill({
                json: {
                    data: {createWorkflowChatAiHubChat: toWorkflowChat(responseBody.data.createWorkflowChatAiHubChat)},
                },
                response,
            });

            return;
        }

        if (operationName === 'aiHubChats') {
            const response = await route.fetch();

            const responseBody = (await response.json()) as {data?: {aiHubChats?: GraphQlChatI[] | null}};

            const chats = responseBody.data?.aiHubChats ?? [];

            const workflowChatId = String(workflowChat.id);

            await route.fulfill({
                json: {
                    ...responseBody,
                    data: {
                        aiHubChats:
                            createCalls.length === 0
                                ? chats.filter((chat) => chat.id !== workflowChatId)
                                : chats.map((chat) => (chat.id === workflowChatId ? toWorkflowChat(chat) : chat)),
                    },
                },
                response,
            });

            return;
        }

        await route.fallback();
    });

    return {createCalls};
}
