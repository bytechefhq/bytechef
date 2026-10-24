import DataSyncsFilterLeftSidebarNav, {
    getDataSyncsFilter,
} from '@/pages/automation/data-syncs/components/DataSyncsFilterLeftSidebarNav';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it} from 'vitest';

const renderNav = (initialEntries: string[], currentDataSyncsFilter?: 'all' | 'scheduled') =>
    render(
        <MemoryRouter initialEntries={initialEntries}>
            <DataSyncsFilterLeftSidebarNav currentDataSyncsFilter={currentDataSyncsFilter} />
        </MemoryRouter>
    );

describe('getDataSyncsFilter', () => {
    it('reads all from the dataSyncs search param', () => {
        expect(getDataSyncsFilter(new URLSearchParams('dataSyncs=all'))).toBe('all');
    });

    it('reads scheduled from the dataSyncs search param', () => {
        expect(getDataSyncsFilter(new URLSearchParams('dataSyncs=scheduled'))).toBe('scheduled');
    });

    it('ignores an unrecognized value', () => {
        expect(getDataSyncsFilter(new URLSearchParams('dataSyncs=bogus'))).toBeUndefined();
    });

    it('returns undefined when absent', () => {
        expect(getDataSyncsFilter(new URLSearchParams(''))).toBeUndefined();
    });
});

describe('DataSyncsFilterLeftSidebarNav', () => {
    it('shows the Data Syncs title and both items', () => {
        renderNav(['/']);

        expect(screen.getByText('Data Syncs')).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'All Data Syncs'})).toBeInTheDocument();
        expect(screen.getByRole('link', {name: 'Scheduled'})).toBeInTheDocument();
    });

    it('links to ?dataSyncs=all and ?dataSyncs=scheduled', () => {
        renderNav(['/']);

        expect(screen.getByRole('link', {name: 'All Data Syncs'})).toHaveAttribute('href', '/?dataSyncs=all');
        expect(screen.getByRole('link', {name: 'Scheduled'})).toHaveAttribute('href', '/?dataSyncs=scheduled');
    });

    it('clears the filter on a second click of the active item', () => {
        renderNav(['/?dataSyncs=all'], 'all');

        expect(screen.getByRole('link', {name: 'All Data Syncs'})).toHaveAttribute('href', '/');
        expect(screen.getByRole('link', {name: 'Scheduled'})).toHaveAttribute('href', '/?dataSyncs=scheduled');
    });

    it('preserves unrelated search params', () => {
        renderNav(['/?categoryId=5'], undefined);

        expect(screen.getByRole('link', {name: 'All Data Syncs'})).toHaveAttribute(
            'href',
            '/?categoryId=5&dataSyncs=all'
        );
    });

    it('clears the agents filter when picked', () => {
        renderNav(['/?agents=all'], undefined);

        expect(screen.getByRole('link', {name: 'All Data Syncs'})).toHaveAttribute('href', '/?dataSyncs=all');
        expect(screen.getByRole('link', {name: 'Scheduled'})).toHaveAttribute('href', '/?dataSyncs=scheduled');
    });
});
