import {render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionsTestOutput from '../WorkflowExecutionsTestOutput';

vi.mock('../properties/hooks/useWorkflowExecutions', () => ({
    default: () => ({
        activeTab: 'output',
        deepestFailedExecution: undefined,
        dialogOpen: false,
        handleBreadcrumbNavigate: vi.fn(),
        handleExecutionClick: vi.fn(),
        handleSeeExecutions: vi.fn(),
        isTriggerExecution: false,
        job: undefined,
        jobFailedWithNoExecutions: false,
        jobFailureError: undefined,
        rootJob: undefined,
        selectedExecution: undefined,
        setActiveTab: vi.fn(),
        setDialogOpen: vi.fn(),
        subflowStack: [],
        taskExecutions: [],
        triggerExecution: undefined,
    }),
}));

describe('WorkflowExecutionsTestOutput', () => {
    it('lays out the task list and details panes while a run waits for its first progress snapshot', () => {
        render(<WorkflowExecutionsTestOutput workflowIsRunning />);

        expect(screen.queryByText('Workflow is running...')).not.toBeInTheDocument();
        expect(screen.getByTestId('workflow-test-output-pending')).toBeInTheDocument();
    });
});
