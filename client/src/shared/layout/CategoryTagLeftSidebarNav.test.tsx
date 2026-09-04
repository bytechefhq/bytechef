import CategoryTagLeftSidebarNav from '@/shared/layout/CategoryTagLeftSidebarNav';
import {render, resetAll, screen, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';

const CATEGORIES = [
    {id: 1, name: 'AI'},
    {id: 2, name: 'Records'},
];

const TAGS = [
    {id: 10, name: 'api'},
    {id: 11, name: 'bank'},
];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

const renderNav = (props: Partial<Parameters<typeof CategoryTagLeftSidebarNav>[0]> = {}) =>
    render(
        <MemoryRouter>
            <CategoryTagLeftSidebarNav
                categories={CATEGORIES}
                tags={TAGS}
                tagsEmptyMessage="No defined tags."
                {...props}
            />
        </MemoryRouter>
    );

describe('CategoryTagLeftSidebarNav', () => {
    it('renders both groups once loaded, with no placeholders left behind', () => {
        renderNav();

        expect(screen.getByText('AI')).toBeInTheDocument();
        expect(screen.getByText('api')).toBeInTheDocument();
        expect(screen.queryByTestId('left-sidebar-nav-skeleton')).not.toBeInTheDocument();
    });

    it('shows placeholders in each group while both queries are in flight', () => {
        renderNav({categories: undefined, categoriesIsLoading: true, tags: undefined, tagsIsLoading: true});

        expect(screen.getAllByTestId('left-sidebar-nav-skeleton')).toHaveLength(2);
        expect(screen.getByText('Categories')).toBeInTheDocument();
        expect(screen.getByText('Tags')).toBeInTheDocument();
    });

    it('places placeholders only in the group that is still loading', () => {
        renderNav({tags: undefined, tagsIsLoading: true});

        expect(screen.getByText('AI')).toBeInTheDocument();
        expect(screen.getAllByTestId('left-sidebar-nav-skeleton')).toHaveLength(1);
    });

    it('holds back the All Categories entry until the categories arrive', () => {
        renderNav({categories: undefined, categoriesIsLoading: true});

        expect(screen.queryByText('All Categories')).not.toBeInTheDocument();

        renderNav();

        expect(screen.getByText('All Categories')).toBeInTheDocument();
    });

    it('reports an empty tag list rather than a placeholder once the tags arrive', () => {
        renderNav({tags: []});

        expect(screen.getByText('No defined tags.')).toBeInTheDocument();
        expect(screen.queryByTestId('left-sidebar-nav-skeleton')).not.toBeInTheDocument();
    });

    it('marks the selected category and tag as current', () => {
        renderNav({currentCategoryId: 2, currentTagId: 11});

        expect(screen.getByText('Records').closest('a')).toHaveAttribute('aria-current', 'page');
        expect(screen.getByText('bank').closest('a')).toHaveAttribute('aria-current', 'page');
        expect(screen.getByText('All Categories').closest('a')).not.toHaveAttribute('aria-current');
    });

    it('leaves All Categories unselected when a filter outside these groups is active', () => {
        renderNav({otherFilterActive: true});

        expect(screen.getByText('All Categories').closest('a')).not.toHaveAttribute('aria-current');
    });

    it('appends any extra groups after the tags', () => {
        renderNav({extraGroups: <div data-testid="extra-group" />});

        expect(screen.getByTestId('extra-group')).toBeInTheDocument();
    });
});
