import {render, resetAll, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AddKnowledgeBaseSourceDialog from '../AddKnowledgeBaseSourceDialog';

const hoisted = vi.hoisted(() => {
    return {
        clusterElementProperties: [
            {
                controlType: 'TEXT',
                label: 'Base ID',
                name: 'baseId',
                required: true,
                type: 'STRING',
            },
        ] as Array<{controlType: string; label: string; name: string; required: boolean; type: string}>,
        createMutate: vi.fn(),
        currentEnvironmentId: 1,
        currentWorkspaceId: 7,
        propertiesRenderMock: vi.fn(),
    };
});

vi.mock('@/shared/middleware/graphql', () => ({
    // Mirror the codegen output so the dialog's initial-state default
    // (`useState<TombstoneStrategy>(TombstoneStrategy.PeriodicFullReplace)`) resolves to a valid value at mount.
    // Without this, the enum reference is undefined and React crashes during render with a confusing "module mock
    // missing export" error rather than a typecheck failure.
    TombstoneStrategy: {
        None: 'NONE',
        PeriodicFullReplace: 'PERIODIC_FULL_REPLACE',
        UpstreamChangeFeed: 'UPSTREAM_CHANGE_FEED',
    },
    useClusterElementFieldsQuery: () => ({
        data: {
            clusterElementFields: [
                {label: 'Kind', name: 'kind', type: 'String'},
                {label: 'Owner', name: 'owner', type: 'String'},
                {label: 'Stage', name: 'stage', type: 'String'},
            ],
        },
        isLoading: false,
    }),
    useCreateKnowledgeBaseSourceMutation: ({onSuccess}: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: unknown) => {
            hoisted.createMutate(variables);
            onSuccess?.();
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
            properties: hoisted.clusterElementProperties,
        },
    }),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: {
            clusterElements: [{name: 'airtableSource', type: 'SOURCE'}],
            name: 'airtable',
            version: 1,
        },
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
    hoisted.clusterElementProperties = [
        {
            controlType: 'TEXT',
            label: 'Base ID',
            name: 'baseId',
            required: true,
            type: 'STRING',
        },
    ];

    hoisted.createMutate.mockReset();
    hoisted.propertiesRenderMock.mockReset();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const openDialogAndPickConnection = async (user: ReturnType<typeof userEvent.setup>) => {
    await user.click(screen.getByRole('button', {name: /add source/i}));

    await user.type(screen.getByLabelText('Source Name'), 'Acme Source');

    await user.click(screen.getByTestId('kbs-connection-option-42'));
};

describe('AddKnowledgeBaseSourceDialog', () => {
    it('renders the SOURCE Properties block on step 1 when properties are present', async () => {
        const user = userEvent.setup();

        render(<AddKnowledgeBaseSourceDialog knowledgeBaseId="20" />);

        await openDialogAndPickConnection(user);

        await user.click(screen.getByRole('button', {name: /next/i}));

        expect(screen.getByTestId('kbs-source-configuration-step')).toBeInTheDocument();
        expect(screen.getByTestId('properties-renderer')).toBeInTheDocument();
        expect(screen.getByTestId('property-baseId')).toBeInTheDocument();
        expect(screen.getByTestId('properties-control-path')).toHaveTextContent('sourceParameters');
    });

    it('shows the empty placeholder on step 1 when the SOURCE has no properties', async () => {
        hoisted.clusterElementProperties = [];

        const user = userEvent.setup();

        render(<AddKnowledgeBaseSourceDialog knowledgeBaseId="20" />);

        await openDialogAndPickConnection(user);

        await user.click(screen.getByRole('button', {name: /next/i}));

        expect(screen.getByTestId('kbs-source-configuration-empty')).toBeInTheDocument();
        expect(screen.queryByTestId('properties-renderer')).not.toBeInTheDocument();
    });

    it('includes parameters in the create mutation payload (empty when not filled)', async () => {
        const user = userEvent.setup();

        render(<AddKnowledgeBaseSourceDialog knowledgeBaseId="20" />);

        await openDialogAndPickConnection(user);

        // Connection -> SourceConfig -> MetadataTags -> Cadence -> Review (4 Next clicks then Create Source).
        await user.click(screen.getByRole('button', {name: /next/i}));
        await user.click(screen.getByRole('button', {name: /next/i}));
        await user.click(screen.getByRole('button', {name: /next/i}));
        await user.click(screen.getByRole('button', {name: /next/i}));
        await user.click(screen.getByRole('button', {name: /create source/i}));

        expect(hoisted.createMutate).toHaveBeenCalledTimes(1);

        const payload = hoisted.createMutate.mock.calls[0][0] as {
            input: {
                knowledgeBaseId: string;
                metadataFields?: {fields: string[]};
                name: string;
                parameters: Record<string, unknown>;
            };
        };

        expect(payload.input.knowledgeBaseId).toBe('20');
        expect(payload.input.name).toBe('Acme Source');
        expect(payload.input.parameters).toEqual({});
        // No fields selected on the Metadata Tags step -> omit the key entirely so the server falls back to
        // MVP "every emitted field becomes a tag" behavior.
        expect(payload.input.metadataFields).toBeUndefined();
    });

    it('sends metadataFields whitelist when fields are selected on the Metadata Tags step', async () => {
        const user = userEvent.setup();

        render(<AddKnowledgeBaseSourceDialog knowledgeBaseId="20" />);

        await openDialogAndPickConnection(user);

        await user.click(screen.getByRole('button', {name: /next/i})); // -> Source Config
        await user.click(screen.getByRole('button', {name: /next/i})); // -> Metadata Tags

        await user.click(screen.getByTestId('kbs-metadata-field-kind'));
        await user.click(screen.getByTestId('kbs-metadata-field-stage'));

        await user.click(screen.getByRole('button', {name: /next/i})); // -> Cadence
        await user.click(screen.getByRole('button', {name: /next/i})); // -> Review
        await user.click(screen.getByRole('button', {name: /create source/i}));

        const payload = hoisted.createMutate.mock.calls[0][0] as {
            input: {metadataFields?: {fields: string[]}};
        };

        // The whitelist shape mirrors the server JSONB contract: {fields: ["fieldA", "fieldB"]}.
        expect(payload.input.metadataFields).toEqual({fields: ['kind', 'stage']});
    });
});
