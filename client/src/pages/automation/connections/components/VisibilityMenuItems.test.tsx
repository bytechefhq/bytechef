import {DropdownMenu, DropdownMenuContent, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import VisibilityMenuItems from './VisibilityMenuItems';

const renderMenu = (props: Partial<Parameters<typeof VisibilityMenuItems>[0]>) => {
    const defaults = {
        connectionId: '1',
        onDemoteRequest: vi.fn(),
        onPromoteToWorkspace: vi.fn(),
        onShareWithProjects: vi.fn(),
        visibility: 'PRIVATE' as const,
        workspaceId: '100',
    };

    const merged = {...defaults, ...props};

    render(
        <DropdownMenu defaultOpen={true}>
            <DropdownMenuTrigger>open</DropdownMenuTrigger>

            <DropdownMenuContent>
                <VisibilityMenuItems {...merged} />
            </DropdownMenuContent>
        </DropdownMenu>
    );

    return merged;
};

describe('VisibilityMenuItems', () => {
    it('shows promote and share-with-projects when PRIVATE', () => {
        renderMenu({visibility: 'PRIVATE'});

        expect(screen.getByText('Share with workspace')).toBeInTheDocument();
        expect(screen.getByText('Share with projects…')).toBeInTheDocument();
        expect(screen.queryByText('Make private')).not.toBeInTheDocument();
    });

    it('shows share-with-projects and make-private when PROJECT', () => {
        renderMenu({visibility: 'PROJECT'});

        expect(screen.queryByText('Share with workspace')).not.toBeInTheDocument();
        expect(screen.getByText('Share with projects…')).toBeInTheDocument();
        expect(screen.getByText('Make private')).toBeInTheDocument();
    });

    it('shows only make-private when WORKSPACE', () => {
        renderMenu({visibility: 'WORKSPACE'});

        expect(screen.queryByText('Share with workspace')).not.toBeInTheDocument();
        expect(screen.queryByText('Share with projects…')).not.toBeInTheDocument();
        expect(screen.getByText('Make private')).toBeInTheDocument();
    });

    it('invokes onPromoteToWorkspace with connection and workspace ids', async () => {
        const user = userEvent.setup();
        const props = renderMenu({visibility: 'PRIVATE'});

        await user.click(screen.getByText('Share with workspace'));

        expect(props.onPromoteToWorkspace).toHaveBeenCalledWith({connectionId: '1', workspaceId: '100'});
    });

    it('invokes onDemoteRequest with current visibility', async () => {
        const user = userEvent.setup();
        const props = renderMenu({visibility: 'PROJECT'});

        await user.click(screen.getByText('Make private'));

        expect(props.onDemoteRequest).toHaveBeenCalledWith('PROJECT');
    });

    it('invokes onShareWithProjects callback', async () => {
        const user = userEvent.setup();
        const props = renderMenu({visibility: 'PRIVATE'});

        await user.click(screen.getByText('Share with projects…'));

        expect(props.onShareWithProjects).toHaveBeenCalled();
    });
});
