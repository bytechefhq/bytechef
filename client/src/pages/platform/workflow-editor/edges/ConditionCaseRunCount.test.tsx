import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {render, screen} from '@testing-library/react';
import {afterEach, describe, expect, it} from 'vitest';

import {WorkflowExecutionOverlayContext} from '../providers/workflowExecutionOverlayContext';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import ConditionCaseRunCount from './ConditionCaseRunCount';

const conditionParameters = {caseFalse: [{name: 'odd_label'}], caseTrue: [{name: 'even_label'}]};

const conditionTaskExecution = (childName: string) => ({
    children: [{status: 'COMPLETED', workflowTask: {name: childName}}],
    status: 'COMPLETED',
    workflowTask: {name: 'condition_1', parameters: conditionParameters, type: 'condition/v1'},
});

const loopTaskExecutions = (childNames: string[]) => [
    {
        iterations: childNames.map((childName) => [conditionTaskExecution(childName)]),
        status: 'COMPLETED',
        workflowTask: {name: 'loop_1', type: 'loop/v1'},
    },
];

const setTestRun = (childNames: string[]) =>
    useWorkflowEditorStore.setState({
        workflowTestExecution: {
            job: {taskExecutions: loopTaskExecutions(childNames)},
        } as unknown as WorkflowTestExecution,
    });

describe('ConditionCaseRunCount', () => {
    afterEach(() => {
        useWorkflowEditorStore.setState({workflowTestExecution: undefined});
    });

    it('shows how many times the test run took the case', () => {
        setTestRun(['odd_label', 'even_label', 'odd_label', 'even_label', 'odd_label']);

        render(<ConditionCaseRunCount conditionCase="caseFalse" conditionName="condition_1" />);

        expect(screen.getByText('×3')).toHaveAttribute('title', 'Ran 3 times in this run');
    });

    it('shows nothing when the case ran only once', () => {
        setTestRun(['odd_label', 'even_label', 'even_label']);

        const {container} = render(<ConditionCaseRunCount conditionCase="caseFalse" conditionName="condition_1" />);

        expect(container).toBeEmptyDOMElement();
    });

    it('shows nothing without a run', () => {
        const {container} = render(<ConditionCaseRunCount conditionCase="caseTrue" conditionName="condition_1" />);

        expect(container).toBeEmptyDOMElement();
    });

    it('counts the execution the read-only viewer overlays instead of the test run', () => {
        setTestRun(['even_label', 'even_label', 'even_label']);

        render(
            <WorkflowExecutionOverlayContext.Provider
                value={{
                    isRunning: false,
                    nodeStates: {},
                    taskExecutions: loopTaskExecutions(['even_label', 'even_label']) as never,
                }}
            >
                <ConditionCaseRunCount conditionCase="caseTrue" conditionName="condition_1" />
            </WorkflowExecutionOverlayContext.Provider>
        );

        expect(screen.getByText('×2')).toBeInTheDocument();
    });
});
