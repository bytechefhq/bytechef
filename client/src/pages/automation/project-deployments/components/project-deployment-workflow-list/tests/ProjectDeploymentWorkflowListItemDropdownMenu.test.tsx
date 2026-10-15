import {Workflow} from '@/shared/middleware/automation/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentWorkflowListItemDropdownMenu from '../ProjectDeploymentWorkflowListItemDropdownMenu';

const hoistedScope = vi.hoisted(() => ({canEditDeployment: true}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => hoistedScope.canEditDeployment,
}));

const workflow = {connectionsCount: 1, id: 'workflow-1', inputsCount: 1, label: 'Test Workflow'} as Workflow;

beforeEach(() => {
    hoistedScope.canEditDeployment = true;

    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ProjectDeploymentWorkflowListItemDropdownMenu', () => {
    it('shows Edit with DEPLOYMENT_EDIT', async () => {
        render(<ProjectDeploymentWorkflowListItemDropdownMenu onEditClick={vi.fn()} workflow={workflow} />);

        await userEvent.click(screen.getByRole('button', {name: 'More Deployment Workflow Actions'}));

        expect(screen.getByText('Edit')).toBeInTheDocument();
    });

    it('hides the menu without DEPLOYMENT_EDIT', () => {
        hoistedScope.canEditDeployment = false;

        render(<ProjectDeploymentWorkflowListItemDropdownMenu onEditClick={vi.fn()} workflow={workflow} />);

        expect(screen.queryByRole('button', {name: 'More Deployment Workflow Actions'})).not.toBeInTheDocument();
    });
});
