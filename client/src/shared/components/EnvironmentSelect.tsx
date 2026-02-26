import Badge from '@/components/Badge/Badge';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {BoxIcon, CheckIcon, ChevronDownIcon, FlaskConicalIcon, type LucideIcon, WrenchIcon} from 'lucide-react';
import {useShallow} from 'zustand/react/shallow';

interface EnvironmentConfigI {
    description: string;
    icon: LucideIcon;
    label: string;
    styleType: 'primary-outline' | 'secondary-outline' | 'warning-outline';
}

const ENVIRONMENT_CONFIGS: Record<number, EnvironmentConfigI> = {
    [DEVELOPMENT_ENVIRONMENT]: {
        description: 'Features are unstable, experimental, and may change or break frequently.',
        icon: WrenchIcon,
        label: 'DEVELOPMENT',
        styleType: 'secondary-outline',
    },
    [PRODUCTION_ENVIRONMENT]: {
        description: 'Live environment used by real users. Optimized for performance with strict safeguards.',
        icon: BoxIcon,
        label: 'PRODUCTION',
        styleType: 'primary-outline',
    },
    [STAGING_ENVIRONMENT]: {
        description: 'Used for final testing, QA, and validation before release.',
        icon: FlaskConicalIcon,
        label: 'STAGING',
        styleType: 'warning-outline',
    },
};

const EnvironmentSelect = () => {
    const application = useApplicationInfoStore((state) => state.application);

    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );

    const {data: environmentsData} = useEnvironmentsQuery();

    if (application?.edition !== 'EE' || !environmentsData?.environments) {
        return null;
    }

    const currentConfig = ENVIRONMENT_CONFIGS[currentEnvironmentId];

    if (!currentConfig) {
        return null;
    }

    const CurrentIcon = currentConfig.icon;

    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger asChild>
                    <DropdownMenuTrigger asChild>
                        <button className="flex items-center gap-1">
                            <Badge
                                icon={<CurrentIcon className="size-3" />}
                                label={currentConfig.label}
                                styleType={currentConfig.styleType}
                                weight="semibold"
                            />

                            <ChevronDownIcon className="size-4 text-muted-foreground" />
                        </button>
                    </DropdownMenuTrigger>
                </TooltipTrigger>

                <TooltipContent>{currentConfig.description}</TooltipContent>
            </Tooltip>

            <DropdownMenuContent align="end" className="w-72">
                <DropdownMenuRadioGroup
                    onValueChange={(value) => setCurrentEnvironmentId(+value)}
                    value={currentEnvironmentId.toString()}
                >
                    {environmentsData.environments
                        .filter((environment) => environment?.id != null)
                        .map((environment) => {
                            const environmentId = environment!.id!;
                            const config = ENVIRONMENT_CONFIGS[+environmentId];

                            if (!config) {
                                return null;
                            }

                            const Icon = config.icon;
                            const isSelected = +environmentId === currentEnvironmentId;

                            return (
                                <DropdownMenuRadioItem
                                    className="items-start px-3 py-3 [&>span:first-child]:hidden"
                                    key={environmentId}
                                    value={environmentId}
                                >
                                    <div className="flex flex-1 flex-col gap-1">
                                        <div className="flex items-center justify-between">
                                            <Badge
                                                icon={<Icon className="size-3" />}
                                                label={config.label}
                                                styleType={config.styleType}
                                                weight="semibold"
                                            />

                                            {isSelected && <CheckIcon className="size-4 text-muted-foreground" />}
                                        </div>

                                        <p className="text-xs font-normal text-muted-foreground">
                                            {config.description}
                                        </p>
                                    </div>
                                </DropdownMenuRadioItem>
                            );
                        })}
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default EnvironmentSelect;
