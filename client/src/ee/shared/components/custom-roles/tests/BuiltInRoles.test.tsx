import {mockScrollIntoView, render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import BuiltInRoles from '../components/BuiltInRoles';

const hoisted = vi.hoisted(() => ({
    builtInRoles: [] as {name: string; scopes: string[]}[],
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useBuiltInRolesQuery: vi.fn(() => ({data: {builtInRoles: hoisted.builtInRoles}})),
}));

const permissionScopeGroups = [
    {name: 'WORKFLOW', scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']},
    {name: 'WORKSPACE', scopes: ['WORKSPACE_VIEW', 'WORKSPACE_MEMBER_MANAGE']},
];

describe('BuiltInRoles', () => {
    beforeEach(() => {
        windowResizeObserver();
        mockScrollIntoView();

        vi.clearAllMocks();

        hoisted.builtInRoles = [
            {name: 'VIEWER', scopes: ['WORKFLOW_VIEW', 'WORKSPACE_VIEW']},
            {name: 'EDITOR', scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT', 'WORKSPACE_VIEW']},
        ];
    });

    it('lists every tier with how much it grants, collapsed', () => {
        render(<BuiltInRoles permissionScopeGroups={permissionScopeGroups} />);

        expect(screen.getByRole('button', {name: /Viewer/})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: /Editor/})).toBeInTheDocument();
        expect(screen.getByText('2 permissions')).toBeInTheDocument();

        // ADMIN holds every scope the server was built with, so expanding by default would push the custom roles this
        // section exists to inform off the screen.
        expect(screen.queryByText('View, Edit')).not.toBeInTheDocument();
    });

    it('groups an expanded tier by module, naming each permission by its action', async () => {
        render(<BuiltInRoles permissionScopeGroups={permissionScopeGroups} />);

        await userEvent.click(screen.getByRole('button', {name: /Editor/}));

        expect(screen.getByText('Workflow')).toBeInTheDocument();
        expect(screen.getByText('View, Edit')).toBeInTheDocument();
    });

    it('omits a module the tier holds nothing from', async () => {
        // A heading with an empty list under it reads as "Workspace: nothing", which is a claim about this tier rather
        // than about the module — and it is wrong the moment a module contributes a scope the tier does hold.
        hoisted.builtInRoles = [{name: 'VIEWER', scopes: ['WORKFLOW_VIEW']}];

        render(<BuiltInRoles permissionScopeGroups={permissionScopeGroups} />);

        await userEvent.click(screen.getByRole('button', {name: /Viewer/}));

        expect(screen.getByText('Workflow')).toBeInTheDocument();
        expect(screen.queryByText('Workspace')).not.toBeInTheDocument();
    });

    it('renders nothing until the roles arrive', () => {
        // An empty bordered accordion under a heading claims the server has no built-in roles, which it never does.
        hoisted.builtInRoles = [];

        render(<BuiltInRoles permissionScopeGroups={permissionScopeGroups} />);

        expect(screen.queryByText(/Every member holds one of these/)).not.toBeInTheDocument();
    });
});
