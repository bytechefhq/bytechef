import {NodeDataType, PropertyAllType} from '@/shared/types';
import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import OutputTab from './OutputTab';

const hoisted = vi.hoisted(() => ({
    clearTestOutputError: vi.fn(),
    outputTabState: {} as Record<string, unknown>,
}));

vi.mock('./hooks/useOutputTab', () => ({
    default: () => hoisted.outputTabState,
}));

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputSchemaDisplay', () => ({
    default: ({testErrorAlert}: {testErrorAlert?: ReactNode}) => (
        <div data-testid="output-schema-display">{testErrorAlert}</div>
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

const currentNode = {componentName: 'firecrawl', name: 'firecrawl_1', workflowNodeName: 'firecrawl_1'} as NodeDataType;

const outputSchema = {controlType: 'OBJECT_BUILDER', properties: [], type: 'OBJECT'} as unknown as PropertyAllType;

const testOutputError = {message: 'All scraping engines failed', title: 'Test failed'};

const renderOutputTab = () =>
    render(<OutputTab connectionMissing={false} currentNode={currentNode} outputDefined workflowId="wf-1" />);

beforeEach(() => {
    hoisted.outputTabState = {
        clearTestOutputError: hoisted.clearTestOutputError,
        outputSchema: undefined,
        setShowUploadDialog: vi.fn(),
        showUploadDialog: false,
        testOutputError: undefined,
        testing: false,
        workflowNodeOutputIsFetching: false,
    };
});

afterEach(() => {
    resetAll();
});

describe('OutputTab', () => {
    it('shows no error alert when the last test did not fail', () => {
        renderOutputTab();

        expect(screen.getByTestId('output-schema-creation-controls')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('shows the error above the creation controls when the node has no output schema', async () => {
        hoisted.outputTabState.testOutputError = testOutputError;

        renderOutputTab();

        expect(screen.getByRole('alert')).toHaveTextContent('All scraping engines failed');
        expect(screen.getByTestId('output-schema-creation-controls')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Dismiss error'}));

        expect(hoisted.clearTestOutputError).toHaveBeenCalledTimes(1);
    });

    it('passes the error alert to the output schema display when the node has an output schema', async () => {
        hoisted.outputTabState.outputSchema = outputSchema;
        hoisted.outputTabState.testOutputError = testOutputError;

        renderOutputTab();

        const outputSchemaDisplay = screen.getByTestId('output-schema-display');

        expect(outputSchemaDisplay).toHaveTextContent('All scraping engines failed');
        expect(screen.queryByTestId('output-schema-creation-controls')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Dismiss error'}));

        expect(hoisted.clearTestOutputError).toHaveBeenCalledTimes(1);
    });
});
