import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CustomRolesManager from '../CustomRolesManager';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    customRoles: [] as unknown[],
    deleteMutate: vi.fn(),
    permissionScopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT', 'A_NEWLY_CONTRIBUTED_SCOPE'] as string[],
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateCustomRoleMutation: vi.fn(() => ({mutate: hoisted.createMutate})),
    useCustomRolesQuery: vi.fn(() => ({data: {customRoles: hoisted.customRoles}, isLoading: false})),
    useDeleteCustomRoleMutation: vi.fn(() => ({mutate: hoisted.deleteMutate})),
    usePermissionScopesQuery: vi.fn(() => ({data: {permissionScopes: hoisted.permissionScopes}})),
    useUpdateCustomRoleMutation: vi.fn(() => ({mutate: hoisted.updateMutate})),
}));

vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()}) as never),
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

describe('CustomRolesManager', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        vi.clearAllMocks();
        hoisted.customRoles = [deployerRole, auditorRole];
        hoisted.permissionScopes = ['WORKFLOW_VIEW', 'WORKFLOW_EDIT', 'A_NEWLY_CONTRIBUTED_SCOPE'];
    });

    it('offers the scopes the server reports rather than a hardcoded list', async () => {
        // A scope a module contributed after this page was written must appear without a client change — the server
        // validates against the same registry, so any divergence means offering names it would reject. The label is
        // derived, not looked up, which is what lets an unknown scope render readably at all.
        render(<CustomRolesManager />);

        await userEvent.click(screen.getByRole('button', {name: 'Create Role'}));

        expect(screen.getByText('A Newly Contributed Scope')).toBeInTheDocument();
    });

    it('offers edit and delete for every role', () => {
        render(<CustomRolesManager />);

        // Every role is tenant-global and managed here — there is no read-only tier anymore. Two roles, two icon
        // buttons (edit, delete) each.
        expect(screen.getAllByRole('button', {name: ''})).toHaveLength(4);
    });

    it('creates a role', async () => {
        hoisted.customRoles = [];

        render(<CustomRolesManager />);

        await userEvent.click(screen.getByRole('button', {name: 'Create Role'}));
        await userEvent.type(screen.getByPlaceholderText('Role name'), 'Deployer');
        await userEvent.click(screen.getByText('Workflow Edit'));
        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        expect(hoisted.createMutate).toHaveBeenCalledWith({
            input: {description: '', name: 'Deployer', scopes: ['WORKFLOW_EDIT']},
        });
    });

    it('loads a role into the form for editing and saves it', async () => {
        render(<CustomRolesManager />);

        await userEvent.click(screen.getAllByRole('button', {name: ''})[0]);

        expect(screen.getByText('Edit Role')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Deployer')).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({
            id: '900',
            input: {description: 'Can deploy', name: 'Deployer', scopes: ['WORKFLOW_VIEW']},
        });
    });

    it('deletes a role', async () => {
        render(<CustomRolesManager />);

        await userEvent.click(screen.getAllByRole('button', {name: ''})[1]);

        expect(hoisted.deleteMutate).toHaveBeenCalledWith({id: '900'});
    });
});
