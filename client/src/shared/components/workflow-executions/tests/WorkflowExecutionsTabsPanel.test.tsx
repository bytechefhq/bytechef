import {Job, TaskExecution, TriggerExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionsTabsPanel from '../WorkflowExecutionsTabsPanel';

const {logsContentMock} = vi.hoisted(() => ({logsContentMock: vi.fn()}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionLogsContent', () => ({
    default: (props: Record<string, unknown>) => {
        logsContentMock(props);

        return <div data-testid="logs-content" />;
    },
}));

vi.mock('@/shared/components/JsonView', () => ({
    default: ({src}: {src: object}) => <div data-testid="json-view">{JSON.stringify(src)}</div>,
}));

const triggerExecution = {
    id: '77',
    output: {orderId: '42'},
    startDate: new Date('2026-09-04T10:00:00Z'),
    workflowTrigger: {name: 'trigger_1'},
} as unknown as TriggerExecution;

const taskExecution = {
    id: '10',
    output: {rows: 3},
    startDate: new Date('2026-09-04T10:00:01Z'),
    workflowTask: {name: 'task_1'},
} as unknown as TaskExecution;

const job = {id: '5', inputs: {trigger_1: {orderId: '42', signature: 'abc'}}} as unknown as Job;

const renderPanel = (props: Partial<Parameters<typeof WorkflowExecutionsTabsPanel>[0]>) =>
    render(
        <WorkflowExecutionsTabsPanel
            activeTab="logs"
            dialogOpen={false}
            selectedItem={triggerExecution}
            setActiveTab={vi.fn()}
            setDialogOpen={vi.fn()}
            triggerExecution={triggerExecution}
            {...props}
        />
    );

describe('WorkflowExecutionsTabsPanel', () => {
    it('reads the trigger execution logs for a row that never produced a job', () => {
        logsContentMock.mockClear();

        renderPanel({activeTab: 'logs'});

        expect(screen.getByTestId('logs-content')).toBeInTheDocument();
        expect(logsContentMock).toHaveBeenCalledWith(
            expect.objectContaining({jobId: undefined, triggerExecutionId: '77'})
        );
    });

    it('reads the job logs, scoped to the selected task, for a task of a job', () => {
        logsContentMock.mockClear();

        renderPanel({activeTab: 'logs', job, selectedItem: taskExecution});

        expect(logsContentMock).toHaveBeenCalledWith(
            expect.objectContaining({jobId: '5', taskExecutionId: '10', triggerExecutionId: undefined})
        );
    });

    it('has no logs to read when there is neither a job nor a trigger execution', () => {
        renderPanel({activeTab: 'logs', selectedItem: undefined, triggerExecution: undefined});

        expect(screen.queryByTestId('logs-content')).not.toBeInTheDocument();
    });

    it('shows what the trigger handed the job on the output tab', () => {
        renderPanel({activeTab: 'output', job});

        expect(screen.getByTestId('json-view')).toHaveTextContent('signature');
    });

    it('shows a task output as it is on the output tab', () => {
        renderPanel({activeTab: 'output', job, selectedItem: taskExecution});

        expect(screen.getByTestId('json-view')).toHaveTextContent('rows');
    });
});
