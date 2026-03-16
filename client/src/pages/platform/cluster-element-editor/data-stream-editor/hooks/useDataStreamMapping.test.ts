import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

interface MockWorkflowEditorStoreStateI {
    mainClusterRootComponentDefinition: object | null;
    rootClusterElementNodeData: {
        clusterElements?: Record<string, unknown> | unknown[];
        componentName?: string;
        name?: string;
        workflowNodeName?: string;
    } | null;
    setRootClusterElementNodeData: ReturnType<typeof vi.fn>;
}

interface MockWorkflowNodeDetailsPanelStoreStateI {
    currentNode: {
        clusterRoot?: boolean;
        isNestedClusterRoot?: boolean;
        workflowNodeName?: string;
    } | null;
    setCurrentComponent: ReturnType<typeof vi.fn>;
    setCurrentNode: ReturnType<typeof vi.fn>;
}

interface MockWorkflowDataStoreStateI {
    componentDefinitions: Array<{name: string; title: string}>;
    workflow: {
        definition?: string;
        id?: string;
    };
}

let mockEditorStoreState: MockWorkflowEditorStoreStateI;
let mockNodeDetailsPanelStoreState: MockWorkflowNodeDetailsPanelStoreStateI;
let mockDataStoreState: MockWorkflowDataStoreStateI;

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selectorOrShallow: unknown) => {
        if (typeof selectorOrShallow === 'function') {
            return selectorOrShallow(mockEditorStoreState);
        }

        return mockEditorStoreState;
    },
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selectorOrShallow: unknown) => {
        if (typeof selectorOrShallow === 'function') {
            return selectorOrShallow(mockNodeDetailsPanelStoreState);
        }

        return mockNodeDetailsPanelStoreState;
    },
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: unknown) => {
        if (typeof selector === 'function') {
            return (selector as (state: MockWorkflowDataStoreStateI) => unknown)(mockDataStoreState);
        }

        return mockDataStoreState;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: unknown) => {
        if (typeof selector === 'function') {
            return (selector as (state: {currentEnvironmentId: number}) => unknown)({currentEnvironmentId: 1});
        }

        return {currentEnvironmentId: 1};
    },
}));

vi.mock('zustand/shallow', () => ({
    useShallow: (selector: unknown) => selector,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        invalidateWorkflowQueries: vi.fn(),
        updateWorkflowMutation: vi.fn(),
    }),
}));

vi.mock('@/shared/queries/platform/workflowNodeParameters.queries', () => ({
    useGetClusterElementParameterDisplayConditionsQuery: (_request: unknown, enabled?: boolean) => ({
        data: undefined,
        isEnabled: enabled,
        isLoading: false,
    }),
}));

vi.mock('@/pages/platform/cluster-element-editor/utils/clusterElementsUtils', () => ({
    getClusterElementByName: vi.fn(),
    initializeClusterElementsObject: vi.fn(() => ({})),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getFormattedName', () => ({
    default: vi.fn((name: string) => name),
}));

vi.mock('@/pages/platform/workflow-editor/utils/getParametersWithDefaultValues', () => ({
    default: vi.fn(() => ({})),
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

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ClusterElementDefinitionApi: vi.fn(),
    WorkflowNodeOptionApi: vi.fn(),
    WorkflowNodeParameterApi: vi.fn(),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    ClusterElementDefinitionKeys: {
        clusterElementDefinition: vi.fn(() => ['cluster-element-definition']),
    },
}));

function createTestQueryClient() {
    return new QueryClient({
        defaultOptions: {
            mutations: {
                retry: false,
            },
            queries: {
                retry: false,
            },
        },
    });
}

function createWrapper() {
    const testQueryClient = createTestQueryClient();

    return ({children}: {children: ReactNode}) =>
        createElement(QueryClientProvider, {client: testQueryClient}, children);
}

describe('useDataStreamMapping', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockEditorStoreState = {
            mainClusterRootComponentDefinition: null,
            rootClusterElementNodeData: null,
            setRootClusterElementNodeData: vi.fn(),
        };

        mockNodeDetailsPanelStoreState = {
            currentNode: null,
            setCurrentComponent: vi.fn(),
            setCurrentNode: vi.fn(),
        };

        mockDataStoreState = {
            componentDefinitions: [],
            workflow: {
                definition: '{"tasks":[]}',
                id: 'workflow-1',
            },
        };
    });

    describe('isProcessorReady gate', () => {
        it('should return null processor and empty processorProperties when currentNode workflowNodeName does not match processor name', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    processor: {
                        componentName: 'dataStreamProcessor',
                        name: 'dataStreamProcessor_1',
                        operationName: 'fieldMapper',
                        type: 'dataStreamProcessor/v1/fieldMapper',
                        workflowNodeName: 'dataStreamProcessor_1',
                    },
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            mockNodeDetailsPanelStoreState.currentNode = {
                workflowNodeName: 'differentNode',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor).toBeNull();
            expect(result.current.processorProperties).toEqual([]);
        });

        it('should return processor and processorProperties when currentNode workflowNodeName matches processor name', async () => {
            const processorName = 'dataStreamProcessor_1';

            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    processor: {
                        componentName: 'dataStreamProcessor',
                        name: processorName,
                        operationName: 'fieldMapper',
                        type: 'dataStreamProcessor/v1/fieldMapper',
                        workflowNodeName: processorName,
                    },
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            mockNodeDetailsPanelStoreState.currentNode = {
                workflowNodeName: processorName,
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor).not.toBeNull();
            expect(result.current.processor?.name).toBe(processorName);
            expect(result.current.processor?.componentName).toBe('dataStreamProcessor');
        });
    });

    describe('processor name extraction', () => {
        it('should fall back to processorElement.name when workflowNodeName is empty', async () => {
            const elementName = 'dataStreamProcessor_fallback';

            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    processor: {
                        componentName: 'dataStreamProcessor',
                        name: elementName,
                        operationName: 'fieldMapper',
                        type: 'dataStreamProcessor/v1/fieldMapper',
                        workflowNodeName: '',
                    },
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            mockNodeDetailsPanelStoreState.currentNode = {
                workflowNodeName: elementName,
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor).not.toBeNull();
            expect(result.current.processor?.name).toBe(elementName);
        });

        it('should use workflowNodeName when both workflowNodeName and name are present', async () => {
            const workflowNodeName = 'dataStreamProcessor_workflow';

            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    processor: {
                        componentName: 'dataStreamProcessor',
                        name: 'dataStreamProcessor_element',
                        operationName: 'fieldMapper',
                        type: 'dataStreamProcessor/v1/fieldMapper',
                        workflowNodeName,
                    },
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            mockNodeDetailsPanelStoreState.currentNode = {
                workflowNodeName,
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor?.name).toBe(workflowNodeName);
        });
    });

    describe('displayConditionsQuery', () => {
        it('should be disabled when processor name is empty', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    processor: {
                        componentName: 'dataStreamProcessor',
                        name: '',
                        operationName: 'fieldMapper',
                        type: 'dataStreamProcessor/v1/fieldMapper',
                        workflowNodeName: '',
                    },
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            mockNodeDetailsPanelStoreState.currentNode = {
                workflowNodeName: '',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.displayConditionsQuery.isEnabled).toBe(false);
        });

        it('should be enabled when processor name is present and workflow data exists', async () => {
            const processorName = 'dataStreamProcessor_1';

            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    processor: {
                        componentName: 'dataStreamProcessor',
                        name: processorName,
                        operationName: 'fieldMapper',
                        type: 'dataStreamProcessor/v1/fieldMapper',
                        workflowNodeName: processorName,
                    },
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            mockNodeDetailsPanelStoreState.currentNode = {
                workflowNodeName: processorName,
            };

            mockDataStoreState.workflow = {
                definition: '{"tasks":[]}',
                id: 'workflow-1',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.displayConditionsQuery.isEnabled).toBe(true);
        });
    });

    describe('processor is null when clusterElements missing', () => {
        it('should return null processor when rootClusterElementNodeData has no clusterElements', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                workflowNodeName: 'dataStream_1',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor).toBeNull();
            expect(result.current.processorProperties).toEqual([]);
        });

        it('should return null processor when clusterElements is an array', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: [],
                workflowNodeName: 'dataStream_1',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor).toBeNull();
        });

        it('should return null processor when there is no processor key in clusterElements', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.processor).toBeNull();
        });
    });

    describe('hasSourceAndDestination', () => {
        it('should be true when both source and destination exist', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                    source: {componentName: 'src', type: 'src/v1/read'},
                },
                workflowNodeName: 'dataStream_1',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.hasSourceAndDestination).toBe(true);
        });

        it('should be false when source is missing', async () => {
            mockEditorStoreState.rootClusterElementNodeData = {
                clusterElements: {
                    destination: {componentName: 'dest', type: 'dest/v1/write'},
                },
                workflowNodeName: 'dataStream_1',
            };

            const {default: useDataStreamMapping} = await import('./useDataStreamMapping');

            const {result} = renderHook(() => useDataStreamMapping(), {
                wrapper: createWrapper(),
            });

            expect(result.current.hasSourceAndDestination).toBe(false);
        });
    });
});
