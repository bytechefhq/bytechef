import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {Skeleton} from '@/components/ui/skeleton';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import useAgents from '@/pages/automation/agents/hooks/useAgents';
import getAgentPath from '@/pages/automation/agents/utils/getAgentPath';
import useDataSyncs from '@/pages/automation/data-syncs/hooks/useDataSyncs';
import getDataSyncPath from '@/pages/automation/data-syncs/utils/getDataSyncPath';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {ArrowLeftRightIcon, BotIcon, ChevronDownIcon, WorkflowIcon} from 'lucide-react';
import {useMemo} from 'react';
import {useNavigate} from 'react-router-dom';

interface ProjectItemSelectProps {
    currentAgentId?: string;
    currentDataSyncId?: string;
    currentLabel?: string;
    currentProjectWorkflowId?: number;
    onWorkflowValueChange: (projectWorkflowId: number) => void;
    projectId: number;
    projectWorkflows: Workflow[];
}

const ProjectItemSelect = ({
    currentAgentId,
    currentDataSyncId,
    currentLabel,
    currentProjectWorkflowId,
    onWorkflowValueChange,
    projectId,
    projectWorkflows,
}: ProjectItemSelectProps) => {
    const {agents} = useAgents();
    const {dataSyncs} = useDataSyncs();
    const navigate = useNavigate();

    const projectAgents = useMemo(() => agents.filter((agent) => +agent.projectId === projectId), [agents, projectId]);

    const projectDataSyncs = useMemo(
        () => dataSyncs.filter((dataSync) => +dataSync.projectId === projectId),
        [dataSyncs, projectId]
    );

    const currentValue = currentDataSyncId
        ? `dataSync:${currentDataSyncId}`
        : currentAgentId
          ? `agent:${currentAgentId}`
          : currentProjectWorkflowId !== undefined
            ? `workflow:${currentProjectWorkflowId}`
            : undefined;

    const handleValueChange = (value: string) => {
        const separatorIndex = value.indexOf(':');

        const kind = value.slice(0, separatorIndex);
        const id = value.slice(separatorIndex + 1);

        if (kind === 'workflow') {
            onWorkflowValueChange(Number(id));

            return;
        }

        if (kind === 'agent') {
            const selectedAgent = projectAgents.find((agent) => agent.id === id);

            if (selectedAgent) {
                navigate(getAgentPath(selectedAgent));
            }

            return;
        }

        if (kind === 'dataSync') {
            const selectedDataSync = projectDataSyncs.find((dataSync) => dataSync.id === id);

            if (selectedDataSync) {
                navigate(getDataSyncPath(selectedDataSync));
            }
        }
    };

    return (
        <DropdownMenu>
            <Tooltip>
                <TooltipTrigger asChild>
                    <DropdownMenuTrigger
                        aria-label="Project item select"
                        className="flex max-w-64 items-center gap-1 rounded-md px-1.5 py-1 text-content-neutral-primary outline-hidden hover:bg-surface-neutral-primary-hover data-[state=open]:bg-surface-neutral-primary-hover"
                    >
                        {currentLabel ? (
                            <span className="truncate">{currentLabel}</span>
                        ) : (
                            <Skeleton className="h-3 w-44" />
                        )}

                        <ChevronDownIcon className="size-4 shrink-0 text-content-neutral-secondary" />
                    </DropdownMenuTrigger>
                </TooltipTrigger>

                {currentLabel && currentLabel.length > 30 && <TooltipContent>{currentLabel}</TooltipContent>}
            </Tooltip>

            <DropdownMenuContent align="start" className="max-w-lg min-w-64">
                <DropdownMenuRadioGroup onValueChange={handleValueChange} value={currentValue ?? ''}>
                    {projectWorkflows.length > 0 && (
                        <>
                            <DropdownMenuLabel>Workflows</DropdownMenuLabel>

                            {projectWorkflows.map((workflow) => (
                                <DropdownMenuRadioItem
                                    className="pl-2 data-[state=checked]:bg-surface-brand-secondary data-[state=checked]:text-content-brand-primary [&>span:first-child]:hidden"
                                    key={workflow.projectWorkflowId!}
                                    title={workflow.label!.length > 55 ? workflow.label! : undefined}
                                    value={`workflow:${workflow.projectWorkflowId!}`}
                                >
                                    <WorkflowIcon className="size-4 shrink-0" />

                                    <span className="truncate">{workflow.label!}</span>
                                </DropdownMenuRadioItem>
                            ))}
                        </>
                    )}

                    {projectAgents.length > 0 && (
                        <>
                            <DropdownMenuLabel>Agents</DropdownMenuLabel>

                            {projectAgents.map((agent) => (
                                <DropdownMenuRadioItem
                                    className="pl-2 data-[state=checked]:bg-surface-brand-secondary data-[state=checked]:text-content-brand-primary [&>span:first-child]:hidden"
                                    key={agent.id}
                                    title={agent.title.length > 55 ? agent.title : undefined}
                                    value={`agent:${agent.id}`}
                                >
                                    <BotIcon className="size-4 shrink-0" />

                                    <span className="truncate">{agent.title}</span>
                                </DropdownMenuRadioItem>
                            ))}
                        </>
                    )}

                    {projectDataSyncs.length > 0 && (
                        <>
                            <DropdownMenuLabel>Data Syncs</DropdownMenuLabel>

                            {projectDataSyncs.map((dataSync) => (
                                <DropdownMenuRadioItem
                                    className="cursor-pointer pl-2 data-[state=checked]:bg-surface-brand-secondary data-[state=checked]:text-content-brand-primary [&>span:first-child]:hidden"
                                    key={dataSync.id}
                                    title={dataSync.title.length > 55 ? dataSync.title : undefined}
                                    value={`dataSync:${dataSync.id}`}
                                >
                                    <ArrowLeftRightIcon className="size-4 shrink-0" />

                                    <span className="truncate">{dataSync.title}</span>
                                </DropdownMenuRadioItem>
                            ))}
                        </>
                    )}
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
};

export default ProjectItemSelect;
