import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {zodResolver} from '@hookform/resolvers/zod';
import {SendIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import * as z from 'zod';

const formSchema = z.object({
    description: z.string().max(256).optional(),
});

const PublishPopover = ({
    isPending,
    onPublishProjectSubmit,
}: {
    isPending: boolean;
    onPublishProjectSubmit: ({description, onSuccess}: {description?: string; onSuccess: () => void}) => void;
}) => {
    const [open, setOpen] = useState(false);

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const handlePublishProject = ({description}: {description?: string}) => {
        onPublishProjectSubmit({
            description,
            onSuccess: () => {
                reset();
                setOpen(false);
            },
        });
    };

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <Tooltip>
                <PopoverTrigger asChild>
                    <TooltipTrigger asChild>
                        <Button
                            className="[&[data-state=open]]:border-stroke-brand-secondary [&[data-state=open]]:bg-surface-brand-secondary [&[data-state=open]]:text-content-brand-primary"
                            icon={<SendIcon />}
                            label="Publish"
                            variant="outline"
                        />
                    </TooltipTrigger>
                </PopoverTrigger>

                <TooltipContent>Publish the project</TooltipContent>
            </Tooltip>

            <PopoverContent align="end" className="flex h-full w-96 flex-col justify-between space-y-4">
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
