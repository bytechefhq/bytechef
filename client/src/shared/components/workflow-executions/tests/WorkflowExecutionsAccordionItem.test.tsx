import {Accordion} from '@/components/ui/accordion';
import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionsAccordionItem from '../WorkflowExecutionsAccordionItem';
import WorkflowTaskExecutionItem from '../WorkflowTaskExecutionItem';

function createTaskExecution(id: string, status: string): TaskExecution {
    return {
        id,
        jobId: '7',
        priority: 0,
        startDate: new Date('2026-09-07T18:47:00Z'),
        status,
        workflowTask: {label: 'Condition', name: 'condition_1', type: 'condition/v1'},
    } as TaskExecution;
}

const firstAttempt = createTaskExecution('11', 'FAILED');
const secondAttempt = createTaskExecution('12', 'FAILED');
const latestAttempt = createTaskExecution('13', 'FAILED');

const renderAccordionItem = (
    onExecutionClick: (execution: unknown) => void,
    previousTaskExecutions?: TaskExecution[]
) =>
    render(
        <Accordion type="multiple">
            <WorkflowExecutionsAccordionItem
                execution={latestAttempt}
                onExecutionClick={onExecutionClick}
                previousTaskExecutions={previousTaskExecutions}
                selectedExecutionId=""
            >
                <WorkflowTaskExecutionItem attemptLabel="Attempt 3" taskExecution={latestAttempt} />
            </WorkflowExecutionsAccordionItem>
        </Accordion>
    );

describe('WorkflowExecutionsAccordionItem', () => {
    it('lists earlier attempts under the latest attempt, oldest first', async () => {
        renderAccordionItem(vi.fn(), [firstAttempt, secondAttempt]);

        await userEvent.click(screen.getByText('Attempt 3'));

        const attemptLabels = screen.getAllByText(/^Attempt \d$/).map((element) => element.textContent);

        expect(attemptLabels).toEqual(['Attempt 3', 'Attempt 1', 'Attempt 2']);
    });

    it('selects an earlier attempt when it is clicked', async () => {
        const onExecutionClick = vi.fn();

        renderAccordionItem(onExecutionClick, [firstAttempt, secondAttempt]);

        await userEvent.click(screen.getByText('Attempt 3'));
        await userEvent.click(screen.getByText('Attempt 1'));

        expect(onExecutionClick).toHaveBeenLastCalledWith(firstAttempt);
    });

    it('is not expandable when the task ran once', () => {
        renderAccordionItem(vi.fn());

        expect(screen.queryByRole('region')).not.toBeInTheDocument();
        expect(screen.getByRole('button')).not.toHaveAttribute('aria-expanded');
    });
});
