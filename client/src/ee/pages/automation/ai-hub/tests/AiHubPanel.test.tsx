import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubPanel from '@/ee/pages/automation/ai-hub/AiHubPanel';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubThread', () => ({
    default: () => <div data-testid="cc-thread" />,
}));

vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubChatComposer', () => ({
    default: () => <div data-testid="cc-composer" />,
}));

vi.mock('@/ee/pages/automation/ai-hub/tasks/hooks/useTasks', () => ({
    useAiHubTasksQuery: () => ({data: [], isLoading: false}),
}));

// EnvironmentSelect now lives in the panel header (next to Ask/Build) and pulls useEnvironmentsQuery via
// react-query. Stubbing it here keeps the panel test focused on its own surface and avoids dragging a
// QueryClientProvider into the wrapper for a control unrelated to the assertions below.
vi.mock('@/shared/components/EnvironmentSelect', () => ({
    default: () => <div data-testid="env-select" />,
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({
        defaultOptions: {queries: {retry: false}},
    });

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <TooltipProvider>{ui}</TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

describe('AiHubPanel', () => {
    it('renders the task title (New Task fallback) and the thread + composer', () => {
        wrap(<AiHubPanel />);

        // Header now shows the task's title (or "New Task" when no title yet) — the
        // legacy header text and the bot icon were removed when the runtime provider was hoisted to
        // AiHubContent. The Ask/Build control moved out of the header into the composer (ModeSwitch),
        // which is mocked here, so it's asserted in the composer's own tests rather than this one.
        expect(screen.getByText('New Task')).toBeInTheDocument();
        expect(screen.getByTestId('cc-thread')).toBeInTheDocument();
        expect(screen.getByTestId('cc-composer')).toBeInTheDocument();
    });

    it('does not render the global SubagentProgressLine — subagent progress is rendered per tool-call card', () => {
        const {container} = wrap(<AiHubPanel />);

        expect(container.querySelector('.italic')).toBeNull();
    });

    it('fills its container (no hardcoded w-[450px])', () => {
        const {container} = wrap(<AiHubPanel />);

        expect(container.querySelector('.w-\\[450px\\]')).toBeNull();
        expect(container.querySelector('.size-full')).not.toBeNull();
    });
});
