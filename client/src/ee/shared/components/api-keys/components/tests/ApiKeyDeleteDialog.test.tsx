import {useApiKeysStore} from '@/ee/shared/components/api-keys/stores/useApiKeysStore';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiKeyDeleteDialog from '../ApiKeyDeleteDialog';

const hoisted = vi.hoisted(() => ({
    handleDeleteMock: vi.fn(),
    isDeletePending: false,
}));

vi.mock('@/ee/shared/components/api-keys/hooks/useApiKeys', () => ({
    default: () => ({
        handleDelete: hoisted.handleDeleteMock,
        isDeletePending: hoisted.isDeletePending,
    }),
}));

beforeEach(() => {
    windowResizeObserver();

    hoisted.isDeletePending = false;

    useApiKeysStore.setState({currentApiKey: {id: '5', name: 'Production key'}, showDeleteDialog: true});
});

afterEach(() => {
    resetAll();
});

describe('ApiKeyDeleteDialog', () => {
    it('deletes the current API key when confirmed', async () => {
        render(<ApiKeyDeleteDialog />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.handleDeleteMock).toHaveBeenCalledWith(5);
    });

    it('clears the current API key and hides the dialog when cancelled', async () => {
        render(<ApiKeyDeleteDialog />);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(useApiKeysStore.getState().showDeleteDialog).toBe(false);
        expect(useApiKeysStore.getState().currentApiKey).toBeUndefined();
        expect(hoisted.handleDeleteMock).not.toHaveBeenCalled();
    });

    it('disables both actions while the deletion is in flight', () => {
        hoisted.isDeletePending = true;

        render(<ApiKeyDeleteDialog />);

        expect(screen.getByRole('button', {name: 'Delete'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Cancel'})).toBeDisabled();
    });
});
