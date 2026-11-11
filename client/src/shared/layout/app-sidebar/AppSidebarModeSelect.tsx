import reactLogo from '@/assets/logo.svg';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {PLATFORM_MODES, getPlatformModePath} from '@/pages/home/constants/platformModes';
import {PlatformType, usePlatformTypeStore} from '@/pages/home/stores/usePlatformTypeStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {CheckIcon, ChevronDownIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

export const AppSidebarLogo = () => (
    <span className="flex size-10 shrink-0 items-center justify-center">
        <img alt="ByteChef" className="size-8 max-w-none shrink-0" src={reactLogo} />
    </span>
);

interface AppSidebarModeSelectProps {
    currentMode: PlatformType;
}

export function AppSidebarModeSelect({currentMode}: AppSidebarModeSelectProps) {
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
                    aria-label="Switch mode"
                    className="flex shrink-0 items-center rounded-md pr-1 group-data-[collapsible=icon]:pr-0 hover:bg-sidebar-accent data-[state=open]:bg-sidebar-accent"
                    type="button"
                >
                    <AppSidebarLogo />

                    <ChevronDownIcon className="size-3.5 text-muted-foreground group-data-[collapsible=icon]:hidden" />
                </button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="start" className="w-72">
                <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">Mode</DropdownMenuLabel>

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

                        {mode.type === currentMode && (
                            <CheckIcon aria-label="Current mode" className="size-4 shrink-0" />
                        )}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
