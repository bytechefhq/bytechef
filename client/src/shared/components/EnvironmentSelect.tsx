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
import {ENVIRONMENT_CONFIGS, type EnvironmentConfigI} from '@/shared/constants/environmentConfigs';
import {useEnvironmentsQuery} from '@/shared/middleware/graphql';
import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {CheckIcon, ChevronDownIcon} from 'lucide-react';
import {useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

interface EnvironmentOptionI {
    config: EnvironmentConfigI;
    id: string;
}

interface EnvironmentSelectPropsI {
    onChange?: (environmentId: number) => void;
    /** `compact` shortens the label to fit the app sidebar header beside the wordmark; `icon` drops the label
     * and chevron entirely for the 56px collapsed rail, leaving the environment's icon as the whole control.
     * Both open the menu rightwards — an end-aligned menu would run off the left edge of the screen from
     * inside the rail. */
    variant?: 'compact' | 'default' | 'icon';
}

const EnvironmentSelect = ({onChange, variant = 'default'}: EnvironmentSelectPropsI = {}) => {
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
    const isCompact = variant === 'compact';
    const isIcon = variant === 'icon';

    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger asChild>
                    <DropdownMenuTrigger asChild>
                        <Button
                            className={twMerge('h-auto gap-1 p-2', isCompact && 'px-1', isIcon && 'p-1')}
                            variant="ghost"
                        >
                            {isIcon ? (
                                <Badge
                                    aria-label={currentConfig.label}
                                    icon={<CurrentIcon className="size-3" />}
                                    styleType={currentConfig.styleType}
                                    weight="semibold"
                                />
                            ) : (
                                <Badge
                                    icon={<CurrentIcon className="size-3" />}
                                    label={isCompact ? currentConfig.shortLabel : currentConfig.label}
                                    styleType={currentConfig.styleType}
                                    weight="semibold"
                                />
                            )}

                            {/* The chevron is the first thing to go when space runs out: the badge alone still
                                reads as a control, and the rail has no room for both. */}

                            {!isIcon && (
                                <ChevronDownIcon
                                    className={twMerge('size-4 text-muted-foreground', isCompact && 'size-3')}
                                />
                            )}
                        </Button>
                    </DropdownMenuTrigger>
                </TooltipTrigger>

                <TooltipContent>{currentConfig.description}</TooltipContent>
            </Tooltip>

            <DropdownMenuContent align={isCompact || isIcon ? 'start' : 'end'} className="w-72">
                <DropdownMenuRadioGroup
                    onValueChange={(value) => {
                        const nextEnvironmentId = +value;

                        setCurrentEnvironmentId(nextEnvironmentId);

                        onChange?.(nextEnvironmentId);
                    }}
                    value={currentEnvironmentId.toString()}
                >
                    {environmentOptions.map(({config, id}) => {
                        const Icon = config.icon;
                        const isSelected = +id === currentEnvironmentId;

                        return (
                            <DropdownMenuRadioItem
                                className="cursor-pointer items-start px-3 py-3 [&>span:first-child]:hidden"
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
