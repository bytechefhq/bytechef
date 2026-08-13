import {TooltipProvider} from '@/components/ui/tooltip';
import CopilotButton from '@/shared/components/copilot/CopilotButton';
import {MODE, Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    return {
        mockCopilotEnabled: {value: true},
        mockGenerateConversationId: vi.fn(),
        mockResetMessages: vi.fn(),
        mockSaveConversationState: vi.fn(),
        mockSetComposerPlaceholder: vi.fn(),
        mockSetContext: vi.fn(),
        mockSetCopilotPanelOpen: vi.fn(),
        mockSetGlobalPanelConversationToken: vi.fn(),
    };
});

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {ai: {copilot: {enabled: boolean}}}) => unknown) =>
        selector({ai: {copilot: {enabled: hoisted.mockCopilotEnabled.value}}}),
}));

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
    ) => selector({copilotPanelOpen: false, setCopilotPanelOpen: hoisted.mockSetCopilotPanelOpen}),
}));

describe('CopilotButton', () => {
    beforeEach(() => {
        hoisted.mockCopilotEnabled.value = true;
        hoisted.mockGenerateConversationId.mockClear();
        hoisted.mockResetMessages.mockClear();
        hoisted.mockSaveConversationState.mockClear();
        hoisted.mockSetComposerPlaceholder.mockClear();
        hoisted.mockSetContext.mockClear();
        hoisted.mockSetCopilotPanelOpen.mockClear();
        hoisted.mockSetGlobalPanelConversationToken.mockClear();
    });

    it('should render nothing when copilot is disabled', () => {
        hoisted.mockCopilotEnabled.value = false;

        render(
            <TooltipProvider>
                <CopilotButton source={Source.DATA_TABLE} />
            </TooltipProvider>
        );

        expect(screen.queryByLabelText('Ask Copilot')).not.toBeInTheDocument();
    });

    it('should set an ASK context for the source and open the panel', async () => {
        render(
            <TooltipProvider>
                <CopilotButton source={Source.DATA_TABLE} />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByLabelText('Ask Copilot'));

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.ASK,
            parameters: {},
            source: Source.DATA_TABLE,
        });
        expect(hoisted.mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });

    // Opening from a listing page must push the conversation it covers onto the stack, or the surface
    // underneath is destroyed rather than restored when this panel closes.
    it('should push the covered conversation onto the stack before opening', async () => {
        render(
            <TooltipProvider>
                <CopilotButton source={Source.DATA_TABLE} />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByLabelText('Ask Copilot'));

        expect(hoisted.mockSaveConversationState).toHaveBeenCalledOnce();
    });

    it('should forward parameters and an explicit mode', async () => {
        render(
            <TooltipProvider>
                <CopilotButton mode={MODE.BUILD} parameters={{dataTableId: '7'}} source={Source.DATA_TABLE} />
            </TooltipProvider>
        );

        await userEvent.click(screen.getByLabelText('Ask Copilot'));

        expect(hoisted.mockSetContext).toHaveBeenCalledWith({
            mode: MODE.BUILD,
            parameters: {dataTableId: '7'},
            source: Source.DATA_TABLE,
        });
    });
});
