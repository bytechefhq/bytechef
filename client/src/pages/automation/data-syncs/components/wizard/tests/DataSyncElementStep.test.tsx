import DataSyncElementStep from '@/pages/automation/data-syncs/components/wizard/DataSyncElementStep';
import {useClusterElementContext} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {render, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    mockComponentDefinition: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useClusterElementDefinitionQuery: () => ({
            data: {
                clusterElementDefinition: {
                    properties: [{name: 'baseId', type: 'STRING'}],
                },
            },
        }),
        useSetDataSyncElementMutation: () => ({isPending: false, mutate: vi.fn()}),
        useUpdateDataSyncElementMutation: () => ({isPending: false, mutate: vi.fn()}),
    };
});

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {clusterElementsCount: {SOURCE: 1}, name: 'airtable', title: 'Airtable', version: 1},
            {clusterElementsCount: {SOURCE: 1}, name: 'csvFile', title: 'CSV File', version: 1},
        ],
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: hoisted.mockComponentDefinition()}),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/useFormDisplayConditions', () => ({
    default: () => ({displayConditions: undefined, isEvaluating: false}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => {
    const PropertiesStub = () => {
        const clusterElementContext = useClusterElementContext();

        return (
            <div
                data-connection-id={String(clusterElementContext?.connectionId)}
                data-connection-required={String(clusterElementContext?.connectionRequired)}
                data-testid="properties-renderer"
            />
        );
    };

    return {default: PropertiesStub};
});

const airtableElement = {
    componentName: 'airtable',
    componentVersion: 1,
    connectionId: null,
    id: '5',
    kind: DataSyncElementKind.Source,
    operationName: 'read',
    parameters: {},
};

const csvFileElement = {
    ...airtableElement,
    componentName: 'csvFile',
};

const renderStep = (element: typeof airtableElement) =>
    render(
        <DataSyncElementStep dataSync={{elements: [element], id: '10'} as never} kind={DataSyncElementKind.Source} />
    );

beforeEach(() => {
    hoisted.mockComponentDefinition.mockReturnValue({
        clusterElements: [{name: 'read', title: 'Read table row', type: 'SOURCE'}],
        connection: {},
        connectionRequired: true,
        name: 'airtable',
        version: 1,
    });
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('DataSyncElementStep', () => {
    it('reports the component as requiring a connection while none is chosen yet', () => {
        renderStep(airtableElement);

        const propertiesRenderer = screen.getByTestId('properties-renderer');

        expect(propertiesRenderer).toHaveAttribute('data-connection-required', 'true');
        expect(propertiesRenderer).toHaveAttribute('data-connection-id', 'undefined');
    });

    it('reports a component with no connection definition as not requiring one', () => {
        hoisted.mockComponentDefinition.mockReturnValue({
            clusterElements: [{name: 'read', title: 'Read', type: 'SOURCE'}],
            connection: undefined,
            connectionRequired: false,
            name: 'csvFile',
            version: 1,
        });

        renderStep(csvFileElement);

        const propertiesRenderer = screen.getByTestId('properties-renderer');

        expect(propertiesRenderer).toHaveAttribute('data-connection-required', 'false');
        expect(propertiesRenderer).toHaveAttribute('data-connection-id', 'undefined');
    });
});
