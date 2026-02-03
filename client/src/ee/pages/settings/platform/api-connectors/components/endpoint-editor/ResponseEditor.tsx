import Button from '@/components/Button/Button';
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
import {PlusIcon, Trash2Icon} from 'lucide-react';

import {ResponseDefinitionI} from '../../types/api-connector-wizard.types';
import {getStatusCodeColor} from '../../utils/endpointEditor-utils';
import useResponseEditor from './hooks/useResponseEditor';

interface ResponseEditorProps {
    onChange: (responses: ResponseDefinitionI[]) => void;
    responses: ResponseDefinitionI[];
}

const ResponseEditor = ({onChange, responses}: ResponseEditorProps) => {
    const {
        control,
        editingResponse,
        handleAddDialog,
        handleDialogOpen,
        handleEditDialog,
        handleRemoveResponse,
        handleSaveResponse,
        handleSubmit,
        isDialogOpen,
        responsesWithIds,
    } = useResponseEditor({onChange, responses});

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Responses</span>

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

            {responsesWithIds.length === 0 ? (
                <p className="text-xs text-muted-foreground">No responses defined.</p>
            ) : (
                <ul className="divide-y rounded-md border text-sm">
                    {responsesWithIds.map((response) => (
                        <li
                            className="flex cursor-pointer items-center justify-between p-2 hover:bg-gray-50"
                            key={response.id}
                            onClick={() => handleEditDialog(response)}
                            onKeyDown={(event) => {
                                if (event.key === 'Enter' || event.key === ' ') {
                                    event.preventDefault();
                                    handleEditDialog(response);
                                }
                            }}
                            role="button"
                            tabIndex={0}
                        >
                            <div className="flex items-center gap-2">
                                <span
                                    className={`rounded px-1.5 py-0.5 text-xs font-medium ${getStatusCodeColor(response.statusCode)}`}
                                >
                                    {response.statusCode}
                                </span>

                                <span className="text-gray-600">{response.description}</span>

                                {response.contentType && (
                                    <span className="text-xs text-gray-400">({response.contentType})</span>
                                )}
                            </div>

                            <Button
                                icon={<Trash2Icon className="size-3" />}
                                onClick={(event) => {
                                    event.stopPropagation();
                                    handleRemoveResponse(response.id);
                                }}
                                size="icon"
                                type="button"
                                variant="ghost"
                            />
                        </li>
                    ))}
                </ul>
            )}

            <Dialog onOpenChange={handleDialogOpen} open={isDialogOpen}>
                <DialogContent className="max-w-lg">
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleSaveResponse)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>{editingResponse ? 'Edit' : 'Add'} Response</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={control}
                                name="statusCode"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Status Code</FormLabel>

                                        <Select onValueChange={field.onChange} value={field.value}>
                                            <FormControl>
                                                <SelectTrigger>
                                                    <SelectValue />
                                                </SelectTrigger>
                                            </FormControl>

                                            <SelectContent>
                                                <SelectItem value="200">200 OK</SelectItem>

                                                <SelectItem value="201">201 Created</SelectItem>

                                                <SelectItem value="204">204 No Content</SelectItem>

                                                <SelectItem value="400">400 Bad Request</SelectItem>

                                                <SelectItem value="401">401 Unauthorized</SelectItem>

                                                <SelectItem value="403">403 Forbidden</SelectItem>

                                                <SelectItem value="404">404 Not Found</SelectItem>

                                                <SelectItem value="500">500 Server Error</SelectItem>
                                            </SelectContent>
                                        </Select>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

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

                                                <SelectItem value="text/plain">text/plain</SelectItem>

                                                <SelectItem value="text/html">text/html</SelectItem>
                                            </SelectContent>
                                        </Select>

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
                                        <Input placeholder="Successful response" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: 'Description is required'}}
                        />

                        <FormField
                            control={control}
                            name="schema"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>JSON Schema (optional)</FormLabel>

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
                        />

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">{editingResponse ? 'Update' : 'Add'}</Button>
                        </DialogFooter>
                    </form>
                </DialogContent>
            </Dialog>
        </div>
    );
};

export default ResponseEditor;
