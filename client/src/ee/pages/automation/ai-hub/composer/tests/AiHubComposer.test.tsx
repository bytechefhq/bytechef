import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiHubComposer from '../AiHubComposer';

vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: ({trigger}: {trigger: ReactNode}) => <div data-testid="resource-picker-menu">{trigger}</div>,
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
