import Button from '@/components/Button/Button';
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
import {QueryClient} from '@tanstack/react-query';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AppDialog from '../components/AppDialog';

const hoisted = vi.hoisted(() => ({
    appCategories: [
        {id: '4', name: 'Sales'},
        {id: '9', name: 'Finance'},
    ],
    scopes: ['APP_VIEW', 'APP_MANAGE'] as string[],
    userAuthAvailabilityLoading: false,
    userAuthAvailable: true,
}));

vi.mock('@/shared/middleware/graphql', async () => ({
    ...(await vi.importActual<Record<string, unknown>>('@/shared/middleware/graphql')),
    useAppCategoriesQuery: vi.fn(),
    useAppUserAuthAvailableQuery: vi.fn(() =>
        hoisted.userAuthAvailabilityLoading
            ? {data: undefined, isLoading: true}
            : {data: {appUserAuthAvailable: hoisted.userAuthAvailable}, isLoading: false}
    ),
    useCreateAppMutation: vi.fn(),
    useMyWorkspaceScopesQuery: vi.fn(() => ({data: {myWorkspaceScopes: hoisted.scopes}})),
    useSetAppVisibilityMutation: vi.fn(),
    useUpdateAppMutation: vi.fn(),
}));

const {useAppCategoriesQuery, useCreateAppMutation, useSetAppVisibilityMutation, useUpdateAppMutation} =
    await import('@/shared/middleware/graphql');

const loadedCategories = () =>
    ({data: {appCategories: hoisted.appCategories}, isError: false, isLoading: false}) as never;

const lastCategoriesQueryCall = () => vi.mocked(useAppCategoriesQuery).mock.calls.at(-1) as unknown[];

const createMutation = stubMutation(vi.mocked(useCreateAppMutation));
const setAppVisibilityMutation = stubMutation(vi.mocked(useSetAppVisibilityMutation));
const updateMutation = stubMutation(vi.mocked(useUpdateAppMutation));

const iconFile = (name = 'icon.png', type = 'image/png', size = 16) => new File([new Uint8Array(size)], name, {type});

const fetchMock = vi.fn();

const createObjectURLMock = vi.fn();
const revokeObjectURLMock = vi.fn();

const getCategoryInput = () =>
    screen.getAllByRole('combobox').find((combobox) => combobox.tagName === 'INPUT') as HTMLInputElement;

const sharedApp = {description: '', id: '5', name: 'crm', visibility: 'WORKSPACE', workspaceId: '7'} as never;

describe('AppDialog', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        if (!Element.prototype.hasPointerCapture) {
            Element.prototype.hasPointerCapture = vi.fn(() => false);
            Element.prototype.setPointerCapture = vi.fn();
            Element.prototype.releasePointerCapture = vi.fn();
        }

        vi.clearAllMocks();

        hoisted.scopes = ['APP_VIEW', 'APP_MANAGE'];
        hoisted.userAuthAvailabilityLoading = false;
        hoisted.userAuthAvailable = true;

        vi.mocked(useAppCategoriesQuery).mockImplementation(loadedCategories);

        fetchMock.mockResolvedValue(new Response(null, {status: 204}));

        vi.stubGlobal('fetch', fetchMock);

        let objectUrlCount = 0;

        createObjectURLMock.mockImplementation(() => {
            objectUrlCount += 1;

            return `blob:icon-${objectUrlCount}`;
        });

        URL.createObjectURL = createObjectURLMock;
        URL.revokeObjectURL = revokeObjectURLMock;

        environmentStore.setState({currentEnvironmentId: 2});
        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('rejects an empty name', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        expect(await screen.findByText('Name is required')).toBeInTheDocument();
        expect(createMutation.mutate).not.toHaveBeenCalled();
    });

    it('asks no URL: it belongs to an instance', () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        expect(screen.queryByLabelText('URL')).not.toBeInTheDocument();
    });

    it('creates an app and then shows the secret key once', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                environmentId: '2',
                input: {
                    description: undefined,
                    hostingTarget: 'VERCEL',
                    name: 'crm-dashboard',
                    portalDiscoverable: false,
                    userAuthMode: 'REQUIRED',
                },
                workspaceId: '7',
            });
        });

        createMutation.triggerOnSuccess(
            {createApp: {appInstance: {id: '11'}, secretKey: 'abcdefghijklmnop'}},
            undefined
        );

        expect(await screen.findByDisplayValue('abcdefghijklmnop')).toBeInTheDocument();
        expect(screen.getByText(/BYTECHEF_APP_KEY=abcdefghijklmnop/)).toBeInTheDocument();
        expect(screen.getByText(/ByteChef cannot show it again/)).toBeInTheDocument();
    });

    it('updates an existing app, clearing the description with an empty string', async () => {
        render(
            <AppDialog app={{description: 'old', id: '5', name: 'crm'} as never} onOpenChange={vi.fn()} open={true} />
        );

        expect(screen.queryByText('Hosting target')).not.toBeInTheDocument();

        await userEvent.clear(screen.getByLabelText('Description'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '5',
                input: {description: '', name: 'crm', portalDiscoverable: false},
            });
        });
    });

    it('shows the current app values when the edit dialog is reopened after a save', async () => {
        const app = {description: '', id: '5', name: 'crm'} as never;

        const {rerender} = render(<AppDialog app={app} triggerNode={<Button label="Edit" />} />);

        await userEvent.click(screen.getByRole('button', {name: 'Edit'}));
        await userEvent.clear(screen.getByLabelText('Name'));
        await userEvent.type(screen.getByLabelText('Name'), 'crm-renamed');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        updateMutation.triggerOnSuccess(undefined, undefined);

        rerender(
            <AppDialog
                app={{description: '', id: '5', name: 'crm-renamed'} as never}
                triggerNode={<Button label="Edit" />}
            />
        );

        await userEvent.click(await screen.findByRole('button', {name: 'Edit'}));

        expect(await screen.findByLabelText('Name')).toHaveValue('crm-renamed');
    });

    it('shows the current visibility of the app to someone who may share it', () => {
        render(<AppDialog app={sharedApp} canShare={true} onOpenChange={vi.fn()} open={true} />);

        expect(screen.getByText('Visibility')).toBeInTheDocument();
        expect(screen.getByRole('radio', {name: 'Shared with workspace'})).toBeChecked();
        expect(screen.getByRole('radio', {name: 'Private'})).not.toBeChecked();
        expect(screen.queryByRole('radio', {name: 'Specific people'})).not.toBeInTheDocument();
    });

    it('saves a visibility change through setAppVisibility, not through the app update', async () => {
        render(<AppDialog app={sharedApp} canShare={true} onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('radio', {name: 'Private'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(setAppVisibilityMutation.mutate).toHaveBeenCalledWith({
                appId: '5',
                visibility: 'PRIVATE',
                workspaceId: '7',
            });
        });

        expect(updateMutation.mutate).toHaveBeenCalledWith({
            id: '5',
            input: {description: '', name: 'crm', portalDiscoverable: false},
        });
    });

    it('does not call setAppVisibility when the visibility is unchanged', async () => {
        render(<AppDialog app={sharedApp} canShare={true} onOpenChange={vi.fn()} open={true} />);

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalled();
        });

        expect(setAppVisibilityMutation.mutate).not.toHaveBeenCalled();
    });

    it('hides the visibility from someone who may not share the app', () => {
        render(<AppDialog app={sharedApp} onOpenChange={vi.fn()} open={true} />);

        expect(screen.queryByText('Visibility')).not.toBeInTheDocument();
        expect(screen.queryByRole('radio', {name: 'Private'})).not.toBeInTheDocument();
    });

    it('offers no visibility when creating an app', () => {
        render(<AppDialog canShare={true} onOpenChange={vi.fn()} open={true} />);

        expect(screen.queryByText('Visibility')).not.toBeInTheDocument();
    });

    it('preselects Required for the first instance of a new app', () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('Required');
    });

    it('does not allow creating an app until sign-in availability has answered', async () => {
        hoisted.userAuthAvailabilityLoading = true;

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');

        expect(screen.getByRole('button', {name: 'Create App'})).toBeDisabled();

        fireEvent.submit(screen.getByRole('button', {name: 'Create App'}));

        expect(createMutation.mutate).not.toHaveBeenCalled();
    });

    it('preselects None and explains why when sign-in is unavailable', async () => {
        hoisted.userAuthAvailable = false;

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await waitFor(() => {
            expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('None');
        });

        expect(screen.getByText(/Sign-in is unavailable/)).toBeInTheDocument();
    });

    it('sends None for the first instance when sign-in is unavailable', async () => {
        hoisted.userAuthAvailable = false;

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await waitFor(() => {
            expect(screen.getByRole('combobox', {name: 'Sign-in'})).toHaveTextContent('None');
        });

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                environmentId: '2',
                input: {
                    description: undefined,
                    hostingTarget: 'VERCEL',
                    name: 'crm-dashboard',
                    portalDiscoverable: false,
                    userAuthMode: 'NONE',
                },
                workspaceId: '7',
            });
        });
    });

    it('sends None when it is chosen for the first instance', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.click(screen.getByRole('combobox', {name: 'Sign-in'}));
        await userEvent.click(await screen.findByRole('option', {name: 'None'}));
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                environmentId: '2',
                input: {
                    description: undefined,
                    hostingTarget: 'VERCEL',
                    name: 'crm-dashboard',
                    portalDiscoverable: false,
                    userAuthMode: 'NONE',
                },
                workspaceId: '7',
            });
        });
    });

    it('asks no sign-in mode when editing an app', () => {
        render(
            <AppDialog app={{description: 'old', id: '5', name: 'crm'} as never} onOpenChange={vi.fn()} open={true} />
        );

        expect(screen.queryByRole('combobox', {name: 'Sign-in'})).not.toBeInTheDocument();
    });

    it('refuses an icon of another type or over 256 KB before uploading anything', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile('icon.gif', 'image/gif')]}});

        expect(await screen.findByText('The icon must be a PNG, JPEG or WebP image.')).toBeInTheDocument();

        fireEvent.change(screen.getByLabelText('Icon file'), {
            target: {files: [iconFile('big.png', 'image/png', 256 * 1024 + 1)]},
        });

        expect(await screen.findByText('The icon cannot be larger than 256 KB.')).toBeInTheDocument();
        expect(screen.queryByText(/Selected:/)).not.toBeInTheDocument();
        expect(fetchMock).not.toHaveBeenCalled();
    });

    it('uploads the icon of a new app only after the app is created', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile()]}});

        expect(await screen.findByText('Selected: icon.png')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalled();
        });

        expect(fetchMock).not.toHaveBeenCalled();

        createMutation.triggerOnSuccess(
            {createApp: {appInstance: {app: {id: '5'}, id: '11'}, secretKey: 'abcdefghijklmnop'}},
            undefined
        );

        await waitFor(() => {
            expect(fetchMock).toHaveBeenCalledTimes(1);
        });

        const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];

        expect(url).toBe('/api/automation/internal/apps/5/icon');
        expect(init.method).toBe('POST');
        expect((init.body as FormData).get('file')).toBeInstanceOf(File);
        expect(((init.body as FormData).get('file') as File).name).toBe('icon.png');
        expect(await screen.findByDisplayValue('abcdefghijklmnop')).toBeInTheDocument();
    });

    it('keeps the new key on screen and says what happened when the icon upload fails after the create', async () => {
        fetchMock.mockResolvedValue(
            new Response(JSON.stringify({detail: 'The icon is not a supported image.'}), {status: 400})
        );

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile()]}});

        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalled();
        });

        createMutation.triggerOnSuccess(
            {createApp: {appInstance: {app: {id: '5'}, id: '11'}, secretKey: 'abcdefghijklmnop'}},
            undefined
        );

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'The app was created, but its icon was not uploaded: The icon is not a supported image.'
        );
        expect(screen.getByDisplayValue('abcdefghijklmnop')).toBeInTheDocument();
    });

    it('previews the current icon from the admin endpoint and removes it after the save', async () => {
        render(
            <AppDialog
                app={{description: '', hasIcon: true, id: '5', name: 'crm'} as never}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        expect(screen.getByRole('img', {name: 'Current icon'}).getAttribute('src')).toMatch(
            /^\/api\/automation\/internal\/apps\/5\/icon(\?v=\d+)?$/
        );

        await userEvent.click(screen.getByRole('button', {name: 'Remove icon'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalled();
        });

        expect(fetchMock).not.toHaveBeenCalled();

        updateMutation.triggerOnSuccess(undefined, undefined);

        await waitFor(() => {
            expect(fetchMock).toHaveBeenCalledWith(
                '/api/automation/internal/apps/5/icon',
                expect.objectContaining({method: 'DELETE'})
            );
        });
    });

    it('keeps the edit dialog open with the server error when replacing the icon fails', async () => {
        const onOpenChange = vi.fn();

        fetchMock.mockResolvedValue(new Response(JSON.stringify({title: 'Icon too large'}), {status: 400}));

        render(
            <AppDialog
                app={{description: '', hasIcon: false, id: '5', name: 'crm'} as never}
                onOpenChange={onOpenChange}
                open={true}
            />
        );

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile()]}});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalled();
        });

        updateMutation.triggerOnSuccess(undefined, undefined);

        expect(await screen.findByRole('alert')).toHaveTextContent(
            'The app was saved, but its icon was not changed: Icon too large'
        );
        expect(onOpenChange).not.toHaveBeenCalledWith(false);
    });

    it('sends an existing category by id and the portal switch when creating an app', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.click(getCategoryInput());
        await userEvent.click(await screen.findByText('Finance'));
        await userEvent.click(screen.getByRole('switch', {name: 'Show in App Portal'}));
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith({
                environmentId: '2',
                input: {
                    category: {id: '9'},
                    description: undefined,
                    hostingTarget: 'VERCEL',
                    name: 'crm-dashboard',
                    portalDiscoverable: true,
                    userAuthMode: 'REQUIRED',
                },
                workspaceId: '7',
            });
        });
    });

    it('sends a new category by name when one is typed', async () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.type(getCategoryInput(), 'Operations');
        await userEvent.click(await screen.findByText('Create "Operations"'));
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    input: expect.objectContaining({category: {name: 'Operations'}, portalDiscoverable: false}),
                })
            );
        });
    });

    it('hints when an app appears in the portal', () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        expect(screen.getByText(/only when it is ACTIVE and has an instance you can open/)).toBeInTheDocument();
    });

    it('leaves an untouched category as it was and sends the portal switch on update', async () => {
        render(
            <AppDialog
                app={{categoryId: '9', description: '', id: '5', name: 'crm', portalDiscoverable: false} as never}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        expect(await screen.findByText('Finance')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('switch', {name: 'Show in App Portal'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '5',
                input: {description: '', name: 'crm', portalDiscoverable: true},
            });
        });
    });

    it('sends a changed category on update', async () => {
        render(
            <AppDialog
                app={{categoryId: '9', description: '', id: '5', name: 'crm', portalDiscoverable: true} as never}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        await userEvent.click(getCategoryInput());
        await userEvent.click(await screen.findByText('Sales'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '5',
                input: {category: {id: '4'}, description: '', name: 'crm', portalDiscoverable: true},
            });
        });
    });

    it('previews a pending icon from an object URL and revokes it when the file changes and on unmount', async () => {
        const {unmount} = render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile('first.png')]}});

        expect(await screen.findByRole('img', {name: 'Selected icon'})).toHaveAttribute('src', 'blob:icon-1');

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile('second.png')]}});

        await waitFor(() => {
            expect(screen.getByRole('img', {name: 'Selected icon'})).toHaveAttribute('src', 'blob:icon-2');
        });

        expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:icon-1');

        unmount();

        expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:icon-2');
    });

    it('does not show the cached old icon after a successful replace', async () => {
        const app = {description: '', hasIcon: true, id: '77', name: 'crm'} as never;

        const {unmount} = render(<AppDialog app={app} onOpenChange={vi.fn()} open={true} />);

        const srcBefore = screen.getByRole('img', {name: 'Current icon'}).getAttribute('src');

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile()]}});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        updateMutation.triggerOnSuccess(undefined, undefined);

        await waitFor(() => {
            expect(fetchMock).toHaveBeenCalledTimes(1);
        });

        unmount();

        render(<AppDialog app={app} onOpenChange={vi.fn()} open={true} />);

        const srcAfter = screen.getByRole('img', {name: 'Current icon'}).getAttribute('src');

        expect(srcAfter).not.toBe(srcBefore);
        expect(srcAfter).toMatch(/^\/api\/automation\/internal\/apps\/77\/icon\?v=/);
    });

    it('loads the categories for an open dialog of someone who may manage apps', () => {
        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        expect(lastCategoriesQueryCall()).toEqual([{workspaceId: '7'}, expect.objectContaining({enabled: true})]);
    });

    it('does not load the categories while the dialog is closed', () => {
        render(<AppDialog app={sharedApp} triggerNode={<Button label="Edit" />} />);

        expect(lastCategoriesQueryCall()[1]).toEqual(expect.objectContaining({enabled: false}));
    });

    it('does not load the categories without a workspace id', () => {
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        const [variables, options] = lastCategoriesQueryCall();

        expect(options).toEqual(expect.objectContaining({enabled: false}));
        expect(variables).not.toEqual({workspaceId: 'undefined'});
    });

    it('does not load the categories for someone who may not manage apps', () => {
        hoisted.scopes = ['APP_VIEW'];

        render(<AppDialog app={sharedApp} onOpenChange={vi.fn()} open={true} />);

        expect(lastCategoriesQueryCall()[1]).toEqual(expect.objectContaining({enabled: false}));
    });

    it('saves the other fields and leaves the category untouched when the categories cannot be loaded', async () => {
        vi.mocked(useAppCategoriesQuery).mockImplementation(
            () => ({data: undefined, error: new Error('failed'), isError: true, isLoading: false}) as never
        );

        render(
            <AppDialog
                app={{categoryId: '9', description: '', id: '5', name: 'crm', portalDiscoverable: false} as never}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        await userEvent.clear(screen.getByLabelText('Name'));
        await userEvent.type(screen.getByLabelText('Name'), 'crm-renamed');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => {
            expect(updateMutation.mutate).toHaveBeenCalledWith({
                id: '5',
                input: {description: '', name: 'crm-renamed', portalDiscoverable: false},
            });
        });
    });

    it('refreshes the categories after a save that created one by name', async () => {
        const invalidateQueriesSpy = vi.spyOn(QueryClient.prototype, 'invalidateQueries');

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        await userEvent.type(screen.getByLabelText('Name'), 'crm-dashboard');
        await userEvent.type(getCategoryInput(), 'Operations');
        await userEvent.click(await screen.findByText('Create "Operations"'));
        await userEvent.click(screen.getByRole('button', {name: 'Create App'}));

        await waitFor(() => {
            expect(createMutation.mutate).toHaveBeenCalled();
        });

        const createVariables = createMutation.mutate.mock.calls[0][0];

        invalidateQueriesSpy.mockClear();

        createMutation.triggerOnSuccess(
            {createApp: {appInstance: {app: {id: '5'}, id: '11'}, secretKey: 'abcdefghijklmnop'}},
            createVariables
        );

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['appCategories']});

        invalidateQueriesSpy.mockRestore();
    });

    it('shows the icon again after a replace that follows a failed preview', async () => {
        render(
            <AppDialog
                app={{description: '', hasIcon: true, id: '88', name: 'crm'} as never}
                onOpenChange={vi.fn()}
                open={true}
            />
        );

        fireEvent.error(screen.getByRole('img', {name: 'Current icon'}));

        expect(screen.queryByRole('img', {name: 'Current icon'})).not.toBeInTheDocument();

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile()]}});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        updateMutation.triggerOnSuccess(undefined, undefined);

        expect(await screen.findByRole('img', {name: 'Current icon'})).toHaveAttribute(
            'src',
            expect.stringMatching(/^\/api\/automation\/internal\/apps\/88\/icon\?v=/)
        );
    });

    it('never renders a revoked object URL when the pending file is replaced or cleared', async () => {
        const srcsRenderedAtRevoke: Array<string | null> = [];

        revokeObjectURLMock.mockImplementation(() => {
            document.querySelectorAll('img').forEach((image) => srcsRenderedAtRevoke.push(image.getAttribute('src')));
        });

        render(<AppDialog onOpenChange={vi.fn()} open={true} />);

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile('first.png')]}});

        expect(await screen.findByRole('img', {name: 'Selected icon'})).toHaveAttribute('src', 'blob:icon-1');

        fireEvent.change(screen.getByLabelText('Icon file'), {target: {files: [iconFile('second.png')]}});

        await waitFor(() => {
            expect(screen.getByRole('img', {name: 'Selected icon'})).toHaveAttribute('src', 'blob:icon-2');
        });

        expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:icon-1');
        expect(srcsRenderedAtRevoke).not.toContain('blob:icon-1');

        await userEvent.click(screen.getByRole('button', {name: 'Remove icon'}));

        await waitFor(() => {
            expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:icon-2');
        });

        expect(srcsRenderedAtRevoke).not.toContain('blob:icon-2');
        expect(screen.queryByRole('img', {name: 'Selected icon'})).not.toBeInTheDocument();
    });
});
