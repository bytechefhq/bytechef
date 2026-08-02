import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import AiPromptDetail from '../AiPromptDetail';

vi.mock('@/shared/middleware/graphql', () => ({
    useAiPromptQuery: () => ({
        data: {
            aiPrompt: {
                createdDate: '1700000000000',
                description: 'A test prompt',
                id: '1',
                lastModifiedDate: '1700000000000',
                name: 'Test Prompt',
                projectId: '1',
                version: 1,
                versions: [
                    {
                        active: true,
                        commitMessage: 'Initial version',
                        content: 'line one',
                        createdBy: 'admin',
                        createdDate: 1700000000000,
                        environment: 'production',
                        id: 'v1',
                        metrics: null,
                        promptId: '1',
                        type: 'SYSTEM',
                        variables: [],
                        versionNumber: 1,
                    },
                    {
                        active: false,
                        commitMessage: 'Second version',
                        content: 'line two',
                        createdBy: 'admin',
                        createdDate: 1700000100000,
                        environment: 'staging',
                        id: 'v2',
                        metrics: null,
                        promptId: '1',
                        type: 'SYSTEM',
                        variables: [],
                        versionNumber: 2,
                    },
                ],
            },
        },
        isLoading: false,
    }),
    useSetActiveAiPromptVersionMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const renderDetail = () => {
    render(
        <QueryClientProvider client={new QueryClient()}>
            <AiPromptDetail onBack={vi.fn()} promptId="1" />
        </QueryClientProvider>
    );
};

const openCompareDialog = () => {
    const selectButtons = screen.getAllByTitle('Select for comparison');

    fireEvent.click(selectButtons[0]);
    fireEvent.click(selectButtons[1]);

    fireEvent.click(screen.getByRole('button', {name: 'Compare'}));
};

describe('AiPromptDetail compare dialog', () => {
    it('renders with the dialog role once two versions are selected', () => {
        renderDetail();

        openCompareDialog();

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('names the dialog by its comparison title', () => {
        renderDetail();

        openCompareDialog();

        expect(screen.getByRole('dialog', {name: 'Compare v1 → v2'})).toBeInTheDocument();
    });

    it('closes on Escape', () => {
        renderDetail();

        openCompareDialog();

        fireEvent.keyDown(screen.getByRole('dialog'), {key: 'Escape'});

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
