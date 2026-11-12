import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import ProjectListItemPublishMenuItem from '@/pages/automation/projects/components/project-list/ProjectListItemPublishMenuItem';
import ListItemExpandButton from '@/shared/components/ListItemExpandButton';
import TagList from '@/shared/components/TagList';
import {
    AutomationProjectTagsQuery,
    AutomationProjectsQuery,
    useDeleteAutomationProjectMutation,
    useDuplicateAutomationProjectMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {
    CopyIcon,
    DownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    PlusIcon,
    Settings2Icon,
    Trash2Icon,
    UploadIcon,
    WorkflowIcon,
} from 'lucide-react';
import {MouseEvent, useCallback, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';
import {twMerge} from 'tailwind-merge';

type AutomationProjectType = AutomationProjectsQuery['automationProjects'][number];
type EmbeddedTagType = AutomationProjectTagsQuery['automationProjectTags'][number];

interface AutomationProjectListItemProps {
    onCreateConfiguration: (project: AutomationProjectType) => void;
    onCreateWorkflow: (projectId: string) => void;
    onEditProject: (project: AutomationProjectType) => void;
    onImportWorkflow: (projectId: string) => void;
    onPublishProject: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    onUpdateTags: (project: AutomationProjectType, tagNames: string[]) => void;
    project: AutomationProjectType;
    tags: EmbeddedTagType[];
}

const AutomationProjectListItem = ({
    onCreateConfiguration,
    onCreateWorkflow,
    onEditProject,
    onImportWorkflow,
    onPublishProject,
    onSelectWorkflow,
    onUpdateTags,
    project,
    tags,
}: AutomationProjectListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const queryClient = useQueryClient();

    const deleteProjectMutation = useDeleteAutomationProjectMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjectCategories']});
            void queryClient.invalidateQueries({queryKey: ['automationProjectTags']});
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});

            setShowDeleteDialog(false);
        },
    });

    const duplicateProjectMutation = useDuplicateAutomationProjectMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['automationProjects']});

            toast('Project duplicated successfully.');
        },
    });

    const titleClassName = twMerge(
        'text-base font-semibold',
        project.workflowTemplates.length > 0 && 'underline-offset-4 hover:text-content-brand-primary hover:underline'
    );

    const projectTags = tags
        .filter((tag) => project.tagIds.includes(tag.id))
        .map((tag) => ({id: undefined, name: tag.name}));

    const remainingTags = tags
        .filter((tag) => !project.tagIds.includes(tag.id))
        .map((tag) => ({id: undefined, name: tag.name}));

    const updateTagsMutation = useMemo(
        () => ({
            mutate: (tagNames: string[]) => onUpdateTags(project, tagNames),
        }),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [onUpdateTags, project.id]
    );

    const lastPublishedDate = project.lastPublishedDate ? new Date(project.lastPublishedDate) : undefined;

    const handleProjectListItemClick = useCallback((event: MouseEvent) => {
        const target = event.target as HTMLElement;

        const interactiveSelectors = [
            '[data-interactive]',
            '[role="menuitem"]',
            '[role="menu"]',
            '[data-radix-dropdown-menu-trigger]',
            '[data-radix-collapsible-trigger]',
        ].join(', ');

        if (target.closest(interactiveSelectors) || workflowsCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        workflowsCollapsibleTriggerRef.current?.click();
    }, []);

    return (
        <>
            <div
                aria-label={`${project.name}_container`}
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover"
                onClick={(event) => handleProjectListItemClick(event)}
                role="presentation"
            >
                <ListItemExpandButton label="Toggle project" />

                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div aria-label={project.name} className="flex-1" data-testid="project-item">
                        <div className="flex items-center gap-2">
                            {project.description ? (
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <button
                                            className={titleClassName}
                                            onClick={(event) => {
                                                event.stopPropagation();

                                                if (project.workflowTemplates.length > 0) {
                                                    onSelectWorkflow(project.workflowTemplates[0].workflowUuid);
                                                } else {
                                                    workflowsCollapsibleTriggerRef.current?.click();
                                                }
                                            }}
                                            type="button"
                                        >
                                            {project.name}
                                        </button>
                                    </TooltipTrigger>

                                    <TooltipContent>{project.description}</TooltipContent>
                                </Tooltip>
                            ) : (
                                <button
                                    className={titleClassName}
                                    onClick={(event) => {
                                        event.stopPropagation();

                                        if (project.workflowTemplates.length > 0) {
                                            onSelectWorkflow(project.workflowTemplates[0].workflowUuid);
                                        } else {
                                            workflowsCollapsibleTriggerRef.current?.click();
                                        }
                                    }}
                                    type="button"
                                >
                                    {project.name}
                                </button>
                            )}
                        </div>

                        <div className="relative mt-2 sm:flex sm:items-center sm:justify-between">
                            <div className="flex items-center gap-2">
                                <CollapsibleTrigger
                                    className="group flex items-center text-xs font-semibold text-muted-foreground"
                                    ref={workflowsCollapsibleTriggerRef}
                                >
                                    <div className="mr-1">
                                        {project.workflowTemplates.length === 1
                                            ? `${project.workflowTemplates.length} workflow`
                                            : `${project.workflowTemplates.length} workflows`}
                                    </div>

                                    <span className="mx-1">-</span>

                                    <span className="mr-1">
                                        {project.configurationCount === 1
                                            ? '1 configuration'
                                            : `${project.configurationCount} configurations`}
                                    </span>
                                </CollapsibleTrigger>

                                <div onClick={(event) => event.stopPropagation()} role="presentation">
                                    <TagList
                                        getRequest={(_id, updatedTags) => updatedTags.map((tag) => tag.name)}
                                        id={0}
                                        remainingTags={remainingTags}
                                        tags={projectTags}
                                        updateTagsMutation={updateTagsMutation}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6 [&>:last-child]:-ml-3">
                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="flex min-h-6 items-center space-x-2">
                                {project.published ? (
                                    <Badge
                                        className="flex space-x-1 border-transparent"
                                        styleType="success-outline"
                                        weight="semibold"
                                    >
                                        <span>V{project.lastPublishedVersion}</span>

                                        <span>PUBLISHED</span>
                                    </Badge>
                                ) : (
                                    <Badge className="flex space-x-1" styleType="secondary-filled" weight="semibold">
                                        <span>V{project.version}</span>

                                        <span>DRAFT</span>
                                    </Badge>
                                )}
                            </div>

                            <Tooltip>
                                <TooltipTrigger className="flex min-h-6 items-center text-sm text-content-neutral-secondary">
                                    {lastPublishedDate ? (
                                        <span className="text-xs">
                                            {`Published at ${lastPublishedDate.toLocaleDateString()} ${lastPublishedDate.toLocaleTimeString()}`}
                                        </span>
                                    ) : (
                                        <span className="text-xs">Not yet published</span>
                                    )}
                                </TooltipTrigger>

                                <TooltipContent side="left">Last Published Date</TooltipContent>
                            </Tooltip>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="More Project Actions"
                                    data-testid={`${project.id}-moreProjectActionsButton`}
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuGroup aria-label="Project Actions">
                                    <DropdownMenuItem
                                        aria-label="Edit Project"
                                        icon={<EditIcon />}
                                        label="Edit"
                                        onClick={() => onEditProject(project)}
                                    />

                                    <ProjectListItemPublishMenuItem
                                        hasWorkflows={project.workflowTemplates.length > 0}
                                        onClick={() => onPublishProject(project.id)}
                                    />

                                    <DropdownMenuItem
                                        aria-label="Duplicate Project"
                                        icon={<CopyIcon />}
                                        label="Duplicate"
                                        onClick={() => duplicateProjectMutation.mutate({id: project.id})}
                                    />

                                    <DropdownMenuItem
                                        aria-label="Export Project"
                                        icon={<DownloadIcon />}
                                        label="Export"
                                        onClick={() =>
                                            (window.location.href = `/api/automation/internal/projects/${project.id}/export`)
                                        }
                                    />
                                </DropdownMenuGroup>

                                <DropdownMenuSeparator />

                                <DropdownMenuGroup aria-label="Workflow Actions">
                                    <DropdownMenuItem
                                        aria-label="New Workflow"
                                        icon={<PlusIcon />}
                                        label="New Workflow"
                                        onClick={() => onCreateWorkflow(project.id)}
                                    />

                                    <DropdownMenuItem
                                        aria-label="Import Workflow"
                                        icon={<UploadIcon />}
                                        label="Import Workflow"
                                        onClick={() => onImportWorkflow(project.id)}
                                    />

                                    {project.workflowTemplates.length > 0 && (
                                        <DropdownMenuItem
                                            aria-label="View Workflows"
                                            icon={<WorkflowIcon />}
                                            label="View Workflows"
                                            onClick={() => onSelectWorkflow(project.workflowTemplates[0].workflowUuid)}
                                        />
                                    )}
                                </DropdownMenuGroup>

                                <DropdownMenuSeparator />

                                <DropdownMenuGroup aria-label="Configuration Actions">
                                    <Tooltip>
                                        <TooltipTrigger asChild>
                                            <span className="block">
                                                <DropdownMenuItem
                                                    aria-label="New Configuration"
                                                    disabled={!project.published}
                                                    icon={<Settings2Icon />}
                                                    label="New Configuration"
                                                    onClick={() => onCreateConfiguration(project)}
                                                />
                                            </span>
                                        </TooltipTrigger>

                                        {!project.published && (
                                            <TooltipContent>Publish the project before configuring it.</TooltipContent>
                                        )}
                                    </Tooltip>
                                </DropdownMenuGroup>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem
                                    aria-label="Delete Project"
                                    icon={<Trash2Icon />}
                                    label="Delete"
                                    onClick={(event: MouseEvent) => {
                                        setShowDeleteDialog(true);

                                        event.stopPropagation();
                                    }}
                                    variant="destructive"
                                />
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <AlertDialog
                ariaLabel="Confirm Project Deletion"
                description="This action cannot be undone. This will permanently delete the project and the workflow templates it contains."
                isPending={deleteProjectMutation.isPending}
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => deleteProjectMutation.mutate({id: project.id})}
                open={showDeleteDialog}
            />
        </>
    );
};

export default AutomationProjectListItem;
