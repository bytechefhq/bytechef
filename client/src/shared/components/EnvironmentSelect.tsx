import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
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
import {useMemo} from 'react';
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

interface EnvironmentOptionI {
    config: EnvironmentConfigI;
    id: string;
}

const EnvironmentSelect = () => {
    const application = useApplicationInfoStore((state) => state.application);

    const {currentEnvironmentId, setCurrentEnvironmentId} = useEnvironmentStore(
        useShallow((state) => ({
            currentEnvironmentId: state.currentEnvironmentId,
            setCurrentEnvironmentId: state.setCurrentEnvironmentId,
        }))
    );

    const {data: environmentsData} = useEnvironmentsQuery();

    const environmentOptions = useMemo(() => {
        if (!environmentsData?.environments) {
            return [];
        }

        return environmentsData.environments.reduce<EnvironmentOptionI[]>((options, environment) => {
            if (environment?.id == null) {
                return options;
            }

            const config = ENVIRONMENT_CONFIGS[+environment.id];

            if (config) {
                options.push({config, id: environment.id});
            }

            return options;
        }, []);
    }, [environmentsData?.environments]);

    if (application?.edition !== 'EE' || environmentOptions.length === 0) {
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
                        <Button className="h-auto gap-1 p-0" variant="ghost">
                            <Badge
                                icon={<CurrentIcon className="size-3" />}
                                label={currentConfig.label}
                                styleType={currentConfig.styleType}
                                weight="semibold"
                            />

                            <ChevronDownIcon className="size-4 text-muted-foreground" />
                        </Button>
                    </DropdownMenuTrigger>
                </TooltipTrigger>

                <TooltipContent>{currentConfig.description}</TooltipContent>
            </Tooltip>

            <DropdownMenuContent align="end" className="w-72">
                <DropdownMenuRadioGroup
                    onValueChange={(value) => setCurrentEnvironmentId(+value)}
                    value={currentEnvironmentId.toString()}
                >
                    {environmentOptions.map(({config, id}) => {
                        const Icon = config.icon;
                        const isSelected = +id === currentEnvironmentId;

                        return (
                            <DropdownMenuRadioItem
                                className="items-start px-3 py-3 [&>span:first-child]:hidden"
                                key={id}
                                value={id}
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

                                    <p className="text-xs font-normal text-muted-foreground">{config.description}</p>
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
