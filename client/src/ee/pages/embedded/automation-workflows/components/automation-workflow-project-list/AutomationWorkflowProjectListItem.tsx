import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/DropdownMenu/DropdownMenu';
import {ButtonGroup} from '@/components/ui/button-group';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import TagList from '@/shared/components/TagList';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {AutomationWorkflowProjectTagsQuery, AutomationWorkflowProjectsQuery} from '@/shared/middleware/graphql';
import {
    ChevronDownIcon,
    EditIcon,
    EllipsisVerticalIcon,
    PlusIcon,
    SendIcon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
import {MouseEvent, useCallback, useMemo, useRef, useState} from 'react';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

interface AutomationWorkflowProjectListItemProps {
    onCreateWorkflow: (projectId: string) => void;
    onDeleteProject: (projectId: string) => void;
    onEditProject: (project: AutomationWorkflowProjectType) => void;
    onImportWorkflow: (projectId: string) => void;
    onPublishProject: (projectId: string) => void;
    onSelectWorkflow: (workflowUuid: string) => void;
    onUpdateTags: (project: AutomationWorkflowProjectType, tagNames: string[]) => void;
    project: AutomationWorkflowProjectType;
    tags: EmbeddedTagType[];
}

const AutomationWorkflowProjectListItem = ({
    onCreateWorkflow,
    onDeleteProject,
    onEditProject,
    onImportWorkflow,
    onPublishProject,
    onSelectWorkflow,
    onUpdateTags,
    project,
    tags,
}: AutomationWorkflowProjectListItemProps) => {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const canCreateWorkflow = useHasEmbeddedScope('WORKFLOW_CREATE', DEVELOPMENT_ENVIRONMENT);
    const canDeleteProject = useHasEmbeddedScope('INTEGRATION_DELETE', UNION_ENVIRONMENT_ID);
    const canEditWorkflowOnUnion = useHasEmbeddedScope('WORKFLOW_EDIT', UNION_ENVIRONMENT_ID);
    const canManageSettings = useHasEmbeddedScope('INTEGRATION_SETTINGS', UNION_ENVIRONMENT_ID);
    const canPublishScope = useHasEmbeddedScope('INTEGRATION_PUBLISH', UNION_ENVIRONMENT_ID);

    const canPublishProject = canEditWorkflowOnUnion && canPublishScope;

    const workflowsCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

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
            >
                <div className="flex flex-1 items-center py-3 group-data-[state='open']:border-none">
                    <div aria-label={project.name} className="flex-1" data-testid="project-item">
                        <div className="flex items-center gap-2">
                            <button
                                className="text-base font-semibold"
                                onClick={(event) => {
                                    event.stopPropagation();

                                    if (project.workflowTemplates.length > 0) {
                                        onSelectWorkflow(project.workflowTemplates[0].workflowUuid);
                                    } else {
                                        workflowsCollapsibleTriggerRef.current?.click();
                                    }
                                }}
                                title={project.description || undefined}
                                type="button"
                            >
                                {project.name}
                            </button>
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

                                    <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                                </CollapsibleTrigger>

                                {canCreateWorkflow && (
                                    <ButtonGroup aria-label="Workflow Creation Actions">
                                        <Button
                                            aria-label="Create Workflow"
                                            onClick={(event) => {
                                                event.stopPropagation();

                                                onCreateWorkflow(project.id);
                                            }}
                                            size="xs"
                                            variant="outline"
                                        >
                                            <PlusIcon />
                                            Workflow
                                        </Button>

                                        <DropdownMenu>
                                            <DropdownMenuTrigger asChild>
                                                <Button
                                                    aria-label="More Workflow Creation Actions"
                                                    icon={<ChevronDownIcon />}
                                                    size="xs"
                                                    variant="outline"
                                                >
                                                    <> </>
                                                </Button>
                                            </DropdownMenuTrigger>

                                            <DropdownMenuContent align="end">
                                                <DropdownMenuItem
                                                    aria-label="Import Workflow"
                                                    icon={<UploadIcon />}
                                                    label="Import Workflow"
                                                    onClick={(event) => {
                                                        event.stopPropagation();

                                                        onImportWorkflow(project.id);
                                                    }}
                                                />
                                            </DropdownMenuContent>
                                        </DropdownMenu>
                                    </ButtonGroup>
                                )}

                                <div onClick={(event) => event.stopPropagation()}>
                                    <TagList
                                        getRequest={(_id, updatedTags) => updatedTags.map((tag) => tag.name)}
                                        id={0}
                                        readOnly={!canManageSettings}
                                        remainingTags={remainingTags}
                                        tags={projectTags}
                                        updateTagsMutation={updateTagsMutation}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex items-center space-x-2">
                            {!project.automationHubVisible && (
                                <Badge label="Hidden from hub" styleType="secondary-outline" />
                            )}

                            {project.published ? (
                                <Badge className="flex space-x-1" styleType="success-outline" weight="semibold">
                                    <span>V{project.lastPublishedVersion}</span>

                                    <span>PUBLISHED</span>
                                </Badge>
                            ) : (
                                <Badge className="flex space-x-1" styleType="secondary-filled" weight="semibold">
                                    <span>DRAFT</span>
                                </Badge>
                            )}
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
                                {canPublishProject && (
                                    <DropdownMenuItem
                                        aria-label="Publish Project"
                                        icon={<SendIcon />}
                                        label="Publish"
                                        onClick={() => onPublishProject(project.id)}
                                    />
                                )}

                                {canCreateWorkflow && (
                                    <>
                                        <DropdownMenuSeparator />

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
                                    </>
                                )}

                                {canManageSettings && (
                                    <DropdownMenuItem
                                        aria-label="Edit Project"
                                        icon={<EditIcon />}
                                        label="Edit"
                                        onClick={() => onEditProject(project)}
                                    />
                                )}

                                {canDeleteProject && (
                                    <>
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
                                    </>
                                )}
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <AlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onConfirm={() => {
                    onDeleteProject(project.id);

                    setShowDeleteDialog(false);
                }}
                open={showDeleteDialog}
            />
        </>
    );
};

export default AutomationWorkflowProjectListItem;
