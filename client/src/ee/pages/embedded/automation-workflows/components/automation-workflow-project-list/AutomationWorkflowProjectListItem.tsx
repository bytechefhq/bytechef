import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {ButtonGroup} from '@/components/ui/button-group';
import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import TagList from '@/shared/components/TagList';
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
            '.dropdown-menu-item',
            '[data-radix-dropdown-menu-item]',
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
                className="flex w-full cursor-pointer items-center justify-between rounded-md px-2 hover:bg-destructive-foreground"
                onClick={(event) => handleProjectListItemClick(event)}
            >
                <div className="flex flex-1 items-center py-5 group-data-[state='open']:border-none">
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

                                        <DropdownMenuContent align="end" className="p-0">
                                            <DropdownMenuItem
                                                aria-label="Import Workflow"
                                                className="dropdown-menu-item"
                                                onClick={(event) => {
                                                    event.stopPropagation();

                                                    onImportWorkflow(project.id);
                                                }}
                                            >
                                                <UploadIcon /> Import Workflow
                                            </DropdownMenuItem>
                                        </DropdownMenuContent>
                                    </DropdownMenu>
                                </ButtonGroup>

                                <div onClick={(event) => event.stopPropagation()}>
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

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex items-center space-x-2">
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

                            <DropdownMenuContent align="end" className="p-0">
                                <DropdownMenuItem
                                    aria-label="Publish Project"
                                    className="dropdown-menu-item"
                                    onClick={() => onPublishProject(project.id)}
                                >
                                    <SendIcon /> Publish
                                </DropdownMenuItem>

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    aria-label="New Workflow"
                                    className="dropdown-menu-item"
                                    onClick={() => onCreateWorkflow(project.id)}
                                >
                                    <PlusIcon /> New Workflow
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    aria-label="Import Workflow"
                                    className="dropdown-menu-item"
                                    onClick={() => onImportWorkflow(project.id)}
                                >
                                    <UploadIcon /> Import Workflow
                                </DropdownMenuItem>

                                <DropdownMenuItem
                                    aria-label="Edit Project"
                                    className="dropdown-menu-item"
                                    onClick={() => onEditProject(project)}
                                >
                                    <EditIcon /> Edit
                                </DropdownMenuItem>

                                <DropdownMenuSeparator className="m-0" />

                                <DropdownMenuItem
                                    aria-label="Delete Project"
                                    className="dropdown-menu-item-destructive"
                                    onClick={(event: MouseEvent) => {
                                        setShowDeleteDialog(true);

                                        event.stopPropagation();
                                    }}
                                    variant="destructive"
                                >
                                    <Trash2Icon /> Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <DeleteAlertDialog
                onCancel={() => setShowDeleteDialog(false)}
                onDelete={() => {
                    onDeleteProject(project.id);

                    setShowDeleteDialog(false);
                }}
                open={showDeleteDialog}
            />
        </>
    );
};

export default AutomationWorkflowProjectListItem;
