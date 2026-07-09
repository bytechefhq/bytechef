import {McpActivePopoverProvider, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

const {mutateMock} = vi.hoisted(() => ({mutateMock: vi.fn()}));

vi.mock('./hooks/useMcpComponentToolDropdownMenu', () => ({
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

const ActivePopoverProbe = () => {
    const {activePopoverId} = useMcpActivePopover();

    return <div data-testid="active-popover-id">{activePopoverId ?? 'NONE'}</div>;
};

const Harness = () => {
    const [mounted, setMounted] = useState(true);

    return (
        <QueryClientProvider client={new QueryClient()}>
            <McpActivePopoverProvider>
                <ActivePopoverProbe />

                <button onClick={() => setMounted(false)} type="button">
                    collapse
                </button>

                {mounted && (
                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        mcpTool={mcpTool}
                    />
                )}
            </McpActivePopoverProvider>
        </QueryClientProvider>
    );
};

describe('McpComponentToolListItem', () => {
    it('clears the active popover when the item unmounts (card collapse)', () => {
        render(<Harness />);

        expect(screen.getByTestId('active-popover-id')).toHaveTextContent('NONE');

        fireEvent.click(screen.getByTitle('Configure'));

        expect(screen.getByTestId('active-popover-id')).toHaveTextContent('component-tool-42');
        expect(screen.getByText('tool-properties-popover')).toBeInTheDocument();

        // Simulate the Collapsible card collapsing, which unmounts the tool item.
        fireEvent.click(screen.getByText('collapse'));

        // The active popover must reset so re-expanding the card does not reopen it.
        expect(screen.getByTestId('active-popover-id')).toHaveTextContent('NONE');
    });

    it('disables the tool via the enabled switch', () => {
        const queryClient = new QueryClient();

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        render(
            <QueryClientProvider client={queryClient}>
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

        fireEvent.click(screen.getByRole('switch'));

        expect(mutateMock).toHaveBeenCalledWith(
            {enabled: false, id: '42'},
            expect.objectContaining({onSettled: expect.any(Function), onSuccess: expect.any(Function)})
        );

        const mutateOptions = mutateMock.mock.calls[0][1];

        mutateOptions.onSuccess();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
    });
});
