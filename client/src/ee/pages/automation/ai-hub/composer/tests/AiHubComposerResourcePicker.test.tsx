import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {type ResourcePickerSelectionI} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {act, render, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubComposer from '../AiHubComposer';

const {resourcePickerOnSelectRef, resourcePickerOpenRef, resourcePickerWorkspaceIdRef} = vi.hoisted(() => ({
    resourcePickerOnSelectRef: {current: null as ((selection: ResourcePickerSelectionI) => void) | null},
    resourcePickerOpenRef: {current: undefined as boolean | undefined},
    resourcePickerWorkspaceIdRef: {current: undefined as number | undefined},
}));

vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: ({
        onSelect,
        open,
        trigger,
        workspaceId,
    }: {
        onSelect: (selection: ResourcePickerSelectionI) => void;
        open?: boolean;
        trigger: ReactNode;
        workspaceId: number;
    }) => {
        resourcePickerOnSelectRef.current = onSelect;
        resourcePickerOpenRef.current = open;
        resourcePickerWorkspaceIdRef.current = workspaceId;

        return <div data-testid="resource-picker-menu">{trigger}</div>;
    },
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 5),
}));

const LEAD_SYNC: ResourcePickerSelectionI = {
    id: 'wf-3',
    kind: 'workflow',
    name: 'Lead Sync',
    projectId: 'project-9',
    projectWorkflowId: 42,
};

const renderComposer = () =>
    render(
        <TooltipProvider>
            <AiHubComposer />
        </TooltipProvider>
    );

const select = (selection: ResourcePickerSelectionI) => {
    act(() => {
        resourcePickerOnSelectRef.current!(selection);
    });
};

beforeEach(() => {
    aiHubComposerStore.setState({referencedResources: [], resourcePickerOpen: false});
    aiHubTabsStore.getState().reset();

    resourcePickerOnSelectRef.current = null;
    resourcePickerOpenRef.current = undefined;
    resourcePickerWorkspaceIdRef.current = undefined;
});

describe('AiHubComposer ResourcePickerMenu wiring', () => {
    it('passes the current workspace to the picker', () => {
        renderComposer();

        expect(resourcePickerWorkspaceIdRef.current).toBe(5);
    });

    it('adds a workflow pick as a reference and opens it as a workflow tab', () => {
        renderComposer();

        select(LEAD_SYNC);

        expect(aiHubComposerStore.getState().referencedResources).toContainEqual(
            expect.objectContaining({id: 'wf-3', kind: 'workflow', name: 'Lead Sync'})
        );

        const {openTabs, rightPanelOpen} = aiHubTabsStore.getState();

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]).toMatchObject({
            kind: 'workflow',
            name: 'Lead Sync',
            projectId: 'project-9',
            projectWorkflowId: 42,
            workflowId: 'wf-3',
        });
        expect(rightPanelOpen).toBe(true);
    });

    it('records the id of the tab it opened on the reference', () => {
        renderComposer();

        select(LEAD_SYNC);

        const {openTabs} = aiHubTabsStore.getState();
        const [reference] = aiHubComposerStore.getState().referencedResources;

        expect(reference!.tabId).toBe(openTabs[0]!.id);
        expect(reference!.ownsTab).toBe(true);
    });

    it('does not claim ownership of a tab that was already open before the attach', () => {
        aiHubTabsStore.getState().openWorkflowTab({
            name: 'Lead Sync',
            projectId: 'project-9',
            projectWorkflowId: 42,
            workflowId: 'wf-3',
        });

        renderComposer();

        select(LEAD_SYNC);

        const [reference] = aiHubComposerStore.getState().referencedResources;

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(1);
        expect(reference!.ownsTab).toBe(false);
        expect(reference!.tabId).toBe(aiHubTabsStore.getState().openTabs[0]!.id);
    });

    it('replaces the chip of a workflow whose project tab is re-pointed, keeping tab ownership', () => {
        renderComposer();

        select(LEAD_SYNC);
        select({...LEAD_SYNC, id: 'wf-4', name: 'Lead Cleanup', projectWorkflowId: 43});

        const {openTabs} = aiHubTabsStore.getState();
        const {referencedResources} = aiHubComposerStore.getState();

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]).toMatchObject({workflowId: 'wf-4'});
        expect(referencedResources).toHaveLength(1);
        expect(referencedResources[0]).toMatchObject({id: 'wf-4', ownsTab: true, tabId: openTabs[0]!.id});
    });

    it('ignores a selection without workflow coordinates', () => {
        renderComposer();

        select({id: 'wf-5', kind: 'workflow', name: 'Broken'});

        expect(aiHubComposerStore.getState().referencedResources).toHaveLength(0);
        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('passes the store-held picker open flag down to ResourcePickerMenu', async () => {
        renderComposer();

        expect(resourcePickerOpenRef.current).toBe(false);

        act(() => {
            aiHubComposerStore.getState().setResourcePickerOpen(true);
        });

        await waitFor(() => {
            expect(resourcePickerOpenRef.current).toBe(true);
        });
    });
});
