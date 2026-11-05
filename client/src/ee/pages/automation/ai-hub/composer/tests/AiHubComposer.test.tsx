import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubComposer from '../AiHubComposer';

import type {ResourcePickerSelectionI} from '@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu';

vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: ({onSelect, trigger}: {onSelect: (selection: ResourcePickerSelectionI) => void; trigger: ReactNode}) => (
        <div data-testid="resource-picker-menu">
            {trigger}

            <button onClick={() => onSelect({id: '42', kind: 'aiAgent', name: 'Support Agent'})} type="button">
                Pick agent
            </button>
        </div>
    ),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn(() => 1),
}));

const renderComposer = () =>
    render(
        <TooltipProvider>
            <AiHubComposer />
        </TooltipProvider>
    );

describe('AiHubComposer', () => {
    beforeEach(() => {
        aiHubComposerStore.setState({referencedResources: [], resourcePickerOpen: false});
        aiHubTabsStore.setState({activeTabId: undefined, openTabs: [], rightPanelOpen: false});
    });

    it('opens the agent tab and adds an AI Agent reference when an agent is picked', () => {
        renderComposer();

        fireEvent.click(screen.getByText('Pick agent'));

        const {activeTabId, openTabs} = aiHubTabsStore.getState();

        expect(openTabs).toHaveLength(1);
        expect(openTabs[0]).toMatchObject({aiAgentId: '42', kind: 'aiAgent', name: 'Support Agent'});
        expect(aiHubComposerStore.getState().referencedResources).toEqual([
            {id: '42', kind: 'aiAgent', name: 'Support Agent', ownsTab: true, tabId: activeTabId},
        ]);
    });

    it('renders the "Add resources" trigger button', () => {
        renderComposer();

        expect(screen.getByRole('button', {name: /add resources/i})).toBeInTheDocument();
    });

    it('shows no count badge when there are no referenced resources', () => {
        renderComposer();

        expect(screen.getByRole('button', {name: /add resources/i})).toHaveTextContent('');
    });

    it('shows the reference count on the trigger button when references are added', () => {
        aiHubComposerStore.setState({
            referencedResources: [
                {id: 'wf-1', kind: 'workflow', name: 'Lead Sync'},
                {id: 'wf-2', kind: 'workflow', name: 'Invoice Sync'},
            ],
        });

        renderComposer();

        expect(screen.getByRole('button', {name: /add resources/i})).toHaveTextContent('2');
    });
});
