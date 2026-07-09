import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

const {mutateMock} = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('./hooks/useMcpProjectComponentToolDropdownMenu', () => ({
    default: () => ({
        handleConfirmDelete: vi.fn(),
        setShowDeleteDialog: vi.fn(),
        showDeleteDialog: false,
    }),
}));

vi.mock('./McpComponentToolPropertiesPopover', () => ({
    default: () => <div>tool-properties-popover</div>,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useUpdateMcpToolEnabledMutation: () => ({mutate: mutateMock}),
}));

const mcpTool = {enabled: true, id: '42', name: 'createOpportunity', title: 'Create Opportunity'} as McpTool;

const renderItem = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <McpActivePopoverProvider>
                <McpComponentToolListItem
                    componentName="affinity"
                    componentVersion={1}
                    connectionId={null}
                    mcpTool={mcpTool}
                />
            </McpActivePopoverProvider>
        </QueryClientProvider>
    );

describe('McpComponentToolListItem', () => {
    it('disables the tool via the enabled switch', () => {
        renderItem();

        fireEvent.click(screen.getByRole('switch'));

        expect(mutateMock).toHaveBeenCalledWith(
            {enabled: false, id: '42'},
            expect.objectContaining({onSettled: expect.any(Function), onSuccess: expect.any(Function)})
        );
    });
});
