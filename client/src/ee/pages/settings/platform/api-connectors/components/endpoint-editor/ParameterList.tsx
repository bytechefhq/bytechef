import Button from '@/components/Button/Button';
import {Checkbox} from '@/components/ui/checkbox';
import {
    Dialog,
    DialogClose,
    DialogCloseButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog';
import {FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {PlusIcon, Trash2Icon} from 'lucide-react';
import {KeyboardEvent, useCallback} from 'react';

import {ParameterDefinitionI} from '../../types/api-connector-wizard.types';
import {getLocationLabel} from '../../utils/endpointEditor-utils';
import useParameterList from './hooks/useParameterList';

interface ParameterListProps {
    onChange: (parameters: ParameterDefinitionI[]) => void;
    parameters: ParameterDefinitionI[];
}

const ParameterList = ({onChange, parameters}: ParameterListProps) => {
    const {
        control,
        editingParameter,
        handleAddDialog,
        handleDialogOpen,
        handleEditDialog,
        handleRemoveParameter,
        handleSaveParameter,
        handleSubmit,
        isDialogOpen,
    } = useParameterList({onChange, parameters});

    const handleKeyDown = useCallback(
        (event: KeyboardEvent<HTMLLIElement>, parameter: ParameterDefinitionI) => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();

                handleEditDialog(parameter);
            }
        },
        [handleEditDialog]
    );

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Parameters</span>

                <Button
                    icon={<PlusIcon className="size-3" />}
                    onClick={handleAddDialog}
                    size="sm"
                    type="button"
                    variant="ghost"
                >
                    Add
                </Button>
            </div>

            {parameters.length === 0 ? (
                <p className="text-xs text-muted-foreground">No parameters defined.</p>
            ) : (
                <ul className="divide-y rounded-md border text-sm">
                    {parameters.map((parameter) => {
                        const locationLabel = getLocationLabel(parameter.in);

                        return (
                            <li
                                className="flex cursor-pointer items-center justify-between p-2 hover:bg-gray-50"
                                key={parameter.id}
                                onClick={() => handleEditDialog(parameter)}
                                onKeyDown={(event) => handleKeyDown(event, parameter)}
                                role="button"
                                tabIndex={0}
                            >
                                <div className="flex items-center gap-2">
                                    <span className="rounded bg-gray-100 px-1.5 py-0.5 text-xs">{locationLabel}</span>

                                    <span className="font-medium">{parameter.name}</span>

                                    <span className="text-xs text-gray-500">({parameter.type})</span>

                                    {parameter.required && <span className="text-xs text-red-500">*</span>}
                                </div>

                                <Button
                                    icon={<Trash2Icon className="size-3" />}
                                    onClick={(event) => {
                                        event.stopPropagation();
                                        handleRemoveParameter(parameter.id);
                                    }}
                                    size="icon"
                                    type="button"
                                    variant="ghost"
                                />
                            </li>
                        );
                    })}
                </ul>
            )}

            <Dialog onOpenChange={handleDialogOpen} open={isDialogOpen}>
                <DialogContent>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleSaveParameter)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>{editingParameter ? 'Edit' : 'Add'} Parameter</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={control}
                                name="name"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Name</FormLabel>

                                        <FormControl>
                                            <Input placeholder="userId" {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                                rules={{required: 'Name is required'}}
                            />

                            <FormField
                                control={control}
                                name="in"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Location</FormLabel>

                                        <Select onValueChange={field.onChange} value={field.value}>
                                            <FormControl>
                                                <SelectTrigger>
                                                    <SelectValue />
                                                </SelectTrigger>
                                            </FormControl>

                                            <SelectContent>
                                                <SelectItem value="query">Query</SelectItem>

                                                <SelectItem value="path">Path</SelectItem>

                                                <SelectItem value="header">Header</SelectItem>
                                            </SelectContent>
                                        </Select>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={control}
                                name="type"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Type</FormLabel>

                                        <Select onValueChange={field.onChange} value={field.value}>
                                            <FormControl>
                                                <SelectTrigger>
                                                    <SelectValue />
                                                </SelectTrigger>
                                            </FormControl>

                                            <SelectContent>
                                                <SelectItem value="string">String</SelectItem>

                                                <SelectItem value="number">Number</SelectItem>

                                                <SelectItem value="integer">Integer</SelectItem>

                                                <SelectItem value="boolean">Boolean</SelectItem>

                                                <SelectItem value="array">Array</SelectItem>

                                                <SelectItem value="object">Object</SelectItem>
                                            </SelectContent>
                                        </Select>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <FormField
                                control={control}
                                name="example"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Example</FormLabel>

                                        <FormControl>
                                            <Input placeholder="123" {...field} />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>

                        <FormField
                            control={control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Input placeholder="The unique user identifier" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="required"
                            render={({field}) => (
                                <FormItem className="flex items-center gap-2 space-y-0">
                                    <FormControl>
                                        <Checkbox checked={field.value} onCheckedChange={field.onChange} />
                                    </FormControl>

                                    <FormLabel className="font-normal">Required</FormLabel>
                                </FormItem>
                            )}
                        />

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">{editingParameter ? 'Update' : 'Add'}</Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        </div>
    );
};

export default ParameterList;
