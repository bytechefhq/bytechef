import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CustomRolesManager from '../CustomRolesManager';

const hoisted = vi.hoisted(() => ({
    builtInRoles: [{name: 'VIEWER', scopes: ['WORKFLOW_VIEW']}] as {name: string; scopes: string[]}[],
    captureCreateOnSuccess: vi.fn(),
    createMutate: vi.fn(),
    customRoles: [] as unknown[],
    customRolesLoading: false,
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    permissionScopeGroups: [
        {name: 'WORKFLOW', scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']},
        {name: 'A_NEWLY_CONTRIBUTED_MODULE', scopes: ['A_NEWLY_CONTRIBUTED_MODULE_INSPECT']},
    ] as {name: string; scopes: string[]}[],
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useBuiltInRolesQuery: vi.fn(() => ({data: {builtInRoles: hoisted.builtInRoles}})),
    useCreateCustomRoleMutation: vi.fn((options: {onSuccess: () => void}) => {
        hoisted.captureCreateOnSuccess(options.onSuccess);

        return {mutate: hoisted.createMutate};
    }),
    useCustomRolesQuery: vi.fn(() => ({
        data: hoisted.customRolesLoading ? undefined : {customRoles: hoisted.customRoles},
        isLoading: hoisted.customRolesLoading,
    })),
    useDeleteCustomRoleMutation: vi.fn(() => ({mutate: hoisted.deleteMutate})),
    usePermissionScopeGroupsQuery: vi.fn(() => ({data: {permissionScopeGroups: hoisted.permissionScopeGroups}})),
    useUpdateCustomRoleMutation: vi.fn(() => ({mutate: hoisted.updateMutate})),
}));

vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: hoisted.invalidateQueries}) as never),
}));

const deployerRole = {
    description: 'Can deploy',
    id: '900',
    name: 'Deployer',
    scopes: ['WORKFLOW_VIEW'],
};

const auditorRole = {
    description: null,
    id: '901',
    name: 'Auditor',
    scopes: ['WORKFLOW_VIEW'],
};

const renderOnCustomRolesTab = async () => {
    render(<CustomRolesManager />);

    await userEvent.click(screen.getByRole('tab', {name: 'Custom Roles'}));
};

describe('CustomRolesManager', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        vi.clearAllMocks();
        hoisted.builtInRoles = [{name: 'VIEWER', scopes: ['WORKFLOW_VIEW']}];
        hoisted.customRoles = [deployerRole, auditorRole];
        hoisted.customRolesLoading = false;
        hoisted.permissionScopeGroups = [
            {name: 'WORKFLOW', scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']},
            {name: 'A_NEWLY_CONTRIBUTED_MODULE', scopes: ['A_NEWLY_CONTRIBUTED_MODULE_INSPECT']},
        ];
    });

    it('opens on the built-in roles and shows the custom roles on their own tab', async () => {
        render(<CustomRolesManager />);

        expect(screen.getByRole('tab', {name: 'Built-in Roles', selected: true})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /Viewer/})).toBeInTheDocument();
        expect(screen.queryByText('Deployer')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Custom Roles'}));

        expect(screen.getByText('Deployer')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /Viewer/})).not.toBeInTheDocument();
    });

    it('says the roles are loading rather than showing an empty table', async () => {
        hoisted.customRolesLoading = true;

        await renderOnCustomRolesTab();

        // An empty table while the fetch is in flight reads as "this tenant has no custom roles", which is a different
        // and actionable statement — the operator goes off to create one that already exists.
        expect(screen.getByText('Loading roles…')).toBeInTheDocument();
        expect(screen.queryByText('No Custom Roles')).not.toBeInTheDocument();
    });

    it('explains an empty list once the fetch has answered', async () => {
        hoisted.customRoles = [];

        await renderOnCustomRolesTab();

        expect(screen.getByText('No Custom Roles')).toBeInTheDocument();
        expect(screen.queryByText('Loading roles…')).not.toBeInTheDocument();
    });

    it('invalidates the operator own scopes after a role changes', async () => {
        hoisted.customRoles = [];

        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Create Role'}));
        await userEvent.type(screen.getByPlaceholderText('Role name'), 'Deployer');
        await userEvent.click(screen.getByText('Edit'));
        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        hoisted.captureCreateOnSuccess.mock.calls.at(-1)![0]();

        // Editing a role moves the scope set of everyone holding it, the operator included, and their own scopes are
        // cached separately from this list. Without this their gated controls stay as they were until a reload.
        const invalidatedKeys = hoisted.invalidateQueries.mock.calls.map(
            (call) => (call[0] as {queryKey: string[]}).queryKey[0]
        );

        expect(invalidatedKeys).toContain('CustomRoles');
        expect(invalidatedKeys).toContain('MyWorkspaceScopes');
    });

    it('offers the scopes the server reports rather than a hardcoded list', async () => {
        // A module contributed after this page was written must appear, heading and scopes, without a client change —
        // the server validates against the same registry, so any divergence means offering names it would reject. Both
        // labels are derived, not looked up, which is what lets an unknown module render readably at all.
        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Create Role'}));

        expect(screen.getByText('A Newly Contributed Module')).toBeInTheDocument();
        expect(screen.getByText('Inspect')).toBeInTheDocument();
    });

    it('labels a scope by its action, under its module heading', async () => {
        // The heading already says Workflow; repeating it on every checkbox ("Workflow View", "Workflow Edit") is the
        // noise the grouping exists to remove.
        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Create Role'}));

        // Scoped to the dialog: the role table behind it lists a role's scopes by their full names, which is right
        // there — a bare "View" in a Permissions column would not say view of what.
        const dialog = within(screen.getByRole('dialog'));

        expect(dialog.getByText('Workflow')).toBeInTheDocument();
        expect(dialog.getByText('View')).toBeInTheDocument();
        expect(dialog.queryByText('Workflow View')).not.toBeInTheDocument();
    });

    it('offers edit and delete for every role, each with an accessible name', async () => {
        await renderOnCustomRolesTab();

        // Every role is tenant-global and managed here — there is no read-only tier anymore. Two roles, two icon
        // buttons (edit, delete) each, and each names the role it acts on: an unlabelled icon button in a table of
        // near-identical rows tells a screen reader nothing about which row it belongs to.
        expect(screen.getByRole('button', {name: 'Edit the Deployer role'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete the Deployer role'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Edit the Auditor role'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete the Auditor role'})).toBeInTheDocument();
        expect(screen.queryAllByRole('button', {name: ''})).toHaveLength(0);
    });

    it('creates a role', async () => {
        hoisted.customRoles = [];

        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Create Role'}));
        await userEvent.type(screen.getByPlaceholderText('Role name'), 'Deployer');
        await userEvent.click(screen.getByText('Edit'));
        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        expect(hoisted.createMutate).toHaveBeenCalledWith({
            input: {description: '', name: 'Deployer', scopes: ['WORKFLOW_EDIT']},
        });
    });

    it('loads a role into the form for editing and saves it', async () => {
        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Edit the Deployer role'}));

        expect(screen.getByText('Edit Role')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Deployer')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: '900',
            input: {description: 'Can deploy', name: 'Deployer', scopes: ['WORKFLOW_VIEW']},
        });
    });

    it('asks for confirmation before deleting a role', async () => {
        // Deleting is not undoable and strips the role's scopes from every member assigned it, so a single click on an
        // unlabelled icon must not be enough — every sibling destructive action in settings confirms first.
        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Delete the Deployer role'}));

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
        expect(screen.getByRole('alertdialog')).toHaveTextContent('Are you absolutely sure?');
    });

    it('abandons the delete when the confirmation is cancelled', async () => {
        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Delete the Deployer role'}));
        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('deletes a role once confirmed', async () => {
        await renderOnCustomRolesTab();

        await userEvent.click(screen.getByRole('button', {name: 'Delete the Deployer role'}));
        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '900'});
    });
});
