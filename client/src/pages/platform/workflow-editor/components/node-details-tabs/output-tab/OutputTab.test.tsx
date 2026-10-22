import {CLUSTER_ELEMENT_TYPE_TOOLS} from '@/shared/constants';
import {TriggerType} from '@/shared/middleware/platform/configuration';
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
        default: ({testable}: {testable?: boolean}) => (
            <div data-testable={String(testable)} data-testid="output-schema-creation-controls" />
        ),
    })
);

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTabSampleDataDialog', () => ({
    default: () => null,
}));

vi.mock('@/pages/platform/workflow-editor/components/WorkflowEditorSkeletons', () => ({
    OutputTabSkeleton: () => <div data-testid="output-tab-skeleton" />,
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
        workflowNodeOutputLoading: false,
    };
});

afterEach(() => {
    resetAll();
});

describe('OutputTab', () => {
    it('shows a skeleton instead of the output while the output is loading', () => {
        hoisted.outputTabState.workflowNodeOutputLoading = true;

        renderOutputTab({outputDefined: true});

        expect(screen.getByTestId('output-tab-skeleton')).toBeInTheDocument();
        expect(screen.queryByTestId('output-schema-creation-controls')).not.toBeInTheDocument();
    });

    it('keeps showing a running test while the output is loading', () => {
        hoisted.outputTabState.testing = true;
        hoisted.outputTabState.workflowNodeOutputLoading = true;

        renderOutputTab({outputDefined: true});

        expect(screen.queryByTestId('output-tab-skeleton')).not.toBeInTheDocument();
    });

    it('shows neither an error alert nor a notice when the last test neither failed nor came back empty', () => {
        renderOutputTab({outputDefined: true});

        expect(screen.getByTestId('output-schema-creation-controls')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('marks the creation controls testable for an action', () => {
        renderOutputTab({outputDefined: true});

        expect(screen.getByTestId('output-schema-creation-controls')).toHaveAttribute('data-testable', 'true');
    });

    it('marks the creation controls untestable for a task dispatcher', () => {
        renderOutputTab({currentNode: {...currentNode, taskDispatcher: true}, outputDefined: true});

        expect(screen.getByTestId('output-schema-creation-controls')).toHaveAttribute('data-testable', 'false');
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

    describe('a running test', () => {
        beforeEach(() => {
            hoisted.outputTabState.copyToClipboard = vi.fn();
            hoisted.outputTabState.handleTestCancelClick = vi.fn();
            hoisted.outputTabState.testing = true;
            hoisted.outputTabState.webhookTestCancelEnabled = true;
            hoisted.outputTabState.webhookTestUrl = 'https://example.com/webhooks/test';
        });

        it('shows the action being tested without a cancel button', () => {
            renderOutputTab();

            expect(screen.getByText('Testing Action')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
            expect(screen.queryByTestId('output-schema-creation-controls')).not.toBeInTheDocument();
        });

        it('shows the tool being tested', () => {
            renderOutputTab({clusterElementType: CLUSTER_ELEMENT_TYPE_TOOLS});

            expect(screen.getByText('Testing Tool')).toBeInTheDocument();
        });

        it('shows a polling trigger being tested without asking for an action', () => {
            renderOutputTab({currentNode: {...currentNode, trigger: true, triggerType: TriggerType.Polling}});

            expect(screen.getByText('Testing Trigger')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
            expect(screen.queryByText('Action Required')).not.toBeInTheDocument();
        });

        it('asks to call the test URL of a static webhook trigger and lets the test be cancelled', async () => {
            renderOutputTab({currentNode: {...currentNode, trigger: true, triggerType: TriggerType.StaticWebhook}});

            expect(screen.getByText('Testing Trigger')).toBeInTheDocument();
            expect(screen.getByText('Action Required')).toBeInTheDocument();
            expect(screen.getByDisplayValue('https://example.com/webhooks/test')).toBeInTheDocument();

            await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(hoisted.outputTabState.handleTestCancelClick).toHaveBeenCalledTimes(1);
        });

        it('asks to act in the service of a dynamic webhook trigger', () => {
            renderOutputTab({
                currentNode: {...currentNode, trigger: true, triggerType: TriggerType.DynamicWebhook},
            });

            expect(
                screen.getByText('Please go to your service and make an action that will activate this trigger')
            ).toBeInTheDocument();
            expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        });
    });
});
