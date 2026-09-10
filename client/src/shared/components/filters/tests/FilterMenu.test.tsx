import {render, resetAll, screen} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import FilterMenu, {type FilterGroupI, hasActiveFilters, isFilterGroupActive, resetFilterGroups} from '../FilterMenu';

vi.mock('@/components/ui/dropdown-menu', () => ({
    DropdownMenu: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuContent: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuGroup: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuItem: ({
        children,
        disabled,
        onClick,
    }: {
        children: React.ReactNode;
        className?: string;
        disabled?: boolean;
        onClick?: () => void;
    }) => (
        <button disabled={disabled} onClick={onClick} type="button">
            {children}
        </button>
    ),
    DropdownMenuLabel: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
    DropdownMenuSeparator: () => <hr />,
    DropdownMenuTrigger: ({children}: {asChild?: boolean; children: React.ReactNode}) => <div>{children}</div>,
}));

const createTagGroup = (value: string, onChange = vi.fn()): FilterGroupI => ({
    allValue: 'ALL_TAGS',
    key: 'tags',
    label: 'Tags',
    onChange,
    options: [
        {label: 'All tags', value: 'ALL_TAGS'},
        {count: 3, label: 'Billing', value: '1'},
    ],
    value,
});

describe('FilterMenu', () => {
    afterEach(() => {
        resetAll();
    });

    describe('isFilterGroupActive', () => {
        it('treats a group sitting on its all value as inactive', () => {
            expect(isFilterGroupActive(createTagGroup('ALL_TAGS'))).toBe(false);
        });

        it('treats any other value as active', () => {
            expect(isFilterGroupActive(createTagGroup('1'))).toBe(true);
        });
    });

    describe('hasActiveFilters', () => {
        it('is false when every group sits on its all value', () => {
            expect(hasActiveFilters([createTagGroup('ALL_TAGS')])).toBe(false);
        });

        it('is true when any group is active', () => {
            expect(hasActiveFilters([createTagGroup('ALL_TAGS'), createTagGroup('1')])).toBe(true);
        });
    });

    describe('resetFilterGroups', () => {
        it('resets only the active groups', () => {
            const activeOnChange = vi.fn();
            const inactiveOnChange = vi.fn();

            resetFilterGroups([createTagGroup('1', activeOnChange), createTagGroup('ALL_TAGS', inactiveOnChange)]);

            expect(activeOnChange).toHaveBeenCalledWith('ALL_TAGS');
            expect(inactiveOnChange).not.toHaveBeenCalled();
        });
    });

    it('renders a label and an option for every group', () => {
        render(<FilterMenu groups={[createTagGroup('ALL_TAGS')]} title="Filter skills" />);

        expect(screen.getByText('Filter skills')).toBeInTheDocument();
        expect(screen.getByText('Tags')).toBeInTheDocument();
        expect(screen.getByText('All tags')).toBeInTheDocument();
        expect(screen.getByText('Billing')).toBeInTheDocument();
    });

    it('renders an option count when the page supplied one', () => {
        render(<FilterMenu groups={[createTagGroup('ALL_TAGS')]} title="Filter skills" />);

        expect(screen.getByText('(3)')).toBeInTheDocument();
    });

    it('reports an inactive filter set on the trigger', () => {
        render(<FilterMenu groups={[createTagGroup('ALL_TAGS')]} title="Filter skills" />);

        expect(screen.getByLabelText('Filter skills')).toBeInTheDocument();
    });

    it('reports an active filter set on the trigger', () => {
        render(<FilterMenu groups={[createTagGroup('1')]} title="Filter skills" />);

        expect(screen.getByLabelText('Filter skills (filters active)')).toBeInTheDocument();
    });

    it('calls the group onChange with the picked option', () => {
        const onChange = vi.fn();

        render(<FilterMenu groups={[createTagGroup('ALL_TAGS', onChange)]} title="Filter skills" />);

        screen.getByText('Billing').click();

        expect(onChange).toHaveBeenCalledWith('1');
    });

    it('disables clearing when nothing is filtered', () => {
        render(<FilterMenu groups={[createTagGroup('ALL_TAGS')]} title="Filter skills" />);

        expect(screen.getByText('Clear all filters').closest('button')).toBeDisabled();
    });

    it('clears every active group from the menu', () => {
        const onChange = vi.fn();

        render(<FilterMenu groups={[createTagGroup('1', onChange)]} title="Filter skills" />);

        screen.getByText('Clear all filters').click();

        expect(onChange).toHaveBeenCalledWith('ALL_TAGS');
    });
});
