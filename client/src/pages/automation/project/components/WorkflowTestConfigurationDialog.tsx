import Properties from '@/components/Properties/Properties';
import {Button} from '@/components/ui/button';
import {
    Dialog,
    DialogClose,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {WorkflowConnectionModel, WorkflowModel} from '@/middleware/helios/configuration';
import {TaskConnectionModel, TriggerOutputModel} from '@/middleware/helios/execution';
import ConnectionDialog from '@/pages/automation/connections/components/ConnectionDialog';
import {useGetComponentDefinitionQuery} from '@/queries/componentDefinitions.queries';
import {useGetConnectionsQuery} from '@/queries/connections.queries';
import {PropertyType} from '@/types/projectTypes';
import Editor from '@monaco-editor/react';
import {Cross2Icon} from '@radix-ui/react-icons';
import * as Portal from '@radix-ui/react-portal';
import {PlusIcon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

interface WorkflowTestConfigurationDialogProps {
    onClose: () => void;
    onRunClick: ({
        connections,
        inputs,
        triggerOutputs,
    }: {
        connections: TaskConnectionModel[];
        inputs: {[key: string]: object};
        triggerOutputs: TriggerOutputModel[];
    }) => void;
    workflow: WorkflowModel;
}

const WorkflowTestConfigurationDialog = ({onClose, onRunClick, workflow}: WorkflowTestConfigurationDialogProps) => {
    const [showNewConnectionDialog, setShowNewConnectionDialog] = useState(false);
    const [workflowConnection, setWorkflowConnection] = useState<WorkflowConnectionModel | undefined>();

    const form = useForm<{
        connections: TaskConnectionModel[];
        inputs: {[key: string]: object};
        triggerOutputs: TriggerOutputModel[];
    }>();

    const {formState, handleSubmit, register} = form;

    const {data: connections} = useGetConnectionsQuery({});

    let workflowConnections: WorkflowConnectionModel[] = [];

    if (workflow.tasks) {
        workflowConnections = workflow.tasks
            .flatMap((task) => (task.connections ? task.connections : []))
            .filter((workflowConnection) => !workflowConnection.id);
    }

    /* eslint-disable @typescript-eslint/no-non-null-asserted-optional-chain */
    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {
            componentName: workflowConnection?.componentName!,
            componentVersion: workflowConnection?.componentVersion!,
        },
        !!workflowConnection
    );

    return (
        <Dialog onOpenChange={onClose} open={true}>
            <DialogContent className="sm:max-w-[425px]" onInteractOutside={(event) => event.preventDefault()}>
                <Form {...form}>
                    <form onSubmit={handleSubmit((values) => onRunClick(values))}>
                        <DialogHeader>
                            <div className="flex items-center justify-between">
                                <DialogTitle>Workflow Test Configuration</DialogTitle>

                                <DialogClose asChild>
                                    <Button size="icon" variant="ghost">
                                        <Cross2Icon className="h-4 w-4 opacity-70" />
                                    </Button>
                                </DialogClose>
                            </div>

                            <DialogDescription>
                                Set workflow input and trigger output values. Click save when you are done.
                            </DialogDescription>
                        </DialogHeader>

                        <div className="grid gap-4 py-4">
                            {workflow.inputs && workflow.inputs.length > 0 && (
                                <>
                                    <Label className="text-gray-500">Inputs</Label>

                                    <Properties
                                        formState={formState}
                                        path="inputs"
                                        properties={workflow.inputs.map((input) => {
                                            if (input.type === 'string') {
                                                return {
                                                    controlType: 'TEXT',
                                                    type: 'STRING',
                                                    ...input,
                                                } as PropertyType;
                                            } else if (input.type === 'number') {
                                                return {
                                                    type: 'NUMBER',
                                                    ...input,
                                                } as PropertyType;
                                            } else {
                                                return {
                                                    controlType: 'SELECT',
                                                    type: 'BOOLEAN',
                                                    ...input,
                                                } as PropertyType;
                                            }
                                        })}
                                        register={register}
                                    />
                                </>
                            )}

                            {workflow.triggers &&
                                workflow.triggers.map((workflowTrigger, index) => (
                                    <div key={index}>
                                        <Label className="text-gray-500">Trigger Outputs</Label>

                                        <FormField
                                            control={form.control}
                                            name={`triggerOutputs.${index}.value`}
                                            render={() => (
                                                <FormItem>
                                                    <FormLabel>
                                                        {`${workflowTrigger.label} `}

                                                        <span className="text-xs text-gray-500">
                                                            {`(${workflowTrigger.name})`}
                                                        </span>
                                                    </FormLabel>

                                                    <Editor
                                                        className="h-[120px] rounded-md border border-input shadow-sm"
                                                        defaultLanguage={workflow.format?.toLowerCase()}
                                                        onChange={(value) => {
                                                            try {
                                                                form.setValue(
                                                                    `triggerOutputs.${index}.value`,
                                                                    value ? JSON.parse(value) : undefined
                                                                );
                                                            } catch (e) {
                                                                form.setValue(
                                                                    `triggerOutputs.${index}.value`,
                                                                    value ? eval(value) : undefined
                                                                );
                                                            }
                                                        }}
                                                    />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            defaultValue={workflowTrigger.name}
                                            name={`triggerOutputs.${index}.triggerName`}
                                            render={({field}) => <input type="hidden" {...field} />}
                                        />
                                    </div>
                                ))}

                            {workflowConnections &&
                                workflowConnections.map((workflowConnection, index) => (
                                    <div key={index}>
                                        <Label className="text-gray-500">Connections</Label>

                                        <FormField
                                            control={form.control}
                                            name={`connections.${index}.id`}
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>
                                                        {'Connection '}

                                                        <span className="text-xs text-gray-500">
                                                            {`(${workflowConnection.key})`}
                                                        </span>
                                                    </FormLabel>

                                                    <Select
                                                        defaultValue={field.value ? field.value.toString() : undefined}
                                                        onValueChange={field.onChange}
                                                    >
                                                        <FormControl>
                                                            <div className="flex space-x-2">
                                                                <SelectTrigger>
                                                                    <SelectValue placeholder="Choose Connection..." />
                                                                </SelectTrigger>

                                                                <Button
                                                                    className="mt-auto p-2"
                                                                    onClick={() => {
                                                                        setWorkflowConnection(workflowConnection);
                                                                        setShowNewConnectionDialog(true);
                                                                    }}
                                                                    title="Create a new connection"
                                                                    type="button"
                                                                    variant="outline"
                                                                >
                                                                    <PlusIcon className="h-5 w-5" />
                                                                </Button>
                                                            </div>
                                                        </FormControl>

                                                        <SelectContent>
                                                            {connections &&
                                                                connections
                                                                    .filter(
                                                                        (connection) =>
                                                                            connection.componentName ===
                                                                            workflowConnection.componentName
                                                                    )
                                                                    .map((connection) => (
                                                                        <SelectItem
                                                                            key={connection.id}
                                                                            value={connection.id!.toString()}
                                                                        >
                                                                            <div className="flex items-center">
                                                                                <span className="mr-1 ">
                                                                                    {connection.name}
                                                                                </span>

                                                                                <span className="text-xs text-gray-500">
                                                                                    {connection?.tags
                                                                                        ?.map((tag) => tag.name)
                                                                                        .join(', ')}
                                                                                </span>
                                                                            </div>
                                                                        </SelectItem>
                                                                    ))}
                                                        </SelectContent>
                                                    </Select>

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                            rules={{
                                                required: workflowConnection.required,
                                            }}
                                        />

                                        <FormField
                                            control={form.control}
                                            defaultValue={workflowConnection.key}
                                            name={`connections.${index}.key`}
                                            render={({field}) => <input type="hidden" {...field} />}
                                        />

                                        <FormField
                                            control={form.control}
                                            defaultValue={workflowConnection.operationName}
                                            name={`connections.${index}.taskName`}
                                            render={({field}) => <input type="hidden" {...field} />}
                                        />
                                    </div>
                                ))}
                        </div>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">Run workflow</Button>
                        </DialogFooter>
                    </form>
                </Form>

                {showNewConnectionDialog && (
                    <Portal.Root>
                        <ConnectionDialog
                            componentDefinition={componentDefinition}
                            onClose={() => setShowNewConnectionDialog(false)}
                        />
                    </Portal.Root>
                )}
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowTestConfigurationDialog;
