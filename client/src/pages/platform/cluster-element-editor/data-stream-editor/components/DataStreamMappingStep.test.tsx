import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {useDataStreamMappingMock} = vi.hoisted(() => ({
    useDataStreamMappingMock: vi.fn(),
}));

vi.mock('@/pages/platform/cluster-element-editor/data-stream-editor/hooks/useDataStreamMapping', () => ({
    default: useDataStreamMappingMock,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => null,
}));

import DataStreamMappingStep from './DataStreamMappingStep';

const renderStep = (readOnly: boolean) =>
    render(
        <WorkflowEditorReadOnlyContext.Provider value={readOnly}>
            <DataStreamMappingStep />
        </WorkflowEditorReadOnlyContext.Provider>
    );

describe('DataStreamMappingStep', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        useDataStreamMappingMock.mockReturnValue({
            autoMapping: false,
            destinationLabel: 'PostgreSQL',
            displayConditionsQuery: undefined,
            handleAutoMap: vi.fn(),
            hasSourceAndDestination: true,
            processor: {name: 'fieldMapper_1', operationName: 'map'},
            processorProperties: [],
            propertiesKey: 'fieldMapper_1',
            sourceLabel: 'CSV',
        });
    });

    it('offers Auto-map when the editor is editable', () => {
        renderStep(false);

        expect(screen.getByRole('button', {name: 'Auto-map matching fields'})).toBeInTheDocument();
    });

    it('hides Auto-map in read-only mode', () => {
        renderStep(true);

        expect(screen.queryByRole('button', {name: 'Auto-map matching fields'})).not.toBeInTheDocument();
    });
});
