import {render, screen, userEvent} from '@/shared/util/test-utils';
import {type ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {
    Dialog,
    DialogClose,
    DialogContent,
    type DialogContentSizeType,
    DialogDescription,
    DialogTitle,
    DialogTrigger,
    useDialogLayout,
} from './Dialog';

function LayoutProbe() {
    const {hasSidebar} = useDialogLayout();

    return <span data-testid="layout-probe">{String(hasSidebar)}</span>;
}

function renderOpenDialog(content?: ReactNode, className?: string, hasSidebar?: boolean, size?: DialogContentSizeType) {
    return render(
        <Dialog open>
            <DialogContent aria-describedby={undefined} className={className} hasSidebar={hasSidebar} size={size}>
                <DialogTitle>Title</DialogTitle>

                {content}
            </DialogContent>
        </Dialog>
    );
}

describe('DialogContent - Surface', () => {
    it('should render a bare ByteChef shell and leave the surface to DialogMain', () => {
        renderOpenDialog();

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveAttribute('data-slot', 'dialog-content');
        expect(dialog).toHaveClass('flex', 'rounded-lg', 'shadow-xl');
        expect(dialog).not.toHaveClass('grid');
        expect(dialog).not.toHaveClass('p-6');
        expect(dialog).not.toHaveClass('bg-background');
        expect(dialog).not.toHaveClass('border');
        expect(dialog).not.toHaveClass('sm:max-w-lg');
    });

    it('should center itself in the viewport', () => {
        renderOpenDialog();

        expect(screen.getByRole('dialog')).toHaveClass(
            'fixed',
            'top-[50%]',
            'left-[50%]',
            'z-50',
            'translate-x-[-50%]',
            'translate-y-[-50%]'
        );
    });

    it('should render a dimmed overlay behind the content', () => {
        renderOpenDialog();

        expect(document.querySelector('[data-slot="dialog-overlay"]')).toHaveClass('fixed', 'inset-0', 'bg-black/50');
    });

    it('should switch to the sidebar layout from lg up when hasSidebar is set', () => {
        renderOpenDialog(undefined, undefined, true);

        expect(screen.getByRole('dialog')).toHaveClass(
            'lg:h-[648px]',
            'lg:w-[860px]',
            'lg:gap-2',
            'lg:bg-surface-main',
            'lg:p-2'
        );
    });

    it('should keep the single card layout without hasSidebar', () => {
        renderOpenDialog();

        expect(screen.getByRole('dialog')).not.toHaveClass('lg:w-[860px]');
    });

    it('should size the dialog to 512 px from sm up by default', () => {
        renderOpenDialog();

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveClass('w-full', 'sm:w-[512px]');
        expect(dialog).not.toHaveClass('sm:w-auto');
    });

    it.each([
        ['md', 'sm:w-[640px]'],
        ['lg', 'sm:w-[800px]'],
        ['xl', 'sm:w-[1000px]'],
    ] as [DialogContentSizeType, string][])('should apply the %s width', (size, widthClass) => {
        renderOpenDialog(undefined, undefined, undefined, size);

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveClass(widthClass);
        expect(dialog).not.toHaveClass('sm:w-[512px]');
    });

    it('should keep the sidebar width from lg up next to the size width', () => {
        renderOpenDialog(undefined, undefined, true, 'md');

        expect(screen.getByRole('dialog')).toHaveClass('sm:w-[640px]', 'lg:w-[860px]');
    });

    it('should let className override the size width', () => {
        renderOpenDialog(undefined, 'sm:w-[900px]');

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveClass('sm:w-[900px]');
        expect(dialog).not.toHaveClass('sm:w-[512px]');
    });

    // The scale lives at the sm breakpoint, so an unprefixed width is a different Tailwind group and twMerge keeps
    // both. Without the custom size a full-bleed dialog collapses to 512 px from 640 px up.
    it('should emit no width for the custom size so an unprefixed className width survives', () => {
        renderOpenDialog(undefined, 'w-[calc(100vw-80px)]', undefined, 'custom');

        const dialog = screen.getByRole('dialog');

        expect(dialog).toHaveClass('w-[calc(100vw-80px)]');
        expect(dialog).not.toHaveClass('sm:w-[512px]');
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

    it('should report a sidebar when hasSidebar is set', () => {
        renderOpenDialog(<LayoutProbe />, undefined, true);

        expect(screen.getByTestId('layout-probe')).toHaveTextContent('true');
    });
});

describe('Dialog - Open state', () => {
    it('should open from DialogTrigger', async () => {
        render(
            <Dialog>
                <DialogTrigger>Open dialog</DialogTrigger>

                <DialogContent aria-describedby={undefined}>
                    <DialogTitle>Title</DialogTitle>
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
                    <DialogTitle>Title</DialogTitle>

                    <DialogClose>Close dialog</DialogClose>
                </DialogContent>
            </Dialog>
        );

        await userEvent.click(screen.getByText('Close dialog'));

        expect(handleOpenChange).toHaveBeenCalledWith(false);
    });
});

describe('Dialog - Accessible name', () => {
    it('should name and describe the dialog from DialogTitle and DialogDescription', () => {
        render(
            <Dialog open>
                <DialogContent>
                    <DialogTitle>Rename workflow</DialogTitle>

                    <DialogDescription>Pick a new name.</DialogDescription>
                </DialogContent>
            </Dialog>
        );

        const dialog = screen.getByRole('dialog', {name: 'Rename workflow'});

        expect(dialog).toHaveAccessibleDescription('Pick a new name.');
        expect(screen.getByText('Rename workflow')).toHaveAttribute('data-slot', 'dialog-title');
        expect(screen.getByText('Pick a new name.')).toHaveAttribute('data-slot', 'dialog-description');
    });
});
