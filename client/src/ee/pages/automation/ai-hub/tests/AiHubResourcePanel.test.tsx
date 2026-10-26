import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import AiHubResourcePanel from '@/ee/pages/automation/ai-hub/AiHubResourcePanel';
import {type ResourcePickerSelectionI} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/AiHubWorkflowViewer', () => ({
    default: ({projectWorkflowId}: {projectWorkflowId: number}) => (
        <div data-testid="workflow-viewer">{`workflow:${projectWorkflowId}`}</div>
    ),
}));

const hoisted = vi.hoisted(() => ({
    pickerProps: {
        current: null as {onSelect: (selection: ResourcePickerSelectionI) => void; workspaceId: number} | null,
    },
}));

vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: (props: {
        onSelect: (selection: ResourcePickerSelectionI) => void;
        trigger: ReactNode;
        workspaceId: number;
    }) => {
        hoisted.pickerProps.current = props;

        return <div data-testid="resource-picker-menu">{props.trigger}</div>;
    },
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 7})
    ),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetProjectQuery: () => ({data: undefined}),
}));

const workflowTab = (projectId: string, name: string, projectWorkflowId: number) => ({
    id: `tab-${projectId}`,
    kind: 'workflow' as const,
    name,
    projectId,
    projectWorkflowId,
    workflowId: `wf-${projectId}`,
});

const renderPanel = () =>
    render(
        <TooltipProvider>
            <AiHubResourcePanel />
        </TooltipProvider>
    );

describe('AiHubResourcePanel', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeChatId: undefined,
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: true,
            snapshotsByChatId: {},
        });
    });

    it('renders the active workflow tab body', () => {
        aiHubTabsStore.setState({
            activeTabId: 'tab-2',
            openTabs: [workflowTab('1', 'Invoices', 11), workflowTab('2', 'Leads', 22)],
        });

        renderPanel();

        expect(screen.getByTestId('workflow-viewer')).toHaveTextContent('workflow:22');
    });

    it('activates a tab from its label', () => {
        aiHubTabsStore.setState({
            activeTabId: 'tab-2',
            openTabs: [workflowTab('1', 'Invoices', 11), workflowTab('2', 'Leads', 22)],
        });

        renderPanel();

        fireEvent.click(screen.getByRole('button', {name: 'Invoices'}));

        expect(aiHubTabsStore.getState().activeTabId).toBe('tab-1');
        expect(screen.getByTestId('workflow-viewer')).toHaveTextContent('workflow:11');
    });

    it('closes a tab', () => {
        aiHubTabsStore.setState({activeTabId: 'tab-1', openTabs: [workflowTab('1', 'Invoices', 11)]});

        renderPanel();

        fireEvent.click(screen.getByRole('button', {name: 'Close Invoices'}));

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
        expect(screen.getByText('Workflows and AI Agents you or Basil open appear here.')).toBeInTheDocument();
    });

    it('hides the panel from its close control', () => {
        renderPanel();

        fireEvent.click(screen.getByRole('button', {name: 'Close resource panel'}));

        expect(aiHubTabsStore.getState().rightPanelOpen).toBe(false);
    });

    it('shows the plain right-panel icon on its close control', () => {
        renderPanel();

        const closeButton = screen.getByRole('button', {name: 'Close resource panel'});

        expect(closeButton.querySelector('svg.lucide-panel-right')).not.toBeNull();
        expect(closeButton.querySelector('svg.lucide-panel-right-close')).toBeNull();
    });

    it('opens a workflow picked from the resource picker as the active tab', () => {
        aiHubTabsStore.setState({activeTabId: 'tab-1', openTabs: [workflowTab('1', 'Invoices', 11)]});

        renderPanel();

        expect(screen.getByRole('button', {name: 'Open a resource'})).toBeInTheDocument();
        expect(hoisted.pickerProps.current?.workspaceId).toBe(7);

        act(() => {
            hoisted.pickerProps.current?.onSelect({
                id: 'wf-2',
                kind: 'workflow',
                name: 'Leads',
                projectId: '2',
                projectWorkflowId: 22,
            });
        });

        const {activeTabId, openTabs} = aiHubTabsStore.getState();

        expect(openTabs).toHaveLength(2);
        expect(openTabs.find((tab) => tab.id === activeTabId)).toMatchObject({
            name: 'Leads',
            projectId: '2',
            projectWorkflowId: 22,
            workflowId: 'wf-2',
        });
        expect(screen.getByTestId('workflow-viewer')).toHaveTextContent('workflow:22');
    });

    it('ignores a workflow selection without its project', () => {
        renderPanel();

        act(() => {
            hoisted.pickerProps.current?.onSelect({id: 'wf-2', kind: 'workflow', name: 'Leads'});
        });

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('shows the open resource control when no tab is open', () => {
        renderPanel();

        expect(screen.getByRole('button', {name: 'Open a resource'})).toBeInTheDocument();
    });

    it('scrolls the active tab into view so it is never left clipped', () => {
        const scrolledElements: Element[] = [];

        const scrollIntoViewSpy = vi.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(function (
            this: Element
        ) {
            scrolledElements.push(this);
        });

        aiHubTabsStore.setState({
            activeTabId: 'tab-2',
            openTabs: [workflowTab('1', 'Invoices', 11), workflowTab('2', 'Leads', 22)],
        });

        renderPanel();

        expect(scrollIntoViewSpy).toHaveBeenCalledWith({block: 'nearest', inline: 'nearest'});
        expect(scrolledElements).toHaveLength(1);
        expect(scrolledElements[0]!.textContent).toContain('Leads');

        scrollIntoViewSpy.mockRestore();
    });

    it('does not scroll when there is no active tab', () => {
        const scrollIntoViewSpy = vi.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(() => {});

        renderPanel();

        expect(scrollIntoViewSpy).not.toHaveBeenCalled();

        scrollIntoViewSpy.mockRestore();
    });
});
