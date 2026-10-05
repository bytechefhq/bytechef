import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {McpServerToolsTabType} from '../McpServerTabs';
import McpServerToolsPanel from '../McpServerToolsPanel';

const renderPanel = (
    activeToolsTab: McpServerToolsTabType,
    {isComponentListEmpty = false, isWorkflowListEmpty = false} = {}
) => {
    const onAddComponentClick = vi.fn();
    const onAddWorkflowsClick = vi.fn();

    render(
        <McpServerToolsPanel
            activeToolsTab={activeToolsTab}
            componentList={<div>Component list</div>}
            isComponentListEmpty={isComponentListEmpty}
            isWorkflowListEmpty={isWorkflowListEmpty}
            onAddComponentClick={onAddComponentClick}
            onAddWorkflowsClick={onAddWorkflowsClick}
            workflowList={<div>Workflow list</div>}
        />
    );

    return {onAddComponentClick, onAddWorkflowsClick};
};

describe('McpServerToolsPanel', () => {
    it('shows only the component list on the Components tab', () => {
        renderPanel('components');

        expect(screen.getByText('Component list')).toBeInTheDocument();
        expect(screen.queryByText('Workflow list')).not.toBeInTheDocument();
    });

    it('shows only the workflow list on the Workflows tab', () => {
        renderPanel('workflows');

        expect(screen.getByText('Workflow list')).toBeInTheDocument();
        expect(screen.queryByText('Component list')).not.toBeInTheDocument();
    });

    it('offers Add Component when the server has no components', async () => {
        const {onAddComponentClick} = renderPanel('components', {isComponentListEmpty: true});

        expect(screen.getByText('No Components')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add Workflows'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));

        expect(onAddComponentClick).toHaveBeenCalledTimes(1);
    });

    it('offers Add Workflows when the server has no workflows', async () => {
        const {onAddWorkflowsClick} = renderPanel('workflows', {isWorkflowListEmpty: true});

        expect(screen.getByText('No Workflows')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Add Workflows'}));

        expect(onAddWorkflowsClick).toHaveBeenCalledTimes(1);
    });
});
