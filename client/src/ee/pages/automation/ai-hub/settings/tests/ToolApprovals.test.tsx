import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {render, screen, userEvent, within} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import ToolApprovals from '../ToolApprovals';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const hoisted = vi.hoisted(() => ({
    createRuleMutate: vi.fn(),
    defaultToolNames: ['deleteRecord', 'sendEmail'] as string[],
    deleteRuleMutate: vi.fn(),
    isTenantAdmin: true,
    rules: [] as {componentName: string | null; id: string; mode: string; toolKind: string; toolName: string}[],
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useAiHubToolApprovalDefaultToolNamesQuery: vi.fn(() => ({
            data: {aiHubToolApprovalDefaultToolNames: hoisted.defaultToolNames},
            isLoading: false,
        })),
        useAiHubToolApprovalRulesQuery: vi.fn(() => ({
            data: {aiHubToolApprovalRules: hoisted.rules},
            isLoading: false,
        })),
        useCreateAiHubToolApprovalRuleMutation: vi.fn(() => ({mutate: hoisted.createRuleMutate})),
        useDeleteAiHubToolApprovalRuleMutation: vi.fn(() => ({mutate: hoisted.deleteRuleMutate})),
    };
});

vi.mock(import('@tanstack/react-query'), async (importOriginal) => ({
    ...(await importOriginal()),
    useQueryClient: vi.fn(() => ({invalidateQueries: vi.fn()}) as never),
}));

const renderPage = () =>
    render(
        <MemoryRouter>
            <ToolApprovals />
        </MemoryRouter>
    );

describe('ToolApprovals', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.defaultToolNames = ['deleteRecord', 'sendEmail'];
        hoisted.isTenantAdmin = true;
        hoisted.rules = [
            {componentName: null, id: 'rule-1', mode: 'EXEMPT', toolKind: 'CATALOG', toolName: 'sendEmail'},
            {componentName: null, id: 'rule-2', mode: 'REQUIRE', toolKind: 'CATALOG', toolName: 'archiveChat'},
        ];

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('creates a catalog exempt rule when a default tool switch is toggled on', async () => {
        renderPage();

        const deleteRecordRow = screen.getByText('deleteRecord').closest('tr');

        const exemptSwitch = within(deleteRecordRow as HTMLElement).getByRole('switch', {name: 'Exempt'});

        expect(exemptSwitch).not.toBeChecked();

        await userEvent.click(exemptSwitch);

        expect(hoisted.createRuleMutate).toHaveBeenCalledWith({
            mode: 'EXEMPT',
            toolKind: 'CATALOG',
            toolName: 'deleteRecord',
            workspaceId: '7',
        });
    });

    it('renders an existing exempt rule as checked and deletes it when toggled off', async () => {
        renderPage();

        const sendEmailRow = screen.getByText('sendEmail').closest('tr');

        const exemptSwitch = within(sendEmailRow as HTMLElement).getByRole('switch', {name: 'Exempt'});

        expect(exemptSwitch).toBeChecked();

        await userEvent.click(exemptSwitch);

        expect(hoisted.deleteRuleMutate).toHaveBeenCalledWith({ruleId: 'rule-1', workspaceId: '7'});
    });

    it('lets an admin delete a workspace rule', async () => {
        renderPage();

        const ruleRow = screen.getByText('archiveChat').closest('tr');

        await userEvent.click(within(ruleRow as HTMLElement).getByRole('button', {name: /delete/i}));

        expect(hoisted.deleteRuleMutate).toHaveBeenCalledWith({ruleId: 'rule-2', workspaceId: '7'});
    });

    it('submits a new catalog rule from the Add rule dialog without a component or kind field', async () => {
        renderPage();

        await userEvent.click(screen.getByRole('button', {name: 'Add rule'}));

        expect(screen.queryByLabelText('Component')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Kind')).not.toBeInTheDocument();

        await userEvent.type(screen.getByLabelText('Tool name'), 'publishWorkflow');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.createRuleMutate).toHaveBeenCalledWith({
            mode: 'REQUIRE',
            toolKind: 'CATALOG',
            toolName: 'publishWorkflow',
            workspaceId: '7',
        });
    });

    it('hides mutation controls for a non-admin but still shows the data', () => {
        hoisted.isTenantAdmin = false;

        renderPage();

        expect(screen.getByText('deleteRecord')).toBeInTheDocument();
        expect(screen.getByText('archiveChat')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add rule'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: /delete/i})).not.toBeInTheDocument();

        const sendEmailRow = screen.getByText('sendEmail').closest('tr');

        expect(within(sendEmailRow as HTMLElement).getByRole('switch', {name: 'Exempt'})).toBeDisabled();
    });
});
