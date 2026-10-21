import reactLogo from '@/assets/logo.svg';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {PLATFORM_MODES, getPlatformModePath} from '@/pages/home/constants/platformModes';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {ChevronDownIcon, CircleCheckIcon, ClipboardCheckIcon, LayoutGridIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

export const AppSidebarLogo = () => (
    <span className="flex size-10 shrink-0 items-center justify-center">
        <img alt="ByteChef" className="size-8 max-w-none shrink-0" src={reactLogo} />
    </span>
);

interface AppSidebarModeSelectProps {
    currentMode: PlatformType;
    showModes?: boolean;
}

export function AppSidebarModeSelect({currentMode, showModes = true}: AppSidebarModeSelectProps) {
    const edition = useApplicationInfoStore((state) => state.application?.edition);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const setCurrentType = usePlatformTypeStore((state) => state.setCurrentType);

    const navigate = useNavigate();

    const handleModeSelect = (type: PlatformType) => {
        if (type === currentMode) {
            return;
        }

        setCurrentType(type);

        void navigate(getPlatformModePath(type, currentEnvironmentId));
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <button
                    aria-label={showModes ? 'Switch mode' : 'ByteChef menu'}
                    className="flex shrink-0 items-center rounded-md pr-1 group-data-[collapsible=icon]:pr-0 hover:bg-sidebar-accent data-[state=open]:bg-sidebar-accent"
                    type="button"
                >
                    <AppSidebarLogo />

                    <ChevronDownIcon className="size-3.5 text-muted-foreground group-data-[collapsible=icon]:hidden" />
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72">
                {showModes && (
                    <>
                        <DropdownMenuLabel className="pl-10 text-xs font-normal text-muted-foreground">
                            Mode
                        </DropdownMenuLabel>

                        {PLATFORM_MODES.map((mode) => (
                            <DropdownMenuItem
                                className="cursor-pointer items-center gap-3 py-2"
                                key={mode.type}
                                onClick={() => handleModeSelect(mode.type)}
                            >
                                <mode.icon className="size-5 shrink-0" />

                                <div className="flex min-w-0 flex-1 flex-col">
                                    <span className="font-medium">{mode.title}</span>

                                    <span className="text-xs text-muted-foreground">{mode.description}</span>
                                </div>

                                {mode.type === currentMode ? (
                                    <CircleCheckIcon
                                        aria-label="Current mode"
                                        className="size-4 shrink-0 text-content-brand-primary"
                                    />
                                ) : (
                                    <span aria-hidden="true" className="size-4 shrink-0" />
                                )}
                            </DropdownMenuItem>
                        ))}
                    </>
                )}

                {currentMode === PlatformType.AUTOMATION && (
                    <>
                        {showModes && <DropdownMenuSeparator />}

                        <DropdownMenuItem
                            className="cursor-pointer items-center gap-3 py-2"
                            onClick={() => navigate('/automation/approval-tasks')}
                        >
                            <ClipboardCheckIcon className="size-5 shrink-0" />

                            <span className="font-medium">Approval Tasks</span>
                        </DropdownMenuItem>

                        {edition === EditionType.EE && (
                            <DropdownMenuItem
                                className="cursor-pointer items-center gap-3 py-2"
                                onClick={() => navigate('/automation/portal')}
                            >
                                <LayoutGridIcon className="size-5 shrink-0" />

                                <span className="font-medium">App Portal</span>
                            </DropdownMenuItem>
                        )}
                    </>
                )}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
