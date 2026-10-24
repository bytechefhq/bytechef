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

    it("shows an agent's tool calls as nested rows", async () => {
        const onExecutionClick = vi.fn();
        const agentExecution = {
            ...createTaskExecution('21', 'COMPLETED'),
            clusterElementCallRecords: [
                {
                    id: 'call-1',
                    startDate: new Date('2026-09-07T18:47:01Z'),
                    status: 'COMPLETED',
                    toolName: 'fetch_ok',
                    workflowNodeName: 'tools_1',
                },
                {
                    id: 'call-2',
                    startDate: new Date('2026-09-07T18:47:02Z'),
                    status: 'COMPLETED',
                    toolName: 'fetch_ok',
                    workflowNodeName: 'tools_1',
                },
            ],
        } as TaskExecution;

        render(
            <Accordion type="multiple">
                <WorkflowExecutionsAccordionItem
                    execution={agentExecution}
                    onExecutionClick={onExecutionClick}
                    selectedExecutionId=""
                >
                    <WorkflowTaskExecutionItem taskExecution={agentExecution} />
                </WorkflowExecutionsAccordionItem>
            </Accordion>
        );

        await userEvent.click(screen.getByText('Condition'));
        await userEvent.click(screen.getByText('fetch_ok #1'));

        expect(screen.getByText('fetch_ok #2')).toBeInTheDocument();
        expect(onExecutionClick).toHaveBeenLastCalledWith(
            expect.objectContaining({id: 'call-1', title: 'fetch_ok #1'})
        );
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

    describe('when the job has failed', () => {
        const loopWorkflowTask = {label: 'Loop over rows', name: 'loop_1', type: 'loop/v1'};

        const renderFailedJobItem = (execution: TaskExecution, previousTaskExecutions?: TaskExecution[]) =>
            render(
                <Accordion type="multiple">
                    <WorkflowExecutionsAccordionItem
                        execution={execution}
                        jobStatus="FAILED"
                        onExecutionClick={vi.fn()}
                        previousTaskExecutions={previousTaskExecutions}
                        selectedExecutionId=""
                    >
                        <WorkflowTaskExecutionItem jobStatus="FAILED" taskExecution={execution} />
                    </WorkflowExecutionsAccordionItem>
                </Accordion>
            );

        it('shows no spinner for an unfinished task inside a loop iteration', async () => {
            const loopTaskExecution = {
                ...createTaskExecution('21', 'STARTED'),
                iterations: [[{id: '22', jobId: '7', priority: 0, status: 'STARTED', title: 'Row task'}]],
                title: 'Loop',
                workflowTask: loopWorkflowTask,
            } as TaskExecution;

            const {container} = renderFailedJobItem(loopTaskExecution);

            await userEvent.click(screen.getByText('Loop over rows'));
            await userEvent.click(screen.getByText('Loop iteration 1'));

            expect(screen.getByText('Row task')).toBeInTheDocument();
            expect(container.querySelector('.animate-spin')).not.toBeInTheDocument();
        });

        it('shows no spinner for an unfinished child task', async () => {
            const parentTaskExecution = {
                ...createTaskExecution('31', 'STARTED'),
                children: [{...createTaskExecution('32', 'STARTED'), title: 'Child task', workflowTask: undefined}],
                workflowTask: loopWorkflowTask,
            } as TaskExecution;

            const {container} = renderFailedJobItem(parentTaskExecution);

            await userEvent.click(screen.getByText('Loop over rows'));

            expect(screen.getByText('Child task')).toBeInTheDocument();
            expect(container.querySelector('.animate-spin')).not.toBeInTheDocument();
        });

        it('shows no spinner for an unfinished earlier attempt', async () => {
            const startedAttempt = createTaskExecution('41', 'STARTED');

            const {container} = renderFailedJobItem(createTaskExecution('42', 'FAILED'), [startedAttempt]);

            await userEvent.click(screen.getByText('Condition'));

            expect(screen.getByText('Attempt 1')).toBeInTheDocument();
            expect(container.querySelector('.animate-spin')).not.toBeInTheDocument();
        });
    });
});
