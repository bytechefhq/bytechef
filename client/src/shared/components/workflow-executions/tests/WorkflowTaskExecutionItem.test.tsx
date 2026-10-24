import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {act, render, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import WorkflowTaskExecutionItem from '../WorkflowTaskExecutionItem';

function createTaskExecution(overrides: Partial<TaskExecution> = {}): TaskExecution {
    return {
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        title: 'Test Task',
        workflowTask: {
            label: 'Test Task Label',
            name: 'testTask',
            type: 'test/v1/testAction',
        },
        ...overrides,
    } as TaskExecution;
}

describe('WorkflowTaskExecutionItem', () => {
    describe('cluster element call rows', () => {
        const createCallExecution = (clusterElementCallNote?: string) =>
            createTaskExecution({
                clusterElementCallId: 'call-1',
                clusterElementCallNote,
                endDate: undefined,
                parentTaskExecutionId: '42',
                status: 'STARTED',
            } as Partial<TaskExecution>);

        it('shows the note and no spinner for a call still running when its agent finished', () => {
            const {container} = render(
                <WorkflowTaskExecutionItem
                    jobEnded={false}
                    taskExecution={createCallExecution('Still running when the agent finished')}
                />
            );

            expect(screen.getByText('Still running when the agent finished')).toBeInTheDocument();
            expect(container.querySelector('.animate-spin')).not.toBeInTheDocument();
        });

        it('keeps the spinner for a running call without a note', () => {
            const {container} = render(
                <WorkflowTaskExecutionItem jobEnded={false} taskExecution={createCallExecution()} />
            );

            expect(container.querySelector('.animate-spin')).toBeInTheDocument();
        });
    });

    describe('outcome rows', () => {
        const createOutcomeExecution = (clusterElementCallOutcome: string) =>
            createTaskExecution({
                clusterElementCallId: 'call-1',
                clusterElementCallOutcome,
                parentTaskExecutionId: '42',
                status: 'COMPLETED',
            } as Partial<TaskExecution>);

        it('shows the ban shield for a blocked row', () => {
            const {container} = render(<WorkflowTaskExecutionItem taskExecution={createOutcomeExecution('BLOCKED')} />);

            expect(container.querySelector('svg.lucide-shield-ban.text-amber-500')).toBeInTheDocument();
        });

        it('shows the alert shield for a withheld row', () => {
            const {container} = render(
                <WorkflowTaskExecutionItem taskExecution={createOutcomeExecution('WITHHELD')} />
            );

            expect(container.querySelector('svg.lucide-shield-alert.text-amber-500')).toBeInTheDocument();
        });

        it('shows the eraser for a redacted row', () => {
            const {container} = render(
                <WorkflowTaskExecutionItem taskExecution={createOutcomeExecution('REDACTED')} />
            );

            expect(container.querySelector('svg.lucide-eraser.text-sky-500')).toBeInTheDocument();
        });
    });

    describe('status icon integration', () => {
        it('should render status icon from utility for completed task', () => {
            const taskExecution = createTaskExecution({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'COMPLETED',
            });

            const {container} = render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(container.querySelector('.text-success')).toBeInTheDocument();
        });

        it('should render status icon from utility for started task', () => {
            const taskExecution = createTaskExecution({
                endDate: undefined,
                status: 'STARTED',
            });

            const {container} = render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(container.querySelector('.animate-spin.text-primary')).toBeInTheDocument();
        });

        it('should not render a spinner for a started task of an ended job', () => {
            const taskExecution = createTaskExecution({
                endDate: undefined,
                status: 'STARTED',
            });

            const {container} = render(<WorkflowTaskExecutionItem jobEnded taskExecution={taskExecution} />);

            expect(container.querySelector('.animate-spin')).not.toBeInTheDocument();
            expect(container.querySelector('.lucide-ban')).toBeInTheDocument();
            expect(container.querySelector('.text-destructive')).not.toBeInTheDocument();
        });

        it('should show a dash instead of a duration for an unfinished task of an ended job', () => {
            const taskExecution = createTaskExecution({
                endDate: undefined,
                status: 'STARTED',
            });

            render(<WorkflowTaskExecutionItem jobEnded taskExecution={taskExecution} />);

            expect(screen.getByText('-')).toBeInTheDocument();
            expect(screen.queryByText('0ms')).not.toBeInTheDocument();
        });

        it('should render a spinner for a started task of a running or suspended job', () => {
            const taskExecution = createTaskExecution({
                endDate: undefined,
                status: 'STARTED',
            });

            const {container} = render(<WorkflowTaskExecutionItem jobEnded={false} taskExecution={taskExecution} />);

            expect(container.querySelector('.animate-spin.text-primary')).toBeInTheDocument();
        });

        it('should render status icon from utility for failed task', () => {
            const taskExecution = createTaskExecution({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'FAILED',
            });

            const {container} = render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(container.querySelector('.text-destructive')).toBeInTheDocument();
        });
    });

    describe('task details display', () => {
        it('should display task label from workflowTask', () => {
            const taskExecution = createTaskExecution({
                title: 'Fallback Title',
                workflowTask: {
                    label: 'Task Label',
                    name: 'taskName',
                    type: 'test/v1/action',
                },
            });

            render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(screen.getByText('Task Label')).toBeInTheDocument();
        });

        it('should display title when workflowTask label is not available', () => {
            const taskExecution = createTaskExecution({
                title: 'Fallback Title',
                workflowTask: {
                    name: 'taskName',
                    type: 'test/v1/action',
                },
            });

            render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(screen.getByText('Fallback Title')).toBeInTheDocument();
        });

        it('should display duration when start and end dates are provided', () => {
            const taskExecution = createTaskExecution({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'COMPLETED',
            });

            render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(screen.getByText('5000ms')).toBeInTheDocument();
        });

        describe('while running', () => {
            afterEach(() => {
                vi.useRealTimers();
            });

            it('should display the elapsed time of a started task and keep it ticking', () => {
                vi.useFakeTimers({toFake: ['Date', 'setInterval', 'clearInterval']});
                vi.setSystemTime(new Date('2024-01-01T10:00:01.500'));

                const taskExecution = createTaskExecution({
                    endDate: undefined,
                    startDate: new Date('2024-01-01T10:00:00'),
                    status: 'STARTED',
                });

                render(<WorkflowTaskExecutionItem jobEnded={false} taskExecution={taskExecution} />);

                expect(screen.getByText('1500ms')).toBeInTheDocument();

                act(() => {
                    vi.advanceTimersByTime(1000);
                });

                expect(screen.getByText('2500ms')).toBeInTheDocument();
            });
        });

        it('should display a dash for a task that has not started', () => {
            const taskExecution = createTaskExecution({
                endDate: undefined,
                startDate: undefined,
                status: 'CREATED',
            });

            render(<WorkflowTaskExecutionItem jobEnded={false} taskExecution={taskExecution} />);

            expect(screen.getByText('-')).toBeInTheDocument();
            expect(screen.queryByText('0ms')).not.toBeInTheDocument();
        });
    });

    describe('attempt label', () => {
        it('shows the attempt label when one is given', () => {
            render(<WorkflowTaskExecutionItem attemptLabel="Attempt 3" taskExecution={createTaskExecution()} />);

            expect(screen.getByText('Attempt 3')).toBeInTheDocument();
        });

        it('shows no attempt label by default', () => {
            render(<WorkflowTaskExecutionItem taskExecution={createTaskExecution()} />);

            expect(screen.queryByText(/Attempt/)).not.toBeInTheDocument();
        });
    });
});
