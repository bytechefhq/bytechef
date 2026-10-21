import {render, resetAll, screen, userEvent, windowResizeObserver, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import ApprovalTaskFilters from '../ApprovalTaskFilters';

import type {ApprovalTaskI, FiltersI, PriorityFilterType, StatusFilterType} from '../../types/types';

const createApprovalTask = (overrides: Partial<ApprovalTaskI>): ApprovalTaskI => ({
    assignee: 'Alice',
    attachments: [],
    comments: [],
    createdAt: '2026-01-01T10:00:00Z',
    dependencies: [],
    description: 'Review the change',
    priority: 'high',
    status: 'open',
    title: 'Approve release',
    ...overrides,
});

const approvalTasks = [
    createApprovalTask({id: '1'}),
    createApprovalTask({id: '2', priority: 'low'}),
    createApprovalTask({assignee: 'Bob', id: '3', priority: 'medium', status: 'completed'}),
];

const STATUS_ROWS: [StatusFilterType, RegExp][] = [
    ['open', /^Open/],
    ['in-progress', /^In Progress/],
    ['completed', /^Completed/],
];

const PRIORITY_ROWS: [PriorityFilterType, RegExp][] = [
    ['high', /High/],
    ['medium', /Medium/],
    ['low', /Low/],
];

const setStoreState = (filters: FiltersI) => {
    useApprovalTasksStore.setState({approvalTasks, filters});
};

const openMenu = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Filter options'}));

    return screen.findByRole('menu');
};

const clickAllRow = async (rowIndex: number) => {
    await openMenu();

    await userEvent.click(screen.getAllByRole('menuitem', {name: /^All/})[rowIndex]);
};

describe('ApprovalTaskFilters', () => {
    beforeEach(() => {
        windowResizeObserver();

        setStoreState({assignee: 'all', priority: 'all', status: 'open'});
    });

    afterEach(() => {
        resetAll();
    });

    it('renders with the design-system DropdownMenu', async () => {
        render(<ApprovalTaskFilters />);

        expect(await openMenu()).toHaveClass('bg-surface-neutral-primary');
        expect(screen.getByText('Filter Approval Tasks')).toBeInTheDocument();
        expect(screen.getByText('Status')).toHaveClass('text-content-neutral-primary');
        expect(screen.getByText('Priority')).toHaveClass('text-content-neutral-primary');
        expect(screen.getByText('Assignee')).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: /^Open/})).toHaveClass('text-content-neutral-primary');
        expect(screen.getByRole('menuitem', {name: 'Clear all filters'})).toHaveClass('text-content-neutral-primary');
    });

    it('shows how many approval tasks each option holds', async () => {
        render(<ApprovalTaskFilters />);

        await openMenu();

        expect(within(screen.getByRole('menuitem', {name: /^Open/})).getByText('(2)')).toHaveClass(
            'text-content-neutral-secondary'
        );
        expect(within(screen.getByRole('menuitem', {name: /^Completed/})).getByText('(1)')).toBeInTheDocument();
        expect(within(screen.getByRole('menuitem', {name: /Bob/})).getByText('(1)')).toBeInTheDocument();
    });

    it.each(STATUS_ROWS)('filters by the %s status', async (status, rowName) => {
        setStoreState({assignee: 'all', priority: 'all', status: 'all'});

        render(<ApprovalTaskFilters />);

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(useApprovalTasksStore.getState().filters.status).toBe(status);
    });

    it.each(PRIORITY_ROWS)('filters by the %s priority', async (priority, rowName) => {
        render(<ApprovalTaskFilters />);

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: rowName}));

        expect(useApprovalTasksStore.getState().filters.priority).toBe(priority);
    });

    it('filters by the chosen assignee', async () => {
        render(<ApprovalTaskFilters />);

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: /Bob/}));

        expect(useApprovalTasksStore.getState().filters.assignee).toBe('Bob');
    });

    it('goes back to every option from the All rows', async () => {
        setStoreState({assignee: 'Bob', priority: 'high', status: 'completed'});

        render(<ApprovalTaskFilters />);

        await clickAllRow(0);

        expect(useApprovalTasksStore.getState().filters.status).toBe('all');

        await clickAllRow(1);

        expect(useApprovalTasksStore.getState().filters.priority).toBe('all');

        await clickAllRow(2);

        expect(useApprovalTasksStore.getState().filters.assignee).toBe('all');
    });

    it('clears every filter', async () => {
        render(<ApprovalTaskFilters />);

        await openMenu();
        await userEvent.click(screen.getByRole('menuitem', {name: 'Clear all filters'}));

        expect(useApprovalTasksStore.getState().filters).toEqual({assignee: 'all', priority: 'all', status: 'all'});
    });

    it('disables Clear all filters while nothing is filtered', async () => {
        setStoreState({assignee: 'all', priority: 'all', status: 'all'});

        render(<ApprovalTaskFilters />);

        await openMenu();

        expect(screen.getByRole('menuitem', {name: 'Clear all filters'})).toHaveAttribute('data-disabled');
    });
});
