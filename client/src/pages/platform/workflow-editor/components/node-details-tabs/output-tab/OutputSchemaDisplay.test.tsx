import {WorkflowEditorReadOnlyContext} from '@/pages/platform/workflow-editor/providers/workflowEditorReadOnlyContext';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import OutputSchemaDisplay from './OutputSchemaDisplay';

vi.mock('@/pages/platform/workflow-editor/components/PropertyField', () => ({
    default: () => <div data-testid="property-field" />,
}));

vi.mock('@/pages/platform/workflow-editor/components/SchemaProperties', () => ({
    default: () => <div data-testid="schema-properties" />,
}));

vi.mock('./ClusterElementTestButton', () => ({
    default: () => <div data-testid="cluster-element-test-button" />,
}));

const currentNode = {
    componentName: 'subflow',
    label: 'Subflow',
    name: 'subflow_1',
    workflowNodeName: 'subflow_1',
} as NodeDataType;

const outputSchema = {
    controlType: 'OBJECT_BUILDER',
    properties: [{controlType: 'TEXT', name: 'message', type: 'STRING'}],
    type: 'OBJECT',
} as PropertyAllType;

const variableOutputSchema = {
    controlType: 'OBJECT_BUILDER',
    properties: [{controlType: 'TEXT', name: 'item', type: 'STRING'}],
    type: 'OBJECT',
} as PropertyAllType;

const renderOutputSchemaDisplay = (props: Partial<Parameters<typeof OutputSchemaDisplay>[0]> = {}) =>
    render(
        <OutputSchemaDisplay
            connectionMissing={false}
            copiedValue={null}
            copyToClipboard={vi.fn()}
            currentNode={currentNode}
            handlePredefinedOutputSchemaClick={vi.fn()}
            handleTestOperationClick={vi.fn()}
            outputDefined={true}
            outputSchema={outputSchema}
            sampleOutput={{message: 'sample message'}}
            saveWorkflowNodeTestOutputMutation={{isPending: false}}
            setShowUploadDialog={vi.fn()}
            {...props}
        />
    );

afterEach(() => {
    resetAll();
});

describe('OutputSchemaDisplay', () => {
    it('renders the test error alert between the header and the schema', () => {
        renderOutputSchemaDisplay({testErrorAlert: <div data-testid="test-error-alert">Test failed</div>});

        expect(screen.getByTestId('test-error-alert')).toHaveTextContent('Test failed');
    });

    it('should not render the Item Schema heading when there is no variable output schema', () => {
        renderOutputSchemaDisplay({variableOutputSchema: undefined, variablePropertiesDefined: true});

        expect(screen.queryByText('Item Schema')).not.toBeInTheDocument();
    });

    it('should render the Item Schema heading when a variable output schema is present', () => {
        renderOutputSchemaDisplay({variableOutputSchema, variablePropertiesDefined: true});

        expect(screen.getByText('Item Schema')).toBeInTheDocument();
    });

    it('should render the Output Schema section independently of the Item Schema section', () => {
        renderOutputSchemaDisplay({variableOutputSchema: undefined, variablePropertiesDefined: true});

        expect(screen.getByText('Output Schema')).toBeInTheDocument();
    });

    it('should render the test notice below the Output Schema header', () => {
        renderOutputSchemaDisplay({testNotice: <div data-testid="test-notice" />});

        expect(screen.getByTestId('test-notice')).toBeInTheDocument();
    });

    it('should warn that the output is placeholder data when it does not come from a test run', () => {
        renderOutputSchemaDisplay({testOutputResponse: false});

        expect(screen.getByText('Placeholder sample data')).toBeInTheDocument();
        expect(screen.getByText(/Click Test Action to get real data first/)).toBeInTheDocument();
    });

    it('should warn for a falsey placeholder sample output', () => {
        renderOutputSchemaDisplay({sampleOutput: 0 as unknown as object, testOutputResponse: false});

        expect(screen.getByText('Placeholder sample data')).toBeInTheDocument();
    });

    it('should not warn when there is no sample output at all', () => {
        renderOutputSchemaDisplay({sampleOutput: undefined, testOutputResponse: false});

        expect(screen.queryByText('Placeholder sample data')).not.toBeInTheDocument();
    });

    it('should not warn when the output comes from a test run', () => {
        renderOutputSchemaDisplay({testOutputResponse: true});

        expect(screen.queryByText('Placeholder sample data')).not.toBeInTheDocument();
    });

    it('should not warn for task dispatchers', () => {
        renderOutputSchemaDisplay({currentNode: {...currentNode, taskDispatcher: true}, testOutputResponse: false});

        expect(screen.queryByText('Placeholder sample data')).not.toBeInTheDocument();
    });

    it('should offer Test Action and no Upload Sample Output button for a testable action', () => {
        renderOutputSchemaDisplay();

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Upload Sample Output'})).not.toBeInTheDocument();
    });

    it('should offer Upload Sample Output instead of Test Action for a task dispatcher', () => {
        renderOutputSchemaDisplay({currentNode: {...currentNode, taskDispatcher: true}});

        expect(screen.getByRole('button', {name: 'Upload Sample Output'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Test Action'})).not.toBeInTheDocument();
    });

    it('should suggest uploading a sample output when the operation cannot be tested', () => {
        renderOutputSchemaDisplay({resumePerformFunctionDefined: true, testOutputResponse: false});

        expect(screen.getByText(/Upload a sample output to get real data first/)).toBeInTheDocument();
    });

    it('should offer testing and the output options when the editor is editable', () => {
        renderOutputSchemaDisplay();

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'More Options'})).toBeInTheDocument();
    });

    it('should hide testing, uploading and resetting the output in read-only mode', () => {
        render(
            <WorkflowEditorReadOnlyContext.Provider value={true}>
                <OutputSchemaDisplay
                    connectionMissing={false}
                    copiedValue={null}
                    copyToClipboard={vi.fn()}
                    currentNode={currentNode}
                    handlePredefinedOutputSchemaClick={vi.fn()}
                    handleTestOperationClick={vi.fn()}
                    outputDefined={true}
                    outputSchema={outputSchema}
                    sampleOutput={{message: 'sample message'}}
                    saveWorkflowNodeTestOutputMutation={{isPending: false}}
                    setShowUploadDialog={vi.fn()}
                />
            </WorkflowEditorReadOnlyContext.Provider>
        );

        expect(screen.getByText('Output Schema')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Test Action'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'More Options'})).not.toBeInTheDocument();
    });
});
