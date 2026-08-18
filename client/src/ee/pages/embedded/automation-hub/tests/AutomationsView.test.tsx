import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {AutomationWorkflowProject, ConnectedUserProjectWorkflow} from '@/ee/shared/middleware/embedded/public';
import {fireEvent, render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationsView from '../views/AutomationsView';

// vi.mock factories hoist above module-scope `const`s, so refs they close over must come from
// vi.hoisted — see CLAUDE.md's Vitest mock factory hoisting note.
const {
    createBlankMutateMock,
    deleteAutomationMutateMock,
    deprovisionMutateMock,
    navigateMock,
    setEnabledMutateMock,
    useGetAutomationsQueryMock,
    useGetTemplateProjectsQueryMock,
} = vi.hoisted(() => ({
    createBlankMutateMock: vi.fn(),
    deleteAutomationMutateMock: vi.fn(),
    deprovisionMutateMock: vi.fn(),
    navigateMock: vi.fn(),
    setEnabledMutateMock: vi.fn(),
    useGetAutomationsQueryMock: vi.fn(),
    useGetTemplateProjectsQueryMock: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {...actual, useNavigate: () => navigateMock};
});

vi.mock('@/ee/pages/embedded/automation-hub/queries/automationHub.queries', () => ({
    useGetAutomationsQuery: useGetAutomationsQueryMock,
    useGetTemplateProjectsQuery: useGetTemplateProjectsQueryMock,
}));

vi.mock('@/ee/pages/embedded/automation-hub/mutations/automationHub.mutations', () => ({
    useCreateBlankAutomationMutation: () => ({mutate: createBlankMutateMock}),
    useDeleteAutomationMutation: () => ({mutate: deleteAutomationMutateMock}),
    useDeprovisionReferenceMutation: () => ({mutate: deprovisionMutateMock}),
    useSetAutomationEnabledMutation: () => ({mutate: setEnabledMutateMock}),
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="component icon" src={src} />,
}));

const salesProject: AutomationWorkflowProject = {
    description: 'Templates for the sales team',
    id: 1,
    kind: 'COPY',
    name: 'Sales',
    workflowTemplates: [
        {
            components: [],
            description: 'Sync new leads into the CRM',
            id: 'wf-1',
            label: 'Sync leads',
        },
        {
            components: [{icon: '<svg/>', name: 'slack', title: 'Slack'}],
            description: 'Send new lead updates to Slack',
            id: 'wf-2',
            label: 'Slack sync',
        },
    ],
};

const supportProject: AutomationWorkflowProject = {
    description: 'Templates for the support team',
    id: 2,
    kind: 'REFERENCE',
    name: 'Support',
    workflowTemplates: [
        {
            components: [],
            description: 'Triage a new support ticket',
            id: 'wf-3',
            label: 'Triage ticket',
        },
    ],
};

const copyAutomation: ConnectedUserProjectWorkflow = {
    components: [{icon: '<svg/>', name: 'gmail', title: 'Gmail'}],
    copiedFromWorkflowUuid: 'wf-1',
    dangling: false,
    enabled: true,
    kind: 'COPY',
    label: 'Sync leads',
    workflowUuid: 'copy-uuid',
};

const referenceAutomation: ConnectedUserProjectWorkflow = {
    catalogWorkflowUuid: 'wf-3',
    components: [],
    dangling: false,
    enabled: false,
    kind: 'REFERENCE',
    label: 'Triage ticket',
    workflowUuid: 'ref-uuid',
};

const blankAutomation: ConnectedUserProjectWorkflow = {
    components: [{icon: '<svg/>', name: 'gmail', title: 'Gmail'}],
    dangling: false,
    enabled: true,
    kind: 'COPY',
    label: 'Weekly digest',
    workflowUuid: 'blank-uuid',
};

const danglingReferenceAutomation: ConnectedUserProjectWorkflow = {
    catalogWorkflowUuid: 'withdrawn-uuid',
    components: [],
    dangling: true,
    enabled: true,
    kind: 'REFERENCE',
    label: 'Retired sync',
    workflowUuid: 'dangling-uuid',
};

const onActivate = vi.fn();

const renderView = () =>
    render(
        <MemoryRouter>
            <AutomationsView onActivate={onActivate} />
        </MemoryRouter>
    );

const templateCard = (label: string) => screen.getByText(label).closest('[data-slot="card"]') as HTMLElement;

describe('AutomationsView', () => {
    beforeEach(() => {
        createBlankMutateMock.mockReset();
        deleteAutomationMutateMock.mockReset();
        deprovisionMutateMock.mockReset();
        navigateMock.mockReset();
        onActivate.mockReset();
        setEnabledMutateMock.mockReset();
        useGetAutomationsQueryMock.mockReset();
        useGetTemplateProjectsQueryMock.mockReset();

        useGetTemplateProjectsQueryMock.mockReturnValue({
            data: [salesProject, supportProject],
            error: null,
            isLoading: false,
        });

        useGetAutomationsQueryMock.mockReturnValue({
            data: [copyAutomation, referenceAutomation, blankAutomation, danglingReferenceAutomation],
            error: null,
            isLoading: false,
        });

        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: true,
            sharedConnectionIds: [],
            tabs: {automations: true, connections: true, newWorkflow: true},
            theme: {},
        });

        createBlankMutateMock.mockImplementation(
            (_variables: undefined, options?: {onSuccess?: (workflowUuid: string) => void}) => {
                options?.onSuccess?.('new-workflow-uuid');
            }
        );
    });

    describe('template grid', () => {
        it('groups templates under their project heading with a description', () => {
            renderView();

            expect(screen.getByRole('heading', {level: 2, name: 'Sales'})).toBeInTheDocument();
            expect(screen.getByText('Templates for the sales team')).toBeInTheDocument();
            expect(screen.getByRole('heading', {level: 2, name: 'Support'})).toBeInTheDocument();
            expect(screen.getByText('Templates for the support team')).toBeInTheDocument();

            expect(screen.getByText('Sync new leads into the CRM')).toBeInTheDocument();
            expect(screen.getByText('Send new lead updates to Slack')).toBeInTheDocument();
            expect(screen.getByText('Triage a new support ticket')).toBeInTheDocument();
        });

        it('filters cards by label via the search box', async () => {
            const user = userEvent.setup();

            renderView();

            await user.type(screen.getByPlaceholderText(/search templates/i), 'sync');

            expect(screen.getByText('Sync new leads into the CRM')).toBeInTheDocument();
            expect(screen.getByText('Send new lead updates to Slack')).toBeInTheDocument();
            expect(screen.queryByText('Triage a new support ticket')).not.toBeInTheDocument();
            expect(screen.queryByRole('heading', {level: 2, name: 'Support'})).not.toBeInTheDocument();
        });

        it('shows an empty state when there are no template projects', () => {
            useGetTemplateProjectsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

            renderView();

            expect(screen.getByText(/no templates/i)).toBeInTheDocument();
        });

        it('shows a loading indicator while the hub queries are loading', () => {
            useGetTemplateProjectsQueryMock.mockReturnValue({data: undefined, error: null, isLoading: true});

            renderView();

            expect(screen.getByTestId('automations-view-loading')).toBeInTheDocument();
            expect(screen.queryByRole('heading', {level: 2})).not.toBeInTheDocument();
        });

        it('shows an inline alert when the templates query errors', () => {
            useGetTemplateProjectsQueryMock.mockReturnValue({
                data: undefined,
                error: new Error('boom'),
                isLoading: false,
            });

            renderView();

            expect(screen.getByText('Unable to load templates')).toBeInTheDocument();
        });

        it('shows an inline alert when the automations query errors', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: undefined,
                error: new Error('boom'),
                isLoading: false,
            });

            renderView();

            expect(screen.getByText('Unable to load automations')).toBeInTheDocument();
        });

        it('offers "Use template" on an unused template and calls onActivate with the template and project kind', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Slack sync');

            await user.click(within(card).getByRole('button', {name: 'Use template'}));

            expect(onActivate).toHaveBeenCalledTimes(1);
            expect(onActivate).toHaveBeenCalledWith(salesProject.workflowTemplates![1], 'COPY');
        });

        it('offers a toggle and a Customize menu item on an activated COPY template', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Sync leads');

            expect(within(card).queryByRole('button', {name: 'Use template'})).not.toBeInTheDocument();

            await user.click(within(card).getByRole('switch'));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: false, workflowUuid: 'copy-uuid'});

            await user.click(within(card).getByRole('button', {name: 'Sync leads actions'}));
            await user.click(screen.getByRole('menuitem', {name: /customize/i}));

            expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-uuid');
        });

        it('offers a toggle but no Customize on an activated REFERENCE template', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Triage ticket');

            expect(within(card).queryByRole('button', {name: 'Use template'})).not.toBeInTheDocument();

            await user.click(within(card).getByRole('switch'));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'ref-uuid'});

            await user.click(within(card).getByRole('button', {name: 'Triage ticket actions'}));

            expect(screen.queryByRole('menuitem', {name: /customize/i})).not.toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /remove/i})).toBeInTheDocument();
        });

        it('removes an activated COPY template via useDeleteAutomationMutation after confirming', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(within(templateCard('Sync leads')).getByRole('button', {name: 'Sync leads actions'}));
            await user.click(screen.getByRole('menuitem', {name: /remove/i}));
            await user.click(screen.getByRole('button', {name: 'Delete'}));

            expect(deleteAutomationMutateMock).toHaveBeenCalledWith('copy-uuid');
            expect(deprovisionMutateMock).not.toHaveBeenCalled();
        });

        it('removes an activated REFERENCE template by its catalog workflow uuid after confirming', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(
                within(templateCard('Triage ticket')).getByRole('button', {name: 'Triage ticket actions'})
            );
            await user.click(screen.getByRole('menuitem', {name: /remove/i}));
            await user.click(screen.getByRole('button', {name: 'Delete'}));

            expect(deprovisionMutateMock).toHaveBeenCalledWith('wf-3');
            expect(deleteAutomationMutateMock).not.toHaveBeenCalled();
        });

        it('disables activation while the automations query is failing', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: undefined,
                error: new Error('boom'),
                isLoading: false,
            });

            renderView();

            expect(within(templateCard('Sync leads')).getByRole('button', {name: 'Use template'})).toBeDisabled();
        });
    });

    describe('your automations', () => {
        it('lists only automations that do not match a published template', () => {
            renderView();

            const section = screen.getByRole('region', {name: 'Your automations'});

            expect(within(section).getByRole('row', {name: /Weekly digest/})).toBeInTheDocument();
            expect(within(section).getByRole('row', {name: /Retired sync/})).toBeInTheDocument();
            expect(within(section).queryByRole('row', {name: /Sync leads/})).not.toBeInTheDocument();
            expect(within(section).queryByRole('row', {name: /Triage ticket/})).not.toBeInTheDocument();
        });

        it('marks a dangling reference as needing attention', () => {
            renderView();

            const danglingRow = screen.getByRole('row', {name: /Retired sync/});

            expect(within(danglingRow).getByText('Needs attention')).toBeInTheDocument();
            expect(within(screen.getByRole('row', {name: /Weekly digest/})).getByText('Enabled')).toBeInTheDocument();
        });

        it('offers no working enable toggle on a dangling reference, leaving Delete as the only action', async () => {
            const user = userEvent.setup();

            renderView();

            const danglingRow = screen.getByRole('row', {name: /Retired sync/});

            // A dangling reference points at a withdrawn catalog workflow: enabling it fails
            // server-side with nothing on screen to explain it (spec §3 and Risks and notes).
            expect(within(danglingRow).getByRole('switch')).toBeDisabled();

            fireEvent.click(within(danglingRow).getByRole('switch'));

            expect(setEnabledMutateMock).not.toHaveBeenCalled();

            expect(within(danglingRow).getByText('Needs attention')).toBeInTheDocument();

            await user.click(within(danglingRow).getByRole('button', {name: 'Automation actions'}));

            expect(screen.getByRole('menuitem', {name: /delete/i})).toBeInTheDocument();
        });

        it('hides the section entirely when every automation matches a published template', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [copyAutomation, referenceAutomation],
                error: null,
                isLoading: false,
            });

            renderView();

            expect(screen.queryByRole('region', {name: 'Your automations'})).not.toBeInTheDocument();
        });

        it('lists every automation beyond the first matching one template, so none is hidden', async () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [copyAutomation, {...copyAutomation, label: 'Sync leads 2', workflowUuid: 'copy-uuid-2'}],
                error: null,
                isLoading: false,
            });

            renderView();

            const section = screen.getByRole('region', {name: 'Your automations'});

            // The header row plus exactly one data row: the first copy took the card, the second
            // fell through here rather than disappearing.
            expect(within(section).getAllByRole('row')).toHaveLength(2);
            expect(within(section).getByRole('row', {name: /Sync leads 2/})).toBeInTheDocument();

            await userEvent.setup().click(within(templateCard('Sync leads')).getByRole('switch'));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: false, workflowUuid: 'copy-uuid'});
        });

        it('keeps a dangling reference out of its template card even when that template is still published', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [{...referenceAutomation, dangling: true, label: 'Triage ticket'}],
                error: null,
                isLoading: false,
            });

            renderView();

            const section = screen.getByRole('region', {name: 'Your automations'});

            expect(within(section).getByText('Needs attention')).toBeInTheDocument();
            expect(
                within(templateCard('Triage a new support ticket')).getByRole('button', {name: 'Use template'})
            ).toBeInTheDocument();
        });

        it('offers "Open in builder" only on the COPY row menu', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(
                within(screen.getByRole('row', {name: /Weekly digest/})).getByRole('button', {
                    name: 'Automation actions',
                })
            );

            expect(screen.getByRole('menuitem', {name: /open in builder/i})).toBeInTheDocument();

            await user.keyboard('{Escape}');

            await user.click(
                within(screen.getByRole('row', {name: /Retired sync/})).getByRole('button', {
                    name: 'Automation actions',
                })
            );

            expect(screen.queryByRole('menuitem', {name: /open in builder/i})).not.toBeInTheDocument();
        });

        it('toggles a row enabled switch by calling the setEnabled mutation', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(within(screen.getByRole('row', {name: /Weekly digest/})).getByRole('switch'));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: false, workflowUuid: 'blank-uuid'});
        });

        it('deletes a COPY automation via useDeleteAutomationMutation after confirming', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(
                within(screen.getByRole('row', {name: /Weekly digest/})).getByRole('button', {
                    name: 'Automation actions',
                })
            );
            await user.click(screen.getByRole('menuitem', {name: /delete/i}));
            await user.click(screen.getByRole('button', {name: 'Delete'}));

            expect(deleteAutomationMutateMock).toHaveBeenCalledWith('blank-uuid');
            expect(deprovisionMutateMock).not.toHaveBeenCalled();
        });

        it('deprovisions a REFERENCE automation by its catalog workflow uuid after confirming', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(
                within(screen.getByRole('row', {name: /Retired sync/})).getByRole('button', {
                    name: 'Automation actions',
                })
            );
            await user.click(screen.getByRole('menuitem', {name: /delete/i}));
            await user.click(screen.getByRole('button', {name: 'Delete'}));

            expect(deprovisionMutateMock).toHaveBeenCalledWith('withdrawn-uuid');
            expect(deleteAutomationMutateMock).not.toHaveBeenCalled();
        });
    });

    describe('new automation', () => {
        it('hides the "New automation" button when the newWorkflow tab is disabled, keeping the page heading', () => {
            useAutomationHubStore.setState({tabs: {automations: true, connections: true, newWorkflow: false}});

            renderView();

            expect(screen.queryByRole('button', {name: 'New automation'})).not.toBeInTheDocument();
            expect(screen.getByRole('heading', {level: 1, name: 'Automations'})).toBeInTheDocument();
        });

        it('creates a blank automation and navigates to its builder on success', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(screen.getByRole('button', {name: 'New automation'}));

            expect(createBlankMutateMock).toHaveBeenCalled();
            expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/new-workflow-uuid');
        });

        it('still offers the "New automation" button when the user has no automations at all', () => {
            useGetAutomationsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

            renderView();

            expect(screen.getByRole('button', {name: 'New automation'})).toBeInTheDocument();
            expect(screen.queryByRole('region', {name: 'Your automations'})).not.toBeInTheDocument();
        });
    });
});
