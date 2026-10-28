import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import Switch from '@/components/Switch/Switch';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import useOpenInProject from '@/pages/automation/project-deployments/hooks/useOpenInProject';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import McpToolConfigureButton from '@/shared/components/mcp-server/McpToolConfigureButton';
import WorkflowToolRowLabel from '@/shared/components/mcp-server/WorkflowToolRowLabel';
import useWorkflowComponentDefinitions, {
    getWorkflowComponentNames,
} from '@/shared/hooks/useWorkflowComponentDefinitions';
import {A2aProjectWorkflowsByA2aProjectIdQuery} from '@/shared/middleware/graphql';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useMemo} from 'react';

import {A2aProjectItemType} from './hooks/useA2aProjectList';

export type A2aProjectWorkflowItemType = NonNullable<
    NonNullable<A2aProjectWorkflowsByA2aProjectIdQuery['a2aProjectWorkflowsByA2aProjectId']>[number]
>;

interface A2aProjectWorkflowListItemProps {
    a2aProject: A2aProjectItemType;
    a2aProjectWorkflow: A2aProjectWorkflowItemType;
    enablePending: boolean;
    onConfigureClick: () => void;
    onEditClick: () => void;
    onEnabledChange: (enabled: boolean) => void;
    onRemoveClick: () => void;
    projectName?: string;
}

const A2aProjectWorkflowListItem = ({
    a2aProject,
    a2aProjectWorkflow,
    enablePending,
    onConfigureClick,
    onEditClick,
    onEnabledChange,
    onRemoveClick,
    projectName,
}: A2aProjectWorkflowListItemProps) => {
    const openProjectDeploymentWorkflowSheet = useProjectDeploymentWorkflowSheetStore(
        (state) => state.openProjectDeploymentWorkflowSheet
    );
    const setWorkflowExecutionSheetOpen = useWorkflowExecutionSheetStore(
        (state) => state.setWorkflowExecutionSheetOpen
    );

    const {openProjectWorkflow} = useOpenInProject();
    const {workflowComponentDefinitions, workflowTaskDispatcherDefinitions} = useWorkflowComponentDefinitions();

    const {data: workflow} = useGetWorkflowQuery(
        a2aProjectWorkflow.workflowId ?? '',
        a2aProjectWorkflow.workflowId != null
    );

    const filteredComponentNames = useMemo(() => getWorkflowComponentNames(workflow), [workflow]);

    const workflowLabel = a2aProjectWorkflow.workflowLabel || a2aProjectWorkflow.workflowId || 'Unnamed Workflow';

    const projectId = a2aProject.projectId != null ? Number(a2aProject.projectId) : undefined;
    const projectDeploymentId =
        a2aProject.projectDeploymentId != null ? Number(a2aProject.projectDeploymentId) : undefined;

    const canOpenExecutions = workflow != null && projectDeploymentId != null;
    const showOpenInProject = projectId != null && workflow?.workflowUuid != null;

    const handleWorkflowClick = () => {
        if (workflow && projectDeploymentId != null) {
            setWorkflowExecutionSheetOpen(false);

            openProjectDeploymentWorkflowSheet({
                projectDeploymentId,
                projectId,
                projectName,
                projectVersion: a2aProject.projectVersion ?? undefined,
                workflow,
            });
        }
    };

    const handleOpenInProjectClick = () => {
        if (projectId != null) {
            void openProjectWorkflow(projectId, workflow?.workflowUuid);
        }
    };

    return (
        <div className="-mx-3 flex min-h-12 items-center gap-2 rounded-md px-3 py-1 hover:bg-surface-neutral-primary-hover">
            <WorkflowToolRowLabel
                canOpenExecutions={canOpenExecutions}
                filteredComponentNames={filteredComponentNames}
                label={workflowLabel}
                onOpenInProjectClick={showOpenInProject ? handleOpenInProjectClick : undefined}
                onWorkflowClick={handleWorkflowClick}
                subLabel={
                    a2aProjectWorkflow.skillName &&
                    a2aProjectWorkflow.skillName !== workflowLabel && (
                        <span className="truncate text-xs text-muted-foreground">{`Skill: ${a2aProjectWorkflow.skillName}`}</span>
                    )
                }
                workflow={workflow}
                workflowComponentDefinitions={workflowComponentDefinitions}
                workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
            />

            <div className="flex shrink-0 items-center gap-2">
                <McpToolConfigureButton anchorsPopover label={workflowLabel} onClick={onConfigureClick} />

                <Switch
                    aria-label={`Enable ${workflowLabel}`}
                    checked={a2aProjectWorkflow.enabled}
                    className="mr-2"
                    disabled={enablePending}
                    onCheckedChange={onEnabledChange}
                />

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            aria-label={`${workflowLabel} actions`}
                            icon={<EllipsisVerticalIcon />}
                            size="iconSm"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem className="dropdown-menu-item" onClick={onEditClick}>
                            <EditIcon /> Edit
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={onRemoveClick}
                            variant="destructive"
                        >
                            <Trash2Icon /> Remove
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>
        </div>
    );
};

export default A2aProjectWorkflowListItem;
