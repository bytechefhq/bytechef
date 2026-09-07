import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowNodeDetailsPanel from '../components/WorkflowNodeDetailsPanel';

const mockHandleVersionSelectChange = vi.fn();
const mockUseWorkflowNodeDetailsPanel = vi.fn();

vi.mock('@/pages/platform/workflow-editor/components/hooks/useWorkflowNodeDetailsPanel', () => ({
    default: () => mockUseWorkflowNodeDetailsPanel(),
}));

vi.mock('@/pages/platform/workflow-editor/components/CurrentOperationSelect', () => ({
    default: () => <div>CurrentOperationSelect</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/DescriptionTab', () => ({
    default: () => <div>DescriptionTab</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/connection-tab/ConnectionTab', () => ({
    default: () => <div>ConnectionTab</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/node-details-tabs/output-tab/OutputTab', () => ({
    default: () => <div>OutputTab</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div>Properties</div>,
}));

vi.mock('@/shared/components/copilot/hooks/useCopilotLayoutShifted', () => ({
    default: () => false,
}));

const buildHookReturnValue = ({
    componentDefinitionVersions,
    nodeVersion,
}: {
    componentDefinitionVersions?: Array<{name: string; version: number}>;
    nodeVersion: string;
}) => ({
    activeDisplayConditionsQuery: undefined,
    activeTab: 'description',
    componentDefinitionVersions,
    currentActionDefinition: undefined,
    currentComponentDefinition: {name: 'test', version: Number(nodeVersion)},
    currentNode: {componentName: 'test', label: 'Test', name: 'test_1', workflowNodeName: 'test_1'},
    currentOperationName: 'get',
    currentOperationProperties: [],
    currentTaskDispatcherDefinition: undefined,
    currentTriggerDefinition: undefined,
    currentWorkflowNode: {name: 'test', title: 'Test', version: Number(nodeVersion)},
    currentWorkflowNodeConnections: [],
    currentWorkflowNodeOperations: [],
    errors: [],
    errorsAccordionOpen: false,
    errorsLoading: false,
    filteredClusterElementOperations: undefined,
    getNodeVersion: () => nodeVersion,
    handleOperationSelectChange: vi.fn(),
    handlePanelClose: vi.fn(),
    handleVersionSelectChange: mockHandleVersionSelectChange,
    nodeDefinition: {name: 'test', title: 'Test'},
    nodeTabs: [{label: 'Description', name: 'description'}],
    operationDataMissing: false,
    outputDefined: false,
    outputFunctionDefined: false,
    rootClusterElementNodeData: undefined,
    setActiveTab: vi.fn(),
    setErrorsAccordionOpen: vi.fn(),
    tabDataExists: true,
    workflow: {id: 'workflow-1'},
    workflowNodeDetailsPanelOpen: true,
    workflowTestConfigurationConnections: [],
});

const renderPanel = () =>
    render(
        <WorkflowNodeDetailsPanel
            previousComponentDefinitions={[]}
            updateWorkflowMutation={{} as never}
            workflowNodeOutputs={[]}
        />
    );

describe('WorkflowNodeDetailsPanel version select', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should show the version the node uses when it is not the first version', () => {
        mockUseWorkflowNodeDetailsPanel.mockReturnValue(
            buildHookReturnValue({
                componentDefinitionVersions: [
                    {name: 'test', version: 1},
                    {name: 'test', version: 2},
                ],
                nodeVersion: '2',
            })
        );

        renderPanel();

        expect(screen.getByRole('combobox', {name: 'Component version'})).toHaveTextContent('v2');
    });

    it('should offer every registered version of the component', async () => {
        mockUseWorkflowNodeDetailsPanel.mockReturnValue(
            buildHookReturnValue({
                componentDefinitionVersions: [
                    {name: 'test', version: 1},
                    {name: 'test', version: 2},
                ],
                nodeVersion: '2',
            })
        );

        renderPanel();

        fireEvent.click(screen.getByRole('combobox', {name: 'Component version'}));

        await waitFor(() => {
            expect(screen.getByRole('option', {name: 'v1'})).toBeInTheDocument();
            expect(screen.getByRole('option', {name: 'v2'})).toBeInTheDocument();
        });
    });

    it('should report the picked version to the version change handler', async () => {
        mockUseWorkflowNodeDetailsPanel.mockReturnValue(
            buildHookReturnValue({
                componentDefinitionVersions: [
                    {name: 'test', version: 1},
                    {name: 'test', version: 2},
                ],
                nodeVersion: '2',
            })
        );

        renderPanel();

        fireEvent.click(screen.getByRole('combobox', {name: 'Component version'}));

        await waitFor(() => {
            expect(screen.getByRole('option', {name: 'v1'})).toBeInTheDocument();
        });

        fireEvent.click(screen.getByRole('option', {name: 'v1'}));

        expect(mockHandleVersionSelectChange).toHaveBeenCalledWith('1');
    });

    it('should still show the version the node uses before the versions are fetched', () => {
        mockUseWorkflowNodeDetailsPanel.mockReturnValue(
            buildHookReturnValue({componentDefinitionVersions: undefined, nodeVersion: '3'})
        );

        renderPanel();

        expect(screen.getByRole('combobox', {name: 'Component version'})).toHaveTextContent('v3');
    });
});
