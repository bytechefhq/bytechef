import DataSyncWizard from '@/pages/automation/data-syncs/components/wizard/DataSyncWizard';
import {useClusterElementContext} from '@/pages/platform/workflow-editor/components/properties/ClusterElementContext';
import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    propertiesRenders: [] as Array<{clusterElementName: string; propertyNames: string}>,
}));

const PROPERTIES_BY_OPERATION: Record<string, Array<{name: string; type: string}>> = {
    read: [
        {name: 'baseId', type: 'STRING'},
        {name: 'tableId', type: 'STRING'},
        {name: 'lastModifiedFieldName', type: 'STRING'},
    ],
    write: [
        {name: 'baseId', type: 'STRING'},
        {name: 'tableId', type: 'STRING'},
    ],
};

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async () => {
    const actual = await vi.importActual<typeof import('@/shared/middleware/graphql')>('@/shared/middleware/graphql');

    return {
        ...actual,
        useClusterElementDefinitionQuery: (variables: {clusterElementName: string}) => ({
            data: {
                clusterElementDefinition: {
                    properties: PROPERTIES_BY_OPERATION[variables.clusterElementName] ?? [],
                },
            },
        }),
        useSetDataSyncElementMutation: () => ({isPending: false, mutate: vi.fn()}),
        useUpdateDataSyncElementMutation: () => ({isPending: false, mutate: vi.fn()}),
    };
});

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [{clusterElementsCount: {DESTINATION: 1, SOURCE: 1}, name: 'airtable', title: 'Airtable', version: 1}],
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: {
            clusterElements: [
                {name: 'read', title: 'Read table row', type: 'SOURCE'},
                {name: 'write', title: 'Write table rows', type: 'DESTINATION'},
            ],
            connection: undefined,
            connectionRequired: false,
            name: 'airtable',
            version: 1,
        },
    }),
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

vi.mock('@/pages/automation/data-syncs/components/wizard/DataSyncMappingStep', () => ({
    default: () => <div data-testid="mapping-step" />,
}));

vi.mock('@/pages/automation/data-syncs/components/wizard/DataSyncTestStep', () => ({
    default: () => <div data-testid="test-step" />,
}));

vi.mock('@/pages/automation/data-syncs/components/wizard/DataSyncTriggerStep', () => ({
    default: () => <div data-testid="trigger-step" />,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => {
    const PropertiesStub = ({properties}: {properties?: Array<{name?: string}>}) => {
        const clusterElementContext = useClusterElementContext();

        hoisted.propertiesRenders.push({
            clusterElementName: clusterElementContext?.clusterElementName ?? '',
            propertyNames: (properties ?? []).map((property) => property.name).join(','),
        });

        return <div data-testid="properties-renderer" />;
    };

    return {default: PropertiesStub};
});

const sourceElement = {
    componentName: 'airtable',
    componentVersion: 1,
    connectionId: null,
    id: '5',
    kind: DataSyncElementKind.Source,
    operationName: 'read',
    parameters: {baseId: 'appOne'},
};

const destinationElement = {
    ...sourceElement,
    id: '6',
    kind: DataSyncElementKind.Destination,
    operationName: 'write',
    parameters: {baseId: 'appTwo'},
};

const dataSync = {
    draftWorkflowId: 'workflow-1',
    elements: [sourceElement, destinationElement],
    id: '10',
} as never;

beforeEach(() => {
    hoisted.propertiesRenders.length = 0;
});

afterEach(() => {
    vi.clearAllMocks();
});

describe('DataSyncWizard', () => {
    it('never renders one step’s properties under the other step’s cluster element', async () => {
        render(<DataSyncWizard dataSync={dataSync} />);

        await userEvent.click(screen.getByText('Next'));
        await userEvent.click(screen.getByText('Next'));

        const mismatched = hoisted.propertiesRenders.filter(
            ({clusterElementName, propertyNames}) =>
                propertyNames !==
                (PROPERTIES_BY_OPERATION[clusterElementName] ?? []).map((property) => property.name).join(',')
        );

        expect(mismatched).toEqual([]);
    });
});
