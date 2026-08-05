import Button from '@/components/Button/Button';
import {Input} from '@/components/Input/Input';
import RequiredMark from '@/components/RequiredMark';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Checkbox} from '@/components/ui/checkbox';
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
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {useWorkflowEditor} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {resolveComponentInputGroup} from '@/shared/components/InputConfigurationList';
import {Workflow, WorkflowInput} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {WorkflowInputType} from '@/shared/types';
import {RefObject, useEffect, useRef} from 'react';
import {Control, FieldValues, UseFormReturn, useWatch} from 'react-hook-form';

import WorkflowInputComponentTestValue from './WorkflowInputComponentTestValue';
import {getBackingWorkflowNodeName} from './utils/getBackingWorkflowNodeName';
import {getComponentInputGroupLabel} from './utils/getComponentInputGroupLabel';
import {getComponentPropertyInputValues} from './utils/getComponentPropertyInputValues';
import getWorkflowComponentNames from './utils/getWorkflowComponentNames';
import {getWorkflowComponentVersions} from './utils/getWorkflowComponentVersions';

interface WorkflowInputsEditDialogProps {
    closeDialog: () => void;
    currentInputIndex?: number;
    form: UseFormReturn<WorkflowInputType, unknown, WorkflowInputType>;
    internalOnlyVisible: boolean;
    isEditDialogOpen: boolean;
    nameInputRef: RefObject<HTMLInputElement | null>;
    openEditDialog: (index?: number) => void;
    saveWorkflowInput: (input: WorkflowInputType) => void;
    workflow: Workflow;
}

const WorkflowInputsEditDialog = ({
    closeDialog,
    currentInputIndex,
    form,
    internalOnlyVisible,
    isEditDialogOpen,
    nameInputRef,
    openEditDialog,
    saveWorkflowInput,
    workflow,
}: WorkflowInputsEditDialogProps) => {
    const {codeWorkflow, useGetComponentDefinitionsQuery} = useWorkflowEditor();

    // A code workflow's inputs are generated from its source on every save, so editing a declaration here would
    // only be undone by the next one. The test value is not source-owned and stays editable.
    const declarationReadOnly = codeWorkflow === true;

    const selectedType = useWatch({control: form.control, name: 'type'});
    const selectedComponentName = useWatch({control: form.control, name: 'componentReference.componentName'});
    const selectedGroupName = useWatch({control: form.control, name: 'componentReference.groupName'});

    // setValue on fields that are not bound to a registered <FormField> does not notify their
    // useWatch subscribers, so drive the dependent values explicitly through field.onChange below.

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({});

    // A component input borrows the connection of a workflow node already using that component, so only components
    // present in the workflow are offerable. Of those, keep the ones that declare selectable inputs (inputsCount > 0)
    // so the Property picker is never empty. Map each to its display title so the dropdown shows the label, not the name.
    const inputComponentTitles = new Map(
        (componentDefinitions ?? [])
            .filter((componentDefinition) => (componentDefinition.inputsCount ?? 0) > 0)
            .map((componentDefinition) => [
                componentDefinition.name,
                componentDefinition.title ?? componentDefinition.name,
            ])
    );

    const componentNames = getWorkflowComponentNames(workflow).filter((componentName) =>
        inputComponentTitles.has(componentName)
    );

    const componentVersions = getWorkflowComponentVersions(workflow);

    const resolvedComponentVersion = selectedComponentName ? componentVersions[selectedComponentName] : undefined;

    const {data: componentDefinition} = useGetComponentDefinitionQuery(
        {componentName: selectedComponentName ?? '', componentVersion: resolvedComponentVersion ?? 1},
        !!selectedComponentName
    );

    const selectedPropertyOrGroup = selectedGroupName ? `group:${selectedGroupName}` : '';

    const backingWorkflowNodeName = getBackingWorkflowNodeName(workflow, selectedComponentName);

    const resolvedComponentGroup =
        selectedType === 'component' && selectedComponentName && selectedGroupName
            ? resolveComponentInputGroup(
                  {
                      componentReference: {
                          componentName: selectedComponentName,
                          componentVersion: resolvedComponentVersion,
                          groupName: selectedGroupName,
                      },
                      label: form.getValues('label'),
                      name: form.getValues('name'),
                  } as WorkflowInput,
                  componentDefinition
              )
            : undefined;

    const testValueInputTypeMap: Record<string, string> = {
        date: 'date',
        date_time: 'datetime-local',
        integer: 'number',
        number: 'number',
        time: 'time',
    };

    const testValueInputType = (selectedType && testValueInputTypeMap[selectedType]) ?? 'text';

    const handleComponentChange = (value: string, onChange: (value: string) => void) => {
        onChange(value);

        form.setValue('componentReference.componentVersion', componentVersions[value]);
        form.setValue('componentReference.groupName', undefined);
    };

    const handlePropertyOrGroupChange = (value: string) => {
        const values = getComponentPropertyInputValues({
            componentDefinition,
            componentName: selectedComponentName ?? '',
            componentVersion: resolvedComponentVersion ?? 1,
            currentLabel: form.getValues('label') ?? '',
            currentName: form.getValues('name') ?? '',
            selection: value,
        });

        form.setValue('componentReference.componentName', values.componentName);
        form.setValue('componentReference.componentVersion', values.componentVersion);
        form.setValue('componentReference.groupName', values.groupName);

        if (values.name !== undefined) {
            form.setValue('name', values.name);
        }

        if (values.label !== undefined) {
            form.setValue('label', values.label);
        }
    };

    const previousTypeRef = useRef<string | undefined>(undefined);

    // Clear the scalar test value only when the user actually switches the type to a non-component type, not on
    // the initial load of an existing input — otherwise editing a saved string/number input would wipe its
    // persisted test value before it can be shown.
    useEffect(() => {
        if (
            previousTypeRef.current !== undefined &&
            previousTypeRef.current !== selectedType &&
            selectedType !== 'component'
        ) {
            form.setValue('testValue', '');
        }

        previousTypeRef.current = selectedType;
    }, [form, selectedType]);

    return (
        <Dialog
            onOpenChange={(open) => {
                if (open) {
                    openEditDialog(currentInputIndex);
                } else {
                    closeDialog();
                }
            }}
            open={isEditDialogOpen}
        >
            <DialogContent>
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={form.handleSubmit(saveWorkflowInput)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${currentInputIndex === -1 ? 'Create a new' : 'Edit'} Input`}</DialogTitle>

                                <DialogDescription>
                                    {declarationReadOnly
                                        ? "Declared in the workflow's source — only the test value can be changed here."
                                        : 'Add a new workflow input definition.'}
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={form.control}
                            name="type"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>
                                        Type <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <Select
                                            disabled={declarationReadOnly}
                                            onValueChange={field.onChange}
                                            value={field.value ?? ''}
                                        >
                                            <SelectTrigger className="w-full">
                                                <SelectValue placeholder="Select input type" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                <SelectItem value="component">Component property</SelectItem>

                                                <SelectItem value="boolean">Boolean</SelectItem>

                                                <SelectItem value="date">Date</SelectItem>

                                                <SelectItem value="date_time">Date Time</SelectItem>

                                                <SelectItem value="field_mapping">Field Mapping</SelectItem>

                                                <SelectItem value="integer">Integer</SelectItem>

                                                <SelectItem value="number">Number</SelectItem>

                                                <SelectItem value="string">String</SelectItem>

                                                <SelectItem value="time">Time</SelectItem>
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                        />

                        {selectedType === 'component' && (
                            <>
                                <FormField
                                    control={form.control}
                                    name="componentReference.componentName"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>
                                                Component <RequiredMark />
                                            </FormLabel>

                                            <FormControl>
                                                <Select
                                                    onValueChange={(value) =>
                                                        handleComponentChange(value, field.onChange)
                                                    }
                                                    value={field.value ?? ''}
                                                >
                                                    <SelectTrigger className="w-full">
                                                        <SelectValue placeholder="Select component" />
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        {componentNames.map((currentComponentName) => (
                                                            <SelectItem
                                                                key={currentComponentName}
                                                                value={currentComponentName}
                                                            >
                                                                {inputComponentTitles.get(currentComponentName) ??
                                                                    currentComponentName}
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                    rules={{required: true}}
                                />

                                <FormItem>
                                    <FormLabel>
                                        Input <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <Select
                                            disabled={!selectedComponentName}
                                            onValueChange={handlePropertyOrGroupChange}
                                            value={selectedPropertyOrGroup}
                                        >
                                            <SelectTrigger className="w-full">
                                                <SelectValue placeholder="Select input" />
                                            </SelectTrigger>

                                            <SelectContent>
                                                {componentDefinition?.inputs?.map((group) => (
                                                    <SelectItem
                                                        key={`group:${group.name}`}
                                                        value={`group:${group.name}`}
                                                    >
                                                        {getComponentInputGroupLabel(group)}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            </>
                        )}

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
                                        <Input
                                            {...field}
                                            placeholder="Input name (will be used as a dynamic value key)"
                                            readOnly={currentInputIndex !== -1 || declarationReadOnly}
                                            ref={nameInputRef}
                                        />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                        />

                        <FormField
                            control={form.control}
                            name="label"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel className="gap-0">
                                        Label
                                        <RequiredMark />
                                    </FormLabel>

                                    <FormControl>
                                        <Input {...field} placeholder="Input label" readOnly={declarationReadOnly} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                        />

                        <FormField
                            control={form.control}
                            name="required"
                            render={({field}) => (
                                <FormItem>
                                    <div className="flex items-center space-x-2">
                                        <FormControl>
                                            <Checkbox
                                                checked={!!field.value}
                                                disabled={declarationReadOnly}
                                                id="required"
                                                onCheckedChange={field.onChange}
                                            />
                                        </FormControl>

                                        <FormLabel htmlFor="required">Required</FormLabel>
                                    </div>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        {internalOnlyVisible && (
                            <FormField
                                control={form.control}
                                name="internalOnly"
                                render={({field}) => (
                                    <FormItem>
                                        <div className="flex items-center space-x-2">
                                            <FormControl>
                                                <Checkbox
                                                    checked={!!field.value}
                                                    disabled={declarationReadOnly}
                                                    id="internalOnly"
                                                    onCheckedChange={field.onChange}
                                                />
                                            </FormControl>

                                            <FormLabel htmlFor="internalOnly">Internal only</FormLabel>
                                        </div>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

                        {selectedType === 'field_mapping' ? (
                            <FormField
                                control={form.control}
                                name="testValue"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Test Value</FormLabel>

                                        <FormControl>
                                            <textarea
                                                {...field}
                                                className="min-h-40 w-full rounded-md border p-2 font-mono text-sm"
                                                data-testid="field-mapping-json-editor"
                                                placeholder='{"Contacts": {"applicationFields": [], "integrationFields": [], "objectTypes": []}}'
                                                value={field.value ?? ''}
                                            />
                                        </FormControl>

                                        <FormMessage />

                                        <p className="text-sm text-content-neutral-secondary">
                                            Static mapObjectFields-shaped sample; the top-level key is the object name.
                                        </p>
                                    </FormItem>
                                )}
                            />
                        ) : selectedType === 'component' ? (
                            <fieldset className="space-y-2 border-0 p-0">
                                <FormLabel>Test Value</FormLabel>

                                {resolvedComponentGroup ? (
                                    <WorkflowInputComponentTestValue
                                        backingWorkflowNodeName={backingWorkflowNodeName}
                                        control={form.control as unknown as Control<FieldValues>}
                                        members={resolvedComponentGroup.members}
                                        workflowId={workflow.id}
                                    />
                                ) : (
                                    <p className="text-sm text-content-neutral-secondary">
                                        Select an input to set its test value.
                                    </p>
                                )}

                                <p className="text-sm text-content-neutral-secondary">Configured at deployment time.</p>
                            </fieldset>
                        ) : (
                            <FormField
                                control={form.control}
                                name="testValue"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Test Value</FormLabel>

                                        <FormControl>
                                            {selectedType === 'boolean' ? (
                                                <Select
                                                    onValueChange={(value) => field.onChange(value)}
                                                    value={field.value ?? ''}
                                                >
                                                    <SelectTrigger className="w-full">
                                                        <SelectValue placeholder="Select value" />
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        <SelectItem value="true">True</SelectItem>

                                                        <SelectItem value="false">False</SelectItem>
                                                    </SelectContent>
                                                </Select>
                                            ) : (
                                                <Input {...field} placeholder="Enter value" type={testValueInputType} />
                                            )}
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        )}

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

export default WorkflowInputsEditDialog;
