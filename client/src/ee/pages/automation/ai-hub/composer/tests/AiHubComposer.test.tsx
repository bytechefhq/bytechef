import {TooltipProvider} from '@/components/ui/tooltip';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * AiHubComposer post-extraction suite.
 *
 * The composer was split: the references/chips and the attach-paperclip moved to AiHubChatComposer, and
 * the nested resource-picker dropdown was extracted into the shared ResourcePickerMenu. What remains in
 * AiHubComposer is the "+" trigger button — which shows a count badge once references exist — plus the
 * onSelect routing and the Tools branch.
 *
 * This file covers only what AiHubComposer still owns: the trigger button and its reference-count badge.
 * The behaviours that moved out are covered by their new homes and are intentionally NOT re-tested here:
 *   - resource-picker drill-down, search, "Show more" pagination, workflow aggregation, the 8 root
 *     entries → ResourcePickerMenu.test.tsx
 *   - onSelect → store / tab routing and the toolsBranch wiring → AiHubComposerResourcePicker.test.tsx
 *   - file upload / retry / dismiss / error handling → useAiHubAttachmentUpload.test.ts
 *   - drag-and-drop file handling → AiHubComposerDropZone.test.tsx
 */

// ResourcePickerMenu renders a heavy nested popover/command tree. AiHubComposer's own behaviour under test
// here is just the trigger button it passes in, so render only that trigger.
vi.mock('@/ee/pages/automation/ai-hub/resource-picker/ResourcePickerMenu', () => ({
    default: ({trigger}: {trigger: ReactNode}) => <div data-testid="resource-picker-menu">{trigger}</div>,
}));

// TaskToolDialog is a sibling heavy modal, not under test here — stub it out.
vi.mock('@/ee/pages/automation/ai-hub/tools/dialogs/TaskToolDialog', () => ({
    default: () => null,
}));

// The composer issues useAiHubTaskToolableComponentsQuery for the Tools branch catalog walk. Stub it so the
// component mounts without a live GraphQL fetch.
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
        <TooltipProvider>
            <AiHubComposer />
        </TooltipProvider>
    );
};

describe('AiHubComposer', () => {
    beforeEach(() => {
        aiHubComposerStore.setState({referencedResources: []});
    });

    it('renders the "Add resources" trigger button', async () => {
        await renderComposer();

        expect(screen.getByRole('button', {name: /add resources/i})).toBeInTheDocument();
    });

    it('shows no count badge when there are no referenced resources', async () => {
        await renderComposer();

        const button = screen.getByRole('button', {name: /add resources/i});

        // With an empty store the button renders only the "+" icon — no numeric badge text.
        expect(button).toHaveTextContent('');
    });

    it('shows the reference count on the trigger button when references are added', async () => {
        aiHubComposerStore.setState({
            referencedResources: [
                {id: 'file-1', kind: 'file', name: 'readme.md'},
                {id: 'wf-1', kind: 'workflow', name: 'Lead Sync'},
            ],
        });

        await renderComposer();

        const button = screen.getByRole('button', {name: /add resources/i});

        expect(button).toHaveTextContent('2');
    });
});
