import {render, resetAll, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, describe, expect, it} from 'vitest';

import {LeftSidebarNav, LeftSidebarNavItem} from './LeftSidebarNav';

afterEach(() => {
    resetAll();
});

const renderNav = (props: Partial<Parameters<typeof LeftSidebarNav>[0]> = {}) =>
    render(
        <MemoryRouter>
            <LeftSidebarNav
                body={<LeftSidebarNavItem item={{current: false, id: 1, name: 'Templates'}} toLink="?categoryId=1" />}
                title="Categories"
                {...props}
            />
        </MemoryRouter>
    );

const placeholderRows = () => screen.getByTestId('left-sidebar-nav-skeleton').children;

describe('LeftSidebarNav', () => {
    it('renders its body when nothing is loading', () => {
        renderNav();

        expect(screen.getByText('Templates')).toBeInTheDocument();
        expect(screen.queryByTestId('left-sidebar-nav-skeleton')).not.toBeInTheDocument();
    });

    it('replaces the body with placeholder rows while loading', () => {
        renderNav({loading: true});

        expect(screen.queryByText('Templates')).not.toBeInTheDocument();
        expect(placeholderRows()).toHaveLength(4);
    });

    it('keeps its title while loading, so the group does not disappear', () => {
        renderNav({loading: true});

        expect(screen.getByText('Categories')).toBeInTheDocument();
    });

    it('renders as many placeholder rows as asked for', () => {
        renderNav({loading: true, loadingRows: 7});

        expect(placeholderRows()).toHaveLength(7);
    });

    it('announces itself as busy so assistive tech does not read it as an empty group', () => {
        renderNav({loading: true});

        const placeholder = screen.getByRole('status');

        expect(placeholder).toHaveAttribute('aria-busy', 'true');
        expect(placeholder).toHaveAccessibleName('Loading');
    });

    it('gives each placeholder the height of a real nav row so the group keeps its footprint', () => {
        renderNav();

        const navItemClasses = screen.getByRole('link').className;

        renderNav({loading: true});

        expect(navItemClasses).toContain('h-9');
        expect(placeholderRows()[0]).toHaveClass('h-9');
    });
});
