import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubResourcePanel from '@/ee/pages/automation/ai-hub/AiHubResourcePanel';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/AiHubWorkflowViewer', () => ({
    default: ({projectWorkflowId}: {projectWorkflowId: number}) => (
        <div data-testid="workflow-viewer">{`workflow:${projectWorkflowId}`}</div>
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
        expect(screen.getByText('Workflows you or the assistant open appear here.')).toBeInTheDocument();
    });

    it('hides the panel from its close control', () => {
        renderPanel();

        fireEvent.click(screen.getByRole('button', {name: 'Close resource panel'}));

        expect(aiHubTabsStore.getState().rightPanelOpen).toBe(false);
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
