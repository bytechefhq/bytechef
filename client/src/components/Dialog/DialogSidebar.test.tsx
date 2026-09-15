import {render, screen} from '@/shared/util/test-utils';
import {RocketIcon} from 'lucide-react';
import {type ReactNode} from 'react';
import {describe, expect, it} from 'vitest';

import {Dialog, DialogContent, useDialogLayout} from './Dialog';
import {DialogSidebar} from './DialogSidebar';

function LayoutProbe() {
    const {hasSidebar} = useDialogLayout();

    return <span data-testid="layout-probe">{String(hasSidebar)}</span>;
}

function renderInDialog(content: ReactNode) {
    return render(
        <Dialog open>
            <DialogContent aria-describedby={undefined}>{content}</DialogContent>
        </Dialog>
    );
}

describe('DialogSidebar - Content', () => {
    it('should name the dialog with its title', () => {
        renderInDialog(<DialogSidebar title="New Deployment" />);

        expect(screen.getByRole('dialog', {name: 'New Deployment'})).toBeInTheDocument();
        expect(screen.getByRole('heading', {name: 'New Deployment'})).toHaveClass(
            'text-xl',
            'leading-7',
            'font-bold',
            'text-content-neutral-primary'
        );
    });

    it('should describe the dialog with its description', () => {
        render(
            <Dialog open>
                <DialogContent>
                    <DialogSidebar description="Deploy a project version" title="New Deployment" />
                </DialogContent>
            </Dialog>
        );

        expect(screen.getByRole('dialog')).toHaveAccessibleDescription('Deploy a project version');
    });

    it('should render the icon', () => {
        renderInDialog(<DialogSidebar icon={<RocketIcon data-testid="sidebar-icon" />} title="New Deployment" />);

        expect(screen.getByTestId('sidebar-icon')).toBeInTheDocument();
    });

    it('should render children after the heading', () => {
        renderInDialog(
            <DialogSidebar title="New Deployment">
                <p>Steps go here</p>
            </DialogSidebar>
        );

        expect(screen.getByText('Steps go here')).toBeInTheDocument();
    });
});

describe('DialogSidebar - Layout', () => {
    it('should render the sidebar slot with its layout classes', () => {
        renderInDialog(<DialogSidebar title="New Deployment" />);

        const sidebar = screen.getByRole('dialog').querySelector('[data-slot="dialog-sidebar"]');

        expect(sidebar).toHaveClass('hidden', 'w-80', 'shrink-0', 'flex-col', 'gap-4', 'p-6', 'lg:flex');
    });

    it('should register itself in the layout context', () => {
        renderInDialog(
            <>
                <DialogSidebar title="New Deployment" />

                <LayoutProbe />
            </>
        );

        expect(screen.getByTestId('layout-probe')).toHaveTextContent('true');
    });

    it('should merge className', () => {
        renderInDialog(<DialogSidebar className="w-72" title="New Deployment" />);

        const sidebar = screen.getByRole('dialog').querySelector('[data-slot="dialog-sidebar"]');

        expect(sidebar).toHaveClass('w-72');
        expect(sidebar).not.toHaveClass('w-80');
    });
});

describe('DialogSidebar - TypeScript', () => {
    it('should require title', () => {
        // @ts-expect-error - title is required
        const element = <DialogSidebar />;

        expect(element).toBeTruthy();
    });
});
