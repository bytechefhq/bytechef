import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import OutputSchemaCreationControls from './OutputSchemaCreationControls';

vi.mock('./ClusterElementTestButton', () => ({
    default: () => <div data-testid="cluster-element-test-button" />,
}));

const renderOutputSchemaCreationControls = (props: Partial<Parameters<typeof OutputSchemaCreationControls>[0]> = {}) =>
    render(
        <OutputSchemaCreationControls
            handleTestOperationClick={vi.fn()}
            outputDefined
            saveWorkflowNodeTestOutputMutationPending={false}
            setShowUploadDialog={vi.fn()}
            showUploadSampleOutputButton
            uploadSampleOutputRequestMutationPending={false}
            {...props}
        />
    );

afterEach(() => {
    resetAll();
});

describe('OutputSchemaCreationControls', () => {
    it('offers both Test Action and Upload Sample Output Data for a testable node', () => {
        renderOutputSchemaCreationControls();

        expect(screen.getByRole('heading', {level: 3, name: 'Define Output Schema'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test Action'})).toBeInTheDocument();
        expect(screen.getByText('or')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Upload Sample Output Data'})).toBeInTheDocument();
        expect(screen.getByText('Define the expected output schema with one of the methods')).toBeInTheDocument();
    });

    it('disables Test Action and lists the missing required properties when one is missing', () => {
        renderOutputSchemaCreationControls({missingRequiredProperties: ['Key']});

        const notice = screen.getByRole('alert');

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeDisabled();
        expect(notice).toHaveTextContent('Set these properties to test the action:Key');
        expect(notice.compareDocumentPosition(screen.getByRole('button', {name: 'Test Action'}))).toBe(
            Node.DOCUMENT_POSITION_FOLLOWING
        );
    });

    it('disables Test Action when the connection is missing', () => {
        renderOutputSchemaCreationControls({connectionMissing: true});

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeDisabled();
    });

    it('offers only Upload Sample Output Data for a node that cannot be tested', () => {
        renderOutputSchemaCreationControls({testable: false});

        expect(screen.queryByRole('button', {name: 'Test Action'})).not.toBeInTheDocument();
        expect(screen.queryByText('or')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Upload Sample Output Data'})).toBeInTheDocument();
        expect(
            screen.getByText('Define the expected output schema by uploading sample output data')
        ).toBeInTheDocument();
    });

    it('asks to test an action that does not define its output', () => {
        renderOutputSchemaCreationControls({outputDefined: false, showUploadSampleOutputButton: false});

        expect(screen.getByRole('heading', {level: 3, name: 'Action Test'})).toBeInTheDocument();
        expect(
            screen.getByText('Test the action to see the expected behavior as this action does not define the output.')
        ).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Upload Sample Output Data'})).not.toBeInTheDocument();
    });

    it('asks to test a trigger that does not define its output', () => {
        renderOutputSchemaCreationControls({outputDefined: false, showUploadSampleOutputButton: false, trigger: true});

        expect(screen.getByRole('heading', {level: 3, name: 'Trigger Test'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test Trigger'})).toBeInTheDocument();
    });

    it('asks to explore the output schema of a tool', () => {
        renderOutputSchemaCreationControls({clusterElementType: 'tools'});

        expect(screen.getByRole('heading', {level: 3, name: 'Explore Output Schema'})).toBeInTheDocument();
        expect(screen.getByText('Explore the expected output schema by testing the tool')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test Tool'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Upload Sample Output Data'})).not.toBeInTheDocument();
    });
});
