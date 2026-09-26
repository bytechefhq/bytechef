import {CLUSTER_ELEMENT_TYPE_TOOLS} from '@/shared/constants';
import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import OutputTab from './OutputTab';

const hoisted = vi.hoisted(() => ({
    clearTestOutputError: vi.fn(),
    handleNoOutputNoticeDismiss: vi.fn(),
    outputTabState: {} as Record<string, unknown>,
}));

vi.mock('./hooks/useOutputTab', () => ({
    default: () => hoisted.outputTabState,
}));

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaDisplay', () => ({
    default: ({testErrorAlert, testNotice}: {testErrorAlert?: ReactNode; testNotice?: ReactNode}) => (
        <div data-testid="output-schema-display">
            {testErrorAlert}

            {testNotice}
        </div>
    ),
}));

vi.mock(
    '@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaCreationControls',
    () => ({
        default: () => <div data-testid="output-schema-creation-controls" />,
    })
);

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog', () => ({
    default: () => null,
}));

const currentNode = {
    componentName: 'dataStorage',
    name: 'dataStorage_1',
    workflowNodeName: 'dataStorage_1',
} as NodeDataType;

const outputSchema = {controlType: 'ARRAY_BUILDER', type: 'ARRAY'} as unknown as PropertyAllType;

const testOutputError = {message: 'All scraping engines failed', title: 'Test failed'};

const renderOutputTab = (props: Partial<Parameters<typeof OutputTab>[0]> = {}) =>
    render(<OutputTab connectionMissing={false} currentNode={currentNode} workflowId="wf-1" {...props} />);

beforeEach(() => {
    hoisted.outputTabState = {
        clearTestOutputError: hoisted.clearTestOutputError,
        handleNoOutputNoticeDismiss: hoisted.handleNoOutputNoticeDismiss,
        outputSchema: undefined,
        setShowUploadDialog: vi.fn(),
        showUploadDialog: false,
        testOutputError: undefined,
        testReturnedNoOutput: false,
        testing: false,
        workflowNodeOutputIsFetching: false,
    };
});

afterEach(() => {
    resetAll();
});

describe('OutputTab', () => {
    it('shows neither an error alert nor a notice when the last test neither failed nor came back empty', () => {
        renderOutputTab({outputDefined: true});

        expect(screen.getByTestId('output-schema-creation-controls')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    describe('a failed test', () => {
        it('shows the error above the creation controls when the node has no output schema', async () => {
            hoisted.outputTabState.testOutputError = testOutputError;

            renderOutputTab({outputDefined: true});

            expect(screen.getByRole('alert')).toHaveTextContent('All scraping engines failed');
            expect(screen.getByTestId('output-schema-creation-controls')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Dismiss error'}));

            expect(hoisted.clearTestOutputError).toHaveBeenCalledTimes(1);
        });

        it('passes the error alert to the output schema display when the node has an output schema', async () => {
            hoisted.outputTabState.outputSchema = outputSchema;
            hoisted.outputTabState.testOutputError = testOutputError;

            renderOutputTab({outputDefined: true});

            const outputSchemaDisplay = screen.getByTestId('output-schema-display');

            expect(outputSchemaDisplay).toHaveTextContent('All scraping engines failed');
            expect(screen.queryByTestId('output-schema-creation-controls')).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Dismiss error'}));

            expect(hoisted.clearTestOutputError).toHaveBeenCalledTimes(1);
        });
    });

    describe('a test that returned no data', () => {
        it('shows the notice above the creation controls when the node has no output schema', async () => {
            hoisted.outputTabState.testReturnedNoOutput = true;

            renderOutputTab();

            expect(screen.getByRole('alert')).toHaveTextContent('The action ran successfully but returned no data.');
            expect(screen.getByTestId('output-schema-creation-controls')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Dismiss notice'}));

            expect(hoisted.handleNoOutputNoticeDismiss).toHaveBeenCalledTimes(1);
        });

        it('passes the notice to the output schema display when the node has an output schema', async () => {
            hoisted.outputTabState.outputSchema = outputSchema;
            hoisted.outputTabState.testReturnedNoOutput = true;

            renderOutputTab({outputDefined: true});

            expect(screen.getByTestId('output-schema-display')).toHaveTextContent(
                'The action ran successfully but returned no data.'
            );
            expect(screen.queryByTestId('output-schema-creation-controls')).not.toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Dismiss notice'}));

            expect(hoisted.handleNoOutputNoticeDismiss).toHaveBeenCalledTimes(1);
        });

        it('names a trigger in the notice', () => {
            hoisted.outputTabState.testReturnedNoOutput = true;

            renderOutputTab({currentNode: {...currentNode, trigger: true}});

            expect(screen.getByRole('alert')).toHaveTextContent('The trigger ran successfully but returned no data.');
        });

        it('names a tool in the notice', () => {
            hoisted.outputTabState.testReturnedNoOutput = true;

            renderOutputTab({clusterElementType: CLUSTER_ELEMENT_TYPE_TOOLS});

            expect(screen.getByRole('alert')).toHaveTextContent('The tool ran successfully but returned no data.');
        });
    });
});
