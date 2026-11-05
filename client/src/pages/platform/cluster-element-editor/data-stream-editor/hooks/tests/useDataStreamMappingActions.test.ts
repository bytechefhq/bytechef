import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataStreamMapping from '../useDataStreamMapping';

type NodeUpdaterType = (previousNode: Record<string, unknown> | null) => Record<string, unknown>;

interface MockRootClusterElementNodeDataI {
    clusterElements?: Record<string, unknown> | unknown[];
    componentName?: string;
    name?: string;
    workflowNodeName?: string;
}

const hoisted = vi.hoisted(() => ({
    dataStoreState: {
        componentDefinitions: [] as Array<{name: string; title?: string}>,
        workflow: {} as {definition?: string; id?: string},
    },
    editorStoreState: {
        clusterRootComponentDefinitions: {} as Record<string, object>,
        rootClusterElementNodeData: null as MockRootClusterElementNodeDataI | null,
        setRootClusterElementNodeData: vi.fn(),
    },
    getClusterElementByName: vi.fn(),
    getClusterElementNodeOptions: vi.fn(),
    getComponentClusterElementDefinition: vi.fn(),
    getParametersWithDefaultValues: vi.fn(),
    getTask: vi.fn(),
    handleComponentAddedSuccess: vi.fn(),
    initializeClusterElementsObject: vi.fn(),
    invalidateWorkflowQueries: vi.fn(),
    nodeDetailsPanelState: {
        currentNode: null as Record<string, unknown> | null,
        setCurrentNode: vi.fn(),
    },
    processClusterElementsHierarchy: vi.fn(),
    saveWorkflowDefinition: vi.fn(),
    updateClusterElementParameter: vi.fn(),
    updateWorkflowMutation: {mutate: vi.fn()},
    useGetClusterElementParameterDisplayConditionsQuery: vi.fn(),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (selector: (state: typeof hoisted.editorStoreState) => unknown) => selector(hoisted.editorStoreState),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (selector: (state: typeof hoisted.nodeDetailsPanelState) => unknown) =>
        selector(hoisted.nodeDetailsPanelState),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: (selector: (state: typeof hoisted.dataStoreState) => unknown) => selector(hoisted.dataStoreState),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('zustand/shallow', () => ({
    useShallow: (selector: unknown) => selector,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({
        invalidateWorkflowQueries: hoisted.invalidateWorkflowQueries,
        updateWorkflowMutation: hoisted.updateWorkflowMutation,
    }),
}));

vi.mock('@/shared/queries/platform/workflowNodeParameters.queries', () => ({
    useGetClusterElementParameterDisplayConditionsQuery: hoisted.useGetClusterElementParameterDisplayConditionsQuery,
}));

vi.mock('@/pages/platform/cluster-element-editor/utils/clusterElementsUtils', () => ({
    getClusterElementByName: hoisted.getClusterElementByName,
    initializeClusterElementsObject: hoisted.initializeClusterElementsObject,
}));

vi.mock('@/pages/platform/workflow-editor/utils/getFormattedName', () => ({
    default: (name: string) => name,
}));

vi.mock('@/pages/platform/workflow-editor/utils/getParametersWithDefaultValues', () => ({
    default: hoisted.getParametersWithDefaultValues,
}));

vi.mock('@/pages/platform/workflow-editor/utils/getTask', () => ({
    getTask: hoisted.getTask,
}));

vi.mock('@/pages/platform/workflow-editor/utils/handleComponentAddedSuccess', () => ({
    default: hoisted.handleComponentAddedSuccess,
}));

vi.mock('@/pages/platform/workflow-editor/utils/processClusterElementsHierarchy', () => ({
    default: hoisted.processClusterElementsHierarchy,
}));

vi.mock('@/pages/platform/workflow-editor/utils/saveWorkflowDefinition', () => ({
    default: hoisted.saveWorkflowDefinition,
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ClusterElementDefinitionApi: vi.fn(function ClusterElementDefinitionApi() {
        return {getComponentClusterElementDefinition: hoisted.getComponentClusterElementDefinition};
    }),
    WorkflowNodeOptionApi: vi.fn(function WorkflowNodeOptionApi() {
        return {getClusterElementNodeOptions: hoisted.getClusterElementNodeOptions};
    }),
    WorkflowNodeParameterApi: vi.fn(function WorkflowNodeParameterApi() {
        return {updateClusterElementParameter: hoisted.updateClusterElementParameter};
    }),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    ClusterElementDefinitionKeys: {
        clusterElementDefinition: (request: object) => ['clusterElementDefinition', request],
    },
}));

vi.mock('sonner', () => ({toast: {error: vi.fn(), info: vi.fn(), success: vi.fn()}}));

const PROCESSOR_NAME = 'dataStreamProcessor_1';

const processorElement = {
    componentName: 'dataStreamProcessor',
    label: 'Field Mapper',
    name: PROCESSOR_NAME,
    operationName: 'fieldMapper',
    type: 'dataStreamProcessor/v2/fieldMapper',
    workflowNodeName: PROCESSOR_NAME,
};

const sourceElement = {componentName: 'postgresql', type: 'postgresql/v1/read'};
const destinationElement = {componentName: 'mysql', type: 'mysql/v1/write'};

const createDeferred = <T>() => {
    let resolve!: (value: T) => void;
    let reject!: (reason: unknown) => void;

    const promise = new Promise<T>((promiseResolve, promiseReject) => {
        resolve = promiseResolve;
        reject = promiseReject;
    });

    return {promise, reject, resolve};
};

const renderMapping = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const wrapper = ({children}: {children: ReactNode}) =>
        createElement(QueryClientProvider, {client: queryClient}, children);

    return renderHook(() => useDataStreamMapping(), {wrapper});
};

const setClusterElements = (clusterElements: Record<string, unknown> | unknown[] | undefined) => {
    hoisted.editorStoreState.rootClusterElementNodeData = {
        clusterElements,
        componentName: 'dataStream',
        name: 'dataStream_1',
        workflowNodeName: 'dataStream_1',
    };
};

const getLastNodeUpdater = () => hoisted.nodeDetailsPanelState.setCurrentNode.mock.lastCall?.[0] as NodeUpdaterType;

describe('useDataStreamMapping', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.dataStoreState.componentDefinitions = [];
        hoisted.dataStoreState.workflow = {definition: '{"tasks":[{"name":"dataStream_1"}]}', id: 'workflow-1'};
        hoisted.editorStoreState.clusterRootComponentDefinitions = {};
        hoisted.editorStoreState.rootClusterElementNodeData = null;
        hoisted.nodeDetailsPanelState.currentNode = null;

        hoisted.getComponentClusterElementDefinition.mockResolvedValue({properties: []});
        hoisted.useGetClusterElementParameterDisplayConditionsQuery.mockReturnValue({data: undefined});
    });

    describe('labels', () => {
        it('uses the component definition titles of the source and destination', () => {
            hoisted.dataStoreState.componentDefinitions = [
                {name: 'mysql', title: 'MySQL'},
                {name: 'postgresql', title: 'PostgreSQL'},
            ];

            setClusterElements({destination: destinationElement, source: sourceElement});

            const {result} = renderMapping();

            expect(result.current.sourceLabel).toBe('PostgreSQL');
            expect(result.current.destinationLabel).toBe('MySQL');
        });

        it('derives the component name from the type when no definition matches', () => {
            setClusterElements({
                destination: {type: 'googleSheets/v1/append'},
                source: {type: 'airtable/v1/list'},
            });

            const {result} = renderMapping();

            expect(result.current.sourceLabel).toBe('airtable');
            expect(result.current.destinationLabel).toBe('googleSheets');
        });

        it('falls back to the generic labels without usable elements', () => {
            setClusterElements({destination: [destinationElement]});

            const {result} = renderMapping();

            expect(result.current.sourceLabel).toBe('Source');
            expect(result.current.destinationLabel).toBe('Destination');
        });

        it('falls back to the generic labels without cluster elements', () => {
            setClusterElements([]);

            const {result} = renderMapping();

            expect(result.current.sourceLabel).toBe('Source');
            expect(result.current.destinationLabel).toBe('Destination');
            expect(result.current.hasSourceAndDestination).toBe(false);
        });
    });

    describe('processor', () => {
        it('reads the first element of an array processor and derives missing fields from its type', () => {
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: 'processor_2'};

            setClusterElements({
                processor: [{name: 'processor_2', type: 'dataStreamProcessor/v1/fieldMapper'}],
            });

            const {result} = renderMapping();

            expect(result.current.processor).toEqual({
                componentName: 'dataStreamProcessor',
                label: 'processor_2',
                name: 'processor_2',
                operationName: 'fieldMapper',
                type: 'dataStreamProcessor/v1/fieldMapper',
            });
        });

        it('requests the display conditions of the processor', () => {
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: PROCESSOR_NAME};

            setClusterElements({processor: processorElement});

            renderMapping();

            expect(hoisted.useGetClusterElementParameterDisplayConditionsQuery).toHaveBeenLastCalledWith(
                {
                    clusterElementType: 'PROCESSOR',
                    clusterElementWorkflowNodeName: PROCESSOR_NAME,
                    environmentId: 1,
                    id: 'workflow-1',
                    workflowNodeName: 'dataStream_1',
                },
                true
            );
        });
    });

    describe('processor properties', () => {
        it('loads the properties of the processor cluster element definition', async () => {
            const definitionProperties = [{name: 'mappings', type: 'ARRAY'}];

            hoisted.getComponentClusterElementDefinition.mockResolvedValue({properties: definitionProperties});
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: PROCESSOR_NAME};

            setClusterElements({processor: processorElement});

            const {result} = renderMapping();

            await waitFor(() => {
                expect(result.current.processorProperties).toEqual(definitionProperties);
            });

            expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalledWith({
                clusterElementName: 'fieldMapper',
                clusterElementType: 'PROCESSOR',
                componentName: 'dataStreamProcessor',
                componentVersion: 2,
            });
        });

        it('uses version 1 when the type carries no parsable version', async () => {
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: PROCESSOR_NAME};

            setClusterElements({processor: {...processorElement, type: 'dataStreamProcessor/latest/fieldMapper'}});

            renderMapping();

            await waitFor(() => {
                expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalledWith(
                    expect.objectContaining({componentVersion: 1})
                );
            });
        });

        it('reports a failed definition load and keeps the properties empty', async () => {
            hoisted.getComponentClusterElementDefinition.mockRejectedValue(new Error('Not found'));
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: PROCESSOR_NAME};

            setClusterElements({processor: processorElement});

            const {result} = renderMapping();

            await waitFor(() => {
                expect(toast.error).toHaveBeenCalledWith('Failed to load processor properties');
            });

            expect(result.current.processorProperties).toEqual([]);
        });

        it('hides the properties until the processor is the current node', async () => {
            hoisted.getComponentClusterElementDefinition.mockResolvedValue({properties: [{name: 'mappings'}]});
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: 'otherNode'};

            setClusterElements({processor: processorElement});

            const {result} = renderMapping();

            await waitFor(() => {
                expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalled();
            });

            await act(async () => {
                await Promise.resolve();
            });

            expect(result.current.processorProperties).toEqual([]);
        });
    });

    describe('node details panel setup', () => {
        it('opens the processor in the node details panel with its stored metadata and parameters', async () => {
            const processorTask = {clusterElements: {processor: {}}};

            hoisted.getTask.mockReturnValue(processorTask);
            hoisted.getClusterElementByName.mockReturnValue({
                metadata: {ui: {dynamicPropertyTypes: {}}},
                parameters: {mappings: []},
            });

            setClusterElements({processor: processorElement});

            renderMapping();

            await waitFor(() => {
                expect(hoisted.nodeDetailsPanelState.setCurrentNode).toHaveBeenCalled();
            });

            expect(hoisted.getTask).toHaveBeenCalledWith({
                tasks: [{name: 'dataStream_1'}],
                workflowNodeName: 'dataStream_1',
            });
            expect(hoisted.getClusterElementByName).toHaveBeenCalledWith(processorTask.clusterElements, PROCESSOR_NAME);

            const nodeUpdater = getLastNodeUpdater();

            expect(nodeUpdater({displayConditions: {mappings: true}})).toEqual({
                clusterElementName: 'fieldMapper',
                clusterElementType: 'processor',
                componentName: 'dataStreamProcessor',
                connections: [],
                description: '',
                displayConditions: {mappings: true},
                label: 'Field Mapper',
                metadata: {ui: {dynamicPropertyTypes: {}}},
                name: PROCESSOR_NAME,
                operationName: 'fieldMapper',
                parameters: {mappings: []},
                parentClusterRootId: 'dataStream_1',
                type: 'dataStreamProcessor/v2/fieldMapper',
                version: 2,
                workflowNodeName: PROCESSOR_NAME,
            });
        });

        it('leaves metadata and parameters empty without a workflow definition', async () => {
            hoisted.dataStoreState.workflow = {id: 'workflow-1'};

            setClusterElements({processor: processorElement});

            renderMapping();

            await waitFor(() => {
                expect(hoisted.nodeDetailsPanelState.setCurrentNode).toHaveBeenCalled();
            });

            expect(hoisted.getTask).not.toHaveBeenCalled();

            const processorNode = getLastNodeUpdater()(null);

            expect(processorNode.metadata).toBeUndefined();
            expect(processorNode.parameters).toBeUndefined();
            expect(processorNode.displayConditions).toBeUndefined();
        });
    });

    describe('handleAutoMap', () => {
        beforeEach(() => {
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: PROCESSOR_NAME};

            setClusterElements({
                destination: destinationElement,
                processor: processorElement,
                source: sourceElement,
            });
        });

        it('does nothing without a processor', async () => {
            setClusterElements({destination: destinationElement, source: sourceElement});

            const {result} = renderMapping();

            await act(async () => {
                await result.current.handleAutoMap();
            });

            expect(hoisted.getClusterElementNodeOptions).not.toHaveBeenCalled();
            expect(result.current.autoMapping).toBe(false);
        });

        it('maps the fields that exist in both the source and the destination', async () => {
            hoisted.getClusterElementNodeOptions.mockImplementation(({propertyName}: {propertyName: string}) =>
                Promise.resolve(
                    propertyName === 'mappings[0].sourceField'
                        ? [{value: 'email'}, {value: 'name'}, {value: 'phone'}]
                        : [{value: 'name'}, {value: ''}, {value: 'email'}]
                )
            );
            hoisted.updateClusterElementParameter.mockResolvedValue({});

            const {result} = renderMapping();

            const initialPropertiesKey = result.current.propertiesKey;

            await act(async () => {
                await result.current.handleAutoMap();
            });

            expect(hoisted.getClusterElementNodeOptions).toHaveBeenCalledWith({
                clusterElementType: 'PROCESSOR',
                clusterElementWorkflowNodeName: PROCESSOR_NAME,
                environmentId: 1,
                id: 'workflow-1',
                propertyName: 'mappings[0].sourceField',
                workflowNodeName: 'dataStream_1',
            });
            expect(hoisted.getClusterElementNodeOptions).toHaveBeenCalledWith(
                expect.objectContaining({propertyName: 'mappings[0].destinationField'})
            );

            const expectedMappings = [
                {destinationField: 'email', sourceField: 'email'},
                {destinationField: 'name', sourceField: 'name'},
            ];

            expect(hoisted.updateClusterElementParameter).toHaveBeenCalledWith({
                clusterElementType: 'PROCESSOR',
                clusterElementWorkflowNodeName: PROCESSOR_NAME,
                environmentId: 1,
                id: 'workflow-1',
                updateClusterElementParameterRequest: {
                    path: 'mappings',
                    type: 'ARRAY',
                    value: expectedMappings,
                },
                workflowNodeName: 'dataStream_1',
            });

            expect(getLastNodeUpdater()({name: PROCESSOR_NAME, parameters: {other: 1}})).toEqual({
                name: PROCESSOR_NAME,
                parameters: {mappings: expectedMappings, other: 1},
            });
            expect(result.current.propertiesKey).toBe(initialPropertiesKey + 1);
            expect(hoisted.invalidateWorkflowQueries).toHaveBeenCalledTimes(1);
            expect(result.current.autoMapping).toBe(false);
        });

        it('reports when no field matches and saves nothing', async () => {
            hoisted.getClusterElementNodeOptions.mockImplementation(({propertyName}: {propertyName: string}) =>
                Promise.resolve(propertyName === 'mappings[0].sourceField' ? [{value: 'email'}] : [{value: 'name'}])
            );

            const {result} = renderMapping();

            await act(async () => {
                await result.current.handleAutoMap();
            });

            expect(toast.info).toHaveBeenCalledWith('No matching fields found between source and destination');
            expect(hoisted.updateClusterElementParameter).not.toHaveBeenCalled();
            expect(hoisted.invalidateWorkflowQueries).not.toHaveBeenCalled();
            expect(result.current.propertiesKey).toBe(0);
            expect(result.current.autoMapping).toBe(false);
        });

        it('reports a failure and resets the auto-mapping flag', async () => {
            hoisted.getClusterElementNodeOptions.mockRejectedValue(new Error('Network error'));

            const {result} = renderMapping();

            await act(async () => {
                await result.current.handleAutoMap();
            });

            expect(toast.error).toHaveBeenCalledWith('Auto-map failed');
            expect(result.current.autoMapping).toBe(false);
        });

        it('flags auto-mapping while the options load', async () => {
            const sourceOptionsDeferred = createDeferred<Array<{value: string}>>();

            hoisted.getClusterElementNodeOptions.mockImplementation(({propertyName}: {propertyName: string}) =>
                propertyName === 'mappings[0].sourceField' ? sourceOptionsDeferred.promise : Promise.resolve([])
            );

            const {result} = renderMapping();

            let autoMapPromise: Promise<void> | undefined;

            act(() => {
                autoMapPromise = result.current.handleAutoMap();
            });

            expect(result.current.autoMapping).toBe(true);

            await act(async () => {
                sourceOptionsDeferred.resolve([]);

                await autoMapPromise;
            });

            expect(result.current.autoMapping).toBe(false);
        });
    });

    describe('default processor creation', () => {
        const mainClusterRootTask = {clusterElements: {source: {}}, name: 'dataStream_1', type: 'dataStream/v1'};
        const nestedClusterElements = {destination: {}, processor: {name: 'dataStreamProcessor'}, source: {}};

        beforeEach(() => {
            hoisted.editorStoreState.clusterRootComponentDefinitions = {dataStream_1: {name: 'dataStream'}};

            setClusterElements({destination: destinationElement, source: sourceElement});

            hoisted.getComponentClusterElementDefinition.mockResolvedValue({properties: [{name: 'mappings'}]});
            hoisted.getParametersWithDefaultValues.mockReturnValue({mappings: []});
            hoisted.getTask.mockReturnValue(mainClusterRootTask);
            hoisted.initializeClusterElementsObject.mockReturnValue({initialized: true});
            hoisted.processClusterElementsHierarchy.mockReturnValue({nestedClusterElements});
        });

        it('creates and saves a field mapper processor', async () => {
            renderMapping();

            await waitFor(() => {
                expect(hoisted.saveWorkflowDefinition).toHaveBeenCalledTimes(1);
            });

            expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalledWith({
                clusterElementName: 'fieldMapper',
                clusterElementType: 'PROCESSOR',
                componentName: 'dataStreamProcessor',
                componentVersion: 1,
            });
            expect(hoisted.getParametersWithDefaultValues).toHaveBeenCalledWith({properties: [{name: 'mappings'}]});
            expect(hoisted.initializeClusterElementsObject).toHaveBeenCalledWith({
                clusterElementsData: mainClusterRootTask.clusterElements,
                mainClusterRootComponentDefinition: {name: 'dataStream'},
                mainClusterRootTask,
            });
            expect(hoisted.processClusterElementsHierarchy).toHaveBeenCalledWith({
                clusterElementData: {
                    label: 'Processor',
                    metadata: {},
                    name: 'dataStreamProcessor',
                    parameters: {mappings: []},
                    type: 'dataStreamProcessor/v1/fieldMapper',
                },
                clusterElements: {initialized: true},
                elementType: 'processor',
                isMultipleElements: false,
                mainRootId: 'dataStream_1',
                sourceNodeId: 'dataStream_1',
            });
            expect(hoisted.saveWorkflowDefinition).toHaveBeenCalledWith({
                nodeData: {
                    ...mainClusterRootTask,
                    clusterElements: nestedClusterElements,
                    componentName: 'dataStream',
                    workflowNodeName: 'dataStream_1',
                },
                onSuccess: expect.any(Function),
                updateWorkflowMutation: hoisted.updateWorkflowMutation,
            });
        });

        it('stores the new cluster elements once the save succeeds', async () => {
            hoisted.nodeDetailsPanelState.currentNode = {clusterRoot: true, name: 'dataStream_1'};

            renderMapping();

            await waitFor(() => {
                expect(hoisted.saveWorkflowDefinition).toHaveBeenCalledTimes(1);
            });

            const {onSuccess} = hoisted.saveWorkflowDefinition.mock.calls[0][0] as {onSuccess: () => void};

            onSuccess();

            expect(hoisted.editorStoreState.setRootClusterElementNodeData).toHaveBeenCalledWith(
                expect.objectContaining({clusterElements: nestedClusterElements, workflowNodeName: 'dataStream_1'})
            );
            expect(hoisted.nodeDetailsPanelState.setCurrentNode).toHaveBeenCalledWith({
                clusterElements: nestedClusterElements,
                clusterRoot: true,
                name: 'dataStream_1',
            });
            expect(hoisted.handleComponentAddedSuccess).toHaveBeenCalledWith(
                expect.objectContaining({
                    nodeData: expect.objectContaining({clusterElements: nestedClusterElements}),
                    workflow: hoisted.dataStoreState.workflow,
                })
            );
        });

        it('leaves a nested cluster root current node untouched after the save', async () => {
            hoisted.nodeDetailsPanelState.currentNode = {clusterRoot: true, isNestedClusterRoot: true};

            renderMapping();

            await waitFor(() => {
                expect(hoisted.saveWorkflowDefinition).toHaveBeenCalledTimes(1);
            });

            const {onSuccess} = hoisted.saveWorkflowDefinition.mock.calls[0][0] as {onSuccess: () => void};

            onSuccess();

            expect(hoisted.nodeDetailsPanelState.setCurrentNode).not.toHaveBeenCalled();
            expect(hoisted.editorStoreState.setRootClusterElementNodeData).toHaveBeenCalled();
        });

        it('does not save when the cluster root task is missing from the definition', async () => {
            hoisted.getTask.mockReturnValue(undefined);

            renderMapping();

            await waitFor(() => {
                expect(hoisted.getTask).toHaveBeenCalled();
            });

            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
            expect(toast.error).not.toHaveBeenCalled();
        });

        it('reports a failed creation', async () => {
            hoisted.getComponentClusterElementDefinition.mockRejectedValue(new Error('Not found'));

            renderMapping();

            await waitFor(() => {
                expect(toast.error).toHaveBeenCalledWith('Failed to create default processor');
            });

            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
        });

        it('does not create a processor without the cluster root component definition', async () => {
            hoisted.editorStoreState.clusterRootComponentDefinitions = {};

            renderMapping();

            await act(async () => {
                await Promise.resolve();
            });

            expect(hoisted.getComponentClusterElementDefinition).not.toHaveBeenCalled();
        });

        it('does not create a processor when the destination is missing', async () => {
            setClusterElements({source: sourceElement});

            renderMapping();

            await act(async () => {
                await Promise.resolve();
            });

            expect(hoisted.getComponentClusterElementDefinition).not.toHaveBeenCalled();
        });

        it('does not create a processor when one already exists', async () => {
            hoisted.nodeDetailsPanelState.currentNode = {workflowNodeName: PROCESSOR_NAME};

            setClusterElements({destination: destinationElement, processor: processorElement, source: sourceElement});

            renderMapping();

            await waitFor(() => {
                expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalledTimes(1);
            });

            expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalledWith(
                expect.objectContaining({componentVersion: 2})
            );
            expect(hoisted.saveWorkflowDefinition).not.toHaveBeenCalled();
        });

        it('starts only one creation while a previous one is still in progress', async () => {
            const definitionDeferred = createDeferred<{properties: object[]}>();

            hoisted.getComponentClusterElementDefinition.mockReturnValue(definitionDeferred.promise);

            const {rerender} = renderMapping();

            hoisted.nodeDetailsPanelState.currentNode = {name: 'changed'};

            rerender();

            expect(hoisted.getComponentClusterElementDefinition).toHaveBeenCalledTimes(1);

            await act(async () => {
                definitionDeferred.resolve({properties: []});
            });

            await waitFor(() => {
                expect(hoisted.saveWorkflowDefinition).toHaveBeenCalledTimes(1);
            });
        });
    });
});
