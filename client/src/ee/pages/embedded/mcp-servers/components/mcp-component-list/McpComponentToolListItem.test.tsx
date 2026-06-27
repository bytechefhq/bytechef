import {McpActivePopoverProvider, useMcpActivePopover} from '@/shared/contexts/McpActivePopoverContext';
import {McpTool} from '@/shared/middleware/graphql';
import {fireEvent, render, screen} from '@testing-library/react';
import {useState} from 'react';
import {describe, expect, it, vi} from 'vitest';

import McpComponentToolListItem from './McpComponentToolListItem';

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

const mcpTool = {id: '42', name: 'createOpportunity', title: 'Create Opportunity'} as McpTool;

const ActivePopoverProbe = () => {
    const {activePopoverId} = useMcpActivePopover();

    return <div data-testid="active-popover-id">{activePopoverId ?? 'NONE'}</div>;
};

const Harness = () => {
    const [mounted, setMounted] = useState(true);

    return (
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
});
