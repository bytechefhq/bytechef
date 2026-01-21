import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

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

        it('should display 0ms when end date is not available', () => {
            const taskExecution = createTaskExecution({
                endDate: undefined,
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'STARTED',
            });

            render(<WorkflowTaskExecutionItem taskExecution={taskExecution} />);

            expect(screen.getByText('0ms')).toBeInTheDocument();
        });
    });
});
