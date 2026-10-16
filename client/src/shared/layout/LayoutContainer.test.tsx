import {TooltipProvider} from '@/components/ui/tooltip';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {act, fireEvent, render, screen} from '@/shared/util/test-utils';
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

describe('LayoutContainer', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
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
});
