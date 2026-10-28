import IntegrationListItemTabs from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItemTabs';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

const renderIntegrationListItemTabs = (activeTab: 'configurations' | 'workflows', onTabChange = vi.fn()) =>
    render(
        <IntegrationListItemTabs
            actions={<button>New Workflow</button>}
            activeTab={activeTab}
            configurationsContent={<div>Configurations content</div>}
            onTabChange={onTabChange}
            workflowsContent={<div>Workflows content</div>}
        />
    );

describe('IntegrationListItemTabs', () => {
    it('shows the content of the active tab', () => {
        renderIntegrationListItemTabs('workflows');

        expect(screen.getByRole('tab', {name: 'Workflows'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByText('Workflows content')).toBeInTheDocument();
        expect(screen.queryByText('Configurations content')).not.toBeInTheDocument();
    });

    it('shows the configurations content when configurations is active', () => {
        renderIntegrationListItemTabs('configurations');

        expect(screen.getByRole('tab', {name: 'Configurations'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByText('Configurations content')).toBeInTheDocument();
        expect(screen.queryByText('Workflows content')).not.toBeInTheDocument();
    });

    it('reports the selected tab', async () => {
        const onTabChange = vi.fn();

        renderIntegrationListItemTabs('workflows', onTabChange);

        await userEvent.click(screen.getByRole('tab', {name: 'Configurations'}));

        expect(onTabChange).toHaveBeenCalledWith('configurations');
    });

    it('renders the actions next to the tabs', () => {
        renderIntegrationListItemTabs('workflows');

        expect(screen.getByRole('button', {name: 'New Workflow'})).toBeInTheDocument();
    });
});
