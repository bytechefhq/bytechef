import {TooltipProvider} from '@/components/ui/tooltip';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodesTabs from '../components/workflow-nodes-tabs/WorkflowNodesTabs';

const hoisted = vi.hoisted(() => ({
    featureFlagEnabled: false,
}));

vi.mock('../stores/useWorkflowDataStore', () => ({
    default: (selector: (state: object) => unknown) => selector({edges: [], nodes: [], workflow: {id: 'workflow-1'}}),
}));

vi.mock('../stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: object) => unknown) => selector({copiedNode: undefined, copiedWorkflowId: undefined}),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => hoisted.featureFlagEnabled,
}));

const actionComponentDefinitions: Array<ComponentDefinitionBasic> = [
    {actionsCount: 3, name: 'claudeCode', title: 'Claude Code', version: 1},
    {actionsCount: 5, name: 'gmail', title: 'Gmail', version: 1},
    {actionsCount: 4, name: 'logger', title: 'Logger', version: 1},
    {actionsCount: 3, name: 'script', title: 'Script', version: 1},
];

const renderWorkflowNodesTabs = (props: Partial<Parameters<typeof WorkflowNodesTabs>[0]> = {}) =>
    render(
        <TooltipProvider>
            <WorkflowNodesTabs
                actionComponentDefinitions={actionComponentDefinitions}
                hideClusterElementComponents
                hideTriggerComponents
                taskDispatcherDefinitions={[]}
                triggerComponentDefinitions={[]}
                {...props}
            />
        </TooltipProvider>
    );

describe('WorkflowNodesTabs', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.featureFlagEnabled = false;
    });

    afterEach(() => {
        resetAll();
    });

    it('should show the Helpers tab after the Flows tab', () => {
        renderWorkflowNodesTabs();

        const tabNames = screen.getAllByRole('tab').map((tab) => tab.textContent);

        expect(tabNames).toEqual(['Actions', 'Flows', 'Helpers']);
    });

    it('should not show the Helpers tab when action components are hidden', () => {
        renderWorkflowNodesTabs({hideActionComponents: true});

        expect(screen.queryByRole('tab', {name: 'Helpers'})).not.toBeInTheDocument();
    });

    it('should keep components without a connection out of the Actions tab', () => {
        renderWorkflowNodesTabs();

        expect(screen.getByText('Gmail')).toBeInTheDocument();
        expect(screen.queryByText('Logger')).not.toBeInTheDocument();
        expect(screen.queryByText('Script')).not.toBeInTheDocument();
    });

    it('should list components without a connection in the Helpers tab', async () => {
        renderWorkflowNodesTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Helpers'}));

        expect(screen.getByText('Logger')).toBeInTheDocument();
        expect(screen.getByText('Script')).toBeInTheDocument();
        expect(screen.queryByText('Gmail')).not.toBeInTheDocument();
    });

    it('should hide feature-flagged helpers when their flag is off', async () => {
        renderWorkflowNodesTabs();

        await userEvent.click(screen.getByRole('tab', {name: 'Helpers'}));

        expect(screen.queryByText('Claude Code')).not.toBeInTheDocument();
    });

    it('should highlight the selected component in the Helpers tab', async () => {
        renderWorkflowNodesTabs({selectedComponentName: 'logger'});

        await userEvent.click(screen.getByRole('tab', {name: 'Helpers'}));

        expect(screen.getByText('Logger').closest('li')).toHaveClass('border-blue-500');
    });
});
