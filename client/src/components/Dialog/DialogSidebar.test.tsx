import {DialogTitle as ShadcnDialogTitle} from '@/components/ui/dialog';
import {render, screen} from '@/shared/util/test-utils';
import {RocketIcon} from 'lucide-react';
import {type ReactNode} from 'react';
import {describe, expect, it} from 'vitest';

import {Dialog, DialogContent} from './Dialog';
import {DialogSidebar} from './DialogSidebar';

function renderInDialog(content: ReactNode) {
    return render(
        <Dialog open>
            <DialogContent aria-describedby={undefined} hasSidebar>
                <ShadcnDialogTitle>Deployment</ShadcnDialogTitle>

                {content}
            </DialogContent>
        </Dialog>
    );
}

describe('DialogSidebar - Content', () => {
    it('should render its title with the sidebar heading tokens', () => {
        renderInDialog(<DialogSidebar title="New Deployment" />);

        expect(screen.getByText('New Deployment')).toHaveClass(
            'text-xl',
            'leading-7',
            'font-bold',
            'text-content-neutral-primary'
        );
    });

    it('should leave the dialog title and description to DialogHeader', () => {
        renderInDialog(<DialogSidebar description="Deploy a project version" title="New Deployment" />);

        expect(screen.getByRole('dialog', {name: 'Deployment'})).toBeInTheDocument();
        expect(screen.getByText('New Deployment')).toHaveAttribute('aria-hidden', 'true');
        expect(screen.getByText('Deploy a project version')).toHaveAttribute('aria-hidden', 'true');
    });

    it('should render the description with the secondary tokens', () => {
        renderInDialog(<DialogSidebar description="Deploy a project version" title="New Deployment" />);

        expect(screen.getByText('Deploy a project version')).toHaveClass('text-sm', 'text-content-neutral-secondary');
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
