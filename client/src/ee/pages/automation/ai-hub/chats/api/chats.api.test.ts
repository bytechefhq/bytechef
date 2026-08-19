import {beforeEach, describe, expect, it, vi} from 'vitest';

import {createAiHubChat, generateAiHubChatTitle} from './chats.api';

describe('chats.api', () => {
    beforeEach(() => {
        vi.restoreAllMocks();
    });

    it('createAiHubChat sends GraphQL variables matching the createAiHubChat mutation', async () => {
        // The schema declares createAiHubChat(workspaceId: ID!, environment: Int!, threadId: String!) — pin
        // the variable shape so a refactor can't silently rename or drop a field. The previous REST contract
        // used a query-string + body split; that ambiguity is gone now that everything is a single GraphQL
        // request, but the GraphQL fetcher still serialises {query, variables} into the POST body and the
        // server-side @Argument binding is by name, so getting the variable names wrong here surfaces as a
        // server-side "missing argument" error rather than a typecheck failure.
        const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            new Response(
                JSON.stringify({
                    data: {
                        createAiHubChat: {
                            createdAt: 1_700_000_000_000,
                            environmentId: 0,
                            id: '1',
                            lastPreview: null,
                            messageCount: 0,
                            status: 'ACTIVE',
                            threadId: 'thread-x',
                            title: null,
                            updatedAt: 1_700_000_000_000,
                            userId: 1,
                            workspaceId: 42,
                        },
                    },
                }),
                {headers: {'content-type': 'application/json'}, status: 200}
            )
        );

        const result = await createAiHubChat({environment: 0, threadId: 'thread-x', workspaceId: 42});

        expect(fetchSpy).toHaveBeenCalledTimes(1);

        const init = fetchSpy.mock.calls[0]![1];
        const body = JSON.parse(init?.body as string);

        expect(body.query).toContain('createAiHubChat');
        expect(body.variables).toEqual({
            environment: 0,
            threadId: 'thread-x',
            workspaceId: '42',
        });
        expect(result.id).toBe(1);
        expect(result.threadId).toBe('thread-x');
    });

    it('generateAiHubChatTitle calls the generateAiHubChatTitle mutation with id+workspaceId', async () => {
        const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            new Response(
                JSON.stringify({
                    data: {
                        generateAiHubChatTitle: {
                            createdAt: 1_700_000_000_000,
                            environmentId: 0,
                            id: '7',
                            lastPreview: null,
                            messageCount: 6,
                            status: 'ACTIVE',
                            threadId: 'thread-x',
                            title: 'Generated',
                            updatedAt: 1_700_000_000_000,
                            userId: 1,
                            workspaceId: 42,
                        },
                    },
                }),
                {status: 200}
            )
        );

        const result = await generateAiHubChatTitle({chatId: 7, workspaceId: 42});

        const body = JSON.parse(fetchSpy.mock.calls[0]![1]?.body as string);

        expect(body.query).toContain('generateAiHubChatTitle');
        expect(body.variables).toEqual({
            id: '7',
            workspaceId: '42',
        });
        expect(result.title).toBe('Generated');
    });

    it('generateAiHubChatTitle throws with the server message when the upstream model is unavailable', async () => {
        // Pin: a server-side `TitleGenerationFailedException` arrives as a single GraphQL error from the
        // fetcher; the caller's existing .catch() toast must fire instead of silently returning. A success
        // path would leave the user staring at "Untitled" with zero feedback.
        vi.spyOn(globalThis, 'fetch').mockResolvedValue(
            new Response(JSON.stringify({errors: [{message: 'Title generation is temporarily unavailable'}]}), {
                status: 200,
            })
        );

        await expect(generateAiHubChatTitle({chatId: 7, workspaceId: 42})).rejects.toThrow(
            /Title generation is temporarily unavailable/
        );
    });
});
