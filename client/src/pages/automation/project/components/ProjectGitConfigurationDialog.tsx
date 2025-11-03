import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Switch} from '@/components/ui/switch';
import {ProjectGitConfiguration} from '@/ee/shared/middleware/automation/configuration';
import {useGetProjectRemoteBranchesQuery} from '@/ee/shared/mutations/automation/projectGit.queries';
import {zodResolver} from '@hookform/resolvers/zod';
import React from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

const formSchema = z.object({
    branch: z.string().min(2, {
        message: 'Branch must be at least 2 characters.',
    }),
    enabled: z.boolean(),
});

const ProjectGitConfigurationDialog = ({
    onClose,
    onUpdateProjectGitConfigurationSubmit,
    projectGitConfiguration,
    projectId,
}: {
    onClose: () => void;
    onUpdateProjectGitConfigurationSubmit: ({
        onSuccess,
        projectGitConfiguration,
    }: {
        projectGitConfiguration: z.infer<typeof formSchema>;
        onSuccess: () => void;
    }) => void;
    projectGitConfiguration?: ProjectGitConfiguration;
    projectId: number;
}) => {
    const form = useForm<z.infer<typeof formSchema>>({
        defaultValues: {
            branch: projectGitConfiguration?.branch || '',
            enabled: projectGitConfiguration?.enabled || false,
        },
        resolver: zodResolver(formSchema),
    });

    const {data: remoteBranches, isLoading: isLoadingBranches} = useGetProjectRemoteBranchesQuery(
        projectId,
        !!projectId
    );

    function handleSubmit(projectGitConfiguration: z.infer<typeof formSchema>) {
        onUpdateProjectGitConfigurationSubmit({
            onSuccess: onClose,
            projectGitConfiguration,
        });
    }

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent>
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <div className="flex flex-col space-y-1">
                        <DialogTitle>Update Git Configuration</DialogTitle>

                        <DialogDescription>
                            Set the repository branch where the project will be saved.
                        </DialogDescription>
                    </div>

                    <DialogCloseButton />
                </DialogHeader>

                <Form {...form}>
                    <form className="space-y-8" onSubmit={form.handleSubmit(handleSubmit)}>
                        <div className="grid gap-4 py-4">
                            <FormField
                                control={form.control}
                                name="branch"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Branch</FormLabel>

                                        <FormControl>
                                            <Select
                                                disabled={isLoadingBranches}
                                                onValueChange={field.onChange}
                                                value={field.value}
                                            >
                                                <SelectTrigger>
                                                    <SelectValue
                                                        placeholder={
                                                            isLoadingBranches
                                                                ? 'Loading branches...'
                                                                : 'Select a branch'
                                                        }
                                                    />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    {remoteBranches?.map((branch) => (
                                                        <SelectItem key={branch} value={branch}>
                                                            {branch}
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </FormControl>

                                        <FormDescription>This is the branch name of a git repository.</FormDescription>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={form.control}
                                name="enabled"
                                render={({field}) => (
                                    <FormItem>
                                        <div className="flex items-center gap-2">
                                            <FormLabel>Enabled</FormLabel>

                                            <FormControl>
                                                <Switch checked={field.value} onCheckedChange={field.onChange} />
                                            </FormControl>
                                        </div>

                                        <FormDescription>Enable git configuration for this project.</FormDescription>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button label="Cancel" type="button" variant="outline" />
                            </DialogClose>

                            <Button label="Save" type="submit" />
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default ProjectGitConfigurationDialog;
