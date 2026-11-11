import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import {PLATFORM_MODES, getPlatformModePath} from '@/pages/home/constants/platformModes';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ArrowRightIcon, CheckIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

import {PlatformType, usePlatformTypeStore} from '../stores/usePlatformTypeStore';

interface ModeSelectionDialogProps {
    isDialogOpen: boolean;
    handleDialogClose: () => void;
}

const ModeSelectionDialog = ({handleDialogClose, isDialogOpen}: ModeSelectionDialogProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCurrentType = usePlatformTypeStore((state) => state.setCurrentType);

    const navigate = useNavigate();

    const handleModeClick = (type: PlatformType) => {
        setCurrentType(type);

        handleDialogClose();

        void navigate(type === PlatformType.EMBEDDED ? '/embedded' : getPlatformModePath(type, currentEnvironmentId));
    };

    return (
        <Dialog open={isDialogOpen}>
            <DialogContent
                onEscapeKeyDown={(event) => event.preventDefault()}
                onInteractOutside={(event) => event.preventDefault()}
                size="lg"
            >
                <DialogMain>
                    <DialogHeader
                        description="Pick where you want to start. You can switch anytime from the logo menu at the top of the sidebar."
                        showCloseButton={false}
                        title="How will you use ByteChef?"
                    />

                    <DialogBody>
                        <div className="grid gap-4 sm:grid-cols-2">
                            {PLATFORM_MODES.map((mode) => (
                                <button
                                    aria-label={`Start with ${mode.title}`}
                                    className="group flex flex-col gap-4 rounded-lg border border-stroke-neutral-secondary p-5 text-left transition-colors hover:border-stroke-brand-primary hover:bg-surface-brand-secondary focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
                                    key={mode.type}
                                    onClick={() => handleModeClick(mode.type)}
                                    type="button"
                                >
                                    <span className="flex size-10 items-center justify-center rounded-md bg-surface-neutral-secondary text-content-brand-primary">
                                        <mode.icon className="size-6" />
                                    </span>

                                    <span className="flex flex-col gap-1">
                                        <span className="text-base font-semibold">{mode.title}</span>

                                        <span className="text-sm text-content-neutral-secondary">
                                            {mode.description}
                                        </span>
                                    </span>

                                    <ul className="flex flex-1 flex-col gap-2">
                                        {mode.highlights.map((highlight) => (
                                            <li className="flex items-start gap-2 text-sm" key={highlight}>
                                                <CheckIcon className="mt-0.5 size-4 shrink-0 text-content-brand-primary" />

                                                <span>{highlight}</span>
                                            </li>
                                        ))}
                                    </ul>

                                    <span className="flex items-center gap-1 text-sm font-medium text-content-brand-primary">
                                        {`Start with ${mode.title}`}

                                        <ArrowRightIcon className="size-4 transition-transform group-hover:translate-x-0.5" />
                                    </span>
                                </button>
                            ))}
                        </div>
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default ModeSelectionDialog;
