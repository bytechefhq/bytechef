import {TooltipProvider} from '@/components/ui/tooltip';
import CopilotPanel from '@/shared/components/copilot/CopilotPanel';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {render, resetAll, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/components/assistant-ui/thread', () => ({
    Thread: ({composerActions}: {composerActions?: ReactNode}) => <div data-testid="thread">{composerActions}</div>,
}));

vi.mock('@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider', () => ({
    CopilotRuntimeProvider: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('@/shared/components/ai-chat/messages/aiChatDataComponents', () => ({
    aiChatDataComponents: {},
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: () => null,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiDefaultModelQuery: () => ({
        data: {aiDefaultModel: {model: 'model-1', provider: 'provider-1'}},
        isPending: false,
    }),
}));

const renderCopilotPanel = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <CopilotPanel open={true} />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('CopilotPanel', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({buildModeDisabled: false});
    });

    afterEach(() => {
        resetAll();
    });

    it('offers the Build mode switch in the workflow editor', () => {
        renderCopilotPanel();

        expect(screen.getByRole('switch', {name: 'Build mode off (Ask)'})).toBeInTheDocument();
    });

    it('hides the Build mode switch while the workflow editor is read-only', () => {
        useCopilotPanelStore.setState({buildModeDisabled: true});

        renderCopilotPanel();

        expect(screen.getByTestId('thread')).toBeInTheDocument();
        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
    });
});
