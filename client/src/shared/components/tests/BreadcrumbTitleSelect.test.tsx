import {TooltipProvider} from '@/components/ui/tooltip';
import BreadcrumbTitleSelect, {getPublishedVersion} from '@/shared/components/BreadcrumbTitleSelect';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {FolderIcon} from 'lucide-react';
import {ComponentProps} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockOnValueChange = vi.fn();

const mockItems = [
    {id: '1', name: 'Current'},
    {id: '2', name: 'Published', publishedVersion: 3},
    {disabled: true, id: '3', name: 'Empty'},
];

const renderBreadcrumbTitleSelect = (props: Partial<ComponentProps<typeof BreadcrumbTitleSelect>> = {}) =>
    render(
        <TooltipProvider>
            <BreadcrumbTitleSelect
                ariaLabel="Item select"
                badge={<span>V4 DRAFT</span>}
                currentId="1"
                icon={FolderIcon}
                items={mockItems}
                menuLabel="Items"
                name="Current"
                onValueChange={mockOnValueChange}
                {...props}
            />
        </TooltipProvider>
    );

beforeEach(() => {
    vi.clearAllMocks();
});

describe('BreadcrumbTitleSelect', () => {
    it('shows the name and the badge inside the select trigger', () => {
        renderBreadcrumbTitleSelect();

        const trigger = screen.getByLabelText('Item select');

        expect(trigger).toContainElement(screen.getByRole('heading', {name: 'Current'}));
        expect(trigger).toHaveTextContent('V4 DRAFT');
    });

    it('shows a plain title without a select when there are no items to switch to', () => {
        renderBreadcrumbTitleSelect({items: []});

        expect(screen.getByRole('heading', {name: 'Current'})).toBeInTheDocument();
        expect(screen.getByText('V4 DRAFT')).toBeInTheDocument();
        expect(screen.queryByLabelText('Item select')).not.toBeInTheDocument();
    });

    it('shows a plain title without a select when there is no change handler', () => {
        renderBreadcrumbTitleSelect({onValueChange: undefined});

        expect(screen.queryByLabelText('Item select')).not.toBeInTheDocument();
    });

    it('lists the items under the menu label, checks the current one and disables the disabled ones', async () => {
        renderBreadcrumbTitleSelect();

        await userEvent.click(screen.getByLabelText('Item select'));

        expect(screen.getByText('Items')).toBeInTheDocument();
        expect(screen.getByRole('menuitemradio', {name: 'Current'})).toHaveAttribute('data-state', 'checked');
        expect(screen.getByRole('menuitemradio', {name: 'Empty'})).toHaveAttribute('data-disabled');
    });

    it('marks only published items with a dot', async () => {
        renderBreadcrumbTitleSelect();

        await userEvent.click(screen.getByLabelText('Item select'));

        expect(screen.getByRole('menuitemradio', {name: /Published/})).toContainElement(
            screen.getByText('Published V3')
        );
        expect(screen.getAllByText(/Published V/)).toHaveLength(1);
    });

    it('calls the change handler with the selected item id', async () => {
        renderBreadcrumbTitleSelect();

        await userEvent.click(screen.getByLabelText('Item select'));
        await userEvent.click(screen.getByRole('menuitemradio', {name: /Published/}));

        expect(mockOnValueChange).toHaveBeenCalledWith('2');
    });
});

describe('getPublishedVersion', () => {
    it('returns nothing for an item that was never published', () => {
        expect(getPublishedVersion(undefined, 3, false)).toBeUndefined();
    });

    it('returns nothing without a version', () => {
        expect(getPublishedVersion(new Date(), undefined, false)).toBeUndefined();
    });

    it('returns the version before the open draft', () => {
        expect(getPublishedVersion(new Date(), 3, false)).toBe(2);
    });

    it('returns the last version when it is the published one', () => {
        expect(getPublishedVersion(new Date(), 3, true)).toBe(3);
    });
});
