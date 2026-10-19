import {TooltipProvider} from '@/components/ui/tooltip';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {act, fireEvent, render, screen, within} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const renderLayout = (props: {leftSidebarBody?: boolean; leftSidebarOpen?: boolean} = {}) => {
    const {leftSidebarBody = true, leftSidebarOpen} = props;

    return render(
        <TooltipProvider>
            <LayoutContainer
                header={<Header position="main" title="Projects" />}
                leftSidebarBody={leftSidebarBody ? <div>Categories</div> : undefined}
                leftSidebarOpen={leftSidebarOpen}
            >
                <div>Content</div>
            </LayoutContainer>
        </TooltipProvider>
    );
};

const desktopWidth = window.innerWidth;

const setViewportWidth = (width: number) => {
    Object.defineProperty(window, 'innerWidth', {configurable: true, value: width, writable: true});
};

describe('LayoutContainer', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();

        setViewportWidth(desktopWidth);
    });

    it('closes the left sidebar from the header toggle and reopens it', () => {
        renderLayout();

        const toggleButton = screen.getByRole('button', {name: 'Toggle sidebar'});

        fireEvent.click(toggleButton);

        expect(screen.getByText('Categories').closest('aside')).toHaveClass('lg:-translate-x-full');

        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(screen.queryByText('Categories')).not.toBeInTheDocument();

        fireEvent.click(toggleButton);

        expect(screen.getByText('Categories').closest('aside')).toHaveClass('lg:translate-x-0');
    });

    it('offers no toggle on a page without a left sidebar', () => {
        renderLayout({leftSidebarBody: false});

        expect(screen.queryByRole('button', {name: 'Toggle sidebar'})).not.toBeInTheDocument();
    });

    it('offers no toggle on a page that controls its own left sidebar', () => {
        renderLayout({leftSidebarOpen: true});

        expect(screen.queryByRole('button', {name: 'Toggle sidebar'})).not.toBeInTheDocument();
    });

    // A page learns it is empty only after loading, so the sidebar shown meanwhile must go at once, not slide out.
    it('drops a hidden left sidebar at once and offers no toggle', () => {
        const renderEmptyPage = (leftSidebarHidden: boolean) => (
            <TooltipProvider>
                <LayoutContainer
                    header={<Header position="main" title="Projects" />}
                    leftSidebarBody={<div>Categories</div>}
                    leftSidebarHidden={leftSidebarHidden}
                >
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        const {rerender} = render(renderEmptyPage(false));

        expect(screen.getByText('Categories')).toBeInTheDocument();

        rerender(renderEmptyPage(true));

        expect(screen.queryByText('Categories')).not.toBeInTheDocument();
        expect(screen.getByText('Content').closest('.size-full.overflow-auto > div')).not.toHaveClass(
            'transition-[padding]'
        );
        expect(screen.queryByRole('button', {name: 'Toggle sidebar'})).not.toBeInTheDocument();
    });

    // With the sidebar hidden, the main header carries the page title the sidebar header would have shown.
    it('styles the main header title like the sidebar header while the sidebar is hidden', () => {
        render(
            <TooltipProvider>
                <LayoutContainer
                    header={<Header position="main" title="Projects" />}
                    leftSidebarBody={<div>Categories</div>}
                    leftSidebarHidden
                >
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        expect(screen.getByText('Projects')).toHaveClass('font-semibold');
    });

    it('lets a nested sidebar-less container use the outer container toggle', () => {
        render(
            <TooltipProvider>
                <LayoutContainer leftSidebarBody={<div>Settings nav</div>}>
                    <LayoutContainer header={<Header position="main" title="Users" />} leftSidebarOpen={false}>
                        <div>Content</div>
                    </LayoutContainer>
                </LayoutContainer>
            </TooltipProvider>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Toggle sidebar'}));

        expect(screen.getByText('Settings nav').closest('aside')).toHaveClass('lg:-translate-x-full');
    });

    it('opens the sidebar in a drawer when toggled on a small screen', () => {
        setViewportWidth(800);

        renderLayout();

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle sidebar'}));

        expect(within(screen.getByRole('dialog')).getByText('Categories')).toBeInTheDocument();
    });

    it('offers no drawer on a small screen while the sidebar is hidden', () => {
        setViewportWidth(800);

        render(
            <TooltipProvider>
                <LayoutContainer
                    header={<Header position="main" title="Projects" />}
                    leftSidebarBody={<div>Categories</div>}
                    leftSidebarHidden
                >
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        expect(screen.queryByRole('button', {name: 'Toggle sidebar'})).not.toBeInTheDocument();
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('does not open a drawer when toggled on a large screen', () => {
        setViewportWidth(1280);

        renderLayout();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle sidebar'}));

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
});
