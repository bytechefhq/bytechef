import {buildCopilotSubscriber} from '@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider';
import {GetWorkspaceFilesQuery, WorkspaceFileSource} from '@/shared/middleware/graphql';
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('sonner', () => ({
    toast: {
        success: vi.fn(),
    },
}));

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const callEvent = (subscriber: any, name: string, payload: Record<string, unknown>) => {
    return subscriber[name]?.(payload);
};

describe('buildCopilotSubscriber', () => {
    let queryClient: QueryClient;
    const appendToLastAssistantMessage = vi.fn<(text: string) => void>();

    beforeEach(() => {
        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        vi.clearAllMocks();
    });

    it('appends text message content to the last assistant message', () => {
        const subscriber = buildCopilotSubscriber({appendToLastAssistantMessage, queryClient});

        callEvent(subscriber, 'onTextMessageContentEvent', {
            event: {delta: ' world'},
            textMessageBuffer: 'hello',
        });

        expect(appendToLastAssistantMessage).toHaveBeenCalledWith('hello world');
    });

    it('prepends new workspace file when createWorkspaceFile tool result arrives', async () => {
        const {toast} = await import('sonner');

        const seededFile: GetWorkspaceFilesQuery['workspaceFiles'][number] = {
            __typename: 'WorkspaceFile',
            createdBy: 'u',
            createdDate: 1,
            description: null,
            downloadUrl: '/seed',
            generatedByAgentSource: null,
            generatedFromPrompt: null,
            id: '1',
            lastModifiedBy: 'u',
            lastModifiedDate: 1,
            mimeType: 'text/plain',
            name: 'existing.txt',
            sizeBytes: 10,
            source: WorkspaceFileSource.UserUpload,
            tags: [],
        };

        queryClient.setQueryData<GetWorkspaceFilesQuery>(
            ['GetWorkspaceFiles', {mimeTypePrefix: null, tagIds: undefined, workspaceId: '100'}],
            {workspaceFiles: [seededFile]}
        );

        const subscriber = buildCopilotSubscriber({appendToLastAssistantMessage, queryClient});

        callEvent(subscriber, 'onToolCallStartEvent', {
            event: {toolCallId: 'tc-1', toolCallName: 'createWorkspaceFile'},
        });

        callEvent(subscriber, 'onToolCallEndEvent', {
            event: {toolCallId: 'tc-1'},
            toolCallArgs: {description: 'make a spec', filename: 'spec.md', mimeType: 'text/markdown'},
            toolCallName: 'createWorkspaceFile',
        });

        callEvent(subscriber, 'onToolCallResultEvent', {
            event: {
                content: JSON.stringify({
                    downloadUrl: '/api/automation/internal/workspace-files/42/content',
                    id: 42,
                    name: 'spec.md',
                    sizeBytes: 128,
                }),
                toolCallId: 'tc-1',
            },
        });

        const updated = queryClient.getQueryData<GetWorkspaceFilesQuery>([
            'GetWorkspaceFiles',
            {mimeTypePrefix: null, tagIds: undefined, workspaceId: '100'},
        ]);

        expect(updated?.workspaceFiles).toHaveLength(2);
        expect(updated?.workspaceFiles[0].id).toBe('42');
        expect(updated?.workspaceFiles[0].name).toBe('spec.md');
        expect(updated?.workspaceFiles[0].mimeType).toBe('text/markdown');
        expect(updated?.workspaceFiles[0].source).toBe(WorkspaceFileSource.AiGenerated);
        expect(updated?.workspaceFiles[0].generatedFromPrompt).toBe('make a spec');
        expect(toast.success).toHaveBeenCalledWith('Created "spec.md"');
    });

    it('ignores non-matching tool results', async () => {
        const {toast} = await import('sonner');

        const subscriber = buildCopilotSubscriber({appendToLastAssistantMessage, queryClient});

        callEvent(subscriber, 'onToolCallStartEvent', {
            event: {toolCallId: 'tc-2', toolCallName: 'listWorkspaceFiles'},
        });

        callEvent(subscriber, 'onToolCallResultEvent', {
            event: {content: JSON.stringify({files: []}), toolCallId: 'tc-2'},
        });

        expect(toast.success).not.toHaveBeenCalled();
    });

    it('ignores createWorkspaceFile results that carry an error payload', async () => {
        const {toast} = await import('sonner');

        const subscriber = buildCopilotSubscriber({appendToLastAssistantMessage, queryClient});

        callEvent(subscriber, 'onToolCallStartEvent', {
            event: {toolCallId: 'tc-3', toolCallName: 'createWorkspaceFile'},
        });

        callEvent(subscriber, 'onToolCallEndEvent', {
            event: {toolCallId: 'tc-3'},
            toolCallArgs: {filename: 'bad.bin', mimeType: 'application/x-custom'},
            toolCallName: 'createWorkspaceFile',
        });

        callEvent(subscriber, 'onToolCallResultEvent', {
            event: {content: JSON.stringify({error: 'Unsupported mime type'}), toolCallId: 'tc-3'},
        });

        expect(toast.success).not.toHaveBeenCalled();
    });
});
