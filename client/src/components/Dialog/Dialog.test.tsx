import {DialogTitle as ShadcnDialogTitle} from '@/components/ui/dialog';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {type ReactNode, useLayoutEffect} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {Dialog, DialogClose, DialogContent, DialogTrigger, useDialogLayout} from './Dialog';

function SidebarProbe() {
    const {registerSidebar} = useDialogLayout();

    useLayoutEffect(() => registerSidebar(), [registerSidebar]);

    return null;
}

function LayoutProbe() {
    const {hasSidebar} = useDialogLayout();

    return <span data-testid="layout-probe">{String(hasSidebar)}</span>;
}

function renderOpenDialog(content?: ReactNode, className?: string) {
    return render(
        <Dialog open>
            <DialogContent aria-describedby={undefined} className={className}>
                <ShadcnDialogTitle>Title</ShadcnDialogTitle>

                {content}
            </DialogContent>
        </Dialog>
    );
}

describe('DialogContent - Surface', () => {
    it('should replace the shadcn surface with the ByteChef base classes', () => {
        renderOpenDialog();

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveAttribute('data-slot', 'dialog-content');
        expect(dialog).toHaveClass('flex', 'rounded-lg', 'border-0', 'bg-transparent', 'p-0', 'shadow-xl');
        expect(dialog).not.toHaveClass('grid');
        expect(dialog).not.toHaveClass('p-6');
        expect(dialog).not.toHaveClass('sm:max-w-lg');
    });

    it('should switch to the sidebar layout from lg up when a sidebar is present', () => {
        renderOpenDialog();

        expect(screen.getByRole('dialog')).toHaveClass(
            'lg:has-[[data-slot=dialog-sidebar]]:h-[648px]',
            'lg:has-[[data-slot=dialog-sidebar]]:w-[860px]',
            'lg:has-[[data-slot=dialog-sidebar]]:gap-2',
            'lg:has-[[data-slot=dialog-sidebar]]:bg-surface-main',
            'lg:has-[[data-slot=dialog-sidebar]]:p-2'
        );
    });

    it('should merge className', () => {
        renderOpenDialog(undefined, 'max-h-96');

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveClass('max-h-96', 'flex');
        expect(dialog).not.toHaveClass('max-h-[calc(100dvh-2rem)]');
    });
});

describe('DialogContent - Layout context', () => {
    it('should report no sidebar by default', () => {
        renderOpenDialog(<LayoutProbe />);

        expect(screen.getByTestId('layout-probe')).toHaveTextContent('false');
    });

    it('should report a sidebar once one registers', () => {
        renderOpenDialog(
            <>
                <SidebarProbe />

                <LayoutProbe />
            </>
        );

        expect(screen.getByTestId('layout-probe')).toHaveTextContent('true');
    });
});

describe('Dialog - Open state', () => {
    it('should open from DialogTrigger', async () => {
        render(
            <Dialog>
                <DialogTrigger>Open dialog</DialogTrigger>

                <DialogContent aria-describedby={undefined}>
                    <ShadcnDialogTitle>Title</ShadcnDialogTitle>
                </DialogContent>
            </Dialog>
        );

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByText('Open dialog'));

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('should close from DialogClose', async () => {
        const handleOpenChange = vi.fn();

        render(
            <Dialog onOpenChange={handleOpenChange} open>
                <DialogContent aria-describedby={undefined}>
                    <ShadcnDialogTitle>Title</ShadcnDialogTitle>

                    <DialogClose>Close dialog</DialogClose>
                </DialogContent>
            </Dialog>
        );

        await userEvent.click(screen.getByText('Close dialog'));

        expect(handleOpenChange).toHaveBeenCalledWith(false);
    });
});
