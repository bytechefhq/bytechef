import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {type ResourcePickerSelectionI} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * Focused coverage for the composer's ResourcePickerMenu wiring. The old AiHubComposer.test.tsx suite is
 * `describe.skip`-ed pending a full layout rewrite — it predates the ResourcePickerMenu extraction. This
 * file does NOT attempt to revive that suite; it only exercises the post-extraction seam:
 *   - the composer's `onSelect` callback routing (non-workflow → handleSelect, workflow → handleSelectWorkflow).
 *
 * ResourcePickerMenu itself is mocked so the captured `onSelect` prop can be driven directly without
 * rendering the real nested popover.
 */

// vi.hoisted is the only place top-level refs can live and still be referenced inside vi.mock factories —
// vi.mock calls hoist above module-scope `const` declarations, so a plain const would not be initialised yet.
const {resourcePickerOnSelectRef} = vi.hoisted(() => ({
    resourcePickerOnSelectRef: {current: null as ((selection: ResourcePickerSelectionI) => void) | null},
}));

// Capture the props the composer passes to ResourcePickerMenu. Render only the trigger so the composer
// tree mounts cleanly without the real popover/command machinery.
vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: ({onSelect, trigger}: {onSelect: (selection: ResourcePickerSelectionI) => void; trigger: ReactNode}) => {
        resourcePickerOnSelectRef.current = onSelect;

        return <div data-testid="resource-picker-menu">{trigger}</div>;
    },
}));

// TaskToolDialog renders a heavy modal tree and is a sibling, not under test here — stub it out.
vi.mock('@/ee/pages/automation/ai-hub/tools/dialogs/TaskToolDialog', () => ({
    default: () => null,
}));

// The composer issues useAiHubTaskToolableComponentsQuery for the Tools branch catalog walk. Stub it so the
// component mounts without a live GraphQL fetch; the Tools catalog content itself is not under test here.
vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useAiHubTaskToolableComponentsQuery: vi.fn().mockReturnValue({data: undefined, isLoading: false}),
    };
});

// The workspace / environment stores are read via selectors; a constant return is enough for these tests.
vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 1),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn(() => 0),
}));

const renderComposer = async () => {
    const {default: AiHubComposer} = await import('../AiHubComposer');

    return render(
        <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
            <TooltipProvider>
                <AiHubComposer />
            </TooltipProvider>
        </QueryClientProvider>
    );
};

beforeEach(() => {
    aiHubComposerStore.setState({referencedResources: []});

    aiHubTabsStore.setState({
        activeTabId: undefined,
        activeTaskId: undefined,
        openTabs: [],
        rightPanelOpen: false,
        snapshotsByTaskId: {},
    });

    resourcePickerOnSelectRef.current = null;
});

describe('AiHubComposer ResourcePickerMenu wiring', () => {
    it('routes a non-workflow resource pick through handleSelect into the composer store', async () => {
        await renderComposer();

        expect(resourcePickerOnSelectRef.current).not.toBeNull();

        resourcePickerOnSelectRef.current!({id: 'file-7', kind: 'file', name: 'notes.md'});

        const {referencedResources} = aiHubComposerStore.getState();

        expect(referencedResources).toContainEqual({id: 'file-7', kind: 'file', name: 'notes.md'});

        // handleSelect opens a viewer tab for file-kind resources — confirms the plain (non-workflow) path ran.
        const {openTabs} = aiHubTabsStore.getState();

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]).toMatchObject({fileId: 'file-7', kind: 'file', name: 'notes.md'});
    });

    it('routes a workflow resource pick through handleSelectWorkflow', async () => {
        await renderComposer();

        expect(resourcePickerOnSelectRef.current).not.toBeNull();

        resourcePickerOnSelectRef.current!({
            id: 'wf-3',
            kind: 'workflow',
            name: 'Lead Sync',
            projectId: 'project-9',
            projectWorkflowId: 42,
        });

        const {referencedResources} = aiHubComposerStore.getState();

        expect(referencedResources).toContainEqual({id: 'wf-3', kind: 'workflow', name: 'Lead Sync'});

        // The workflow path differs from the plain path: it opens a workflow tab (carrying projectId /
        // projectWorkflowId), not a file/dataTable/knowledgeBase tab.
        const {openTabs} = aiHubTabsStore.getState();

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]).toMatchObject({
            kind: 'workflow',
            name: 'Lead Sync',
            projectId: 'project-9',
            projectWorkflowId: 42,
            workflowId: 'wf-3',
        });
    });

    it('opens a workflow-execution tab when the picker selects a workflowExecution', async () => {
        await renderComposer();

        expect(resourcePickerOnSelectRef.current).not.toBeNull();

        // ResourcePickerMenu stringifies execution.id at the call site, so the composer must coerce back
        // to a number when opening the tab — this test pins that round-trip.
        resourcePickerOnSelectRef.current!({id: '101', kind: 'workflowExecution', name: 'agent2'});

        const {referencedResources} = aiHubComposerStore.getState();

        expect(referencedResources).toContainEqual({id: '101', kind: 'workflowExecution', name: 'agent2'});

        const {openTabs} = aiHubTabsStore.getState();

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]).toMatchObject({kind: 'workflowExecution', name: 'agent2', workflowExecutionId: 101});
    });
});
