import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockCopilotPanelOpen: {value: false},
        mockGenerateConversationId: vi.fn(),
        mockResetMessages: vi.fn(),
        mockSaveConversationState: vi.fn(),
        mockSetComposerPlaceholder: vi.fn(),
        mockSetContext: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockSetGlobalPanelConversationToken: vi.fn(),
    };
});

vi.mock('@/shared/components/copilot/stores/useCopilotStore', async () => {
    const actual = await vi.importActual<typeof import('@/shared/components/copilot/stores/useCopilotStore')>(
        '@/shared/components/copilot/stores/useCopilotStore'
    );

    const mockUseCopilotStore = (
        selector: (state: {
            setComposerPlaceholder: typeof hoisted.mockSetComposerPlaceholder;
            setContext: typeof hoisted.mockSetContext;
        }) => unknown
    ) =>
        selector({
            setComposerPlaceholder: hoisted.mockSetComposerPlaceholder,
            setContext: hoisted.mockSetContext,
        });

    mockUseCopilotStore.getState = () => ({
        generateConversationId: hoisted.mockGenerateConversationId,
        resetMessages: hoisted.mockResetMessages,
        saveConversationState: hoisted.mockSaveConversationState,
        setGlobalPanelConversationToken: hoisted.mockSetGlobalPanelConversationToken,
    });

    return {
        ...actual,
        useCopilotStore: mockUseCopilotStore,
    };
});

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (
        selector: (state: {
            copilotPanelOpen: boolean;
            setCopilotPanelOpen: typeof hoisted.mockSetCopilotPanelOpen;
        }) => unknown
    ) =>
        selector({
            copilotPanelOpen: hoisted.mockCopilotPanelOpen.value,
            setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen,
        }),
}));

const {default: useOpenCopilot} = await import('@/shared/components/copilot/hooks/useOpenCopilot');

describe('useOpenCopilot', () => {
    beforeEach(() => {
        hoisted.mockCopilotPanelOpen.value = false;
        hoisted.mockSetComposerPlaceholder.mockClear();
        hoisted.mockSetCopilotPanelOpen.mockClear();
        hoisted.mockSetGlobalPanelConversationToken.mockClear();

        // mockReset (not mockClear) for the four mocks the ordering test below installs a
        // mockImplementation on: it strips both recorded calls and the implementation. Doing this in
        // beforeEach rather than at the end of that one test means an assertion failure mid-test can't skip
        // the cleanup and leak the callOrder-pushing implementation into whichever test runs next.
        hoisted.mockGenerateConversationId.mockReset();
        hoisted.mockResetMessages.mockReset();
        hoisted.mockSaveConversationState.mockReset();
        hoisted.mockSetContext.mockReset();
    });

    it('sets an ASK context with empty parameters by default and opens the panel', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {},
            source: Source.PROJECT,
        });
        expect(hoisted.mockSetComposerPlaceholder).toHaveBeenCalledWith(undefined);
        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });

    /*
     * The conversation being covered has to be pushed before it is cleared, or the snapshot captures an
     * already-emptied conversation and the surface underneath is lost. The seven local-panel surfaces share
     * this ordering; usePropertyCodeEditorDialogToolbar.test.ts pins it for one of them.
     */
    it('pushes the current conversation before clearing it, then installs the new context', () => {
        const callOrder: string[] = [];

        hoisted.mockSaveConversationState.mockImplementation(() => callOrder.push('save'));
        hoisted.mockResetMessages.mockImplementation(() => callOrder.push('reset'));
        hoisted.mockGenerateConversationId.mockImplementation(() => callOrder.push('generate'));
        hoisted.mockSetContext.mockImplementation(() => callOrder.push('setContext'));

        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        expect(callOrder).toEqual(['save', 'reset', 'generate', 'setContext']);
    });

    // Regression guard for the global-panel leak: a listing page's per-row Copilot action calls this open
    // function once per row click. Clicking project A then project B while the panel stays open must not
    // push twice — the stack would gain an entry the single eventual close can never pop back to.
    it('does not push a second entry when the panel is already open', () => {
        hoisted.mockCopilotPanelOpen.value = true;

        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        expect(hoisted.mockSaveConversationState).not.toHaveBeenCalled();
        expect(hoisted.mockSetGlobalPanelConversationToken).not.toHaveBeenCalled();

        // The surface still replaces the visible conversation even though nothing was pushed.
        expect(hoisted.mockResetMessages).toHaveBeenCalledOnce();
        expect(hoisted.mockGenerateConversationId).toHaveBeenCalledOnce();
        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {},
            source: Source.PROJECT,
        });
        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });

    it('forwards an explicit mode and parameters', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({mode: MODE.BUILD, parameters: {projectId: '7'}, source: Source.PROJECT});
        });

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.BUILD,
            parameters: {projectId: '7'},
            source: Source.PROJECT,
        });
    });

    it('forwards an explicit composer placeholder and clears it when the next open omits one', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({
                composerPlaceholder: 'Describe what the workflow should do.',
                mode: MODE.BUILD,
                parameters: {intent: 'generate_workflow'},
                source: Source.PROJECT,
            });
        });

        expect(hoisted.mockSetComposerPlaceholder).toHaveBeenCalledWith('Describe what the workflow should do.');

        hoisted.mockSetComposerPlaceholder.mockClear();

        act(() => {
            result.current({source: Source.WORKFLOW_EDITOR});
        });

        expect(hoisted.mockSetComposerPlaceholder).toHaveBeenCalledWith(undefined);
    });

    it('sets a fresh context rather than spreading the previous one', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({mode: MODE.BUILD, parameters: {previous: 'value'}, source: Source.DATA_TABLE});
        });

        hoisted.mockSetContext.mockClear();

        act(() => {
            result.current({source: Source.PROJECT});
        });

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {},
            source: Source.PROJECT,
        });
        expect(hoisted.mockSetContext).not.toHaveBeenCalledWith(
            expect.objectContaining({parameters: {previous: 'value'}})
        );
    });
});
