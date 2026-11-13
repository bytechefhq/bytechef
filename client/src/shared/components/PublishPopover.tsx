import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {PUBLISH_DESCRIPTION_PLACEHOLDER} from '@/shared/constants';
import {zodResolver} from '@hookform/resolvers/zod';
import {SendIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';
import {twMerge} from 'tailwind-merge';
import * as z from 'zod';

const formSchema = z.object({
    description: z.string().max(256).optional(),
});

interface PublishPopoverProps {
    disabled?: boolean;
    isPending: boolean;
    onPublishSubmit: ({description, onSuccess}: {description?: string; onSuccess: () => void}) => void;
    submitButtonClassName?: string;
    title: string;
    tooltip: string;
    triggerButtonClassName?: string;
}

const PublishPopover = ({
    disabled,
    isPending,
    onPublishSubmit,
    submitButtonClassName,
    title,
    tooltip,
    triggerButtonClassName,
}: PublishPopoverProps) => {
    const [open, setOpen] = useState(false);

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
    });

    const {control, handleSubmit, reset} = form;

    const handlePublish = ({description}: {description?: string}) => {
        onPublishSubmit({
            description,
            onSuccess: () => {
                reset();
                setOpen(false);
            },
        });
    };

    if (disabled) {
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

                <TooltipContent>No changes to publish</TooltipContent>
            </Tooltip>
        );
    }

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <Tooltip>
                <PopoverTrigger asChild>
                    <TooltipTrigger asChild>
                        <Button
                            className={twMerge(
                                'data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary',
                                triggerButtonClassName
                            )}
                            icon={<SendIcon />}
                            id="publish-button"
                            label="Publish"
                            variant="outline"
                        />
                    </TooltipTrigger>
                </PopoverTrigger>

                <TooltipContent>{tooltip}</TooltipContent>
            </Tooltip>

            <PopoverContent
                align="end"
                className="flex h-full w-96 flex-col justify-between space-y-4"
                id="publish-popover"
            >
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handlePublish)}>
                        <h3 className="font-semibold">{title}</h3>

                        <div className="flex-1">
                            <FormField
                                control={control}
                                name="description"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Description</FormLabel>

                                        <FormControl>
                                            <Textarea
                                                className="h-28"
                                                placeholder={PUBLISH_DESCRIPTION_PLACEHOLDER}
                                                {...field}
                                            ></Textarea>
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <div className="flex justify-end">
                            <Button
                                aria-label="Publish button"
                                className={submitButtonClassName}
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
