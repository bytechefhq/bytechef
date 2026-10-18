import {AiHubChatStatus} from '@/shared/middleware/graphql-types';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    createAiHubChat,
    generateAiHubChatTitle,
    getChatArtifacts,
    getChatDisplayTitle,
    getChatMessages,
    getToolApprovals,
    patchChat,
    toChat,
} from '../chats.api';

const graphQlChat = {
    autoTitled: false,
    createdAt: '1716000000000',
    environmentId: '0',
    id: '101',
    isOwner: true,
    lastPreview: null,
    messageCount: 5,
    ownerUserId: '3',
    status: AiHubChatStatus.Active,
    threadId: 'thread-1',
    title: 'Hello',
    updatedAt: '1716000060000',
    userId: '3',
    workspaceId: '7',
};

function mockGraphQlResponse(data: Record<string, unknown>) {
    return vi
        .spyOn(globalThis, 'fetch')
        .mockResolvedValue(
            new Response(JSON.stringify({data}), {headers: {'content-type': 'application/json'}, status: 200})
        );
}

function requestBody(fetchSpy: ReturnType<typeof mockGraphQlResponse>) {
    return JSON.parse(fetchSpy.mock.calls[0]![1]?.body as string);
}

describe('chats.api', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('createAiHubChat sends GraphQL variables matching the createAiHubChat mutation', async () => {
        const fetchSpy = mockGraphQlResponse({createAiHubChat: {...graphQlChat, threadId: 'thread-x'}});

        const result = await createAiHubChat({environment: 0, threadId: 'thread-x', workspaceId: 42});

        expect(fetchSpy).toHaveBeenCalledTimes(1);

        const body = requestBody(fetchSpy);

        expect(body.query).toContain('createAiHubChat');
        expect(body.variables).toEqual({environment: 0, threadId: 'thread-x', workspaceId: '42'});
        expect(result.id).toBe(101);
        expect(result.threadId).toBe('thread-x');
    });

    it('generateAiHubChatTitle calls the generateAiHubChatTitle mutation with id+workspaceId', async () => {
        const fetchSpy = mockGraphQlResponse({generateAiHubChatTitle: {...graphQlChat, title: 'Generated'}});

        const result = await generateAiHubChatTitle({chatId: 7, workspaceId: 42});

        const body = requestBody(fetchSpy);

        expect(body.query).toContain('generateAiHubChatTitle');
        expect(body.variables).toEqual({id: '7', workspaceId: '42'});
        expect(result.title).toBe('Generated');
    });

    it('generateAiHubChatTitle throws with the server message when the upstream model is unavailable', async () => {
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            new Response(JSON.stringify({errors: [{message: 'Title generation is temporarily unavailable'}]}), {
                status: 200,
            })
        );

        await expect(generateAiHubChatTitle({chatId: 7, workspaceId: 42})).rejects.toThrow(
            /Title generation is temporarily unavailable/
        );
    });

    it('patchChat sends the patch as the updateAiHubChat input', async () => {
        const fetchSpy = mockGraphQlResponse({updateAiHubChat: graphQlChat});

        await patchChat({chatId: 101, patch: {lastPreview: 'hi', messageCount: 2}, workspaceId: 7});

        expect(requestBody(fetchSpy).variables).toEqual({
            input: {id: '101', lastPreview: 'hi', messageCount: 2, workspaceId: '7'},
        });
    });

    it('getChatMessages maps the timestamp and tool events', async () => {
        mockGraphQlResponse({
            aiHubChatMessages: [{content: 'hi', role: 'USER', timestamp: '1716000000000', toolEventsJson: null}],
        });

        const messages = await getChatMessages({chatId: 101, workspaceId: 7});

        expect(messages).toEqual([
            {content: 'hi', role: 'USER', timestamp: new Date(1716000000000).toISOString(), toolEventsJson: null},
        ]);
    });

    it('getChatArtifacts maps ids to numbers', async () => {
        mockGraphQlResponse({
            aiHubChatArtifactsByAiHubChat: [
                {
                    artifactId: 'wf-1',
                    artifactName: 'Workflow',
                    chatId: '101',
                    createdAt: '1716000000000',
                    environmentId: '0',
                    id: '9',
                    kind: 'WORKFLOW_CREATED',
                    metadataJson: null,
                    status: 'APPLIED',
                    statusChangedAt: null,
                },
            ],
        });

        const artifacts = await getChatArtifacts({chatId: 101, workspaceId: 7});

        expect(artifacts[0]).toMatchObject({chatId: 101, id: 9, kind: 'WORKFLOW_CREATED', status: 'APPLIED'});
    });

    it('getToolApprovals maps the approval rows', async () => {
        mockGraphQlResponse({
            aiHubToolApprovals: [
                {
                    chatId: '101',
                    comment: null,
                    componentName: null,
                    decidedAt: null,
                    decidedByUserId: '3',
                    executionError: null,
                    expiresAt: null,
                    id: '4',
                    status: 'APPROVED',
                    toolName: 'deleteWorkflow',
                },
            ],
        });

        const approvals = await getToolApprovals({chatId: 101, workspaceId: 7});

        expect(approvals).toEqual([
            {
                componentName: null,
                decidedByUserId: 3,
                executionError: null,
                id: 4,
                status: 'APPROVED',
                toolName: 'deleteWorkflow',
            },
        ]);
    });
});

describe('toChat', () => {
    it('maps the GraphQL chat to the domain shape', () => {
        expect(toChat(graphQlChat)).toEqual({
            autoTitled: false,
            createdAt: new Date(1716000000000).toISOString(),
            environmentId: 0,
            id: 101,
            isOwner: true,
            lastPreview: null,
            messageCount: 5,
            ownerUserId: 3,
            status: 'ACTIVE',
            threadId: 'thread-1',
            title: 'Hello',
            updatedAt: new Date(1716000060000).toISOString(),
            userId: 3,
            workspaceId: 7,
        });
    });
});

describe('getChatDisplayTitle', () => {
    it('returns the stored title when present', () => {
        expect(getChatDisplayTitle({title: 'Custom title'})).toBe('Custom title');
    });

    it('falls back to "New Chat" for an untitled chat', () => {
        expect(getChatDisplayTitle({title: null})).toBe('New Chat');
    });
});
