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
