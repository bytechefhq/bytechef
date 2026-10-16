import {TooltipProvider} from '@/components/ui/tooltip';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AiAgentTestingPanel from './AiAgentTestingPanel';

const {hookState} = vi.hoisted(() => ({
    hookState: {
        conversationId: 'conversation-1',
        handleReset: vi.fn(),
        handleTestAgent: vi.fn(),
        isTestingAgent: false,
    },
}));

vi.mock('./hooks/useAiAgentTestingPanel', () => ({default: () => hookState}));
vi.mock('./AiAgentTestingPanelThread', () => ({Thread: () => <div data-testid="thread" />}));
vi.mock(
    '@/pages/platform/cluster-element-editor/ai-agent-editor/components/ai-agent-testing-panel/runtime-providers/AiAgentTestRuntimeProvider',
    () => ({default: ({children}: {children: React.ReactNode}) => <div>{children}</div>})
);

describe('AiAgentTestingPanel', () => {
    beforeEach(() => {
        hookState.isTestingAgent = false;
        hookState.handleTestAgent.mockReset();
    });

    it('shows the landing and waits for Test Agent by default', () => {
        render(
            <TooltipProvider>
                <AiAgentTestingPanel />
            </TooltipProvider>
        );

        expect(screen.getByText('Test this Agent')).toBeInTheDocument();
        expect(hookState.handleTestAgent).not.toHaveBeenCalled();
    });

    it('enters testing on mount and never paints the landing when skipIntro is set', () => {
        render(
            <TooltipProvider>
                <AiAgentTestingPanel skipIntro />
            </TooltipProvider>
        );

        expect(hookState.handleTestAgent).toHaveBeenCalledTimes(1);
        expect(screen.queryByText('Test this Agent')).not.toBeInTheDocument();
    });

    it('keeps an already-running conversation instead of resetting it', () => {
        hookState.isTestingAgent = true;

        render(
            <TooltipProvider>
                <AiAgentTestingPanel skipIntro />
            </TooltipProvider>
        );

        expect(hookState.handleTestAgent).not.toHaveBeenCalled();
        expect(screen.getByTestId('thread')).toBeInTheDocument();
    });
});
