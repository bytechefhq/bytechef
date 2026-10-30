import {
    ApiKeysProvider,
    CreateApiKeyMutationProps,
    SimpleMutationProps,
} from '@/shared/components/api-keys/providers/apiKeysProvider';
import {useApiKeysStore} from '@/shared/components/api-keys/stores/useApiKeysStore';
import {ApiKey} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {UseMutationResult, UseQueryResult} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiKeyDialog from '../ApiKeyDialog';

const hoisted = vi.hoisted(() => ({
    copyToClipboardMock: vi.fn(),
    createMutateMock: vi.fn(),
    createProps: undefined as CreateApiKeyMutationProps | undefined,
    toastMock: vi.fn(),
    updateMutateMock: vi.fn(),
    updateProps: undefined as SimpleMutationProps | undefined,
}));

vi.mock('@uidotdev/usehooks', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@uidotdev/usehooks')>()),
    useCopyToClipboard: () => [null, hoisted.copyToClipboardMock],
}));

vi.mock('sonner', async (importOriginal) => ({
    ...(await importOriginal<typeof import('sonner')>()),
    toast: hoisted.toastMock,
}));

const mutation = (mutate: () => void) => ({isPending: false, mutate}) as unknown as UseMutationResult<unknown, Error>;

const renderDialog = () =>
    render(
        <ApiKeysProvider
            value={{
                useApiKeysQuery: () =>
                    ({data: [], error: null, isLoading: false}) as unknown as UseQueryResult<ApiKey[], Error>,
                useCreateApiKeyMutation: (props) => {
                    hoisted.createProps = props;

                    return mutation(hoisted.createMutateMock);
                },
                useDeleteApiKeyMutation: () => mutation(vi.fn()),
                useUpdateApiKeyMutation: (props) => {
                    hoisted.updateProps = props;

                    return mutation(hoisted.updateMutateMock);
                },
            }}
        >
            <ApiKeyDialog />
        </ApiKeysProvider>
    );

beforeEach(() => {
    windowResizeObserver();

    useApiKeysStore.setState({currentApiKey: undefined, secretKey: undefined, showEditDialog: false});
});

afterEach(() => {
    resetAll();
});

describe('ApiKeyDialog', () => {
    it('creates a key and shows its secret once', async () => {
        hoisted.createMutateMock.mockImplementation(() => hoisted.createProps?.onSuccess?.('secret-123'));

        renderDialog();

        await userEvent.type(screen.getByLabelText('Name'), 'MCP client');
        await userEvent.click(screen.getByRole('button', {name: 'Create API Key'}));

        expect(hoisted.createMutateMock).toHaveBeenCalledWith({name: 'MCP client'});
        expect(screen.getByText('Save your API Key')).toBeInTheDocument();
        expect(screen.getByDisplayValue('secret-123')).toBeInTheDocument();
    });

    it('copies the secret key', async () => {
        useApiKeysStore.setState({secretKey: 'secret-123'});

        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Copy'}));

        expect(hoisted.copyToClipboardMock).toHaveBeenCalledWith('secret-123');
        expect(hoisted.toastMock).toHaveBeenCalledWith('The secret API key is copied.');
    });

    it('updates an existing key and closes the edit dialog', async () => {
        hoisted.updateMutateMock.mockImplementation(() => hoisted.updateProps?.onSuccess?.(undefined));

        useApiKeysStore.setState({currentApiKey: {id: '5', name: 'Production key'}, showEditDialog: true});

        renderDialog();

        expect(screen.getByText('Edit API Key')).toBeInTheDocument();

        await userEvent.clear(screen.getByLabelText('Name'));
        await userEvent.type(screen.getByLabelText('Name'), 'Renamed key');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateMutateMock).toHaveBeenCalledWith({id: '5', name: 'Renamed key'});
        expect(useApiKeysStore.getState().showEditDialog).toBe(false);
        expect(useApiKeysStore.getState().currentApiKey).toBeUndefined();
    });

    it('rejects a name shorter than two characters', async () => {
        renderDialog();

        await userEvent.type(screen.getByLabelText('Name'), 'a');
        await userEvent.click(screen.getByRole('button', {name: 'Create API Key'}));

        expect(await screen.findByText('Name must be at least 2 characters.')).toBeInTheDocument();
        expect(hoisted.createMutateMock).not.toHaveBeenCalled();
    });

    it('clears the dialog state when cancelled', async () => {
        useApiKeysStore.setState({currentApiKey: {id: '5', name: 'Production key'}, showEditDialog: true});

        renderDialog();

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(useApiKeysStore.getState().showEditDialog).toBe(false);
        expect(useApiKeysStore.getState().currentApiKey).toBeUndefined();
        expect(screen.queryByText('Edit API Key')).not.toBeInTheDocument();
    });
});
