import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubResourcePanel from '@/ee/pages/automation/ai-hub/AiHubResourcePanel';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/AiHubWorkflowViewer', () => ({
    default: ({projectWorkflowId}: {projectWorkflowId: number}) => (
        <div data-testid="workflow-viewer">{`workflow:${projectWorkflowId}`}</div>
    ),
}));

vi.mock('@/ee/pages/automation/ai-hub/AiHubFileViewer', () => ({
    default: ({fileId, viewMode}: {fileId: string; viewMode: string}) => (
        <div data-testid="file-viewer">{`file:${fileId}:${viewMode}`}</div>
    ),
}));

vi.mock('@/ee/pages/automation/ai-hub/AiHubFilePicker', () => ({
    default: () => <button type="button">Open file</button>,
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

const fileTab = (fileId: string, name: string, viewMode: 'editor' | 'preview' | 'split') => ({
    fileId,
    id: `file-tab-${fileId}`,
    kind: 'file' as const,
    name,
    viewMode,
});

const renderPanel = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AiHubResourcePanel />
            </TooltipProvider>
        </MemoryRouter>
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
        expect(
            screen.getByText('Workflows, AI Agents and files you or the assistant open appear here.')
        ).toBeInTheDocument();
    });

    it('renders a file tab with its view mode, Asset Files link and download link', () => {
        aiHubTabsStore.setState({activeTabId: 'file-tab-7', openTabs: [fileTab('7', 'notes.md', 'preview')]});

        renderPanel();

        expect(screen.getByTestId('file-viewer')).toHaveTextContent('file:7:preview');
        expect(screen.getByRole('link', {name: 'Open in Asset Files'})).toHaveAttribute(
            'href',
            '/automation/asset-files/7'
        );
        expect(screen.getByRole('link', {name: 'Download file'})).toHaveAttribute(
            'href',
            '/api/automation/internal/asset-files/7/content'
        );
    });

    it('switches a text file tab between view modes', () => {
        aiHubTabsStore.setState({activeTabId: 'file-tab-7', openTabs: [fileTab('7', 'notes.md', 'preview')]});

        renderPanel();

        fireEvent.click(screen.getByRole('radio', {name: 'Split'}));

        const [tab] = aiHubTabsStore.getState().openTabs;

        expect(tab).toMatchObject({kind: 'file', viewMode: 'split'});
    });

    it('offers only Preview for a binary file tab', () => {
        aiHubTabsStore.setState({activeTabId: 'file-tab-8', openTabs: [fileTab('8', 'report.pdf', 'preview')]});

        renderPanel();

        expect(screen.getByRole('radio', {name: 'Preview'})).toBeInTheDocument();
        expect(screen.queryByRole('radio', {name: 'Editor'})).not.toBeInTheDocument();
        expect(screen.queryByRole('radio', {name: 'Split'})).not.toBeInTheDocument();
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
