import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import WorkflowRightSidebar, {WorkflowRightSidebarProps} from './WorkflowRightSidebar';

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: false}}}),
}));

const onWorkflowConnectionsClick = vi.fn();

function createSidebar(props: Partial<WorkflowRightSidebarProps> = {}) {
    return (
        <WorkflowRightSidebar
            copilotPanelOpen={false}
            issueCount={0}
            issuesSidebarOpen={false}
            missingConnectionCount={0}
            onComponentsAndFlowControlsClick={vi.fn()}
            onCopilotClick={vi.fn()}
            onWorkflowCodeEditorClick={vi.fn()}
            onWorkflowConnectionsClick={onWorkflowConnectionsClick}
            onWorkflowInputsClick={vi.fn()}
            onWorkflowIssuesClick={vi.fn()}
            onWorkflowOutputsClick={vi.fn()}
            rightSidebarOpen={false}
            {...props}
        />
    );
}

function renderSidebar(props: Partial<WorkflowRightSidebarProps> = {}) {
    const wrapper = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

    return render(createSidebar(props), {wrapper});
}

afterEach(() => {
    vi.clearAllMocks();
});

describe('WorkflowRightSidebar', () => {
    it('renders Workflow Connections and calls onWorkflowConnectionsClick', async () => {
        renderSidebar();

        expect(screen.getAllByRole('button').map((button) => button.getAttribute('aria-label'))).toEqual([
            'Workflow Issues',
            'Components & Flow Controls',
            'Workflow Connections',
            'Workflow Inputs',
            'Workflow Code Editor',
        ]);

        await userEvent.click(screen.getByRole('button', {name: 'Workflow Connections'}));

        expect(onWorkflowConnectionsClick).toHaveBeenCalledTimes(1);
    });

    it('shows the missing count badge only when connections are missing', async () => {
        const {rerender} = renderSidebar({missingConnectionCount: 3});

        const connectionsButton = screen.getByRole('button', {name: 'Workflow Connections, 3 missing'});

        expect(connectionsButton).toHaveTextContent('3');

        await userEvent.hover(connectionsButton);

        expect(await screen.findByRole('tooltip')).toHaveTextContent(/^Workflow Connections$/);

        rerender(createSidebar({missingConnectionCount: 0}));

        expect(screen.getByRole('button', {name: 'Workflow Connections'}).textContent).toBe('');
    });
});
