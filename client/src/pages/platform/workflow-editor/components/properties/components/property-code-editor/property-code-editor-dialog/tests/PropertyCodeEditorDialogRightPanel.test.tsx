import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import PropertyCodeEditorDialogRightPanel from '../PropertyCodeEditorDialogRightPanel';

vi.mock('../PropertyCodeEditorDialogRightPanelInputs', () => ({
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

vi.mock('@/pages/platform/workflow-editor/utils/getTask', () => ({
    getTask: ({
        tasks,
        workflowNodeName,
    }: {
        tasks: Array<{name: string; parameters?: {input?: object}}>;
        workflowNodeName: string;
    }) => {
        return tasks.find((task) => task.name === workflowNodeName);
    },
}));

describe('PropertyCodeEditorDialogRightPanel', () => {
    const defaultProps = {
        componentConnections: [
            {componentName: 'slack', componentVersion: 1, key: 'slack_1', required: true, workflowNodeName: 'testNode'},
            {
                componentName: 'github',
                componentVersion: 1,
                key: 'github_1',
                required: false,
                workflowNodeName: 'testNode',
            },
        ],
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

    beforeEach(() => {
        windowResizeObserver();
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

        it('should pass empty object when task has no parameters', () => {
            const propsWithNoParams = {
                ...defaultProps,
                workflow: {
                    ...defaultProps.workflow,
                    tasks: [{name: 'testNode', type: 'script/script'}],
                },
            };

            render(<PropertyCodeEditorDialogRightPanel {...propsWithNoParams} />);

            const inputsData = screen.getByTestId('inputs-data');

            expect(inputsData).toHaveTextContent('{}');
        });
    });

    describe('connections panel', () => {
        it('should pass component connections to connections panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('connections-count')).toHaveTextContent('2');
        });

        it('should pass workflow node name to connections panel', () => {
            render(<PropertyCodeEditorDialogRightPanel {...defaultProps} />);

            expect(screen.getByTestId('connections-node-name')).toHaveTextContent('testNode');
        });
    });
});
