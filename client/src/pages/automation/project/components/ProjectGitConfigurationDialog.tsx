import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import Switch from '@/components/Switch/Switch';
import {Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
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
                <DialogMain>
                    <DialogHeader
                        description="Set the repository branch where the project will be saved."
                        title="Update Git Configuration"
                    />

                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={form.handleSubmit(handleSubmit)}>
                            <DialogBody>
                                <div className="grid gap-4">
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

                                                <FormDescription>
                                                    This is the branch name of a git repository.
                                                </FormDescription>

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
                                                        <Switch
                                                            checked={field.value}
                                                            onCheckedChange={field.onChange}
                                                        />
                                                    </FormControl>
                                                </div>

                                                <FormDescription>
                                                    Enable git configuration for this project.
                                                </FormDescription>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                </div>
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

export default ProjectGitConfigurationDialog;
