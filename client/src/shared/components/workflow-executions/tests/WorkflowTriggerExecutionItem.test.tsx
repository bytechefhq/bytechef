import {TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowTriggerExecutionItem from '../WorkflowTriggerExecutionItem';

function createTriggerExecution(overrides: Partial<TriggerExecution> = {}): TriggerExecution {
    return {
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        title: 'Test Trigger',
        workflowTrigger: {
            label: 'Test Trigger Label',
            name: 'testTrigger',
            type: 'test/v1/testTrigger',
        },
        ...overrides,
    } as TriggerExecution;
}

describe('WorkflowTriggerExecutionItem', () => {
    describe('status icon integration', () => {
        it('should render status icon from utility for completed trigger', () => {
            const triggerExecution = createTriggerExecution({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'COMPLETED',
            });

            const {container} = render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(container.querySelector('.text-success')).toBeInTheDocument();
        });

        it('should render status icon from utility for started trigger', () => {
            const triggerExecution = createTriggerExecution({
                endDate: undefined,
                status: 'STARTED',
            });

            const {container} = render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(container.querySelector('.animate-spin.text-primary')).toBeInTheDocument();
        });

        it('should render status icon from utility for failed trigger', () => {
            const triggerExecution = createTriggerExecution({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'FAILED',
            });

            const {container} = render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(container.querySelector('.text-destructive')).toBeInTheDocument();
        });
    });

    describe('trigger details display', () => {
        it('should display trigger label from workflowTrigger', () => {
            const triggerExecution = createTriggerExecution({
                title: 'Fallback Title',
                workflowTrigger: {
                    label: 'Trigger Label',
                    name: 'triggerName',
                    type: 'test/v1/trigger',
                },
            });

            render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(screen.getByText('Trigger Label')).toBeInTheDocument();
        });

        it('should display title when workflowTrigger label is not available', () => {
            const triggerExecution = createTriggerExecution({
                title: 'Fallback Title',
                workflowTrigger: {
                    name: 'triggerName',
                    type: 'test/v1/trigger',
                },
            });

            render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(screen.getByText('Fallback Title')).toBeInTheDocument();
        });

        it('should display duration when start and end dates are provided', () => {
            const triggerExecution = createTriggerExecution({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'COMPLETED',
            });

            render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(screen.getByText('5000ms')).toBeInTheDocument();
        });

        it('should display 0ms when end date is not available', () => {
            const triggerExecution = createTriggerExecution({
                endDate: undefined,
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'STARTED',
            });

            render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(screen.getByText('0ms')).toBeInTheDocument();
        });
    });
});
