import './radioCard.css';

import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {BotIcon, CodeIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import {PlatformType, usePlatformTypeStore} from '../stores/usePlatformTypeStore';

interface ModeSelectionDialogProps {
    isDialogOpen: boolean;
    handleDialogClose: () => void;
}

const ModeSelectionDialog = ({handleDialogClose, isDialogOpen}: ModeSelectionDialogProps) => {
    const [selectedType, setSelectedType] = useState<PlatformType | undefined>(undefined);

    const {currentType, setCurrentType} = usePlatformTypeStore(
        useShallow((state) => ({
            currentType: state.currentType,
            setCurrentType: state.setCurrentType,
        }))
    );

    const navigate = useNavigate();

    const radioValue = useMemo(() => (selectedType ?? currentType ?? '').toString(), [currentType, selectedType]);

    const isEmbeddedChecked = useMemo(() => radioValue === PlatformType.EMBEDDED.toString(), [radioValue]);
    const isAutomationChecked = useMemo(() => radioValue === PlatformType.AUTOMATION.toString(), [radioValue]);

    const handleChangePlatformType = useCallback(() => {
        if (selectedType === currentType) {
            handleDialogClose();

            return;
        }

        if (selectedType !== undefined && selectedType !== currentType) {
            setCurrentType(selectedType);

            if (selectedType === PlatformType.AUTOMATION) {
                navigate('/automation');
            } else if (selectedType === PlatformType.EMBEDDED) {
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
                className={twMerge('flex flex-col p-6', currentType === undefined && '[&>button:last-child]:hidden')}
                onEscapeKeyDown={(event) => currentType === undefined && event.preventDefault()}
                onInteractOutside={(event) => currentType === undefined && event.preventDefault()}
            >
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Select how you will use ByteChef</DialogTitle>

                        <DialogDescription>You can always change this configuration</DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <RadioGroup
                    className="flex flex-col gap-4"
                    onValueChange={(value) => setSelectedType(Number(value))}
                    value={radioValue}
                >
                    <Label
                        className={twMerge('radio-card-label group', isEmbeddedChecked && 'is-checked')}
                        htmlFor="embedded"
                    >
                        <div className="flex items-center gap-4 p-4 pr-2">
                            <div>
                                <CodeIcon size={48} />
                            </div>

                            <div className="flex grow flex-col gap-1">
                                <p className="text-base font-semibold">Embedded</p>

                                <p className="text-sm font-normal text-content-neutral-secondary">
                                    Allow your users to integrate your product with applications they use.
                                </p>
                            </div>
                        </div>

                        <div
                            className={twMerge(
                                'radio-card-indicator-container',
                                isEmbeddedChecked ? 'is-checked' : 'is-unchecked'
                            )}
                        >
                            <RadioGroupItem
                                className={twMerge('radio-card-indicator', isEmbeddedChecked && 'is-checked')}
                                data-testid="embedded"
                                id="embedded"
                                value={PlatformType.EMBEDDED.toString()}
                            />
                        </div>
                    </Label>

                    <Label
                        className={twMerge('radio-card-label group', isAutomationChecked && 'is-checked')}
                        htmlFor="automation"
                    >
                        <div className="flex items-center gap-6 p-4 pr-2">
                            <div>
                                <BotIcon size={48} />
                            </div>

                            <div className="flex grow flex-col gap-1">
                                <p className="text-base font-semibold">Automation</p>

                                <p className="text-sm font-normal text-content-neutral-secondary">
                                    Integrate applications and automate processes inside your organization.
                                </p>
                            </div>
                        </div>

                        <div
                            className={twMerge(
                                'radio-card-indicator-container',
                                isAutomationChecked ? 'is-checked' : 'is-unchecked'
                            )}
                        >
                            <RadioGroupItem
                                className={twMerge('radio-card-indicator', isAutomationChecked && 'is-checked')}
                                data-testid="automation"
                                id="automation"
                                value={PlatformType.AUTOMATION.toString()}
                            />
                        </div>
                    </Label>
                </RadioGroup>

                <DialogFooter>
                    {currentType !== undefined && (
                        <Button
                            aria-label="cancel"
                            label="Cancel"
                            onClick={handleDialogClose}
                            type="button"
                            variant="outline"
                        />
                    )}

                    <Button
                        aria-label="confirm"
                        className="bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-active"
                        disabled={selectedType === undefined}
                        label="Confirm"
                        onClick={handleChangePlatformType}
                        type="button"
                    />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ModeSelectionDialog;
