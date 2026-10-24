import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Input} from '@/components/Input/Input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import getDataSyncPath from '@/pages/automation/data-syncs/utils/getDataSyncPath';
import invalidateDataSyncQueries from '@/pages/automation/data-syncs/utils/invalidateDataSyncQueries';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useCreateDataSyncMutation, useUpdateDataSyncMutation} from '@/shared/middleware/graphql';
import {useGetWorkspaceProjectsQuery} from '@/shared/queries/automation/projects.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate} from 'react-router-dom';
import {z} from 'zod';

const formSchema = z.object({
    description: z.string(),
    projectId: z.string().optional(),
    title: z.string().min(1, {message: 'Title is required'}),
});

type FormValuesType = z.infer<typeof formSchema>;

interface DataSyncDialogProps {
    dataSync?: {description?: string | null; id: string; title: string};
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    projectId?: number;
    triggerNode?: ReactNode;
}

const DataSyncDialog = ({
    dataSync,
    onOpenChange,
    open: controlledOpen,
    projectId,
    triggerNode,
}: DataSyncDialogProps) => {
    const [uncontrolledOpen, setUncontrolledOpen] = useState(false);

    const open = controlledOpen ?? uncontrolledOpen;

    const setOpen = (nextOpen: boolean) => {
        (onOpenChange ?? setUncontrolledOpen)(nextOpen);
    };

    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const navigate = useNavigate();
    const queryClient = useQueryClient();

    const {data: projects} = useGetWorkspaceProjectsQuery({id: currentWorkspaceId}, !dataSync && !projectId);

    const form = useForm<FormValuesType>({
        defaultValues: {
            description: dataSync?.description ?? '',
            projectId: projectId ? String(projectId) : 'new',
            title: dataSync?.title ?? '',
        },
        resolver: zodResolver(formSchema),
    });

    const createDataSyncMutation = useCreateDataSyncMutation();
    const updateDataSyncMutation = useUpdateDataSyncMutation();

    const onSubmit = (values: FormValuesType) => {
        if (dataSync) {
            updateDataSyncMutation.mutate(
                {input: {description: values.description, id: dataSync.id, title: values.title}},
                {
                    onSuccess: () => {
                        invalidateDataSyncQueries(queryClient);

                        setOpen(false);
                    },
                }
            );

            return;
        }

        createDataSyncMutation.mutate(
            {
                input: {
                    description: values.description,
                    projectId: values.projectId && values.projectId !== 'new' ? values.projectId : undefined,
                    title: values.title,
                    workspaceId: currentWorkspaceId + '',
                },
            },
            {
                onSuccess: (data) => {
                    invalidateDataSyncQueries(queryClient, {projects: true});

                    setOpen(false);

                    navigate(getDataSyncPath(data.createDataSync));
                },
            }
        );

        form.reset({});
    };

    return (
        <Dialog onOpenChange={setOpen} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogMain>
                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(onSubmit)}>
                            <DialogHeader
                                description={
                                    dataSync
                                        ? "Change the data sync's name and description."
                                        : 'Create a new data sync by filling out the form below.'
                                }
                                title={dataSync ? 'Edit Data Sync' : 'Create Data Sync'}
                            />

                            <DialogBody>
                                <FormField
                                    control={form.control}
                                    name="title"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Title</FormLabel>

                                            <FormControl>
                                                <Input placeholder="Enter data sync title" {...field} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                {!dataSync && !projectId && (
                                    <FormField
                                        control={form.control}
                                        name="projectId"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Project</FormLabel>

                                                <FormControl>
                                                    <Select onValueChange={field.onChange} value={field.value}>
                                                        <SelectTrigger>
                                                            <SelectValue />
                                                        </SelectTrigger>

                                                        <SelectContent>
                                                            <SelectItem value="new">
                                                                New project named after this data sync
                                                            </SelectItem>

                                                            {projects?.map((project) => (
                                                                <SelectItem key={project.id} value={String(project.id)}>
                                                                    {project.name}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

                                <FormField
                                    control={form.control}
                                    name="description"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Description</FormLabel>

                                            <FormControl>
                                                <Textarea
                                                    placeholder="Enter a description for the data sync"
                                                    rows={3}
                                                    {...field}
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default DataSyncDialog;
