import {TooltipProvider} from '@/components/ui/tooltip';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {render} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionSheet from '../WorkflowExecutionSheet';

const {sheetState} = vi.hoisted(() => ({
    sheetState: {workflowExecution: undefined as unknown, workflowExecutionLoading: false},
}));

vi.mock('../hooks/useWorkflowExecutionSheet', () => ({
    default: () => ({
        copilotEnabled: false,
        copilotPanelOpen: false,
        handleCopilotClick: vi.fn(),
        handleCopilotClose: vi.fn(),
        handleOpenChange: vi.fn(),
        workflowExecution: sheetState.workflowExecution,
        workflowExecutionId: 5,
        workflowExecutionLoading: sheetState.workflowExecutionLoading,
        workflowExecutionSheetOpen: true,
    }),
}));

vi.mock('../WorkflowExecutionDetail', () => ({default: () => <div>Execution detail</div>}));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({default: () => null}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: () => () => false,
}));

const renderSheet = (workflowExecution: object) => {
    sheetState.workflowExecution = workflowExecution as WorkflowExecution;

    return render(
        <TooltipProvider>
            <WorkflowExecutionSheet />
        </TooltipProvider>
    );
};

describe('WorkflowExecutionSheet', () => {
    beforeEach(() => {
        sheetState.workflowExecution = undefined;
        sheetState.workflowExecutionLoading = false;
    });

    it('shows the project version the job ran with after the workflow in the header', () => {
        renderSheet({
            job: {id: '5', metadata: {projectVersion: 2}},
            project: {name: 'Sales'},
            projectDeployment: {projectVersion: 3},
            workflow: {label: 'Order intake'},
        });

        expect(document.querySelector('header')).toHaveTextContent('Sales /Order intake/ V2');
    });

    it('falls back to the deployment version when the job has no project version metadata', () => {
        renderSheet({
            job: {id: '5'},
            project: {name: 'Sales'},
            projectDeployment: {projectVersion: 3},
            workflow: {label: 'Order intake'},
        });

        expect(document.querySelector('header')).toHaveTextContent('Sales /Order intake/ V3');
    });

    it('shows no version when none is known', () => {
        renderSheet({job: {id: '5'}, project: {name: 'Sales'}, workflow: {label: 'Order intake'}});

        expect(document.querySelector('header')).toHaveTextContent('Sales /Order intake');
        expect(document.querySelector('header')).not.toHaveTextContent('/ V');
    });
});
