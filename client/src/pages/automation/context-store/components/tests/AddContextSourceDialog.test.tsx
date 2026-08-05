import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AddContextSourceDialog from '../AddContextSourceDialog';

const hoisted = vi.hoisted(() => {
    return {
        clusterElementFieldsData: {
            clusterElementFields: [] as Array<{name: string; label?: string | null; type?: string | null}>,
        },
        createMutate: vi.fn(),
        currentEnvironmentId: 1,
        currentWorkspaceId: 7,
        navigateMock: vi.fn(),
        propertiesRenderMock: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    ContextStoreTombstoneStrategy: {
        None: 'NONE',
        PeriodicFullReplace: 'PERIODIC_FULL_REPLACE',
        UpstreamChangeFeed: 'UPSTREAM_CHANGE_FEED',
    },
    useClusterElementFieldsQuery: () => ({
        data: hoisted.clusterElementFieldsData,
        isLoading: false,
    }),
    useContextStoresQuery: () => ({
        data: {
            contextStores: [{description: null, environment: 'DEVELOPMENT', id: '7', name: 'Default', version: 0}],
        },
        isLoading: false,
    }),
    useCreateContextStoreSourceMutation: ({
        onSuccess,
    }: {
        onSuccess?: (data: {createContextStoreSource: {id: string}}) => void;
    }) => ({
        isPending: false,
        mutate: (variables: unknown) => {
            hoisted.createMutate(variables);
            onSuccess?.({createContextStoreSource: {id: 'source-1'}});
        },
    }),
    useDataStreamCompatibleConnectionsQuery: () => ({
        data: {
            dataStreamCompatibleConnections: [
                {
                    componentName: 'airtable',
                    componentVersion: 1,
                    id: '42',
                    name: 'Acme Airtable',
                },
            ],
        },
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetClusterElementDefinitionQuery: () => ({
        data: {
            componentName: 'airtable',
            componentVersion: 1,
            name: 'airtableSource',
            properties: [
                {
                    controlType: 'TEXT',
                    label: 'Base ID',
                    name: 'baseId',
                    required: true,
                    type: 'STRING',
                },
            ],
        },
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    ComponentDefinitionKeys: {
        filteredComponentDefinitions: (request: unknown) => ['componentDefinitions', request],
    },
    useGetComponentDefinitionQuery: () => ({
        data: {
            clusterElements: [{name: 'airtableSource', type: 'SOURCE'}],
            name: 'airtable',
            version: 1,
        },
    }),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {
                description: 'Airtable description',
                icon: 'airtable-icon',
                name: 'airtable',
                title: 'Airtable',
            },
        ],
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: hoisted.currentWorkspaceId}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: hoisted.currentEnvironmentId}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigateMock,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: React.ReactNode}) => (
        <div data-testid="workflow-mock-provider">{children}</div>
    ),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: (props: {properties: Array<{name: string}>; controlPath?: string}) => {
        hoisted.propertiesRenderMock(props);

        return (
            <div data-testid="properties-renderer">
                <span data-testid="properties-control-path">{props.controlPath}</span>

                {props.properties.map((property) => (
                    <span data-testid={`property-${property.name}`} key={property.name}>
                        {property.name}
                    </span>
                ))}
            </div>
        );
    },
}));

vi.mock(import('@tanstack/react-query'), async (importOriginal) => {
    const actual = await importOriginal();

    return {
        ...actual,
        useQueryClient: () => ({invalidateQueries: vi.fn()}) as unknown as ReturnType<typeof actual.useQueryClient>,
    };
});

beforeEach(() => {
    hoisted.clusterElementFieldsData = {clusterElementFields: []};
    hoisted.createMutate.mockReset();
    hoisted.navigateMock.mockReset();
    hoisted.propertiesRenderMock.mockReset();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const openWizardAndAdvanceToEntities = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(screen.getByRole('button', {name: /add source/i}));

    // Pick the parent Context Store first — required now that the wizard no longer auto-creates a Default store.
    // Radix Select doesn't render its options into the dialog DOM under JSDOM (portal + pointer events), so drive the
    // trigger by keyboard: open with Space, then ArrowDown + Enter selects the first item.
    const trigger = screen.getByTestId('context-store-select');

    trigger.focus();

    await user.keyboard('{ }');
    await user.keyboard('{ArrowDown}');
    await user.keyboard('{Enter}');

    await user.type(screen.getByLabelText('Source Name'), 'Acme');

    // Step 0 is now a component-first picker: pick the component card, then the connection appears.
    await user.click(screen.getByTestId('component-option-airtable'));

    await user.click(screen.getByTestId('connection-option-42'));

    await user.click(screen.getByRole('button', {name: /next/i}));
};

describe('AddContextSourceDialog', () => {
    it('renders the source Properties block for the selected connection', async () => {
        const user = userEvent.setup();

        render(<AddContextSourceDialog />);

        await openWizardAndAdvanceToEntities(user);

        expect(screen.getByTestId('properties-renderer')).toBeInTheDocument();
        expect(screen.getByTestId('property-baseId')).toBeInTheDocument();
        expect(screen.getByTestId('properties-control-path')).toHaveTextContent('parameters');
    });

    it('falls back to a free-text ID Field input when no fields are available', async () => {
        const user = userEvent.setup();

        render(<AddContextSourceDialog />);

        await openWizardAndAdvanceToEntities(user);

        const idFieldInput = screen.getByLabelText('ID Field');

        expect(idFieldInput.tagName).toBe('INPUT');
    });

    it('uses a Select for ID Field when fields are returned', async () => {
        hoisted.clusterElementFieldsData = {
            clusterElementFields: [
                {label: 'Name', name: 'Name', type: 'String'},
                {label: 'Score', name: 'Score', type: 'Integer'},
            ],
        };

        const user = userEvent.setup();

        render(<AddContextSourceDialog />);

        await openWizardAndAdvanceToEntities(user);

        const idFieldTrigger = screen.getByRole('combobox', {name: /id field/i});

        expect(idFieldTrigger).toBeInTheDocument();
    });

    it('renders the IndexedFieldsEditor with field-name dropdowns when fields are returned', async () => {
        hoisted.clusterElementFieldsData = {
            clusterElementFields: [
                {label: 'Name', name: 'Name', type: 'String'},
                {label: 'Score', name: 'Score', type: 'Integer'},
            ],
        };

        const user = userEvent.setup();

        render(<AddContextSourceDialog />);

        await openWizardAndAdvanceToEntities(user);

        await user.click(screen.getByRole('button', {name: /add field/i}));

        const fieldTrigger = screen.getByRole('combobox', {name: /field name 1/i});

        expect(fieldTrigger).toBeInTheDocument();
    });

    it('submits the record-shape fields inline in the create mutation payload', async () => {
        // Free-text fallback path (no available fields) so the test does not depend on
        // Radix Select pointer-capture APIs that jsdom does not implement.
        const user = userEvent.setup();

        render(<AddContextSourceDialog />);

        await openWizardAndAdvanceToEntities(user);

        await user.type(screen.getByLabelText(/Entity Name/), 'contacts');
        await user.type(screen.getByLabelText('ID Field'), 'id');

        await user.click(screen.getByRole('button', {name: /add field/i}));

        await user.type(screen.getByLabelText('Field name 1'), 'email');

        await user.click(screen.getByRole('button', {name: /next/i}));
        await user.click(screen.getByRole('button', {name: /next/i}));
        await user.click(screen.getByRole('button', {name: /create source/i}));

        expect(hoisted.createMutate).toHaveBeenCalledTimes(1);

        const payload = hoisted.createMutate.mock.calls[0][0] as {
            input: {
                entityName: string;
                idField: string;
                indexedFields: Record<string, string>;
                parameters: Record<string, unknown>;
            };
        };

        expect(payload.input.entityName).toBe('contacts');
        expect(payload.input.idField).toBe('id');
        expect(payload.input.indexedFields).toEqual({email: 'TEXT'});
        expect(payload.input.parameters).toEqual({});
    });
});
