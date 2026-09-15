import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useClusterElementStepMock} = vi.hoisted(() => ({
    useClusterElementStepMock: vi.fn(),
}));

vi.mock('@/pages/platform/cluster-element-editor/data-stream-editor/hooks/useClusterElementStep', () => ({
    default: useClusterElementStepMock,
}));

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTab', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => null,
}));

import DataStreamDestinationStep from './DataStreamDestinationStep';
import DataStreamSourceStep from './DataStreamSourceStep';

const renderStep = (step: 'destination' | 'source', readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            {step === 'source' ? <DataStreamSourceStep /> : <DataStreamDestinationStep />}
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe.each(['source', 'destination'] as const)('DataStream %s step', (step) => {
    beforeEach(() => {
        vi.clearAllMocks();

        useClusterElementStepMock.mockReturnValue({
            componentConnections: [],
            displayConditionsQuery: undefined,
            elementComponentDefinition: undefined,
            elementItem: undefined,
            elementProperties: [],
            handleComponentChange: vi.fn(),
            handleOperationChange: vi.fn(),
            rootWorkflowNodeName: 'dataStream_1',
            selectedComponentName: 'postgresql',
            selectedOperationName: 'insert',
            stepComponentDefinitions: [{name: 'postgresql', title: 'PostgreSQL'}],
            stepOperations: [{name: 'insert', title: 'Insert'}],
            testConnections: [],
            workflowId: 'workflow-id',
        });
    });

    it('lets the component and operation be changed when the editor is editable', () => {
        renderStep(step, false);

        expect(screen.getByLabelText('Component')).toBeEnabled();
        expect(screen.getByLabelText('Operation')).toBeEnabled();
    });

    it('disables the component and operation selects in read-only mode', () => {
        renderStep(step, true);

        expect(screen.getByLabelText('Component')).toBeDisabled();
        expect(screen.getByLabelText('Operation')).toBeDisabled();
    });
});
