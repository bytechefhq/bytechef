import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox';
import {Input} from '@/components/Input/Input';
import RequiredMark from '@/components/RequiredMark';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Popover, PopoverClose, PopoverContent, PopoverTrigger} from '@/components/ui/popover';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {XIcon} from 'lucide-react';
import {ReactNode} from 'react';
import {z} from 'zod';

import usePropertyCodeEditorDialogRightPanelConnectionsPopover from './hooks/usePropertyCodeEditorDialogRightPanelConnectionsPopover';

export const connectionFormSchema = z.object({
    componentName: z.string().min(1, {
        message: 'Component is required.',
    }),
    componentVersion: z.number(),
    name: z.string().min(2, {
        message: 'Name must be at least 2 characters.',
    }),
});

export interface PropertyCodeEditorDialogRightPanelConnectionsPopoverProps {
    onSubmit: (values: z.infer<typeof connectionFormSchema>) => void;
    triggerNode?: ReactNode;
}

const PropertyCodeEditorDialogRightPanelConnectionsPopover = ({
    onSubmit,
    triggerNode,
}: PropertyCodeEditorDialogRightPanelConnectionsPopoverProps) => {
    const {componentDefinitions, form, open, setOpen} = usePropertyCodeEditorDialogRightPanelConnectionsPopover();

    return (
        <Popover onOpenChange={setOpen} open={open}>
            <PopoverTrigger asChild>
                {triggerNode ? triggerNode : <Button label="Add Connection" size="sm" variant="secondary" />}
            </PopoverTrigger>

            <PopoverContent align="end" className="min-w-property-code-editor-sheet-connections-sheet-width">
                <header className="flex items-center justify-between">
                    <span className="font-medium">Add Connection</span>

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
                                        <FormLabel className="gap-0">
                                            Name
                                            <RequiredMark />
                                        </FormLabel>

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
                                        <FormLabel className="gap-0">
                                            Component
                                            <RequiredMark />
                                        </FormLabel>

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
                            <Button label="Add" type="submit" />
                        </footer>
                    </form>
                </Form>
            </PopoverContent>
        </Popover>
    );
};

export default PropertyCodeEditorDialogRightPanelConnectionsPopover;
