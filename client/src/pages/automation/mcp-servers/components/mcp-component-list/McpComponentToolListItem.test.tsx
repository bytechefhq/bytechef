import {McpActivePopoverProvider, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

const {deleteDialogState, dropdownMenuHookMock, mutateMock, setShowDeleteDialogMock, sharedMutateMock} = vi.hoisted(
    () => ({
        deleteDialogState: {showDeleteDialog: false},
        dropdownMenuHookMock: vi.fn(),
        mutateMock: vi.fn(),
        setShowDeleteDialogMock: vi.fn(),
        sharedMutateMock: vi.fn(),
    })
);

vi.mock('./hooks/useMcpProjectComponentToolDropdownMenu', () => ({
    default: (props: unknown) => {
        dropdownMenuHookMock(props);

        return {
            handleConfirmDelete: vi.fn(),
            isDeletePending: false,
            setShowDeleteDialog: setShowDeleteDialogMock,
            showDeleteDialog: deleteDialogState.showDeleteDialog,
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
    useUpdateMcpToolEnabledMutation: () => ({mutate: sharedMutateMock}),
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

const renderEmbeddedToolListItem = (tool: McpTool = mcpTool) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <McpActivePopoverProvider>
                <McpComponentToolListItem
                    componentName="affinity"
                    componentVersion={1}
                    connectionId={null}
                    embedded
                    mcpTool={tool}
                />
            </McpActivePopoverProvider>
        </QueryClientProvider>
    );

describe('McpComponentToolListItem', () => {
    beforeEach(() => {
        mutateMock.mockClear();
        setShowDeleteDialogMock.mockClear();

        deleteDialogState.showDeleteDialog = false;
    });

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

    it('renders the enabled switch checked for an enabled embedded tool', () => {
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

        expect(screen.getByRole('switch')).toBeChecked();
    });

    it('renders the enabled switch unchecked for a disabled embedded tool', () => {
        render(
            <QueryClientProvider client={new QueryClient()}>
                <McpActivePopoverProvider>
                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        embedded
                        mcpTool={{...mcpTool, enabled: false}}
                    />
                </McpActivePopoverProvider>
            </QueryClientProvider>
        );

        expect(screen.getByRole('switch')).not.toBeChecked();
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

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['embeddedMcpComponentsByServerId']});
    });

    it('disables an automation tool through the shared enabled switch', () => {
        mutateMock.mockClear();

        const queryClient = new QueryClient();

        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        render(
            <QueryClientProvider client={queryClient}>
                <McpActivePopoverProvider>
                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        enabledSwitchVisible
                        mcpTool={mcpTool}
                    />
                </McpActivePopoverProvider>
            </QueryClientProvider>
        );

        fireEvent.click(screen.getByRole('switch'));

        expect(sharedMutateMock).toHaveBeenCalledWith(
            {enabled: false, id: '42'},
            expect.objectContaining({onSettled: expect.any(Function), onSuccess: expect.any(Function)})
        );
        expect(mutateMock).not.toHaveBeenCalled();

        const mutateOptions = sharedMutateMock.mock.calls[0][1];

        mutateOptions.onSuccess();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['mcpComponentsByServerId']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['mcpToolsByComponentId']});
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

    it('keeps the enabled switch disabled until the toggle settles', () => {
        renderEmbeddedToolListItem({...mcpTool, enabled: false});

        fireEvent.click(screen.getByRole('switch'));

        expect(mutateMock).toHaveBeenCalledWith({enabled: true, id: '42'}, expect.anything());
        expect(screen.getByRole('switch')).toBeDisabled();

        const mutateOptions = mutateMock.mock.calls[0][1];

        act(() => mutateOptions.onSettled());

        expect(screen.getByRole('switch')).toBeEnabled();
    });

    it('names each enabled switch after its tool', () => {
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

                    <McpComponentToolListItem
                        componentName="affinity"
                        componentVersion={1}
                        connectionId={null}
                        embedded
                        mcpTool={{...mcpTool, id: '43', name: 'deleteOpportunity', title: 'Delete Opportunity'}}
                    />
                </McpActivePopoverProvider>
            </QueryClientProvider>
        );

        expect(screen.getByRole('switch', {name: 'Enable Create Opportunity'})).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable Delete Opportunity'})).toBeInTheDocument();
    });

    it('opens the delete confirmation from the delete button', () => {
        renderEmbeddedToolListItem();

        fireEvent.click(screen.getByTitle('Delete'));

        expect(setShowDeleteDialogMock).toHaveBeenCalledWith(true);
    });

    it('closes the delete confirmation when it is cancelled', () => {
        deleteDialogState.showDeleteDialog = true;

        renderEmbeddedToolListItem();

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(setShowDeleteDialogMock).toHaveBeenCalledWith(false);
    });
});
