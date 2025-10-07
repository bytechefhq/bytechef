import ComboBox from '@/components/ComboBox';
import {Button} from '@/components/ui/button';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Popover, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {zodResolver} from '@hookform/resolvers/zod';
import {PopoverClose} from '@radix-ui/react-popover';
import {XIcon} from 'lucide-react';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';

export const connectionFormSchema = z.object({
    componentName: z.string(),
    componentVersion: z.number(),
    name: z.string().min(2, {
        message: 'Name must be at least 3 characters.',
    }),
});

export interface PropertyCodeEditorSheetRightPanelConnectionsPopoverProps {
    onSubmit: (values: z.infer<typeof connectionFormSchema>) => void;
    triggerNode?: ReactNode;
}

const PropertyCodeEditorSheetRightPanelConnectionsPopover = ({
    onSubmit,
    triggerNode,
}: PropertyCodeEditorSheetRightPanelConnectionsPopoverProps) => {
    const [open, setOpen] = useState(false);

    const form = useForm<z.infer<typeof connectionFormSchema>>({
        defaultValues: {
            componentName: '',
            name: '',
        },
        resolver: zodResolver(connectionFormSchema),
    });

    const {useGetComponentDefinitionsQuery} = useWorkflowEditor();

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery!({connectionDefinitions: true});

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                {triggerNode ? (
                    triggerNode
                ) : (
                    <Button size="sm" variant="secondary">
                        Add Component
                    </Button>
                )}
            </PopoverTrigger>

            <PopoverContent align="end" className="min-w-property-code-editor-sheet-connections-sheet-width">
                <header className="flex items-center justify-between">
                    <span className="font-medium">Add Component</span>

                    <PopoverClose asChild onClick={() => form.reset()}>
                        <XIcon aria-hidden="true" className="size-4 cursor-pointer" />
                    </PopoverClose>
                </header>

                <Form {...form}>
                    <form
                        onSubmit={form.handleSubmit((values) => {
                            onSubmit(values);
                            setOpen(false);
                            form.reset();
                        })}
                    >
                        <main className="my-2 space-y-4">
                            <FormField
                                control={form.control}
                                name="name"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Name</FormLabel>

                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />

                            <FormField
                                control={form.control}
                                name="componentName"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Component</FormLabel>

                                        <FormControl>
                                            {componentDefinitions && (
                                                <ComboBox
                                                    items={componentDefinitions.map((componentDefinition) => ({
                                                        componentDefinition,
                                                        icon: componentDefinition.icon,
                                                        label: componentDefinition.title!,
                                                        value: componentDefinition.name,
                                                    }))}
                                                    name="componentName"
                                                    onBlur={field.onBlur}
                                                    onChange={(item) => {
                                                        const componentDefinition =
                                                            item?.componentDefinition as ComponentDefinitionBasic;

                                                        form.setValue('componentName', componentDefinition.name, {
                                                            shouldDirty: true,
                                                        });

                                                        form.setValue('componentVersion', componentDefinition.version, {
                                                            shouldDirty: true,
                                                        });
                                                    }}
                                                    value={field.value}
                                                />
                                            )}
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: true}}
                            />
                        </main>

                        <footer className="flex items-center justify-end space-x-2">
                            <Button type="submit">Add</Button>
                        </footer>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

export default PropertyCodeEditorSheetRightPanelConnectionsPopover;
