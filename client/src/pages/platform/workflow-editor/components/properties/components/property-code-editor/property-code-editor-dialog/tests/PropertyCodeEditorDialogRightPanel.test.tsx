import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanel from '../PropertyCodeEditorDialogRightPanel';

vi.mock('../PropertyCodeEditorDialogRightPanelInput', () => ({
    default: ({input}: {input: {[key: string]: object}}) => (
        <div data-testid="right-panel-inputs">
            <span data-testid="inputs-data">{JSON.stringify(input)}</span>
        </div>
    ),
}));

vi.mock('../PropertyCodeEditorDialogRightPanelConnections', () => ({
    default: ({
        componentConnections,
        workflowNodeName,
    }: {
        componentConnections: Array<{key: string}>;
        workflow: object;
        workflowNodeName: string;
    }) => (
        <div data-testid="right-panel-connections">
            <span data-testid="connections-count">{componentConnections.length}</span>

            <span data-testid="connections-node-name">{workflowNodeName}</span>
        </div>
    ),
}));

const mockUseClusterElementScriptInputQuery = vi.fn();
const mockUseWorkflowNodeScriptInputQuery = vi.fn();
const mockUseClusterElementComponentConnectionsQuery = vi.fn();
const mockUseWorkflowNodeComponentConnectionsQuery = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useClusterElementComponentConnectionsQuery: (...args: unknown[]) =>
        mockUseClusterElementComponentConnectionsQuery(...args),
    useClusterElementScriptInputQuery: (...args: unknown[]) => mockUseClusterElementScriptInputQuery(...args),
    useWorkflowNodeComponentConnectionsQuery: (...args: unknown[]) =>
        mockUseWorkflowNodeComponentConnectionsQuery(...args),
    useWorkflowNodeScriptInputQuery: (...args: unknown[]) => mockUseWorkflowNodeScriptInputQuery(...args),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: () => undefined,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: () => ({clusterElementType: undefined, name: 'testNode'}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: () => 1,
}));

describe('PropertyCodeEditorDialogRightPanel', () => {
    const defaultProps = {
        workflow: {
            definition: '{}',
            id: 'workflow-1',
            tasks: [
                {
                    name: 'testNode',
                    parameters: {
                        input: {param1: 'value1', param2: 'value2'},
                    },
                    type: 'script/script',
                },
            ],
            version: 1,
        },
        workflowNodeName: 'testNode',
    };

    const mockComponentConnections = [
        {componentName: 'slack', componentVersion: 1, key: 'slack_1', required: true, workflowNodeName: 'testNode'},
        {componentName: 'github', componentVersion: 1, key: 'github_1', required: false, workflowNodeName: 'testNode'},
    ];

    beforeEach(() => {
        windowResizeObserver();
        mockUseClusterElementScriptInputQuery.mockReturnValue({data: undefined});
        mockUseWorkflowNodeScriptInputQuery.mockReturnValue({
            data: {workflowNodeScriptInput: {param1: 'value1', param2: 'value2'}},
        });
        mockUseClusterElementComponentConnectionsQuery.mockReturnValue({data: undefined});
        mockUseWorkflowNodeComponentConnectionsQuery.mockReturnValue({
            data: {workflowNodeComponentConnections: mockComponentConnections},
        });
    });

    afterEach(() => {
        resetAll();
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render inputs panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('right-panel-inputs')).toBeInTheDocument();
        });

        it('should render connections panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('right-panel-connections')).toBeInTheDocument();
        });
    });

    describe('inputs panel', () => {
        it('should pass task parameters input to inputs panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            const inputsData = screen.getByTestId('inputs-data');

            expect(inputsData).toHaveTextContent('param1');
            expect(inputsData).toHaveTextContent('value1');
        });

        it('should pass empty object when query returns empty', () => {
            mockUseWorkflowNodeScriptInputQuery.mockReturnValue({
                data: {workflowNodeScriptInput: null},
            });

            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            const inputsData = screen.getByTestId('inputs-data');

            expect(inputsData).toHaveTextContent('{}');
        });
    });

    describe('connections panel', () => {
        it('should pass component connections from GraphQL query to connections panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('connections-count')).toHaveTextContent('2');
        });

        it('should pass workflow node name to connections panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('connections-node-name')).toHaveTextContent('testNode');
        });

        it('should pass empty array when query returns no connections', () => {
            mockUseWorkflowNodeComponentConnectionsQuery.mockReturnValue({
                data: {workflowNodeComponentConnections: null},
            });

            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('connections-count')).toHaveTextContent('0');
        });
    });
});
