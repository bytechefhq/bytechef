import AlertDialog from '@/components/AlertDialog';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import Switch from '@/components/Switch/Switch';
import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import TagList from '@/shared/components/TagList';
import {
    A2aServer,
    Tag,
    useDeleteA2aServerMutation,
    useUpdateA2aServerMutation,
    useUpdateA2aServerTagsMutation,
} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {ChevronDownIcon, EllipsisVerticalIcon, NetworkIcon} from 'lucide-react';
import {useState} from 'react';

import A2aServerConnect from './A2aServerConnect';
import A2aServerDialog from './A2aServerDialog';
import A2aServerWorkflowDialog from './A2aServerWorkflowDialog';
import A2aProjectList from './a2a-project-list/A2aProjectList';
import useA2aProjectList from './a2a-project-list/hooks/useA2aProjectList';

interface A2aServerListItemProps {
    a2aServer: A2aServer;
    tags?: Tag[];
}

const A2aServerListItem = ({a2aServer, tags}: A2aServerListItemProps) => {
    const [activeTab, setActiveTab] = useState('projects');
    const [addProjectDialogOpen, setAddProjectDialogOpen] = useState(false);
    const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
    const [editDialogOpen, setEditDialogOpen] = useState(false);

    const queryClient = useQueryClient();

    const {a2aProjects} = useA2aProjectList(a2aServer.id);

    const deleteA2aServerMutation = useDeleteA2aServerMutation();
    const updateA2aServerMutation = useUpdateA2aServerMutation();
    const updateA2aServerTagsMutation = useUpdateA2aServerTagsMutation({
        onSuccess: () => {
            void queryClient.invalidateQueries({queryKey: ['a2aServers']});
            void queryClient.invalidateQueries({queryKey: ['a2aServerTags']});
        },
    });

    const a2aServerTagIds = a2aServer.tags?.map((tag) => tag?.id);

    const workflowCount = a2aProjects.reduce((total, a2aProject) => total + (a2aProject.workflowIds?.length || 0), 0);

    const handleDelete = () => {
        deleteA2aServerMutation.mutate(
            {id: a2aServer.id},
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['a2aServers']});

                    setDeleteDialogOpen(false);
                },
            }
        );
    };

    const handleEnabledChange = (enabled: boolean) => {
        updateA2aServerMutation.mutate(
            {id: a2aServer.id, input: {enabled}},
            {
                onSuccess: () => {
                    void queryClient.invalidateQueries({queryKey: ['a2aServers']});
                },
            }
        );
    };

    return (
        <Collapsible className="group mb-2 rounded border border-border/50">
            <div className="flex w-full items-center justify-between rounded-md px-3 hover:bg-surface-neutral-primary-hover">
                <div className="flex flex-1 items-center py-3">
                    <div className="flex-1">
                        <div className="flex min-h-8 items-center gap-2">
                            <CollapsibleTrigger className="text-base font-semibold">
                                <div className="flex items-center">
                                    <NetworkIcon className="mr-2 size-4 text-content-neutral-secondary" />

                                    <span>{a2aServer.name}</span>
                                </div>
                            </CollapsibleTrigger>

                            {a2aServer.authenticationRequired && (
                                <Badge label="Auth required" styleType="secondary-outline" />
                            )}
                        </div>

                        <div className="mt-2 flex min-h-7 items-center gap-4">
                            <CollapsibleTrigger className="group flex items-center gap-1 text-xs font-semibold text-muted-foreground">
                                {workflowCount === 1 ? '1 workflow skill' : `${workflowCount} workflow skills`}

                                <ChevronDownIcon className="size-4 duration-300 group-data-[state=open]:rotate-180" />
                            </CollapsibleTrigger>

                            <div onClick={(event) => event.preventDefault()}>
                                <TagList
                                    getRequest={(id, tagList) => ({
                                        id: id!,
                                        tags: tagList || [],
                                    })}
                                    id={parseInt(a2aServer.id)}
                                    remainingTags={tags
                                        ?.filter((tag) => !a2aServerTagIds?.includes(tag.id))
                                        .map((tag) => ({id: parseInt(tag.id), name: tag.name}))}
                                    tags={(a2aServer.tags ?? [])
                                        .filter((tag) => tag != null)
                                        .map((tag) => ({id: parseInt(tag!.id), name: tag!.name}))}
                                    updateTagsMutation={updateA2aServerTagsMutation}
                                />
                            </div>

                            {a2aServer.description && (
                                <span className="text-xs text-muted-foreground">{a2aServer.description}</span>
                            )}
                        </div>
                    </div>

                    <div className="flex items-center justify-end gap-x-6">
                        <div className="flex min-w-52 flex-col items-end gap-y-2">
                            <div className="flex min-h-8 items-center">
                                {updateA2aServerMutation.isPending && <LoadingIcon />}

                                <Switch
                                    aria-label="Enabled"
                                    checked={a2aServer.enabled}
                                    disabled={updateA2aServerMutation.isPending}
                                    onCheckedChange={handleEnabledChange}
                                />
                            </div>

                            <span className="flex min-h-7 items-center text-xs text-content-neutral-secondary">
                                {a2aServer.lastModifiedDate
                                    ? `Modified at ${new Date(a2aServer.lastModifiedDate).toLocaleDateString()} ${new Date(a2aServer.lastModifiedDate).toLocaleTimeString()}`
                                    : 'No modifications'}
                            </span>
                        </div>

                        <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                                <Button
                                    aria-label="Server actions"
                                    icon={<EllipsisVerticalIcon />}
                                    size="icon"
                                    variant="ghost"
                                />
                            </DropdownMenuTrigger>

                            <DropdownMenuContent align="end">
                                <DropdownMenuItem onClick={() => setEditDialogOpen(true)}>Edit</DropdownMenuItem>

                                <DropdownMenuSeparator />

                                <DropdownMenuItem onClick={() => setDeleteDialogOpen(true)} variant="destructive">
                                    Delete
                                </DropdownMenuItem>
                            </DropdownMenuContent>
                        </DropdownMenu>
                    </div>
                </div>
            </div>

            <CollapsibleContent className="mx-3 mt-1 mb-3">
                <Tabs onValueChange={setActiveTab} value={activeTab}>
                    <div className="flex items-center justify-between">
                        <TabsList>
                            <TabsTrigger value="projects">Workflows</TabsTrigger>

                            <TabsTrigger value="connect">Connect</TabsTrigger>
                        </TabsList>

                        {activeTab === 'projects' && (
                            <Button
                                label="Add Workflows"
                                onClick={() => setAddProjectDialogOpen(true)}
                                size="sm"
                                variant="secondary"
                            />
                        )}
                    </div>

                    <TabsContent className="pt-2" value="projects">
                        <A2aProjectList a2aServer={a2aServer} />
                    </TabsContent>

                    <TabsContent className="max-w-(--breakpoint-lg) pt-3" value="connect">
                        <A2aServerConnect a2aServer={a2aServer} />
                    </TabsContent>
                </Tabs>
            </CollapsibleContent>

            <A2aServerDialog
                a2aServer={a2aServer}
                onOpenChange={setEditDialogOpen}
                open={editDialogOpen}
                triggerNode={<span className="hidden" />}
            />

            <A2aServerWorkflowDialog
                a2aServer={a2aServer}
                onOpenChange={setAddProjectDialogOpen}
                open={addProjectDialogOpen}
            />

            <AlertDialog
                isPending={deleteA2aServerMutation.isPending}
                onCancel={() => setDeleteDialogOpen(false)}
                onConfirm={handleDelete}
                open={deleteDialogOpen}
            />
        </Collapsible>
    );
};

export default A2aServerListItem;
