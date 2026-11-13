import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import ProjectPublishDialog from '../ProjectPublishDialog';

const hoisted = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    usePublishProjectMutation: () => ({mutate: hoisted.mutateMock}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureProjectPublished: vi.fn()}),
}));

describe('ProjectPublishDialog', () => {
    it('publishes with the description typed into the placeholder-guided field', async () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <ProjectPublishDialog onClose={vi.fn()} project={{id: 1, name: 'Sales', workspaceId: 1}} />
            </QueryClientProvider>
        );

        const descriptionField = screen.getByPlaceholderText(/Describe what changed in this version/);

        await userEvent.type(descriptionField, 'Added a Slack notification');
        await userEvent.click(screen.getByRole('button', {name: 'Publish'}));

        expect(hoisted.mutateMock).toHaveBeenCalledWith({
            id: 1,
            publishProjectRequest: {description: 'Added a Slack notification'},
        });
    });
});
