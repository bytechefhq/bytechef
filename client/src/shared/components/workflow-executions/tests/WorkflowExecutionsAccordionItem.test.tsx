import {Accordion} from '@/components/ui/accordion';
import {TaskExecution} from '@/shared/middleware/automation/workflow/execution';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

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

    describe('loop iterations', () => {
        let intersectionCallbacks: Array<{callback: IntersectionObserverCallback; element: Element}>;

        beforeEach(() => {
            intersectionCallbacks = [];

            vi.stubGlobal(
                'IntersectionObserver',
                class {
                    constructor(private callback: IntersectionObserverCallback) {}

                    disconnect() {
                        intersectionCallbacks = intersectionCallbacks.filter(
                            (entry) => entry.callback !== this.callback
                        );
                    }

                    observe(element: Element) {
                        intersectionCallbacks.push({callback: this.callback, element});
                    }
                }
            );
        });

        afterEach(() => {
            vi.unstubAllGlobals();
        });

        it('loads more iterations when the sentinel scrolls into view after the loop is expanded', async () => {
            const loopTaskExecution = {
                ...createTaskExecution('20', 'COMPLETED'),
                iterations: Array.from({length: 150}, () => []),
                title: 'Loop',
                type: 'loop/v1',
            } as TaskExecution;

            render(
                <Accordion type="multiple">
                    <WorkflowExecutionsAccordionItem
                        execution={loopTaskExecution}
                        onExecutionClick={vi.fn()}
                        selectedExecutionId=""
                    >
                        <span>Loop trigger</span>
                    </WorkflowExecutionsAccordionItem>
                </Accordion>
            );

            await userEvent.click(screen.getByText('Loop trigger'));

            expect(screen.getByText('Loading 50 more iterations...')).toBeInTheDocument();
            expect(screen.queryByText('Loop iteration 150')).not.toBeInTheDocument();

            const sentinel = screen.getByText('Loading 50 more iterations...');
            const observed = intersectionCallbacks.find((entry) => entry.element === sentinel);

            expect(observed).toBeDefined();

            act(() => {
                observed!.callback(
                    [{isIntersecting: true, target: sentinel} as unknown as IntersectionObserverEntry],
                    {} as IntersectionObserver
                );
            });

            expect(screen.getByText('Loop iteration 150')).toBeInTheDocument();
            expect(screen.queryByText(/more iterations\.\.\./)).not.toBeInTheDocument();
        });
    });
});
