import {useApiKeysStore} from '@/shared/components/api-keys/stores/useApiKeysStore';
import {ApiKey} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import ApiKeyTable from '../ApiKeyTable';

const apiKey = {
    createdBy: 'editor@example.com',
    createdDate: '2026-10-19T10:00:00Z',
    id: '1',
    lastUsedDate: '2026-10-19T10:00:00Z',
    name: 'MCP client',
    secretKey: '********1234',
} as ApiKey;

beforeEach(() => {
    windowResizeObserver();

    useApiKeysStore.setState({currentApiKey: undefined, showDeleteDialog: false, showEditDialog: false});
});

afterEach(() => {
    resetAll();
});

describe('ApiKeyTable', () => {
    it('opens the edit dialog for the clicked key', async () => {
        render(<ApiKeyTable apiKeys={[apiKey]} />);

        const [editButton] = screen.getAllByRole('button');

        await userEvent.click(editButton);

        expect(useApiKeysStore.getState().currentApiKey).toEqual(apiKey);
        expect(useApiKeysStore.getState().showEditDialog).toBe(true);
        expect(useApiKeysStore.getState().showDeleteDialog).toBe(false);
    });

    it('opens the delete dialog for the clicked key', async () => {
        render(<ApiKeyTable apiKeys={[apiKey]} />);

        const [, deleteButton] = screen.getAllByRole('button');

        await userEvent.click(deleteButton);

        expect(useApiKeysStore.getState().currentApiKey).toEqual(apiKey);
        expect(useApiKeysStore.getState().showDeleteDialog).toBe(true);
        expect(useApiKeysStore.getState().showEditDialog).toBe(false);
    });

    it('hides the Edit button when canEdit is false', () => {
        render(<ApiKeyTable apiKeys={[apiKey]} canEdit={false} />);

        expect(screen.queryByRole('button', {name: 'Edit API Key'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete API Key'})).toBeInTheDocument();
    });

    it('hides the Delete button when canDelete is false', () => {
        render(<ApiKeyTable apiKeys={[apiKey]} canDelete={false} />);

        expect(screen.getByRole('button', {name: 'Edit API Key'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Delete API Key'})).not.toBeInTheDocument();
    });

    it('masks a secret key that arrives unobfuscated and keeps an obfuscated one as returned', () => {
        render(
            <ApiKeyTable
                apiKeys={[
                    {...apiKey, id: '1', secretKey: 'sk_live_plaintext_ab12cd'},
                    {...apiKey, id: '2', name: 'Second key', secretKey: '....zz99yy'},
                ]}
            />
        );

        expect(screen.queryByText('sk_live_plaintext_ab12cd')).not.toBeInTheDocument();
        expect(screen.getByText(`${'.'.repeat(26)}ab12cd`)).toBeInTheDocument();
        expect(screen.getByText('....zz99yy')).toBeInTheDocument();
    });
});
