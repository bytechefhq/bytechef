import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import ConnectionDialog from '@/shared/components/connection/ConnectionDialog';
import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    embeddedGrants: {} as Record<number, string[]>,
    workspaceGrants: {} as Record<number, string[]>,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            (
                hoisted.embeddedGrants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
            ).includes(scope),
    };
});

vi.mock('@/shared/hooks/useHasWorkspaceScope', async () => {
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        useHasWorkspaceScope: (workspaceId: number | undefined, scope: string, environmentId?: number) =>
            workspaceId !== undefined &&
            (
                hoisted.workspaceGrants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
            ).includes(scope),
    };
});

vi.mock('@/shared/queries/platform/connectionDefinitions.queries', () => ({
    useGetConnectionDefinitionQuery: vi.fn(),
    useGetConnectionDefinitionsQuery: vi.fn(() => ({data: undefined})),
}));

vi.mock('@/shared/queries/platform/oauth2.queries', () => ({
    useGetOAuth2AuthorizationParametersQuery: vi.fn(() => ({data: undefined, error: null, isLoading: false})),
    useGetOAuth2PropertiesQuery: vi.fn(() => ({data: undefined, error: null, isLoading: false})),
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/Properties', () => ({
    default: () => <div data-testid="connection-properties" />,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowMockProvider: ({children}: {children: ReactNode}) => children,
}));

vi.mock('@/shared/components/connection/OAuth2Button', () => ({
    default: () => null,
}));

vi.mock('@/shared/components/connection/Scopes', () => ({
    default: () => null,
}));

vi.mock('@tanstack/react-query', async () => {
    const actual = await vi.importActual<typeof import('@tanstack/react-query')>('@tanstack/react-query');

    return {
        ...actual,
        useQueryClient: vi.fn(() => ({
            invalidateQueries: vi.fn().mockResolvedValue(undefined),
        })),
    };
});

const DEVELOPMENT = 0;
const PRODUCTION = 2;
const HIDDEN_HINT = 'Only members who can edit this connection can see its credentials.';
const WORKSPACE_ID = 1049;

const CONNECTION_DEFINITION = {
    authorizations: [
        {
            name: 'api_key',
            properties: [{name: 'key'}, {name: 'value'}],
            title: 'API Key',
            type: 'API_KEY',
        },
    ],
    properties: [{name: 'subdomain'}],
    version: 1,
};

const CONNECTION = {
    authorizationParameters: {key: 'X-Api-Key', value: 'secret-api-key-value'},
    authorizationType: 'API_KEY',
    componentName: 'slack',
    connectionParameters: {subdomain: 'acme-subdomain'},
    connectionVersion: 1,
    environmentId: PRODUCTION,
    id: 5,
    name: 'Slack',
    parameters: {key: 'X-Api-Key', subdomain: 'acme-subdomain', value: 'secret-api-key-value'},
    tags: [],
    version: 3,
};

const CREDENTIAL_KEYS = ['authorizationParameters', 'connectionParameters', 'parameters'];

const renderEditDialog = (platformType?: PlatformType) => {
    const credentialsMutateAsync = vi.fn().mockResolvedValue(undefined);
    const updateConnectionMutate = vi.fn();

    const {unmount} = render(
        <MemoryRouter>
            <ConnectionDialog
                componentDefinitions={[{name: 'slack', title: 'Slack'}] as never}
                connection={CONNECTION as never}
                connectionTagsQueryKey={['connectionTags']}
                connectionsQueryKey={['connections']}
                platformType={platformType}
                showSharedOption
                useGetConnectionTagsQuery={(() => ({data: [], error: null, isLoading: false})) as never}
                useUpdateConnectionCredentialsMutation={
                    (() => ({isPending: false, mutateAsync: credentialsMutateAsync, reset: vi.fn()})) as never
                }
                useUpdateConnectionMutation={
                    (() => ({isPending: false, mutate: updateConnectionMutate, reset: vi.fn()})) as never
                }
            />
        </MemoryRouter>
    );

    return {credentialsMutateAsync, unmount, updateConnectionMutate};
};

const saveAndGetPayload = async (updateConnectionMutate: ReturnType<typeof vi.fn>) => {
    await userEvent.setup({pointerEventsCheck: 0}).click(screen.getByRole('button', {name: 'Save'}));

    await waitFor(() => expect(updateConnectionMutate).toHaveBeenCalledTimes(1));

    return updateConnectionMutate.mock.calls[0][0] as Record<string, unknown>;
};

describe('ConnectionDialog withheld credentials', () => {
    beforeAll(() => {
        Element.prototype.scrollIntoView = Element.prototype.scrollIntoView || vi.fn();
        Element.prototype.hasPointerCapture = Element.prototype.hasPointerCapture || vi.fn();

        if (!window.ResizeObserver) {
            window.ResizeObserver = class {
                disconnect() {}

                observe() {}

                unobserve() {}
            } as never;
        }
    });

    beforeEach(() => {
        hoisted.embeddedGrants = {};
        hoisted.workspaceGrants = {};

        vi.mocked(useGetConnectionDefinitionQuery).mockReturnValue({
            data: CONNECTION_DEFINITION,
            error: null,
            isLoading: false,
        } as never);

        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
            usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});
        });
    });

    afterEach(() => {
        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
            usePlatformTypeStore.setState({currentType: PlatformType.AUTOMATION});
        });
    });

    it('testAViewerSeesConnectionCredentialsAsHiddenAndSavingSendsNoCredentials', async () => {
        hoisted.embeddedGrants = {
            [DEVELOPMENT]: ['CONNECTION_EDIT', 'CONNECTION_VIEW'],
            [PRODUCTION]: ['CONNECTION_VIEW'],
        };

        const {credentialsMutateAsync, updateConnectionMutate} = renderEditDialog();

        expect(screen.getAllByText('Hidden')).toHaveLength(3);
        expect(screen.getByText(HIDDEN_HINT)).toBeInTheDocument();
        expect(screen.queryByText('secret-api-key-value')).not.toBeInTheDocument();
        expect(screen.queryByText('acme-subdomain')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Update credentials'})).not.toBeInTheDocument();

        const payload = await saveAndGetPayload(updateConnectionMutate);

        CREDENTIAL_KEYS.forEach((credentialKey) => expect(payload).not.toHaveProperty(credentialKey));
        expect(JSON.stringify(payload)).not.toContain('secret-api-key-value');
        expect(payload).toMatchObject({id: 5, name: 'Slack', version: 3});
        expect(credentialsMutateAsync).not.toHaveBeenCalled();
    });

    it('testAnEditorSeesAndEditsConnectionCredentials', async () => {
        hoisted.embeddedGrants = {[PRODUCTION]: ['CONNECTION_EDIT', 'CONNECTION_VIEW']};

        const {credentialsMutateAsync, updateConnectionMutate} = renderEditDialog();

        expect(screen.queryByText('Hidden')).not.toBeInTheDocument();
        expect(screen.queryByText(HIDDEN_HINT)).not.toBeInTheDocument();
        expect(screen.getByText('secret-api-key-value')).toBeInTheDocument();
        expect(screen.getByText('acme-subdomain')).toBeInTheDocument();

        const payload = await saveAndGetPayload(updateConnectionMutate);

        CREDENTIAL_KEYS.forEach((credentialKey) => expect(payload).not.toHaveProperty(credentialKey));

        const user = userEvent.setup({pointerEventsCheck: 0});

        await user.click(screen.getByRole('button', {name: 'Update credentials'}));

        expect(await screen.findAllByTestId('connection-properties')).not.toHaveLength(0);

        await user.click(screen.getByRole('button', {name: 'Update credentials'}));

        await waitFor(() => expect(credentialsMutateAsync).toHaveBeenCalledTimes(1));
        expect(credentialsMutateAsync.mock.calls[0][0]).toMatchObject({id: 5, version: 3});
    });

    it('testAutomationConnectionDialogAlsoShowsHiddenCredentials', async () => {
        act(() => {
            usePlatformTypeStore.setState({currentType: PlatformType.AUTOMATION});
            useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
        });

        hoisted.embeddedGrants = {[DEVELOPMENT]: ['CONNECTION_EDIT'], [PRODUCTION]: ['CONNECTION_EDIT']};
        hoisted.workspaceGrants = {
            [DEVELOPMENT]: ['CONNECTION_EDIT', 'CONNECTION_VIEW'],
            [PRODUCTION]: ['CONNECTION_VIEW'],
        };

        const viewerRender = renderEditDialog();

        expect(screen.getAllByText('Hidden')).toHaveLength(3);
        expect(screen.getByText(HIDDEN_HINT)).toBeInTheDocument();
        expect(screen.queryByText('secret-api-key-value')).not.toBeInTheDocument();

        const payload = await saveAndGetPayload(viewerRender.updateConnectionMutate);

        CREDENTIAL_KEYS.forEach((credentialKey) => expect(payload).not.toHaveProperty(credentialKey));

        viewerRender.unmount();

        hoisted.workspaceGrants = {[PRODUCTION]: ['CONNECTION_EDIT', 'CONNECTION_VIEW']};

        renderEditDialog();

        expect(screen.queryByText('Hidden')).not.toBeInTheDocument();
        expect(screen.getByText('secret-api-key-value')).toBeInTheDocument();
    });

    it('testAnExplicitPlatformTypeWinsOverAStaleStoredType', () => {
        act(() => {
            usePlatformTypeStore.setState({currentType: PlatformType.AUTOMATION});
            useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
        });

        hoisted.embeddedGrants = {[PRODUCTION]: ['CONNECTION_VIEW']};
        hoisted.workspaceGrants = {[PRODUCTION]: ['CONNECTION_EDIT', 'CONNECTION_VIEW']};

        const embeddedRender = renderEditDialog(PlatformType.EMBEDDED);

        expect(screen.getAllByText('Hidden')).toHaveLength(3);
        expect(screen.queryByText('secret-api-key-value')).not.toBeInTheDocument();

        embeddedRender.unmount();

        act(() => {
            usePlatformTypeStore.setState({currentType: PlatformType.EMBEDDED});
        });

        renderEditDialog(PlatformType.AUTOMATION);

        expect(screen.queryByText('Hidden')).not.toBeInTheDocument();
        expect(screen.getByText('secret-api-key-value')).toBeInTheDocument();
    });
});
