import ProjectListItemTabs from '@/pages/automation/projects/components/project-list/ProjectListItemTabs';
import {render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

const renderProjectListItemTabs = (activeTab: 'deployments' | 'workflows', onTabChange = vi.fn()) =>
    render(
        <ProjectListItemTabs
            actions={<button>New Workflow</button>}
            activeTab={activeTab}
            deploymentsContent={<div>Deployments content</div>}
            onTabChange={onTabChange}
            workflowsContent={<div>Workflows content</div>}
        />
    );

describe('ProjectListItemTabs', () => {
    it('shows the content of the active tab', () => {
        renderProjectListItemTabs('workflows');

        expect(screen.getByRole('tab', {name: 'Workflows'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByText('Workflows content')).toBeInTheDocument();
        expect(screen.queryByText('Deployments content')).not.toBeInTheDocument();
    });

    it('shows the deployments content when deployments is active', () => {
        renderProjectListItemTabs('deployments');

        expect(screen.getByRole('tab', {name: 'Deployments'})).toHaveAttribute('aria-selected', 'true');
        expect(screen.getByText('Deployments content')).toBeInTheDocument();
        expect(screen.queryByText('Workflows content')).not.toBeInTheDocument();
    });

    it('reports the selected tab', async () => {
        const onTabChange = vi.fn();

        renderProjectListItemTabs('workflows', onTabChange);

        await userEvent.click(screen.getByRole('tab', {name: 'Deployments'}));

        expect(onTabChange).toHaveBeenCalledWith('deployments');
    });

    it('renders the actions next to the tabs', () => {
        renderProjectListItemTabs('workflows');

        expect(screen.getByRole('button', {name: 'New Workflow'})).toBeInTheDocument();
    });
});
