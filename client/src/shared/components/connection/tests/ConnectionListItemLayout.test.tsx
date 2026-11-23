import {TooltipProvider} from '@/components/ui/tooltip';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectionListItemLayout from '../ConnectionListItemLayout';

const renderLayout = (props: Partial<Parameters<typeof ConnectionListItemLayout>[0]> = {}) =>
    render(
        <TooltipProvider>
            <ul>
                <ConnectionListItemLayout
                    menuItems={<button type="button">Custom menu item</button>}
                    name="My Connection"
                    onEditClick={vi.fn()}
                    tagList={<span>tag list</span>}
                    {...props}
                />
            </ul>
        </TooltipProvider>
    );

describe('ConnectionListItemLayout', () => {
    beforeEach(() => {
        windowResizeObserver();
    });

    afterEach(() => {
        resetAll();
    });

    it('renders the name, tag list, extra badges and children', () => {
        renderLayout({badges: <span>Shared</span>, children: <div>dialog slot</div>});

        expect(screen.getByText('My Connection')).toBeInTheDocument();
        expect(screen.getByText('tag list')).toBeInTheDocument();
        expect(screen.getByText('Shared')).toBeInTheDocument();
        expect(screen.getByText('dialog slot')).toBeInTheDocument();
    });

    it('renders the extra badges before the status badge', () => {
        renderLayout({active: true, badges: <span>Shared</span>, credentialStatus: 'VALID'});

        const sharedBadge = screen.getByText('Shared');
        const statusBadge = screen.getByText('Active');

        expect(sharedBadge.compareDocumentPosition(statusBadge) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('shows Active for a valid active connection', () => {
        renderLayout({active: true, credentialStatus: 'VALID'});

        expect(screen.getByText('Active')).toBeInTheDocument();
    });

    it('shows Not Active for a valid inactive connection', () => {
        renderLayout({active: false, credentialStatus: 'VALID'});

        expect(screen.getByText('Not Active')).toBeInTheDocument();
    });

    it('shows the credential status when it is not valid', () => {
        renderLayout({active: true, credentialStatus: 'REJECTED'});

        expect(screen.getByText('REJECTED')).toBeInTheDocument();
        expect(screen.queryByText('Active')).not.toBeInTheDocument();
    });

    it('shows INVALID when the credential status is missing', () => {
        renderLayout();

        expect(screen.getByText('INVALID')).toBeInTheDocument();
    });

    it('shows the created date only when present', () => {
        const {unmount} = renderLayout();

        expect(screen.queryByText(/Created at/)).not.toBeInTheDocument();

        unmount();

        renderLayout({createdDate: new Date(2026, 10, 9, 18, 0, 0)});

        expect(screen.getByText(/Created at/)).toBeInTheDocument();
    });

    it('renders the tag row only when a tag list is given', () => {
        const {unmount} = renderLayout();

        expect(screen.getByTestId('connection-list-item-tags')).toHaveTextContent('tag list');

        unmount();

        renderLayout({tagList: undefined});

        expect(screen.queryByTestId('connection-list-item-tags')).not.toBeInTheDocument();
    });

    it('renders no menu when onEditClick is not given', () => {
        renderLayout({menuItems: undefined, onEditClick: undefined});

        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('opens the menu with the given items and calls onEditClick', async () => {
        const onEditClick = vi.fn();

        renderLayout({onEditClick});

        await userEvent.click(screen.getByRole('button', {name: ''}));

        expect(screen.getByText('Custom menu item')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Edit'));

        expect(onEditClick).toHaveBeenCalledTimes(1);
    });

    it('toggles the expandable content when the row itself is clicked', async () => {
        const onOpenChange = vi.fn();

        renderLayout({
            expandLabel: 'Workflows',
            expandableContent: <div>workflow list</div>,
            onOpenChange,
            open: false,
        });

        await userEvent.click(screen.getByText('My Connection'));

        expect(onOpenChange).toHaveBeenCalledWith(true);
    });

    it('collapses the open expandable content when the row is clicked again', async () => {
        const onOpenChange = vi.fn();

        renderLayout({expandLabel: 'Workflows', expandableContent: <div>workflow list</div>, onOpenChange, open: true});

        expect(screen.getByText('workflow list')).toBeInTheDocument();

        await userEvent.click(screen.getByText('My Connection'));

        expect(onOpenChange).toHaveBeenCalledWith(false);
    });

    it('does not toggle the row when a control inside it is clicked', async () => {
        const onOpenChange = vi.fn();

        renderLayout({
            expandLabel: 'Workflows',
            expandableContent: <div>workflow list</div>,
            onOpenChange,
            open: false,
            tagList: <button type="button">Add tag</button>,
        });

        await userEvent.click(screen.getByRole('button', {name: 'Add tag'}));

        expect(onOpenChange).not.toHaveBeenCalled();
    });

    it('does not toggle the row when a plain element inside the tag list is clicked', async () => {
        const onOpenChange = vi.fn();

        renderLayout({
            expandLabel: 'Workflows',
            expandableContent: <div>workflow list</div>,
            onOpenChange,
            open: false,
            tagList: <div>Select tags</div>,
        });

        await userEvent.click(screen.getByText('Select tags'));

        expect(onOpenChange).not.toHaveBeenCalled();
    });

    it('does not make the row clickable without expandable content', async () => {
        const onOpenChange = vi.fn();

        renderLayout({onOpenChange});

        await userEvent.click(screen.getByText('My Connection'));

        expect(onOpenChange).not.toHaveBeenCalled();
    });
});
