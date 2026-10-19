import ApiKeysContent from '@/shared/components/api-keys/ApiKeysContent';
import {ApiKeysPermissionsI, ApiKeysProvider} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {ApiKey} from '@/shared/middleware/graphql';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const apiKey = {
    createdBy: 'editor@example.com',
    createdDate: '2026-10-19T10:00:00Z',
    id: '1',
    lastUsedDate: '2026-10-19T10:00:00Z',
    name: 'MCP client',
    secretKey: '********1234',
} as ApiKey;

const mutation = () => ({isPending: false, mutate: vi.fn()}) as unknown as UseMutationResult<unknown, Error>;

const renderContent = (apiKeys: ApiKey[], permissions?: ApiKeysPermissionsI) =>
    render(
        <ApiKeysProvider
            value={{
                permissions,
                useApiKeysQuery: () =>
                    ({data: apiKeys, error: null, isLoading: false}) as unknown as UseQueryResult<ApiKey[], Error>,
                useCreateApiKeyMutation: mutation,
                useDeleteApiKeyMutation: mutation,
                useUpdateApiKeyMutation: mutation,
            }}
        >
            <ApiKeysContent description="Keys" title="API Keys" />
        </ApiKeysProvider>
    );

const viewer: ApiKeysPermissionsI = {canCreate: false, canManage: false, canView: false, loading: false};
const editor: ApiKeysPermissionsI = {canCreate: true, canManage: true, canView: true, loading: false};

describe('ApiKeysContent', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('offers New API Key and the row actions when no permissions are given', () => {
        renderContent([apiKey]);

        expect(screen.getByRole('button', {name: 'New API Key'})).toBeInTheDocument();
        expect(screen.getAllByRole('button')).toHaveLength(3);
    });

    it('offers New API Key and the row actions to an editor', () => {
        renderContent([apiKey], editor);

        expect(screen.getByRole('button', {name: 'New API Key'})).toBeInTheDocument();
        expect(screen.getAllByRole('button')).toHaveLength(3);
    });

    it('shows a viewer that only editors can manage keys, with no actions', () => {
        renderContent([apiKey], viewer);

        expect(screen.getByText('Only workspace editors can create and manage API keys.')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
        expect(screen.queryByText('MCP client')).not.toBeInTheDocument();
    });

    it('hides New API Key in the empty state without the create scope', () => {
        renderContent([], {...editor, canCreate: false});

        expect(screen.getByText('No API Keys')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New API Key'})).not.toBeInTheDocument();
    });

    it('hides the row actions without the delete scope', () => {
        renderContent([apiKey], {...editor, canManage: false});

        expect(screen.getByText('MCP client')).toBeInTheDocument();
        expect(screen.getAllByRole('button')).toHaveLength(1);
    });
});
