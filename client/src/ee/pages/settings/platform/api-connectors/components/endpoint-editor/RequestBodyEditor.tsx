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
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import {EditIcon, PlusIcon, Trash2Icon} from 'lucide-react';

import {RequestBodyDefinitionI} from '../../types/api-connector-wizard.types';
import useRequestBodyEditor from './hooks/useRequestBodyEditor';

interface RequestBodyEditorProps {
    onChange: (requestBody?: RequestBodyDefinitionI) => void;
    requestBody?: RequestBodyDefinitionI;
}

const RequestBodyEditor = ({onChange, requestBody}: RequestBodyEditorProps) => {
    const {
        control,
        handleDialogOpen,
        handleOpenDialog,
        handleRemoveRequestBody,
        handleSaveRequestBody,
        handleSubmit,
        isDialogOpen,
    } = useRequestBodyEditor({onChange, requestBody});

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Request Body</span>

                {!requestBody ? (
                    <Button
                        icon={<PlusIcon className="size-3" />}
                        onClick={handleOpenDialog}
                        size="sm"
                        type="button"
                        variant="ghost"
                    >
                        Add
                    </Button>
                ) : (
                    <div className="flex gap-1">
                        <Button
                            aria-label="Edit request body"
                            icon={<EditIcon className="size-3" />}
                            onClick={handleOpenDialog}
                            size="icon"
                            type="button"
                            variant="ghost"
                        />

                        <Button
                            aria-label="Remove request body"
                            icon={<Trash2Icon className="size-3" />}
                            onClick={handleRemoveRequestBody}
                            size="icon"
                            type="button"
                            variant="ghost"
                        />
                    </div>
                )}
            </div>

            {requestBody ? (
                <div className="rounded-md border p-2 text-sm">
                    <div className="flex items-center gap-2">
                        <span className="rounded bg-gray-100 px-1.5 py-0.5 text-xs">{requestBody.contentType}</span>

                        {requestBody.required && <span className="text-xs text-red-500">Required</span>}
                    </div>

                    {requestBody.description && <p className="mt-1 text-xs text-gray-500">{requestBody.description}</p>}
                </div>
            ) : (
                <p className="text-xs text-muted-foreground">No request body defined.</p>
            )}

            <Dialog onOpenChange={handleDialogOpen} open={isDialogOpen}>
                <DialogContent className="max-w-lg">
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleSaveRequestBody)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>{requestBody ? 'Edit' : 'Add'} Request Body</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="contentType"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Content Type</FormLabel>

                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue />
                                            </SelectTrigger>
                                        </FormControl>

                                        <SelectContent>
                                            <SelectItem value="application/json">application/json</SelectItem>

                                            <SelectItem value="application/xml">application/xml</SelectItem>

                                            <SelectItem value="multipart/form-data">multipart/form-data</SelectItem>

                                            <SelectItem value="application/x-www-form-urlencoded">
                                                application/x-www-form-urlencoded
                                            </SelectItem>

                                            <SelectItem value="text/plain">text/plain</SelectItem>
                                        </SelectContent>
                                    </Select>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Input placeholder="Request body description" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

                        <FormField
                            control={control}
                            name="schema"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>JSON Schema</FormLabel>

                                    <FormControl>
                                        <div className="h-48 overflow-hidden rounded-md border">
                                            <MonacoEditorWrapper
                                                defaultLanguage="json"
                                                onChange={(value) => field.onChange(value || '')}
                                                onMount={() => {}}
                                                options={{
                                                    automaticLayout: true,
                                                    folding: true,
                                                    fontSize: 12,
                                                    lineNumbers: 'on',
                                                    minimap: {enabled: false},
                                                    scrollBeyondLastLine: false,
                                                    tabSize: 2,
                                                    wordWrap: 'on',
                                                }}
                                                value={field.value}
                                            />
                                        </div>
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{
                                required: 'Schema is required',
                                validate: (value: string) => {
                                    if (!value || value.trim().length === 0) {
                                        return 'Schema is required';
                                    }

                                    try {
                                        JSON.parse(value);

                                        return true;
                                    } catch {
                                        return 'Schema must be valid JSON';
                                    }
                                },
                            }}
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

                            <Button type="submit">{requestBody ? 'Update' : 'Add'}</Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        </div>
    );
};

export default RequestBodyEditor;
