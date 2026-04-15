import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Connection} from '@/shared/middleware/automation/configuration';
import {useSetConnectionProjectsMutation} from '@/shared/middleware/graphql';
import {ConnectionKeys} from '@/shared/queries/automation/connections.queries';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {ComponentDefinitionKeys} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {useEffect, useMemo, useRef, useState} from 'react';
import {toast} from 'sonner';

interface ConnectionProjectShareDialogProps {
    connection: Connection;
    onClose: () => void;
    open: boolean;
    workspaceId: number;
}

const ConnectionProjectShareDialog = ({connection, onClose, open, workspaceId}: ConnectionProjectShareDialogProps) => {
    const initialSharedIds = useMemo(
        () => new Set<number>(connection.sharedProjectIds ?? []),
        [connection.sharedProjectIds]
    );

    const [searchQuery, setSearchQuery] = useState('');
    const [selectedIds, setSelectedIds] = useState<Set<number>>(initialSharedIds);
    const [submitting, setSubmitting] = useState(false);

    // Track the open/closed transition so we only reset state when the dialog goes from closed → open.
    // Gating on `initialSharedIds` identity alone would clobber a user's in-progress selections when
    // the parent's connections query refetches while this dialog is open: the `connection` prop gets
    // a fresh `sharedProjectIds` array (identity changes even if values don't) and the effect would
    // reset `selectedIds` mid-edit.
    const wasOpenRef = useRef(false);

    const {data: projects, isLoading} = useGetWorkspaceProjectsQuery({id: workspaceId}, open);

    useEffect(() => {
        if (open && !wasOpenRef.current) {
            setSelectedIds(initialSharedIds);
            setSearchQuery('');
            setSubmitting(false);
        }

        wasOpenRef.current = open;
    }, [initialSharedIds, open]);

    const filteredProjects = useMemo(() => {
        if (!projects) {
            return [];
        }

        const normalizedQuery = searchQuery.trim().toLowerCase();

        if (!normalizedQuery) {
            return projects;
        }

        return projects.filter((project) => project.name.toLowerCase().includes(normalizedQuery));
    }, [projects, searchQuery]);

    const queryClient = useQueryClient();

    const setConnectionProjectsMutation = useSetConnectionProjectsMutation();

    const toggleProjectSelection = (projectId: number, checked: boolean) => {
        setSelectedIds((current) => {
            const next = new Set(current);

            if (checked) {
                next.add(projectId);
            } else {
                next.delete(projectId);
            }

            return next;
        });
    };

    const handleSave = async () => {
        if (!connection.id) {
            return;
        }

        if (
            selectedIds.size === initialSharedIds.size &&
            Array.from(selectedIds).every((id) => initialSharedIds.has(id))
        ) {
            onClose();

            return;
        }

        setSubmitting(true);

        // useFetchInterceptor already surfaces GraphQL errors as toasts globally — do NOT add a local
        // toast.error here or the user sees two toasts for the same failure, and the generic local
        // message hides the specific server message. The catch just records a devtools breadcrumb
        // and leaves surfacing to the global interceptor. setSubmitting(false) lives in finally so
        // an unexpected throw in the post-mutation side-effects (invalidate/toast/onClose) does not
        // leave the dialog stuck with the Save button disabled.
        let mutationSucceeded = false;

        try {
            await setConnectionProjectsMutation.mutateAsync({
                connectionId: String(connection.id),
                projectIds: Array.from(selectedIds).map(String),
                workspaceId: String(workspaceId),
            });

            mutationSucceeded = true;

            queryClient.invalidateQueries({queryKey: ConnectionKeys.connections});
            queryClient.invalidateQueries({queryKey: ComponentDefinitionKeys.componentDefinitions});

            toast(`Project sharing updated for "${connection.name}".`);
        } catch (error) {
            console.error('setConnectionProjects failed', error);
        } finally {
            setSubmitting(false);
        }

        if (mutationSucceeded) {
            onClose();
        }
    };

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (!nextOpen) {
                    onClose();
                }
            }}
            open={open}
        >
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>Share with projects</DialogTitle>

                    <DialogDescription>
                        Select the projects that can use <strong>{connection.name}</strong>. Removing every project
                        reverts the connection back to private.
                    </DialogDescription>

                    <DialogCloseButton />
                </DialogHeader>

                {projects && projects.length > 5 && (
                    <Input
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Filter projects…"
                        value={searchQuery}
                    />
                )}

                <ScrollArea className="max-h-80 pr-4">
                    {isLoading && <div className="text-sm text-muted-foreground">Loading projects…</div>}

                    {!isLoading && projects && projects.length === 0 && (
                        <div className="text-sm text-muted-foreground">No projects in this workspace.</div>
                    )}

                    {!isLoading && projects && projects.length > 0 && filteredProjects.length === 0 && (
                        <div className="text-sm text-muted-foreground">No projects match "{searchQuery}".</div>
                    )}

                    {!isLoading && filteredProjects.length > 0 && (
                        <ul className="flex flex-col gap-2">
                            {filteredProjects.map((project) => (
                                <li className="flex items-center gap-2" key={project.id}>
                                    <Checkbox
                                        checked={selectedIds.has(project.id!)}
                                        id={`share-project-${project.id}`}
                                        onCheckedChange={(checked) =>
                                            toggleProjectSelection(project.id!, checked === true)
                                        }
                                    />

                                    <label className="cursor-pointer text-sm" htmlFor={`share-project-${project.id}`}>
                                        {project.name}
                                    </label>
                                </li>
                            ))}
                        </ul>
                    )}
                </ScrollArea>

                <DialogFooter>
                    <Button disabled={submitting} label="Cancel" onClick={onClose} variant="outline" />

                    <Button disabled={submitting || isLoading} label="Save" onClick={handleSave} />
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
};

export default ConnectionProjectShareDialog;
