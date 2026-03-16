import {getClusterElementByName} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {getTask} from '@/pages/platform/workflow-editor/utils/getTask';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import React from 'react';
import {type Mock, beforeEach, describe, expect, it, vi} from 'vitest';

import useClusterElementStep from './useClusterElementStep';

// --- Mocks ---

vi.mock('zustand/shallow', () => ({
    useShallow: (selector: unknown) => selector,
}));

const mockWorkflowEditorStoreState = vi.fn();

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: unknown) => mockWorkflowEditorStoreState(selector),
}));

const mockWorkflowDataStoreState = vi.fn();

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: unknown) => mockWorkflowDataStoreState(selector),
}));

const mockNodeDetailsPanelStoreState = vi.fn();

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: unknown) => mockNodeDetailsPanelStoreState(selector),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({updateWorkflowMutation: vi.fn()}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: unknown) => {
        if (typeof selector === 'function') {
            return (selector as (state: {currentEnvironmentId: number}) => unknown)({currentEnvironmentId: 1});
        }

        return 1;
    },
}));

const mockGetComponentDefinitionQuery: Mock = vi.fn();

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: (...args: unknown[]) => mockGetComponentDefinitionQuery(...args),
}));

const mockGetDisplayConditionsQuery: Mock = vi.fn();

vi.mock('@/shared/queries/platform/workflowNodeParameters.queries', () => ({
    useGetClusterElementParameterDisplayConditionsQuery: (...args: unknown[]) => mockGetDisplayConditionsQuery(...args),
}));

const mockGetTestConnectionsQuery: Mock = vi.fn();

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationConnectionsQuery: (...args: unknown[]) => mockGetTestConnectionsQuery(...args),
}));

vi.mock('@/pages/platform/cluster-element-editor/utils/clusterElementsUtils', () => ({
    convertNameToSnakeCase: (value: string) =>
        value.replace(/[A-Z]/g, (letter: string) => `_${letter.toLowerCase()}`).toUpperCase(),
    getClusterElementByName: vi.fn(),
    initializeClusterElementsObject: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getFormattedName', () => ({
    default: (name: string) => name,
}));

vi.mock('@/pages/platform/workflow-editor/utils/getParametersWithDefaultValues', () => ({
    default: () => ({}),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getTask', () => ({
    getTask: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/utils/handleComponentAddedSuccess', () => ({
    default: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/utils/processClusterElementsHierarchy', () => ({
    default: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveWorkflowDefinition', () => ({
    default: vi.fn(),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    ClusterElementDefinitionKeys: {clusterElementDefinition: vi.fn()},
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ClusterElementDefinitionApi: vi.fn(),
    ComponentConnection: {},
}));

// --- Helpers ---

const COMPONENT_NAME = 'myComponent';
const OPERATION_NAME = 'readOp';
const ELEMENT_NODE_NAME = 'myComponent_1';
const ROOT_NODE_NAME = 'dataStream_1';
const WORKFLOW_ID = 'workflow-123';
const ELEMENT_TYPE = 'myComponent/v1/readOp';

const mockGetClusterElementByName = getClusterElementByName as Mock;
const mockGetTask = getTask as Mock;

function createQueryClientWrapper() {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false}},
    });

    return function QueryClientWrapper({children}: {children: React.ReactNode}) {
        return React.createElement(QueryClientProvider, {client: queryClient}, children);
    };
}

interface SetupStoresOptionsI {
    clusterElements?: Record<string, unknown>;
    componentDefinitions?: Array<{
        clusterElementsCount?: Record<string, number>;
        icon?: string;
        name: string;
        title?: string;
        version?: number;
    }>;
    currentNodeWorkflowNodeName?: string;
    rootClusterElementNodeData?: Record<string, unknown> | null;
    workflowDefinition?: string;
    workflowId?: string;
}

function setupStores(options: SetupStoresOptionsI = {}) {
    const {
        clusterElements = {
            destination: {
                componentName: COMPONENT_NAME,
                label: 'My Component',
                name: ELEMENT_NODE_NAME,
                operationName: OPERATION_NAME,
                parameters: {key: 'value'},
                type: ELEMENT_TYPE,
                workflowNodeName: ELEMENT_NODE_NAME,
            },
        },
        componentDefinitions = [
            {
                clusterElementsCount: {DESTINATION: 1},
                icon: 'icon.svg',
                name: COMPONENT_NAME,
                title: 'My Component',
                version: 1,
            },
        ],
        currentNodeWorkflowNodeName = ELEMENT_NODE_NAME,
        rootClusterElementNodeData = {
            componentName: 'dataStream',
            name: ROOT_NODE_NAME,
            workflowNodeName: ROOT_NODE_NAME,
        },
        workflowDefinition = JSON.stringify({
            tasks: [
                {
                    clusterElements: {
                        destination: {
                            name: ELEMENT_NODE_NAME,
                            parameters: {storedKey: 'storedValue'},
                            type: ELEMENT_TYPE,
                        },
                    },
                    name: ROOT_NODE_NAME,
                    type: 'dataStream/v1/sync',
                },
            ],
        }),
        workflowId = WORKFLOW_ID,
    } = options;

    const mockSetCurrentComponent = vi.fn();
    const mockSetCurrentNode = vi.fn();
    const mockSetRootClusterElementNodeData = vi.fn();

    mockWorkflowEditorStoreState.mockImplementation((selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            mainClusterRootComponentDefinition: {clusterElementTypes: []},
            rootClusterElementNodeData: rootClusterElementNodeData
                ? {...rootClusterElementNodeData, clusterElements}
                : null,
            setRootClusterElementNodeData: mockSetRootClusterElementNodeData,
        })
    );

    mockWorkflowDataStoreState.mockImplementation((selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            componentDefinitions,
            workflow: {definition: workflowDefinition, id: workflowId},
        })
    );

    mockNodeDetailsPanelStoreState.mockImplementation((selector: (state: Record<string, unknown>) => unknown) =>
        selector({
            currentNode: {workflowNodeName: currentNodeWorkflowNodeName},
            setCurrentComponent: mockSetCurrentComponent,
            setCurrentNode: mockSetCurrentNode,
        })
    );

    return {mockSetCurrentComponent, mockSetCurrentNode, mockSetRootClusterElementNodeData};
}

interface SetupQueryMocksOptionsI {
    connectionData?: Record<string, unknown>;
    displayConditionsData?: Record<string, unknown>;
    testConnectionsData?: Array<{connectionId: number; workflowConnectionKey: string}>;
}

function setupQueryMocks(overrides: SetupQueryMocksOptionsI = {}) {
    const {
        connectionData = undefined,
        displayConditionsData = {displayConditions: {}},
        testConnectionsData = undefined,
    } = overrides;

    mockGetComponentDefinitionQuery.mockReturnValue({data: connectionData});
    mockGetDisplayConditionsQuery.mockReturnValue({data: displayConditionsData});
    mockGetTestConnectionsQuery.mockReturnValue({data: testConnectionsData});
}

// --- Tests ---

describe('useClusterElementStep', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setupQueryMocks();
    });

    describe('isElementMatchingSelection gate', () => {
        it('should return null elementItem after handleComponentChange sets a different selectedComponentName', async () => {
            setupStores();

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.elementItem).not.toBeNull();

            await act(() => {
                result.current.handleComponentChange('differentComponent');
            });

            expect(result.current.elementItem).toBeNull();
            expect(result.current.elementProperties).toEqual([]);
        });

        it('should return null elementItem after handleComponentChange clears selectedOperationName', async () => {
            setupStores();

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.elementItem).not.toBeNull();

            await act(() => {
                result.current.handleComponentChange(COMPONENT_NAME);
            });

            expect(result.current.elementItem).toBeNull();
            expect(result.current.elementProperties).toEqual([]);
        });

        it('should return null elementItem when currentNode.workflowNodeName does not match elementItem.name', () => {
            setupStores({
                currentNodeWorkflowNodeName: 'someOtherNode',
            });

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.elementItem).toBeNull();
            expect(result.current.elementProperties).toEqual([]);
        });

        it('should return actual elementItem when all fields match', () => {
            setupStores();

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.elementItem).not.toBeNull();
            expect(result.current.elementItem?.componentName).toBe(COMPONENT_NAME);
            expect(result.current.elementItem?.name).toBe(ELEMENT_NODE_NAME);
            expect(result.current.elementItem?.operationName).toBe(OPERATION_NAME);
        });
    });

    describe('elementExistsInDefinition', () => {
        it('should not enable displayConditionsQuery when element name is not in workflow definition', () => {
            mockGetClusterElementByName.mockReturnValue(undefined);

            mockGetTask.mockReturnValue({
                clusterElements: {destination: {name: 'unknownElement'}},
                name: ROOT_NODE_NAME,
            });

            setupStores();

            renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(mockGetDisplayConditionsQuery).toHaveBeenCalledWith(expect.anything(), false);
        });

        it('should enable displayConditionsQuery when element exists in workflow definition', () => {
            mockGetClusterElementByName.mockReturnValue({
                name: ELEMENT_NODE_NAME,
                parameters: {storedKey: 'storedValue'},
                type: ELEMENT_TYPE,
            });

            mockGetTask.mockReturnValue({
                clusterElements: {
                    destination: {
                        name: ELEMENT_NODE_NAME,
                        parameters: {storedKey: 'storedValue'},
                        type: ELEMENT_TYPE,
                    },
                },
                name: ROOT_NODE_NAME,
            });

            setupStores();

            renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(mockGetDisplayConditionsQuery).toHaveBeenCalledWith(
                expect.objectContaining({
                    clusterElementWorkflowNodeName: ELEMENT_NODE_NAME,
                }),
                true
            );
        });
    });

    describe('connectionId from testConnections', () => {
        it('should include connectionId from matching test connection in setupNodeDetailsPanel', () => {
            const testConnectionId = 42;

            mockGetClusterElementByName.mockReturnValue({
                name: ELEMENT_NODE_NAME,
                parameters: {storedKey: 'storedValue'},
                type: ELEMENT_TYPE,
            });

            mockGetTask.mockReturnValue({
                clusterElements: {
                    destination: {
                        name: ELEMENT_NODE_NAME,
                        parameters: {storedKey: 'storedValue'},
                        type: ELEMENT_TYPE,
                    },
                },
                name: ROOT_NODE_NAME,
            });

            setupQueryMocks({
                testConnectionsData: [{connectionId: testConnectionId, workflowConnectionKey: ELEMENT_NODE_NAME}],
            });

            const {mockSetCurrentNode} = setupStores();

            renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(mockSetCurrentNode).toHaveBeenCalled();

            const nodeDataArgument = mockSetCurrentNode.mock.calls[0][0];

            expect(nodeDataArgument.connectionId).toBe(testConnectionId);
        });
    });

    describe('parameters from workflow definition', () => {
        it('should read parameters from workflow definition via getClusterElementByName', () => {
            const storedParameters = {apiKey: 'abc123', endpoint: 'https://example.com'};

            mockGetClusterElementByName.mockReturnValue({
                name: ELEMENT_NODE_NAME,
                parameters: storedParameters,
                type: ELEMENT_TYPE,
            });

            mockGetTask.mockReturnValue({
                clusterElements: {
                    destination: {
                        name: ELEMENT_NODE_NAME,
                        parameters: storedParameters,
                        type: ELEMENT_TYPE,
                    },
                },
                name: ROOT_NODE_NAME,
            });

            setupQueryMocks();

            const {mockSetCurrentNode} = setupStores();

            renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(mockSetCurrentNode).toHaveBeenCalled();

            const nodeDataArgument = mockSetCurrentNode.mock.calls[0][0];

            expect(nodeDataArgument.parameters).toEqual(storedParameters);
        });
    });

    describe('elementItem computation', () => {
        it('should return null elementItem when rootClusterElementNodeData has no clusterElements', () => {
            setupStores();

            mockWorkflowEditorStoreState.mockImplementation((selector: (state: Record<string, unknown>) => unknown) =>
                selector({
                    mainClusterRootComponentDefinition: {clusterElementTypes: []},
                    rootClusterElementNodeData: {
                        componentName: 'dataStream',
                        name: ROOT_NODE_NAME,
                        workflowNodeName: ROOT_NODE_NAME,
                    },
                    setRootClusterElementNodeData: vi.fn(),
                })
            );

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.elementItem).toBeNull();
        });

        it('should return null elementItem when rootClusterElementNodeData is null', () => {
            setupStores();

            mockWorkflowEditorStoreState.mockImplementation((selector: (state: Record<string, unknown>) => unknown) =>
                selector({
                    mainClusterRootComponentDefinition: {clusterElementTypes: []},
                    rootClusterElementNodeData: null,
                    setRootClusterElementNodeData: vi.fn(),
                })
            );

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.elementItem).toBeNull();
        });
    });

    describe('stepComponentDefinitions filtering', () => {
        it('should filter component definitions by elementType clusterElementsCount', () => {
            setupStores({
                componentDefinitions: [
                    {clusterElementsCount: {DESTINATION: 2}, name: 'compA', version: 1},
                    {clusterElementsCount: {SOURCE: 1}, name: 'compB', version: 1},
                    {clusterElementsCount: {DESTINATION: 0}, name: 'compC', version: 1},
                ],
            });

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.stepComponentDefinitions).toHaveLength(1);
            expect(result.current.stepComponentDefinitions[0].name).toBe('compA');
        });
    });

    describe('returned values', () => {
        it('should return workflowId and rootWorkflowNodeName from stores', () => {
            setupStores();

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.workflowId).toBe(WORKFLOW_ID);
            expect(result.current.rootWorkflowNodeName).toBe(ROOT_NODE_NAME);
        });

        it('should return selectedComponentName and selectedOperationName', () => {
            setupStores();

            const {result} = renderHook(() => useClusterElementStep('destination'), {
                wrapper: createQueryClientWrapper(),
            });

            expect(result.current.selectedComponentName).toBe(COMPONENT_NAME);
            expect(result.current.selectedOperationName).toBe(OPERATION_NAME);
        });
    });
});
