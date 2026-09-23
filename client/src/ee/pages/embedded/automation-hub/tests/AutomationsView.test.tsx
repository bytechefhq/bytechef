import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {
    AutomationWorkflowProject,
    ConnectedUserProjectWorkflow,
    ResponseError,
} from '@/ee/shared/middleware/embedded/public';
import {render, screen, waitFor, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationsView from '../views/AutomationsView';

const {
    createBlankMutateMock,
    deleteAutomationMutateMock,
    deprovisionMutateMock,
    navigateMock,
    setEnabledMutateMock,
    toastErrorMock,
    updateInputsMutateAsyncMock,
    useGetAutomationsQueryMock,
    useGetTemplateProjectsQueryMock,
} = vi.hoisted(() => ({
    createBlankMutateMock: vi.fn(),
    deleteAutomationMutateMock: vi.fn(),
    deprovisionMutateMock: vi.fn(),
    navigateMock: vi.fn(),
    setEnabledMutateMock: vi.fn(),
    toastErrorMock: vi.fn(),
    updateInputsMutateAsyncMock: vi.fn(),
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
    useDeleteAutomationMutation: () => ({mutateAsync: deleteAutomationMutateMock}),
    useDeprovisionReferenceMutation: () => ({mutateAsync: deprovisionMutateMock}),
    useSetAutomationEnabledMutation: () => ({mutateAsync: setEnabledMutateMock}),
    useUpdateAutomationInputsMutation: () => ({mutateAsync: updateInputsMutateAsyncMock}),
}));

vi.mock('sonner', () => ({
    toast: {error: toastErrorMock},
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <img alt="component icon" src={src} />,
}));

const salesProject: AutomationWorkflowProject = {
    description: 'Templates for the sales team',
    id: 1,
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
    workflowVersion: 3,
};

const referenceAutomation: ConnectedUserProjectWorkflow = {
    automationWorkflowUuid: 'wf-3',
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
    workflowVersion: 1,
};

const referenceAutomationNeedingAttention: ConnectedUserProjectWorkflow = {
    ...referenceAutomation,
    attentionReason: 'MISSING_CONNECTION:slack',
};

const danglingReferenceAutomation: ConnectedUserProjectWorkflow = {
    automationWorkflowUuid: 'withdrawn-uuid',
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
        toastErrorMock.mockReset();
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

        localStorage.clear();

        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            defaultLayout: 'grid',
            includeComponents: undefined,
            initialized: true,
            layoutSwitcherAllowed: true,
            tabs: {automations: true, connections: true, newWorkflow: true},
            theme: {},
        });

        deleteAutomationMutateMock.mockResolvedValue(undefined);
        deprovisionMutateMock.mockResolvedValue(undefined);

        createBlankMutateMock.mockImplementation(
            (_variables: undefined, options?: {onSuccess?: (workflowUuid: string) => void}) => {
                options?.onSuccess?.('new-workflow-uuid');
            }
        );
    });

    describe('template grid', () => {
        it('renders every published template in one flat grid, without project headings', () => {
            renderView();

            expect(screen.queryByRole('heading', {level: 2, name: 'Sales'})).not.toBeInTheDocument();
            expect(screen.queryByText('Templates for the sales team')).not.toBeInTheDocument();

            expect(screen.getByText('Sync new leads into the CRM')).toBeInTheDocument();
            expect(screen.getByText('Send new lead updates to Slack')).toBeInTheDocument();
            expect(screen.getByText('Triage a new support ticket')).toBeInTheDocument();
        });

        it('shows each template component as an icon on its card', () => {
            renderView();

            expect(within(templateCard('Slack sync')).getByTitle('Slack')).toBeInTheDocument();
            expect(within(templateCard('Sync leads')).queryByTitle('Slack')).not.toBeInTheDocument();
        });

        it('shows automations that match no published template as cards after the template cards', () => {
            renderView();

            const titles = screen.getAllByRole('heading', {level: 3}).map((heading) => heading.textContent);

            expect(titles).toEqual(['Sync leads', 'Slack sync', 'Triage ticket', 'Weekly digest', 'Retired sync']);
        });

        it('heads the automations that match no template with My Automations, after the templates', () => {
            renderView();

            const heading = screen.getByRole('heading', {level: 2, name: 'My Automations'});

            expect(heading.className).toContain('col-span-full');

            expect(heading.compareDocumentPosition(screen.getByText('Slack sync'))).toBe(
                Node.DOCUMENT_POSITION_PRECEDING
            );
            expect(heading.compareDocumentPosition(screen.getByText('Weekly digest'))).toBe(
                Node.DOCUMENT_POSITION_FOLLOWING
            );
        });

        it('drops the My Automations section entirely when New Automation is disabled', () => {
            useAutomationHubStore.setState({tabs: {automations: true, connections: true, newWorkflow: false}});

            renderView();

            expect(screen.queryByRole('heading', {name: 'My Automations'})).not.toBeInTheDocument();
            expect(screen.queryByText('Weekly digest')).not.toBeInTheDocument();
            expect(screen.queryByText('Retired sync')).not.toBeInTheDocument();

            expect(screen.getByText('Slack sync')).toBeInTheDocument();
        });

        it('omits the heading when every automation matches a published template', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [copyAutomation, referenceAutomation],
                error: null,
                isLoading: false,
            });

            renderView();

            expect(screen.queryByRole('heading', {name: 'My Automations'})).not.toBeInTheDocument();
        });

        it('shows the deployed version on an activated template and on a from-scratch card', () => {
            renderView();

            expect(within(templateCard('Sync leads')).getByText('V3')).toBeInTheDocument();
            expect(within(templateCard('Weekly digest')).getByText('V1')).toBeInTheDocument();
        });

        it('shows no version on an automation that has never been published', () => {
            renderView();

            expect(within(templateCard('Triage ticket')).queryByText(/^V\d+$/)).not.toBeInTheDocument();
            expect(within(templateCard('Retired sync')).queryByText(/^V\d+$/)).not.toBeInTheDocument();
        });

        it('shows no version on a template the user has not activated', () => {
            renderView();

            expect(within(templateCard('Slack sync')).queryByText(/^V\d+$/)).not.toBeInTheDocument();
        });

        it('will not let an unpublished copy be enabled from its card, and says why', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [{...blankAutomation, enabled: false, workflowVersion: undefined}],
                error: null,
                isLoading: false,
            });

            renderView();

            const statusButton = within(templateCard('Weekly digest')).getByRole('button', {
                name: 'Enable Weekly digest',
            });

            expect(statusButton).toBeDisabled();
            expect(statusButton).toHaveAttribute('title', expect.stringMatching(/publish/i));
        });

        it('still enables a REFERENCE that carries no version, which is never published', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(within(templateCard('Triage ticket')).getByRole('button', {name: 'Enable Triage ticket'}));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: true, workflowUuid: 'ref-uuid'});
        });

        it('names the status control after the action it performs, in the colour of that action', () => {
            renderView();

            const disable = within(templateCard('Weekly digest')).getByRole('button', {name: 'Disable Weekly digest'});

            expect(disable).toHaveTextContent('Disable');

            expect(disable.className).toContain('bg-(--hub-disable)');

            const enable = within(templateCard('Triage ticket')).getByRole('button', {name: 'Enable Triage ticket'});

            expect(enable).toHaveTextContent('Enable');
            expect(enable.className).toContain('bg-(--hub-enable)');
        });

        it('offers a toggle, Customize and Remove on a from-scratch COPY automation card', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Weekly digest');

            await user.click(within(card).getByRole('button', {name: 'Disable Weekly digest'}));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: false, workflowUuid: 'blank-uuid'});

            await user.click(within(card).getByRole('button', {name: 'Weekly digest actions'}));
            await user.click(screen.getByRole('menuitem', {name: /customize/i}));

            expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/blank-uuid');

            await user.click(within(card).getByRole('button', {name: 'Weekly digest actions'}));
            await user.click(screen.getByRole('menuitem', {name: /remove/i}));
            await user.click(screen.getByRole('button', {name: 'Remove'}));

            expect(deleteAutomationMutateMock).toHaveBeenCalledWith('blank-uuid');
        });

        it('marks a dangling reference as needing attention, disables its toggle and offers only Remove', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Retired sync');

            expect(within(card).getByText('Needs attention')).toBeInTheDocument();
            expect(within(card).getByRole('button', {name: 'Disable Retired sync'})).toBeDisabled();

            await user.click(within(card).getByRole('button', {name: 'Retired sync actions'}));

            expect(screen.queryByRole('menuitem', {name: /customize/i})).not.toBeInTheDocument();

            await user.click(screen.getByRole('menuitem', {name: /remove/i}));
            await user.click(screen.getByRole('button', {name: 'Remove'}));

            expect(deprovisionMutateMock).toHaveBeenCalledWith('withdrawn-uuid');
            expect(deleteAutomationMutateMock).not.toHaveBeenCalled();
        });

        it('shows a derived attention reason on a reference and keeps its toggle enabled', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [
                    copyAutomation,
                    referenceAutomationNeedingAttention,
                    blankAutomation,
                    danglingReferenceAutomation,
                ],
                error: null,
                isLoading: false,
            });

            renderView();

            const card = templateCard('Triage ticket');

            expect(within(card).getByText('Needs attention')).toBeInTheDocument();
            expect(within(card).getByText('Connect slack to keep this running')).toBeInTheDocument();
            expect(within(card).getByRole('button', {name: 'Enable Triage ticket'})).not.toBeDisabled();
        });

        it('narrows the grid to activated and from-scratch automations behind the Active chip', async () => {
            const user = userEvent.setup();

            renderView();

            expect(screen.getByRole('radio', {name: 'All'})).toBeChecked();

            await user.click(screen.getByRole('radio', {name: 'Active'}));

            expect(screen.getByRole('radio', {name: 'Active'})).toBeChecked();
            expect(screen.getByRole('radio', {name: 'All'})).not.toBeChecked();

            expect(screen.queryByText('Slack sync')).not.toBeInTheDocument();
            expect(screen.getByText('Sync leads')).toBeInTheDocument();
            expect(screen.getByText('Triage ticket')).toBeInTheDocument();
            expect(screen.getByText('Weekly digest')).toBeInTheDocument();
            expect(screen.getByText('Retired sync')).toBeInTheDocument();

            await user.click(screen.getByRole('radio', {name: 'All'}));

            expect(screen.getByText('Slack sync')).toBeInTheDocument();
        });

        it('narrows again to only what is running behind the Enabled chip', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(screen.getByRole('radio', {name: 'Enabled'}));

            expect(screen.getByText('Sync leads')).toBeInTheDocument();
            expect(screen.getByText('Retired sync')).toBeInTheDocument();
            expect(screen.getByText('Weekly digest')).toBeInTheDocument();

            expect(screen.queryByText('Triage ticket')).not.toBeInTheDocument();
            expect(screen.queryByText('Slack sync')).not.toBeInTheDocument();
        });

        it('switches between the grid and list layouts and remembers the choice', async () => {
            const user = userEvent.setup();

            renderView();

            expect(screen.getByTestId('automations-catalog')).toHaveAttribute('data-layout', 'grid');

            await user.click(screen.getByRole('radio', {name: 'List view'}));

            expect(screen.getByTestId('automations-catalog')).toHaveAttribute('data-layout', 'list');
            expect(localStorage.getItem('automationHub.catalogLayout')).toBe('list');

            await user.click(screen.getByRole('radio', {name: 'Grid view'}));

            expect(screen.getByTestId('automations-catalog')).toHaveAttribute('data-layout', 'grid');
        });

        it("starts on the vendor's default layout when the viewer has made no choice of their own", () => {
            useAutomationHubStore.setState({defaultLayout: 'list'});

            renderView();

            expect(screen.getByTestId('automations-catalog')).toHaveAttribute('data-layout', 'list');
        });

        it("keeps the viewer's stored layout over the vendor default while the switcher is offered", () => {
            localStorage.setItem('automationHub.catalogLayout', 'grid');

            useAutomationHubStore.setState({defaultLayout: 'list'});

            renderView();

            expect(screen.getByTestId('automations-catalog')).toHaveAttribute('data-layout', 'grid');
        });

        it("hides the switcher and pins the vendor's layout when the switcher is not allowed", () => {
            localStorage.setItem('automationHub.catalogLayout', 'grid');

            useAutomationHubStore.setState({defaultLayout: 'list', layoutSwitcherAllowed: false});

            renderView();

            expect(screen.queryByRole('radio', {name: 'Grid view'})).not.toBeInTheDocument();
            expect(screen.queryByRole('radio', {name: 'List view'})).not.toBeInTheDocument();
            expect(screen.getByTestId('automations-catalog')).toHaveAttribute('data-layout', 'list');
        });

        it('shows an empty state when the user has neither templates nor automations', () => {
            useGetTemplateProjectsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});
            useGetAutomationsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

            renderView();

            expect(screen.getByText(/no automations found/i)).toBeInTheDocument();
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

        it('offers "Use" on an unused template and calls onActivate with the template', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Slack sync');

            await user.click(within(card).getByRole('button', {name: 'Use'}));

            expect(onActivate).toHaveBeenCalledTimes(1);
            expect(onActivate).toHaveBeenCalledWith(salesProject.workflowTemplates![1]);
        });

        it('offers a toggle and a Customize menu item on an activated COPY template', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Sync leads');

            expect(within(card).queryByRole('button', {name: 'Use'})).not.toBeInTheDocument();

            await user.click(within(card).getByRole('button', {name: 'Disable Sync leads'}));

            expect(setEnabledMutateMock).toHaveBeenCalledWith({enabled: false, workflowUuid: 'copy-uuid'});

            await user.click(within(card).getByRole('button', {name: 'Sync leads actions'}));
            await user.click(screen.getByRole('menuitem', {name: /customize/i}));

            expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/copy-uuid');
        });

        it('offers a toggle but no Customize on an activated REFERENCE template', async () => {
            const user = userEvent.setup();

            renderView();

            const card = templateCard('Triage ticket');

            expect(within(card).queryByRole('button', {name: 'Use'})).not.toBeInTheDocument();

            await user.click(within(card).getByRole('button', {name: 'Enable Triage ticket'}));

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
            await user.click(screen.getByRole('button', {name: 'Remove'}));

            expect(deleteAutomationMutateMock).toHaveBeenCalledWith('copy-uuid');
            expect(deprovisionMutateMock).not.toHaveBeenCalled();
        });

        it('removes an activated REFERENCE template by its automation workflow uuid after confirming', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(
                within(templateCard('Triage ticket')).getByRole('button', {name: 'Triage ticket actions'})
            );
            await user.click(screen.getByRole('menuitem', {name: /remove/i}));
            await user.click(screen.getByRole('button', {name: 'Remove'}));

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

            expect(within(templateCard('Sync leads')).getByRole('button', {name: 'Use'})).toBeDisabled();
        });
    });

    describe('removal', () => {
        it('keeps the confirmation open with a busy Remove until the removal settles', async () => {
            const user = userEvent.setup();

            let finishRemoval: () => void = () => undefined;

            deleteAutomationMutateMock.mockReturnValue(
                new Promise<void>((resolve) => {
                    finishRemoval = resolve;
                })
            );

            renderView();

            await user.click(
                within(templateCard('Weekly digest')).getByRole('button', {name: 'Weekly digest actions'})
            );
            await user.click(screen.getByRole('menuitem', {name: /remove/i}));

            const removeButton = screen.getByRole('button', {name: 'Remove'});

            await user.click(removeButton);

            expect(deleteAutomationMutateMock).toHaveBeenCalledWith('blank-uuid');

            expect(removeButton).toBeInTheDocument();
            expect(removeButton).toBeDisabled();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeDisabled();

            finishRemoval();

            await waitFor(() => expect(screen.queryByRole('button', {name: 'Remove'})).not.toBeInTheDocument());
        });
    });

    describe('new automation', () => {
        it('hides the "New Automation" button when the newWorkflow tab is disabled, keeping the view toggle', () => {
            useAutomationHubStore.setState({tabs: {automations: true, connections: true, newWorkflow: false}});

            renderView();

            expect(screen.queryByRole('button', {name: 'New Automation'})).not.toBeInTheDocument();
            expect(screen.getByPlaceholderText(/search templates/i)).toBeInTheDocument();
            expect(screen.getByRole('radio', {name: 'Grid view'})).toBeInTheDocument();
        });

        it('creates a blank automation and navigates to its builder on success', async () => {
            const user = userEvent.setup();

            renderView();

            await user.click(screen.getByRole('button', {name: 'New Automation'}));

            expect(createBlankMutateMock).toHaveBeenCalled();
            expect(navigateMock).toHaveBeenCalledWith('/embedded/hub/builder/new-workflow-uuid');
        });

        it('still offers the "New Automation" button when the user has no automations at all', () => {
            useGetAutomationsQueryMock.mockReturnValue({data: [], error: null, isLoading: false});

            renderView();

            expect(screen.getByRole('button', {name: 'New Automation'})).toBeInTheDocument();
        });
    });

    describe('card text', () => {
        it('shows the description the viewer last saved on their own automation', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [{...copyAutomation, description: 'Copies new leads into Airtable'}],
                isLoading: false,
            });

            renderView();

            expect(screen.getByText('Copies new leads into Airtable')).toBeInTheDocument();
        });

        it('prefers the activated automation label over the automation workflow template it came from', () => {
            useGetAutomationsQueryMock.mockReturnValue({
                data: [{...copyAutomation, description: 'My own notes', label: 'My renamed automation'}],
                isLoading: false,
            });

            renderView();

            expect(screen.getByText('My renamed automation')).toBeInTheDocument();
            expect(screen.getByText('My own notes')).toBeInTheDocument();
        });
    });

    describe('automation settings', () => {
        const automationWithInputs: ConnectedUserProjectWorkflow = {
            ...copyAutomation,
            inputValues: {sheetName: 'Leads'},
            inputs: [{label: 'Spreadsheet name', name: 'sheetName', required: true, type: 'STRING'}],
        };

        it('offers Settings and saves a changed value through the same endpoint the wizard uses', async () => {
            const user = userEvent.setup();

            useGetAutomationsQueryMock.mockReturnValue({data: [automationWithInputs], isLoading: false});
            updateInputsMutateAsyncMock.mockResolvedValue(undefined);

            renderView();

            await user.click(screen.getByRole('button', {name: 'Sync leads actions'}));
            await user.click(screen.getByRole('menuitem', {name: /settings/i}));

            const field = screen.getByLabelText(/Spreadsheet name/);

            expect(field).toHaveValue('Leads');

            await user.clear(field);
            await user.type(field, 'Prospects');
            await user.click(screen.getByRole('button', {name: 'Save'}));

            await waitFor(() =>
                expect(updateInputsMutateAsyncMock).toHaveBeenCalledWith({
                    inputs: {sheetName: 'Prospects'},
                    workflowUuid: 'copy-uuid',
                })
            );
        });

        it('withholds Settings from an automation whose workflow declares no inputs', async () => {
            const user = userEvent.setup();

            useGetAutomationsQueryMock.mockReturnValue({data: [copyAutomation], isLoading: false});

            renderView();

            await user.click(screen.getByRole('button', {name: 'Sync leads actions'}));

            expect(screen.queryByRole('menuitem', {name: /settings/i})).not.toBeInTheDocument();
            expect(screen.getByRole('menuitem', {name: /remove/i})).toBeInTheDocument();
        });

        it('refuses to save while a required value is blank', async () => {
            const user = userEvent.setup();

            useGetAutomationsQueryMock.mockReturnValue({data: [automationWithInputs], isLoading: false});

            renderView();

            await user.click(screen.getByRole('button', {name: 'Sync leads actions'}));
            await user.click(screen.getByRole('menuitem', {name: /settings/i}));
            await user.clear(screen.getByLabelText(/Spreadsheet name/));

            expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();
        });
    });

    describe('enable and remove failures', () => {
        const conflictError = (body: object) =>
            new ResponseError(new Response(JSON.stringify(body), {status: 409}), 'Response returned an error code');

        it('explains a missing connection when the server refuses to enable an automation', async () => {
            const user = userEvent.setup();

            setEnabledMutateMock.mockRejectedValueOnce(conflictError({missingConnectionComponentName: 'gmail'}));

            useGetAutomationsQueryMock.mockReturnValue({
                data: [{...blankAutomation, enabled: false}],
                isLoading: false,
            });

            renderView();

            await user.click(screen.getByRole('button', {name: 'Enable Weekly digest'}));

            await waitFor(() =>
                expect(toastErrorMock).toHaveBeenCalledWith('Could not enable "Weekly digest"', {
                    description: 'Connect a Gmail account first, then enable it again.',
                })
            );
        });

        it('opens the settings dialog when the server reports a required input without a value', async () => {
            const user = userEvent.setup();

            setEnabledMutateMock.mockRejectedValueOnce(conflictError({missingInputName: 'sheetName'}));

            useGetAutomationsQueryMock.mockReturnValue({
                data: [
                    {
                        ...copyAutomation,
                        enabled: false,
                        inputs: [{label: 'Spreadsheet name', name: 'sheetName', required: true, type: 'STRING'}],
                    },
                ],
                isLoading: false,
            });

            renderView();

            await user.click(screen.getByRole('button', {name: 'Enable Sync leads'}));

            expect(await screen.findByText('Sync leads settings')).toBeInTheDocument();
            expect(toastErrorMock).toHaveBeenCalledWith('Could not enable "Sync leads"', {
                description: 'The "sheetName" setting needs a value first.',
            });
        });

        it('leaves a non-conflict failure to the global error toast', async () => {
            const user = userEvent.setup();

            setEnabledMutateMock.mockRejectedValueOnce(
                new ResponseError(new Response('{}', {status: 500}), 'Response returned an error code')
            );

            useGetAutomationsQueryMock.mockReturnValue({
                data: [{...blankAutomation, enabled: false}],
                isLoading: false,
            });

            renderView();

            await user.click(screen.getByRole('button', {name: 'Enable Weekly digest'}));

            await waitFor(() => expect(setEnabledMutateMock).toHaveBeenCalled());

            expect(toastErrorMock).not.toHaveBeenCalled();
        });

        it('reports a failed removal and keeps the confirmation open', async () => {
            const user = userEvent.setup();

            deleteAutomationMutateMock.mockRejectedValueOnce(new Error('Network down'));

            renderView();

            const card = templateCard('Weekly digest');

            await user.click(within(card).getByRole('button', {name: 'Weekly digest actions'}));
            await user.click(screen.getByRole('menuitem', {name: /remove/i}));
            await user.click(screen.getByRole('button', {name: 'Remove'}));

            await waitFor(() =>
                expect(toastErrorMock).toHaveBeenCalledWith('Could not remove "Weekly digest"', {
                    description: 'Network down',
                })
            );

            expect(screen.getByRole('button', {name: 'Remove'})).not.toBeDisabled();
        });
    });
});
