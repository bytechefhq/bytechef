import {TooltipProvider} from '@/components/ui/tooltip';
import {CopilotPanelContent} from '@/shared/components/copilot/CopilotPanelImpl';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// Keeps CopilotPanelContent on the "no AI providers enabled" branch, which renders the header (and its
// close button) without pulling in CopilotRuntimeProvider/Thread/ModelPicker — none of which matter for
// exercising closeGlobalPanel. Overriding only this one export (rather than replacing the whole module)
// matters here: `@/shared/middleware/graphql` also exports plain data like `EvaluatorFunctionCategory` that
// unrelated modules pulled in transitively (e.g. property-mentions-input) import at module scope.
vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAiDefaultModelQuery: () => ({data: {aiDefaultModel: null}, isPending: false}),
}));

const wrapper = ({children}: {children: ReactNode}) => (
    <MemoryRouter>
        <TooltipProvider>{children}</TooltipProvider>
    </MemoryRouter>
);

const baseContext = {mode: MODE.ASK, parameters: {covering: true}, source: Source.DATA_TABLE};

// closeGlobalPanel is CopilotPanelContent's own close handler, used whenever no `onClose` prop is
// passed — i.e. for the single global panel mounted in App.tsx. `CopilotPanel.test.tsx` mocks
// CopilotPanelImpl out entirely, so it never exercises this branch; these tests render the real
// component against the real store instead.
describe('CopilotPanelContent closeGlobalPanel', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            composerPlaceholder: 'covering placeholder',
            context: baseContext,
            conversationId: 'covering-conversation',
            conversationStack: [],
            globalPanelConversationToken: null,
            messages: [{content: 'covering turn', role: 'user'}],
        });

        useCopilotPanelStore.setState({copilotPanelOpen: true});
    });

    it('pops the stack and restores the covered conversation when a token was pushed', async () => {
        const user = userEvent.setup();

        const token = useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            context: {mode: MODE.ASK, parameters: {}, source: Source.PROJECT},
            conversationId: 'global-panel-conversation',
            globalPanelConversationToken: token,
            messages: [{content: 'global panel turn', role: 'user'}],
        });

        render(<CopilotPanelContent />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        const state = useCopilotStore.getState();

        expect(state.conversationStack).toHaveLength(0);
        expect(state.conversationId).toBe('covering-conversation');
        expect(state.context).toEqual(baseContext);
        expect(state.composerPlaceholder).toBe('covering placeholder');
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('falls back to the hardcoded reset when nothing was pushed for this open, even with an unrelated entry on the stack', async () => {
        const user = userEvent.setup();

        // An unrelated surface has an entry on the stack (e.g. a dialog still open underneath). This open is
        // simulating one of the six direct-open surfaces (DataTable.tsx and friends): they set their own
        // context and open the panel without ever calling saveConversationState, so the token stays null. A
        // guard that only checked "is the stack non-empty" would wrongly pop the unrelated entry here; the
        // token is what must gate the fallback.
        useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            context: {mode: MODE.ASK, parameters: {dataTableId: '7'}, source: Source.DATA_TABLE},
            globalPanelConversationToken: null,
        });

        render(<CopilotPanelContent />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        const state = useCopilotStore.getState();

        expect(state.context).toEqual({mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR});
        expect(state.composerPlaceholder).toBeUndefined();
        expect(state.conversationStack).toHaveLength(1);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });
});
