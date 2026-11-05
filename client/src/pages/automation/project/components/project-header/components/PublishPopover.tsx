import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {getDisabledControlTooltip} from '@/pages/automation/project/components/project-header/util/permission-tooltip-utils';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkspaceScopeState} from '@/shared/hooks/useHasWorkspaceScope';
import {zodResolver} from '@hookform/resolvers/zod';
import {SendIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    description: z.string().max(256).optional(),
});

const PublishPopover = ({
    disabled,
    isPending,
    onPublishProjectSubmit,
}: {
    disabled?: boolean;
    isPending: boolean;
    onPublishProjectSubmit: ({description, onSuccess}: {description?: string; onSuccess: () => void}) => void;
}) => {
    const [open, setOpen] = useState(false);

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    // Publishing needs BOTH scopes, so gating on either one alone fails open: ProjectFacadeImpl.publishProject is
    // annotated hasPermission(#id, 'Project', 'WORKFLOW_EDIT'), and the ProjectServiceImpl.publishProject and
    // ProjectWorkflowServiceImpl.publishWorkflow calls it makes are annotated hasPermission(..., 'PROJECT_PUBLISH').
    // The built-in roles put both at EDITOR rank so they always co-occur, but a custom role is an arbitrary scope
    // set — a member granted only PROJECT_PUBLISH would otherwise be offered the popover and refused on submit.
    //
    // A third scope, PROJECT_PUSH, is required only for a project whose Git configuration is enabled: the REST
    // controller always publishes with syncWithGit, but ProjectGitSyncEventListenerImpl reaches pushProjectToGit
    // only for such a project. It is deliberately NOT gated on here — the client does not know whether Git sync is
    // configured, and demanding it of everybody would withhold publishing from members who never touch Git. Both sit at
    // EDITOR rank, so only a custom role holding PROJECT_PUBLISH without PROJECT_PUSH fails on submit for a Git-synced
    // project.
    // The four-state hook is used instead of the plain boolean one so that "not loaded yet", "check failed" and "the
    // edition never resolved so nothing was ever asked" stay distinguishable from "denied"; see
    // getDisabledControlTooltip.
    const projectPublishState = useWorkspaceScopeState(currentWorkspaceId, 'PROJECT_PUBLISH');
    const workflowEditState = useWorkspaceScopeState(currentWorkspaceId, 'WORKFLOW_EDIT');

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const canPublishProject = projectPublishState.granted && workflowEditState.granted;
    const permissionsError = projectPublishState.error || workflowEditState.error;
    // An unresolved workspace id counts as "not yet known" rather than as a refusal, for the same reason.
    const permissionsLoading = currentWorkspaceId == null || projectPublishState.loading || workflowEditState.loading;
    const permissionsUnknown = projectPublishState.editionUnknown || workflowEditState.editionUnknown;

    const handlePublishProject = ({description}: {description?: string}) => {
        onPublishProjectSubmit({
            description,
            onSuccess: () => {
                reset();
                setOpen(false);
            },
        });
    };

    if (!canPublishProject || disabled) {
        return (
            <Tooltip>
                <TooltipTrigger asChild>
                    <span className="inline-flex">
                        <Button
                            className="rounded-r-none"
                            disabled
                            icon={<SendIcon />}
                            id="publish-button"
                            label="Publish"
                            variant="outline"
                        />
                    </span>
                </TooltipTrigger>

                <TooltipContent>
                    {getDisabledControlTooltip({
                        deniedMessage: 'You do not have permission to publish this project',
                        granted: canPublishProject,
                        permissionsError,
                        permissionsLoading,
                        permissionsUnknown,
                        unmetPreconditionMessage: 'No changes to publish',
                    })}
                </TooltipContent>
            </Tooltip>
        );
    }

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <Tooltip>
                <PopoverTrigger asChild>
                    <TooltipTrigger asChild>
                        <Button
                            className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                            icon={<SendIcon />}
                            id="publish-button"
                            label="Publish"
                            variant="outline"
                        />
                    </TooltipTrigger>
                </PopoverTrigger>

                <TooltipContent>Publish the project</TooltipContent>
            </Tooltip>

            <PopoverContent
                align="end"
                className="flex h-full w-96 flex-col justify-between space-y-4"
                id="publish-popover"
            >
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handlePublishProject)}>
                        <h3 className="font-semibold">Publish Project</h3>

                        <div className="flex-1">
                            <FormField
                                control={control}
                                name="description"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Description</FormLabel>

                                        <FormControl>
                                            <Textarea className="h-28" {...field}></Textarea>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <div className="flex justify-end">
                            <Button
                                aria-label="Publish button"
                                disabled={isPending}
                                icon={isPending ? <LoadingIcon /> : undefined}
                                label="Publish"
                                size="sm"
                                type="submit"
                            />
                        </div>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

export default PublishPopover;
