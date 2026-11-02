import Button from '@/components/Button/Button';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {useAutomationWorkflowProjectConfigurationsQuery} from '@/shared/middleware/graphql';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {PlusIcon} from 'lucide-react';

interface AutomationWorkflowProjectConfigurationCreateButtonProps {
    onClick: () => void;
    projectId: string;
}

const AutomationWorkflowProjectConfigurationCreateButton = ({
    onClick,
    projectId,
}: AutomationWorkflowProjectConfigurationCreateButtonProps) => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {data: configurationsData} = useAutomationWorkflowProjectConfigurationsQuery({projectId});

    const environmentTaken = (configurationsData?.automationWorkflowProjectConfigurations ?? []).some(
        (configuration) => configuration.environmentId === String(currentEnvironmentId)
    );

    const button = (
        <Button
            aria-label="New Configuration"
            disabled={environmentTaken}
            onClick={onClick}
            size="sm"
            variant="secondary"
        >
            <PlusIcon />
            New Configuration
        </Button>
    );

    if (!environmentTaken) {
        return button;
    }

    const environmentLabel = ENVIRONMENT_CONFIGS[currentEnvironmentId]?.label || 'this environment';

    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <span tabIndex={0}>{button}</span>
            </TooltipTrigger>

            <TooltipContent>{`This project already has a configuration in ${environmentLabel}.`}</TooltipContent>
        </Tooltip>
    );
};

export default AutomationWorkflowProjectConfigurationCreateButton;
