import {TooltipProvider} from '@/components/ui/tooltip';
import Header from '@/shared/layout/Header';
import LayoutContainer from '@/shared/layout/LayoutContainer';
import {act, fireEvent, render, screen} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const renderHeader = (title: string) =>
    render(
        <TooltipProvider>
            <LayoutContainer
                header={<Header centerTitle position="main" title={title} />}
                leftSidebarBody={<div>Categories</div>}
            >
                <div>Content</div>
            </LayoutContainer>
        </TooltipProvider>
    );

describe('Header', () => {
    beforeEach(() => {
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('shows the sidebar toggle beside a title', () => {
        renderHeader('Data Tables');

        expect(screen.getByRole('button', {name: 'Toggle sidebar'})).toBeInTheDocument();
    });

    it('shows the sidebar toggle at the end of the sidebar header while the sidebar is open', () => {
        render(
            <TooltipProvider>
                <LayoutContainer
                    header={<Header position="main" title="Workflow Executions" />}
                    leftSidebarBody={<div>Categories</div>}
                    leftSidebarHeader={
                        <Header position="sidebar" right={<span>Sidebar action</span>} title="Filters" />
                    }
                >
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        const toggleButton = screen.getByRole('button', {name: 'Toggle sidebar'});
        const sidebarHeader = screen.getByText('Filters').closest('header');

        expect(sidebarHeader).toContainElement(toggleButton);
        expect(sidebarHeader?.firstElementChild).not.toHaveClass('3xl:w-4/5');
        expect(screen.getByText('Workflow Executions').closest('header')?.firstElementChild).toHaveClass('3xl:w-4/5');
        expect(toggleButton).toHaveClass(
            'hover:bg-surface-neutral-secondary-hover',
            'dark:hover:bg-surface-neutral-secondary-hover'
        );
        expect(toggleButton).not.toHaveClass('dark:hover:bg-surface-neutral-primary-hover');
        expect(screen.getByText('Sidebar action').compareDocumentPosition(toggleButton)).toBe(
            Node.DOCUMENT_POSITION_FOLLOWING
        );

        fireEvent.click(toggleButton);

        const mainToggleButton = screen.getByRole('button', {name: 'Toggle sidebar'});

        expect(screen.getByText('Workflow Executions').closest('header')).toContainElement(mainToggleButton);
        expect(sidebarHeader).not.toContainElement(mainToggleButton);

        fireEvent.click(mainToggleButton);

        expect(screen.getByText('Filters').closest('header')).toContainElement(
            screen.getByRole('button', {name: 'Toggle sidebar'})
        );
    });

    it('shows no sidebar header toggle in a header outside the sidebar', () => {
        render(
            <TooltipProvider>
                <LayoutContainer
                    header={<Header title="Workflow Executions" />}
                    leftSidebarBody={<div>Categories</div>}
                    leftSidebarHeader={<Header position="sidebar" title="Filters" />}
                >
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        expect(screen.getAllByRole('button', {name: 'Toggle sidebar'})).toHaveLength(1);
        expect(screen.getByText('Workflow Executions').closest('header')).not.toContainElement(
            screen.getByRole('button', {name: 'Toggle sidebar'})
        );
    });

    it('hides the sidebar toggle on an untitled page while the sidebar is open', () => {
        renderHeader('');

        expect(screen.queryByRole('button', {name: 'Toggle sidebar'})).not.toBeInTheDocument();
    });

    it('keeps the sidebar toggle on an untitled page once the sidebar is closed', () => {
        const {rerender} = render(
            <TooltipProvider>
                <LayoutContainer
                    header={<Header position="main" title="Chat" />}
                    leftSidebarBody={<div>Categories</div>}
                >
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        fireEvent.click(screen.getByRole('button', {name: 'Toggle sidebar'}));

        act(() => {
            vi.advanceTimersByTime(300);
        });

        rerender(
            <TooltipProvider>
                <LayoutContainer header={<Header position="main" title="" />} leftSidebarBody={<div>Categories</div>}>
                    <div>Content</div>
                </LayoutContainer>
            </TooltipProvider>
        );

        expect(screen.getByRole('button', {name: 'Toggle sidebar'})).toBeInTheDocument();
    });
});
