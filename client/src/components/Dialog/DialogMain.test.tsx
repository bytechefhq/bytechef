import {render, screen, userEvent} from '@/shared/util/test-utils';
import {RocketIcon} from 'lucide-react';
import {describe, expect, it, vi} from 'vitest';

import {Dialog, DialogContent} from './Dialog';
import {DialogBody, DialogFooter, DialogHeader, DialogMain} from './DialogMain';
import {DialogSidebar} from './DialogSidebar';
import {type DialogStepI, DialogStepsProvider} from './DialogStepsProvider';
import {useDialogSteps} from './hooks/useDialogSteps';

const steps: DialogStepI[] = [
    {id: 'basics', label: 'Basics'},
    {id: 'workflows', label: 'Workflows'},
    {id: 'review', label: 'Review'},
];

function NextStepProbe() {
    const {goToNextStep} = useDialogSteps();

    return (
        <button onClick={() => goToNextStep()} type="button">
            Next step probe
        </button>
    );
}

function renderWizard() {
    return render(
        <Dialog open>
            <DialogContent aria-describedby={undefined} hasSidebar>
                <DialogStepsProvider steps={steps}>
                    <DialogSidebar title="New Deployment" />

                    <DialogMain>
                        <DialogHeader />

                        <DialogBody>
                            <NextStepProbe />
                        </DialogBody>
                    </DialogMain>
                </DialogStepsProvider>
            </DialogContent>
        </Dialog>
    );
}

describe('DialogHeader - One dialog title', () => {
    it('should be the dialog title in a main-only dialog', () => {
        render(
            <Dialog open>
                <DialogContent aria-describedby={undefined}>
                    <DialogMain>
                        <DialogHeader title="Edit Skill" />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        expect(screen.getByRole('dialog', {name: 'Edit Skill'})).toBeInTheDocument();
    });

    it('should stay the only dialog title next to a sidebar', () => {
        renderWizard();

        const dialog = screen.getByRole('dialog', {name: 'Basics'});
        const titleId = dialog.getAttribute('aria-labelledby');

        expect(document.querySelectorAll(`[id="${titleId}"]`)).toHaveLength(1);
        expect(screen.getByRole('heading', {name: 'Basics'})).toHaveAttribute('id', titleId);
    });

    it('should name the dialog with the sidebar title at every width', () => {
        render(
            <Dialog open>
                <DialogContent aria-describedby={undefined} hasSidebar>
                    <DialogMain>
                        <DialogHeader dialogTitle="New Deployment" title="Basics" />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        expect(screen.getByRole('dialog', {name: 'New Deployment - Basics'})).toBeInTheDocument();
        expect(screen.getByText('New Deployment -')).toHaveClass('lg:hidden');
        expect(screen.getByText('Basics')).toBeInTheDocument();
    });

    it('should describe the dialog with the sidebar description at every width', () => {
        render(
            <Dialog open>
                <DialogContent hasSidebar>
                    <DialogMain>
                        <DialogHeader dialogDescription="Deploy a project version" dialogTitle="New Deployment" />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        expect(screen.getByRole('dialog')).toHaveAccessibleDescription('Deploy a project version');
        expect(screen.getByRole('dialog').querySelector('[data-slot="dialog-description"]')).toHaveClass('lg:sr-only');
    });

    it('should describe a main-only dialog with its description', () => {
        render(
            <Dialog open>
                <DialogContent>
                    <DialogMain>
                        <DialogHeader description="Update the skill instructions" title="Edit Skill" />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        expect(screen.getByRole('dialog')).toHaveAccessibleDescription('Update the skill instructions');
    });
});

describe('DialogHeader - Steps', () => {
    it('should default the title to the current step label', () => {
        renderWizard();

        expect(screen.getByRole('heading', {name: 'Basics'})).toHaveClass('text-xl', 'leading-7', 'font-medium');
    });

    it('should show the step position below lg when a sidebar is present', () => {
        renderWizard();

        expect(screen.getByText('Step 1 of 3')).toHaveClass('lg:hidden');
    });

    it('should not show the step position without a sidebar', () => {
        render(
            <Dialog open>
                <DialogContent aria-describedby={undefined}>
                    <DialogStepsProvider steps={steps}>
                        <DialogMain>
                            <DialogHeader />
                        </DialogMain>
                    </DialogStepsProvider>
                </DialogContent>
            </Dialog>
        );

        expect(screen.queryByText('Step 1 of 3')).not.toBeInTheDocument();
    });

    it('should move focus to the heading when the step changes', async () => {
        renderWizard();

        await userEvent.click(screen.getByRole('button', {name: 'Next step probe'}));

        expect(screen.getByRole('heading', {name: 'Workflows'})).toHaveFocus();
    });
});

describe('DialogHeader - Slots', () => {
    it('should render the icon, end content and close button', () => {
        render(
            <Dialog open>
                <DialogContent aria-describedby={undefined}>
                    <DialogMain>
                        <DialogHeader
                            endContent={<span>Advanced mode</span>}
                            icon={<RocketIcon data-testid="header-icon" />}
                            title="Edit Skill"
                        />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        expect(screen.getByTestId('header-icon')).toBeInTheDocument();
        expect(screen.getByText('Advanced mode')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
    });

    it('should close the dialog from the close button', async () => {
        const handleOpenChange = vi.fn();

        render(
            <Dialog onOpenChange={handleOpenChange} open>
                <DialogContent aria-describedby={undefined}>
                    <DialogMain>
                        <DialogHeader title="Edit Skill" />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(handleOpenChange).toHaveBeenCalledWith(false);
    });

    it('should hide the close button when showCloseButton is false', () => {
        render(
            <Dialog open>
                <DialogContent aria-describedby={undefined}>
                    <DialogMain>
                        <DialogHeader showCloseButton={false} title="Edit Skill" />
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );

        expect(screen.queryByRole('button', {name: 'Close'})).not.toBeInTheDocument();
    });
});

describe('DialogMain, DialogBody, DialogFooter', () => {
    function renderMainOnly() {
        return render(
            <Dialog open>
                <DialogContent aria-describedby={undefined}>
                    <DialogMain>
                        <DialogHeader title="Edit Skill" />

                        <DialogBody>Body content</DialogBody>

                        <DialogFooter startContent={<span>Group Connections</span>}>
                            <button type="button">Save</button>
                        </DialogFooter>
                    </DialogMain>
                </DialogContent>
            </Dialog>
        );
    }

    it('should render the main card, body and footer with their tokens', () => {
        renderMainOnly();

        const dialog = screen.getByRole('dialog');

        expect(dialog.querySelector('[data-slot="dialog-main"]')).toHaveClass(
            'rounded-lg',
            'border',
            'border-stroke-neutral-primary',
            'bg-surface-neutral-primary',
            'sm:min-w-[512px]'
        );
        expect(dialog.querySelector('[data-slot="dialog-body"]')).toHaveClass(
            'min-h-0',
            'flex-1',
            'overflow-y-auto',
            'px-4',
            'pb-4'
        );
        expect(dialog.querySelector('[data-slot="dialog-footer"]')).toHaveClass(
            'border-t',
            'border-stroke-neutral-primary',
            'px-4',
            'py-2.5'
        );
    });

    it('should put startContent before the footer actions', () => {
        renderMainOnly();

        const footer = screen.getByRole('dialog').querySelector('[data-slot="dialog-footer"]') as HTMLElement;
        const [startGroup, actionGroup] = Array.from(footer.children);

        expect(startGroup).toHaveTextContent('Group Connections');
        expect(actionGroup).toHaveTextContent('Save');
    });
});
