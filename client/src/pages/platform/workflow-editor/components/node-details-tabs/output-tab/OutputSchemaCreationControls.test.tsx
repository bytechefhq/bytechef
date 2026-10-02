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

        expect(screen.getByRole('button', {name: 'Test Action'})).toBeInTheDocument();
        expect(screen.getByText('or')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Upload Sample Output Data'})).toBeInTheDocument();
        expect(screen.getByText('Define the expected output schema with one of the methods')).toBeInTheDocument();
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
});
