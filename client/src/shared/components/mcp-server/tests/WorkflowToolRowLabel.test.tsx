import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import WorkflowToolRowLabel from '../WorkflowToolRowLabel';

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: ({filteredComponentNames}: {filteredComponentNames?: string[]}) => (
        <div data-testid="trigger-and-components-row">{filteredComponentNames?.join(',')}</div>
    ),
}));

const renderLabel = (props: Partial<React.ComponentProps<typeof WorkflowToolRowLabel>> = {}) =>
    render(
        <TooltipProvider>
            <WorkflowToolRowLabel
                canOpenExecutions
                filteredComponentNames={['webhook', 'slack']}
                label="Lead intake"
                onWorkflowClick={vi.fn()}
                workflowComponentDefinitions={{}}
                workflowTaskDispatcherDefinitions={{}}
                {...props}
            />
        </TooltipProvider>
    );

describe('WorkflowToolRowLabel', () => {
    it('opens the executions from the workflow name and the editor from the link', async () => {
        const user = userEvent.setup();
        const onWorkflowClick = vi.fn();
        const onOpenInProjectClick = vi.fn();

        renderLabel({onOpenInProjectClick, onWorkflowClick, workflow: {workflowTriggerComponentNames: ['webhook']}});

        await user.click(screen.getByRole('button', {name: 'Lead intake'}));
        await user.click(screen.getByRole('button', {name: 'Open in project'}));

        expect(onWorkflowClick).toHaveBeenCalledTimes(1);
        expect(onOpenInProjectClick).toHaveBeenCalledTimes(1);
        expect(screen.getByTestId('trigger-and-components-row')).toHaveTextContent('webhook,slack');
    });

    it('keeps the name inert, hides the link and the components until the workflow is known', () => {
        renderLabel({canOpenExecutions: false, subLabel: <span>Skill: Qualify lead</span>});

        expect(screen.getByRole('button', {name: 'Lead intake'})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Open in project'})).not.toBeInTheDocument();
        expect(screen.queryByTestId('trigger-and-components-row')).not.toBeInTheDocument();
        expect(screen.getByText('Skill: Qualify lead')).toBeInTheDocument();
    });
});
