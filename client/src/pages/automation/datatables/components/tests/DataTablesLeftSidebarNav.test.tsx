import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DataTablesLeftSidebarNav from '../DataTablesLeftSidebarNav';

const hoisted = vi.hoisted(() => {
    return {
        mockUseDataTablesLeftSidebarNav: vi.fn(),
    };
});

vi.mock('../hooks/useDataTablesLeftSidebarNav', () => ({
    default: hoisted.mockUseDataTablesLeftSidebarNav,
}));

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

const renderWithRouter = () => {
    return render(
        <MemoryRouter>
            <DataTablesLeftSidebarNav />
        </MemoryRouter>
    );
};

describe('DataTablesLeftSidebarNav', () => {
    describe('with tags', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesLeftSidebarNav.mockReturnValue({
                isLoading: false,
                tagId: null,
                tags: [
                    {id: 1, name: 'Important'},
                    {id: 2, name: 'Archived'},
                ],
            });
        });

        it('should render Tags title', () => {
            renderWithRouter();

            expect(screen.getByText('Tags')).toBeInTheDocument();
        });

        it('should render all tag names', () => {
            renderWithRouter();

            expect(screen.getByText('Important')).toBeInTheDocument();
            expect(screen.getByText('Archived')).toBeInTheDocument();
        });

        it('should render tag links', () => {
            renderWithRouter();

            const importantLink = screen.getByRole('link', {name: /Important/});
            const archivedLink = screen.getByRole('link', {name: /Archived/});

            expect(importantLink).toHaveAttribute('href', '/?tagId=1');
            expect(archivedLink).toHaveAttribute('href', '/?tagId=2');
        });
    });

    describe('with selected tag', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesLeftSidebarNav.mockReturnValue({
                isLoading: false,
                tagId: '1',
                tags: [
                    {id: 1, name: 'Important'},
                    {id: 2, name: 'Archived'},
                ],
            });
        });

        it('should render both tags', () => {
            renderWithRouter();

            expect(screen.getByText('Important')).toBeInTheDocument();
            expect(screen.getByText('Archived')).toBeInTheDocument();
        });
    });

    describe('empty state', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesLeftSidebarNav.mockReturnValue({
                isLoading: false,
                tagId: null,
                tags: [],
            });
        });

        it('should not render anything when no tags exist', () => {
            const {container} = renderWithRouter();

            expect(container).toBeEmptyDOMElement();
        });
    });

    describe('loading state', () => {
        beforeEach(() => {
            hoisted.mockUseDataTablesLeftSidebarNav.mockReturnValue({
                isLoading: true,
                tagId: null,
                tags: [{id: 1, name: 'Important'}],
            });
        });

        it('should not render anything while loading', () => {
            const {container} = renderWithRouter();

            expect(container).toBeEmptyDOMElement();
        });
    });
});
