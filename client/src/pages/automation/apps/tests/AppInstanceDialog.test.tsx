import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {
    fireEvent,
    mockScrollIntoView,
    render,
    screen,
    stubMutation,
    userEvent,
    waitFor,
    windowResizeObserver,
} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppInstanceDialog from '../components/AppInstanceDialog';

const hoisted = vi.hoisted(() => ({
    manageableWorkspaceIds: ['7', '8'] as string[],
    userAuthAvailabilityLoading: false,
    userAuthAvailable: true,
}));

vi.mock('@/shared/queries/automation/workspaces.queries', () => ({
    useGetUserWorkspacesQuery: vi.fn(() => ({
        data: [
            {id: 7, name: 'Sales'},
            {id: 8, name: 'Germany'},
            {id: 9, name: 'Support'},
        ],
    })),
}));

vi.mock('@/pages/automation/apps/hooks/useAppManageScopes', () => ({
    default: vi.fn(() => (workspaceId: string) => hoisted.manageableWorkspaceIds.includes(workspaceId)),
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppUserAuthAvailableQuery: vi.fn(() =>
        hoisted.userAuthAvailabilityLoading
            ? {data: undefined, isLoading: true}
            : {data: {appUserAuthAvailable: hoisted.userAuthAvailable}, isLoading: false}
    ),
    useCreateAppInstanceMutation: vi.fn(),
    useUpdateAppInstanceMutation: vi.fn(),
    useUpdateAppInstanceUserAuthMutation: vi.fn(),
}));

const {
    useAppUserAuthAvailableQuery,
    useCreateAppInstanceMutation,
    useUpdateAppInstanceMutation,
    useUpdateAppInstanceUserAuthMutation,
} = await import('@/shared/middleware/graphql');

const createMutation = stubMutation(vi.mocked(useCreateAppInstanceMutation));
const updateMutation = stubMutation(vi.mocked(useUpdateAppInstanceMutation));
const updateUserAuthMutation = stubMutation(vi.mocked(useUpdateAppInstanceUserAuthMutation));

const existingInstance = {
    environmentId: '2',
    hostingTarget: 'VERCEL',
    id: '11',
    name: 'crm-prod',
    url: 'https://crm.example.com',
    userAuthMode: 'NONE',
    workspaceId: '7',
} as never;

describe('AppInstanceDialog', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();

        hoisted.manageableWorkspaceIds = ['7', '8'];
        hoisted.userAuthAvailabilityLoading = false;
        hoisted.userAuthAvailable = true;

        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('offers only the workspaces where the user holds APP_MANAGE', async () => {
        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('combobox', {name: 'Workspace'}));

        expect(await screen.findByRole('option', {name: 'Germany'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Sales'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Support'})).not.toBeInTheDocument();
    });

    it('creates an instance in the chosen workspace and shows its key once', async () => {
        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-germany');
        await userEvent.click(screen.getByRole('combobox', {name: 'Workspace'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Germany'}));
        await userEvent.click(screen.getByRole('button', {name: 'Create Instance'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                appId: '1',
                environmentId: '2',
                input: {hostingTarget: 'VERCEL', name: 'crm-germany', userAuthMode: 'REQUIRED'},
                workspaceId: '8',
            });
        });

        createMutation.triggerOnSuccess(
            {createAppInstance: {appInstance: {id: '13'}, secretKey: 'instancesecretkey'}},
            undefined
        );

        expect(await screen.findByDisplayValue('instancesecretkey')).toBeInTheDocument();
        expect(screen.getByText(/ByteChef cannot show it again/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Instance'})).not.toBeInTheDocument();
    });

    it('refuses to create an instance when no workspace may host it', async () => {
        hoisted.manageableWorkspaceIds = [];

        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-germany');
        await userEvent.click(screen.getByRole('button', {name: 'Create Instance'}));

        expect(await screen.findByText('Workspace is required')).toBeInTheDocument();
        expect(createMutation.mutate).not.toHaveBeenCalled();
    });

    it('updates an existing instance, including its URL', async () => {
        render(
            <AppInstanceDialog
                appId="1"
                appInstance={
                    {
                        environmentId: '2',
                        hostingTarget: 'VERCEL',
                        id: '11',
                        name: 'crm-prod',
                        url: '',
                        workspaceId: '7',
                    } as never
                }
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        expect(screen.queryByRole('combobox', {name: 'Workspace'})).not.toBeInTheDocument();

        await userEvent.type(screen.getByLabelText('URL'), 'https://crm.example.com');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '11',
                input: {hostingTarget: 'VERCEL', name: 'crm-prod', url: 'https://crm.example.com'},
            });
        });
    });

    it.each(['ftp://crm.example.com', 'javascript:alert(1)', 'mailto:ops@example.com'])(
        'refuses %s as the URL of an instance',
        async (url) => {
            render(
                <AppInstanceDialog
                    appId="1"
                    appInstance={
                        {
                            environmentId: '2',
                            hostingTarget: 'VERCEL',
                            id: '11',
                            name: 'crm-prod',
                            url: '',
                            workspaceId: '7',
                        } as never
                    }
                    onOpenChange={vi.fn()}
                    open={true}
                />
            );

            await userEvent.type(screen.getByLabelText('URL'), url);
            await userEvent.click(screen.getByRole('button', {name: 'Save'}));

            expect(await screen.findByText('URL must be an http or https URL')).toBeInTheDocument();
            expect(updateMutation.mutate).not.toHaveBeenCalled();
        }
    );

    it('preselects Required on create', () => {
        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('Required');
    });

    it('does not allow creating an instance until sign-in availability has answered', async () => {
        hoisted.userAuthAvailabilityLoading = true;

        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-germany');

        expect(screen.getByRole('button', {name: 'Create Instance'})).toBeDisabled();

        fireEvent.submit(screen.getByRole('button', {name: 'Create Instance'}));

        expect(createMutation.mutate).not.toHaveBeenCalled();
    });

    it('asks sign-in availability for the workspace the instance is created in', async () => {
        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        expect(vi.mocked(useAppUserAuthAvailableQuery)).toHaveBeenLastCalledWith({workspaceId: '7'}, {enabled: true});

        await userEvent.click(screen.getByRole('combobox', {name: 'Workspace'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Germany'}));

        await waitFor(() => {
            expect(vi.mocked(useAppUserAuthAvailableQuery)).toHaveBeenLastCalledWith(
                {workspaceId: '8'},
                {enabled: true}
            );
        });
    });

    it('preselects None on create and explains why when sign-in is unavailable', async () => {
        hoisted.userAuthAvailable = false;

        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await waitFor(() => {
            expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('None');
        });

        expect(screen.getByText(/Sign-in is unavailable/)).toBeInTheDocument();
    });

    it('sends None on create when sign-in is unavailable', async () => {
        hoisted.userAuthAvailable = false;

        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await waitFor(() => {
            expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('None');
        });

        await userEvent.type(screen.getByLabelText('Name'), 'crm-germany');
        await userEvent.click(screen.getByRole('button', {name: 'Create Instance'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                appId: '1',
                environmentId: '2',
                input: {hostingTarget: 'VERCEL', name: 'crm-germany', userAuthMode: 'NONE'},
                workspaceId: '7',
            });
        });
    });

    it('keeps an existing instance editable when sign-in is unavailable', () => {
        hoisted.userAuthAvailable = false;

        render(<AppInstanceDialog appId="1" appInstance={existingInstance} onOpenChange={vi.fn()} open={true} />);

        expect(screen.queryByText(/Sign-in is unavailable/)).not.toBeInTheDocument();
    });

    it('sends None on create when it is chosen', async () => {
        render(<AppInstanceDialog appId="1" onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-germany');
        await userEvent.click(screen.getByRole('combobox', {name: 'Sign-in'}));
        await userEvent.click(await screen.findByRole('option', {name: 'None'}));
        await userEvent.click(screen.getByRole('button', {name: 'Create Instance'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                appId: '1',
                environmentId: '2',
                input: {hostingTarget: 'VERCEL', name: 'crm-germany', userAuthMode: 'NONE'},
                workspaceId: '7',
            });
        });
    });

    it('shows the current sign-in mode of an existing instance', () => {
        render(<AppInstanceDialog appId="1" appInstance={existingInstance} onOpenChange={vi.fn()} open={true} />);

        expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('None');
    });

    it('changes the sign-in mode of an existing instance through the dedicated mutation', async () => {
        render(<AppInstanceDialog appId="1" appInstance={existingInstance} onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('combobox', {name: 'Sign-in'}));
        await userEvent.click(await screen.findByRole('option', {name: 'Optional'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '11',
                input: {hostingTarget: 'VERCEL', name: 'crm-prod', url: 'https://crm.example.com'},
            });
        });

        expect(updateUserAuthMutation.mutate).not.toHaveBeenCalled();

        updateMutation.triggerOnSuccess(undefined, undefined);

        expect(updateUserAuthMutation.mutate).toHaveBeenCalledWith({id: '11', userAuthMode: 'OPTIONAL'});
    });

    it('leaves the sign-in mode alone when it is not changed', async () => {
        render(<AppInstanceDialog appId="1" appInstance={existingInstance} onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalled();
        });

        updateMutation.triggerOnSuccess(undefined, undefined);

        expect(updateUserAuthMutation.mutate).not.toHaveBeenCalled();
    });
});
