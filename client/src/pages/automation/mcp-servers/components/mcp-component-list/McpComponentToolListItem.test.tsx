import {McpActivePopoverProvider, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

const {dropdownMenuHookMock, mutateMock} = vi.hoisted(() => ({
    dropdownMenuHookMock: vi.fn(),
    mutateMock: vi.fn(),
}));

vi.mock('./hooks/useMcpProjectComponentToolDropdownMenu', () => ({
    default: (props: unknown) => {
        dropdownMenuHookMock(props);

        return {
            handleConfirmDelete: vi.fn(),
            isDeletePending: false,
            setShowDeleteDialog: vi.fn(),
            showDeleteDialog: false,
        };
    },
}));

vi.mock('@/pages/platform/mcp-servers/components/McpComponentToolPropertiesPopover', () => ({
    default: ({connectionRequired, embedded}: {connectionRequired?: boolean; embedded?: boolean}) => (
        <div data-connection-required={String(connectionRequired)} data-embedded={String(embedded)}>
            tool-properties-popover
        </div>
    ),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useUpdateEmbeddedMcpToolEnabledMutation: () => ({mutate: mutateMock}),
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
    it('passes connectionRequired through to the tool properties popover', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <McpActivePopoverProvider>
                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        connectionRequired
                        mcpTool={mcpTool}
                    />
                </McpActivePopoverProvider>
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByTitle('Configure'));

        expect(screen.getByText('tool-properties-popover')).toHaveAttribute('data-connection-required', 'true');
    });
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

    it('hides the enabled switch by default', () => {
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

        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
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
                        embedded
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

    it('routes deletes and property updates through the embedded operations for an embedded tool', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <McpActivePopoverProvider>
                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        embedded
                        mcpTool={mcpTool}
                    />
                </McpActivePopoverProvider>
            </QueryClientProvider>
        );

        expect(dropdownMenuHookMock).toHaveBeenCalledWith({embedded: true, mcpTool});

        fireEvent.click(screen.getByTitle('Configure'));

        expect(screen.getByText('tool-properties-popover')).toHaveAttribute('data-embedded', 'true');
    });
});
