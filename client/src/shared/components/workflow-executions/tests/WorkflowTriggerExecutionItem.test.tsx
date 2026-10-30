import {TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {act, render, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

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

        describe('while running', () => {
            afterEach(() => {
                vi.useRealTimers();
            });

            it('should display the elapsed time of a started trigger and keep it ticking', () => {
                vi.useFakeTimers({toFake: ['Date', 'setInterval', 'clearInterval']});
                vi.setSystemTime(new Date('2024-01-01T10:00:00.750'));

                const triggerExecution = createTriggerExecution({
                    endDate: undefined,
                    startDate: new Date('2024-01-01T10:00:00'),
                    status: 'STARTED',
                });

                render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

                expect(screen.getByText('750ms')).toBeInTheDocument();

                act(() => {
                    vi.advanceTimersByTime(1000);
                });

                expect(screen.getByText('1750ms')).toBeInTheDocument();
            });
        });

        it('should display a dash for a trigger without an end date that is not running', () => {
            const triggerExecution = createTriggerExecution({
                endDate: undefined,
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'FAILED',
            });

            render(<WorkflowTriggerExecutionItem triggerExecution={triggerExecution} />);

            expect(screen.getByText('-')).toBeInTheDocument();
            expect(screen.queryByText('0ms')).not.toBeInTheDocument();
        });
    });
});
