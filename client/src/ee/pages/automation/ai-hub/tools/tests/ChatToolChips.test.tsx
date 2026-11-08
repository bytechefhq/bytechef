import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactElement} from 'react';
import {describe, expect, it, vi} from 'vitest';

import ChatToolChips from '../ChatToolChips';

const {chatToolsRef, setRequiresApprovalMock} = vi.hoisted(() => ({
    chatToolsRef: {
        current: [
            {
                chatToolId: 'tool-1',
                clusterElementName: 'sendMessage',
                componentName: 'slack',
                requiresApproval: false,
            },
        ],
    },
    setRequiresApprovalMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiHubChatToolsQuery: () => ({data: {aiHubChatTools: chatToolsRef.current}}),
    useRemoveAiHubChatToolMutation: () => ({isPending: false, mutate: vi.fn()}),
    useSetAiHubChatToolRequiresApprovalMutation: () => ({isPending: false, mutate: setRequiresApprovalMock}),
}));

function renderChips(ui: ReactElement) {
    return render(<QueryClientProvider client={new QueryClient()}>{ui}</QueryClientProvider>);
}

describe('ChatToolChips', () => {
    it('shows "Require approval" for a tool that does not require approval, and toggles it on click', () => {
        chatToolsRef.current = [
            {chatToolId: 'tool-1', clusterElementName: 'sendMessage', componentName: 'slack', requiresApproval: false},
        ];

        renderChips(<ChatToolChips chatId="chat-1" workspaceId={1} />);

        const toggleButton = screen.getByRole('button', {name: 'Require approval'});

        fireEvent.click(toggleButton);

        expect(setRequiresApprovalMock).toHaveBeenCalledWith(
            {chatToolId: 'tool-1', requiresApproval: true, workspaceId: '1'},
            expect.anything()
        );
    });

    it('shows "Approval required" and the shield-check icon for a tool that requires approval', () => {
        chatToolsRef.current = [
            {chatToolId: 'tool-1', clusterElementName: 'sendMessage', componentName: 'slack', requiresApproval: true},
        ];

        renderChips(<ChatToolChips chatId="chat-1" workspaceId={1} />);

        const toggleButton = screen.getByRole('button', {name: 'Approval required'});

        expect(toggleButton).toBeInTheDocument();
        expect(toggleButton.querySelector('svg.lucide-shield-check')).not.toBeNull();
    });
});
