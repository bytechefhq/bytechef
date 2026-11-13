import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import IntegrationPublishDialog from '../IntegrationPublishDialog';

const hoisted = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('@/ee/shared/mutations/embedded/integrations.mutations', () => ({
    usePublishIntegrationMutation: () => ({mutate: hoisted.mutateMock}),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationPublished: vi.fn()}),
}));

describe('IntegrationPublishDialog', () => {
    it('publishes with the description typed into the placeholder-guided field', async () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <IntegrationPublishDialog
                    integration={{componentName: 'sales', id: 1, multipleInstances: false, name: 'Sales'}}
                    onClose={vi.fn()}
                />
            </QueryClientProvider>
        );

        const descriptionField = screen.getByPlaceholderText(/Describe what changed in this version/);

        await userEvent.type(descriptionField, 'Added a Slack notification');
        await userEvent.click(screen.getByRole('button', {name: 'Publish'}));

        expect(hoisted.mutateMock).toHaveBeenCalledWith({
            id: 1,
            publishIntegrationRequest: {description: 'Added a Slack notification'},
        });
    });
});
