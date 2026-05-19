import {fireEvent, render, screen, waitFor} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {ComponentProps} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionDialog from './ConnectionDialog';

// ---------------------------------------------------------------------------
// Hoisted mocks (must not reference outer-scope constants — vi.hoisted runs
// before module initialisation)
// ---------------------------------------------------------------------------

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    registerMutate: vi.fn(),
    storesQueryResult: {
        data: {connectionCredentialStores: [{readOnly: false, type: 'DATABASE'}]},
        isLoading: false,
    } as Record<string, unknown>,
}));

// GraphQL: credential stores query
vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useConnectionCredentialStoresQuery: () => hoisted.storesQueryResult,
    };
});

// REST + GraphQL mutation hooks
vi.mock('@/shared/mutations/automation/connections.mutations', () => ({
    useCreateConnectionMutation: () => ({
        error: null,
        isPending: false,
        mutate: hoisted.createMutate,
        mutateAsync: hoisted.createMutate,
        reset: vi.fn(),
    }),
    useRegisterExistingConnectionMutation: () => ({
        error: null,
        isPending: false,
        mutate: hoisted.registerMutate,
        mutateAsync: hoisted.registerMutate,
        reset: vi.fn(),
    }),
    useUpdateConnectionMutation: () => ({
        error: null,
        isPending: false,
        mutate: vi.fn(),
        mutateAsync: vi.fn(),
        reset: vi.fn(),
    }),
}));

// Connection definition queries — return empty/null so no properties render
vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    ComponentDefinitionKeys: {componentDefinitions: ['componentDefinitions']},
    useGetConnectionDefinitionQuery: () => ({data: null, error: null, isLoading: false}),
    useGetConnectionDefinitionsQuery: () => ({data: [], error: null, isLoading: false}),
}));

// OAuth2 queries
vi.mock('@/shared/queries/platform/oauth2.queries', () => ({
    useGetOAuth2AuthorizationParametersQuery: () => ({data: null, error: null, isLoading: false}),
    useGetOAuth2PropertiesQuery: () => ({data: null, error: null, isLoading: false}),
}));

// Platform store — AUTOMATION platform type so no embedded-only UI branches render
vi.mock('@/pages/home/stores/usePlatformTypeStore', () => ({
    PlatformType: {AUTOMATION: 'AUTOMATION', EMBEDDED: 'EMBEDDED'},
    usePlatformTypeStore: (selector: (state: Record<string, unknown>) => unknown) =>
        selector({currentType: 'AUTOMATION'}),
}));

// Environment store — return id 0 (DEVELOPMENT)
vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentEnvironmentId: 0}),
}));

// Workspace store — provide a default workspace id
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: Record<string, unknown>) => unknown) => selector({currentWorkspaceId: 1}),
}));

// EnvironmentBadge renders nothing that affects these tests
vi.mock('@/shared/components/EnvironmentBadge', () => ({
    default: () => <span data-testid="environment-badge">Development</span>,
}));

// ComponentSelectionInput — not needed for credential-store tests
vi.mock('@/shared/components/connection/ComponentSelectionInput', () => ({
    default: () => <div data-testid="component-selection-input" />,
}));

// Properties (connection/authorization) — not needed here
vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="properties" />,
}));

// useCopyToClipboard polyfill
vi.mock('@uidotdev/usehooks', () => ({
    useCopyToClipboard: () => [null, vi.fn()],
}));

// ---------------------------------------------------------------------------
// Shared minimal component definition (satisfies required props)
// ---------------------------------------------------------------------------

const MOCK_COMPONENT_DEFINITION = {
    icon: undefined,
    name: 'acme',
    title: 'Acme',
};

const MOCK_COMPONENT_DEFINITIONS = [MOCK_COMPONENT_DEFINITION];

// Stub query hook that returns an empty list of tags
const stubGetConnectionTagsQuery = () => ({data: [], error: null, isLoading: false});

// Stub mutation factory returned by the useCreateConnectionMutation prop
function makeCreateMutationStub() {
    return {
        error: null,
        isPending: false,
        mutate: hoisted.createMutate,
        mutateAsync: hoisted.createMutate,
        reset: vi.fn(),
    };
}

/** Render the dialog in create mode (no connection, no triggerNode → opens immediately). */
function renderDialog(overrides: Partial<ComponentProps<typeof ConnectionDialog>> = {}) {
    return render(
        <ConnectionDialog
            componentDefinition={MOCK_COMPONENT_DEFINITION as never}
            componentDefinitions={MOCK_COMPONENT_DEFINITIONS as never}
            connectionTagsQueryKey={['connectionTags']}
            connectionsQueryKey={['connections']}
            useCreateConnectionMutation={() => makeCreateMutationStub() as never}
            useGetConnectionTagsQuery={stubGetConnectionTagsQuery as never}
            {...overrides}
        />
    );
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

describe('ConnectionDialog credential store integration', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        Element.prototype.scrollIntoView = vi.fn();
    });

    it('hides the credential storage picker when only DATABASE is registered', () => {
        hoisted.storesQueryResult = {
            data: {connectionCredentialStores: [{readOnly: false, type: 'DATABASE'}]},
            isLoading: false,
        };

        renderDialog();

        expect(screen.queryByText(/credential storage/i)).not.toBeInTheDocument();
    });

    it('shows the credential storage picker when an external store is configured', () => {
        hoisted.storesQueryResult = {
            data: {
                connectionCredentialStores: [
                    {readOnly: false, type: 'DATABASE'},
                    {readOnly: false, type: 'AWS_SECRETS_MANAGER'},
                ],
            },
            isLoading: false,
        };

        renderDialog();

        expect(screen.getByText('Credential storage')).toBeInTheDocument();
    });

    it('shows the register-existing toggle when an external read-write store is selected', async () => {
        hoisted.storesQueryResult = {
            data: {
                connectionCredentialStores: [
                    {readOnly: false, type: 'DATABASE'},
                    {readOnly: false, type: 'AWS_SECRETS_MANAGER'},
                ],
            },
            isLoading: false,
        };

        renderDialog();

        // Open the credential storage select and pick AWS Secrets Manager
        const selectTrigger = screen.getByRole('combobox', {name: /credential storage/i});

        fireEvent.click(selectTrigger);

        fireEvent.click(await screen.findByText('AWS Secrets Manager'));

        // The toggle should now be visible and unchecked (off by default)
        const toggle = await screen.findByLabelText(/register existing credential/i);

        expect(toggle).toBeInTheDocument();
        expect(toggle).not.toBeChecked();
        expect(toggle).not.toBeDisabled();
    });

    it('auto-enables and locks the toggle when the selected external store is read-only', async () => {
        hoisted.storesQueryResult = {
            data: {
                connectionCredentialStores: [
                    {readOnly: false, type: 'DATABASE'},
                    {readOnly: true, type: 'HASHICORP_VAULT'},
                ],
            },
            isLoading: false,
        };

        renderDialog();

        const selectTrigger = screen.getByRole('combobox', {name: /credential storage/i});

        fireEvent.click(selectTrigger);

        fireEvent.click(await screen.findByText('HashiCorp Vault'));

        // Toggle should be checked and disabled (locked) because the store is read-only
        await waitFor(() => {
            const toggle = screen.getByLabelText(/register existing credential/i);

            expect(toggle).toBeChecked();
            expect(toggle).toBeDisabled();
        });

        // Read-only explanation text should also appear
        expect(screen.getByText(/configured read-only by your administrator/i)).toBeInTheDocument();
    });

    it('shows a disabled credential storage input with explanatory caption in edit mode', () => {
        hoisted.storesQueryResult = {
            data: {
                connectionCredentialStores: [
                    {readOnly: false, type: 'DATABASE'},
                    {readOnly: false, type: 'AWS_SECRETS_MANAGER'},
                ],
            },
            isLoading: false,
        };

        renderDialog({
            connection: {
                componentName: 'acme',
                connectionVersion: 1,
                environmentId: 0,
                id: 42,
                name: 'Existing Acme Connection',
                parameters: {},
                version: 1,
            },
        });

        // Edit mode: picker rendered as disabled Input, not a Select
        expect(screen.getByText(/credential storage cannot be changed after creation/i)).toBeInTheDocument();

        // The register-existing toggle must not appear in edit mode
        expect(screen.queryByLabelText(/register existing credential/i)).not.toBeInTheDocument();
    });

    it('calls useRegisterExistingConnectionMutation when the register-existing toggle is on at submit', async () => {
        const user = userEvent.setup();

        hoisted.storesQueryResult = {
            data: {
                connectionCredentialStores: [
                    {readOnly: false, type: 'DATABASE'},
                    {readOnly: false, type: 'HASHICORP_VAULT'},
                ],
            },
            isLoading: false,
        };

        renderDialog();

        // Select HashiCorp Vault as the credential store
        const selectTrigger = screen.getByRole('combobox', {name: /credential storage/i});

        fireEvent.click(selectTrigger);

        fireEvent.click(await screen.findByText('HashiCorp Vault'));

        // Enable the register-existing toggle
        const toggle = await screen.findByLabelText(/register existing credential/i);

        await user.click(toggle);

        expect(toggle).toBeChecked();

        // Fill in the connection name (required field) — default is "Acme" from componentDefinition.title
        const nameInput = screen.getByPlaceholderText(/my connection/i);

        await user.clear(nameInput);
        await user.type(nameInput, 'My Vault Connection');

        // Fill in the credential reference (required when register-existing is on)
        const credRefInput = await screen.findByPlaceholderText(/bytechef\/connections\//i);

        await user.type(credRefInput, 'secret/bytechef/acme');

        // Click the Save button
        const saveButton = screen.getByRole('button', {name: /save/i});

        await user.click(saveButton);

        await waitFor(() => {
            expect(hoisted.registerMutate).toHaveBeenCalled();
            expect(hoisted.createMutate).not.toHaveBeenCalled();
        });
    });

    it('falls back to no-picker DATABASE behavior when the stores query returns no data', () => {
        hoisted.storesQueryResult = {
            data: undefined,
            error: new Error('GraphQL network error'),
            isLoading: false,
        };

        renderDialog();

        // Without store data, stores defaults to [] → picker hidden
        expect(screen.queryByText(/credential storage/i)).not.toBeInTheDocument();
    });
});
