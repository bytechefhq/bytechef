import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {BotIcon, CodeIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

import {ModeType, useModeTypeStore} from './stores/useModeTypeStore';

interface ModeSelectionDialogProps {
    isDialogOpen: boolean;
    handleDialogClose: () => void;
}

const ModeSelectionDialog = ({handleDialogClose, isDialogOpen}: ModeSelectionDialogProps) => {
    const [selectedType, setSelectedType] = useState<ModeType | undefined>(undefined);

    const {currentType, setCurrentType} = useModeTypeStore();

    const navigate = useNavigate();

    const radioValue = (selectedType ?? currentType ?? '').toString();

    const handleChangeModeType = useCallback(() => {
        if (selectedType === currentType) {
            handleDialogClose();

            return;
        }

        if (selectedType !== undefined && selectedType !== currentType) {
            setCurrentType(selectedType);

            if (selectedType === ModeType.AUTOMATION) {
                navigate('/automation');
            } else if (selectedType === ModeType.EMBEDDED) {
                navigate('/embedded');
            }

            handleDialogClose();
        }
    }, [currentType, handleDialogClose, navigate, selectedType, setCurrentType]);

    useEffect(() => {
        if (!isDialogOpen && currentType !== undefined) {
            setSelectedType(undefined);
        }
    }, [isDialogOpen, currentType]);

    return (
        <Dialog onOpenChange={handleDialogClose} open={isDialogOpen}>
            <DialogContent
                className={twMerge(
                    'gap- flex w-fit flex-col p-6',
                    currentType === undefined && '[&>button:last-child]:hidden'
                )}
                onEscapeKeyDown={(event) => currentType === undefined && event.preventDefault()}
                onInteractOutside={(event) => currentType === undefined && event.preventDefault()}
            >
                <DialogHeader>
                    <DialogTitle className="text-xl">Select how you will use ByteChef</DialogTitle>

                    <DialogDescription>You can change always change this configuration</DialogDescription>
                </DialogHeader>

                <RadioGroup
                    className="flex flex-col gap-4"
                    onValueChange={(value) => setSelectedType(Number(value))}
                    value={radioValue}
                >
                    <Label
                        className="group flex cursor-pointer rounded-lg border border-stroke-neutral-primary hover:border-stroke-brand-secondary-hover [&:has([data-state=checked])]:border-stroke-brand-primary"
                        htmlFor="embedded"
                    >
                        <div className="flex items-center gap-6 p-4 pr-2">
                            <CodeIcon size={48} />

                            <div className="flex grow flex-col gap-1">
                                <p className="text-xl font-bold">Embedded</p>

                                <p className="text-sm font-normal text-content-neutral-secondary">
                                    Allow your users to integrate your product with applications they use.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-center rounded-e-lg border-l border-stroke-neutral-primary bg-surface-neutral-secondary p-4 [&:has([data-state=checked])]:border-stroke-brand-primary [&:has([data-state=checked])]:bg-surface-brand-secondary group-hover:[&:has([data-state=unchecked])]:border-stroke-brand-secondary-hover">
                            <RadioGroupItem
                                className="border-stroke-neutral-secondary bg-background hover:border-stroke-brand-secondary-hover [&:has([data-state=checked])>span>svg]:absolute [&:has([data-state=checked])>span>svg]:size-6 [&:has([data-state=checked])>span>svg]:text-surface-brand-primary [&:has([data-state=checked])]:border-stroke-brand-primary"
                                data-testid="embedded"
                                id="embedded"
                                value={ModeType.EMBEDDED.toString()}
                            />
                        </div>
                    </Label>

                    <Label
                        className="group flex cursor-pointer rounded-lg border border-stroke-neutral-primary hover:border-stroke-brand-secondary-hover [&:has([data-state=checked])]:border-stroke-brand-primary"
                        htmlFor="automation"
                    >
                        <div className="flex items-center gap-6 p-4 pr-2">
                            <BotIcon size={48} />

                            <div className="flex grow flex-col gap-1">
                                <p className="text-xl font-bold">Automation</p>

                                <p className="text-sm font-normal text-content-neutral-secondary">
                                    Integrate applications and automate processes inside your organization.
                                </p>
                            </div>
                        </div>

                        <div className="flex items-center rounded-e-lg border-l border-stroke-neutral-primary bg-surface-neutral-secondary p-4 [&:has([data-state=checked])]:border-stroke-brand-primary [&:has([data-state=checked])]:bg-surface-brand-secondary group-hover:[&:has([data-state=unchecked])]:border-stroke-brand-secondary-hover">
                            <RadioGroupItem
                                className="border-stroke-neutral-secondary bg-background hover:border-stroke-brand-secondary-hover [&:has([data-state=checked])>span>svg]:absolute [&:has([data-state=checked])>span>svg]:size-6 [&:has([data-state=checked])>span>svg]:text-surface-brand-primary [&:has([data-state=checked])]:border-stroke-brand-primary"
                                data-testid="automation"
                                id="automation"
                                value={ModeType.AUTOMATION.toString()}
                            />
                        </div>
                    </Label>
                </RadioGroup>

                <DialogFooter>
                    {currentType !== undefined && (
                        <Button aria-label="cancel" onClick={handleDialogClose} type="button" variant="outline">
                            Cancel
                        </Button>
                    )}

                    <Button
                        aria-label="confirm"
                        className="bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                        disabled={selectedType === undefined}
                        onClick={handleChangeModeType}
                        type="button"
                    >
                        Confirm
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ModeSelectionDialog;
