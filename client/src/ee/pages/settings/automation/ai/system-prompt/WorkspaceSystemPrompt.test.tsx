import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkspaceSystemPrompt from './WorkspaceSystemPrompt';

const {invalidateQueriesMock, mutateMock, queryMock} = vi.hoisted(() => ({
    invalidateQueriesMock: vi.fn(),
    mutateMock: vi.fn(),
    queryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateWorkspaceSystemPromptMutation: () => ({isPending: false, mutate: mutateMock}),
    useWorkspaceSystemPromptQuery: (...args: unknown[]) => queryMock(...args),
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: invalidateQueriesMock}),
}));

describe('WorkspaceSystemPrompt', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        invalidateQueriesMock.mockClear();

        // The real store rather than a mocked hook: a mock keeps passing if the state shape changes underneath the
        // page's selector, which is the regression a page test exists to catch.
        useWorkspaceStore.setState({currentWorkspaceId: 123});

        queryMock.mockReturnValue({
            data: {workspaceSystemPrompt: {prompt: 'Always answer in German.', workspaceId: '123'}},
            error: null,
            isLoading: false,
        });
    });

    it('renders the stored prompt and its character count', () => {
        render(<WorkspaceSystemPrompt />);

        expect(screen.getByLabelText('Workspace system prompt')).toHaveValue('Always answer in German.');
        expect(screen.getByText('24 / 4000')).toBeInTheDocument();
    });

    it('renders empty when the query returns null (no prompt set yet)', () => {
        queryMock.mockReturnValue({
            data: {workspaceSystemPrompt: null},
            error: null,
            isLoading: false,
        });

        render(<WorkspaceSystemPrompt />);

        expect(screen.getByLabelText('Workspace system prompt')).toHaveValue('');
        expect(screen.getByText('0 / 4000')).toBeInTheDocument();
    });

    it('saves the edited prompt', () => {
        render(<WorkspaceSystemPrompt />);

        fireEvent.change(screen.getByLabelText('Workspace system prompt'), {target: {value: 'Be concise.'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {prompt: 'Be concise.', workspaceId: '123'},
        });
    });

    it('saves undefined when cleared, deleting the stored prompt', () => {
        render(<WorkspaceSystemPrompt />);

        fireEvent.change(screen.getByLabelText('Workspace system prompt'), {target: {value: '   '}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(mutateMock).toHaveBeenCalledWith({
            input: {prompt: undefined, workspaceId: '123'},
        });
    });
});
