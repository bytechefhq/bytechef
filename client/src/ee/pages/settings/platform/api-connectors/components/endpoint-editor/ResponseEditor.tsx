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
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Input} from '@/components/ui/input';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import MonacoEditorWrapper from '@/shared/components/MonacoEditorWrapper';
import {PlusIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {useForm} from 'react-hook-form';

import {ResponseDefinitionI} from '../../types/api-connector-wizard.types';

interface ResponseEditorProps {
    onChange: (responses: ResponseDefinitionI[]) => void;
    responses: ResponseDefinitionI[];
}

interface ResponseFormDataI {
    contentType: string;
    description: string;
    schema: string;
    statusCode: string;
}

const defaultResponseValues: ResponseFormDataI = {
    contentType: 'application/json',
    description: '',
    schema: '',
    statusCode: '200',
};

interface ResponseDefinitionWithIdI extends ResponseDefinitionI {
    id: string;
}

const ResponseEditor = ({onChange, responses}: ResponseEditorProps) => {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [editingResponse, setEditingResponse] = useState<ResponseDefinitionWithIdI | null>(null);

    const responsesWithIds: ResponseDefinitionWithIdI[] = responses.map((response, index) => ({
        ...response,
        id: `response-${index}`,
    }));

    const form = useForm<ResponseFormDataI>({
        defaultValues: defaultResponseValues,
    });

    const {control, handleSubmit, reset} = form;

    const openAddDialog = () => {
        setEditingResponse(null);
        reset(defaultResponseValues);
        setIsDialogOpen(true);
    };

    const openEditDialog = (response: ResponseDefinitionWithIdI) => {
        setEditingResponse(response);

        reset({
            contentType: response.contentType || 'application/json',
            description: response.description,
            schema: response.schema || '',
            statusCode: response.statusCode,
        });

        setIsDialogOpen(true);
    };

    const handleSaveResponse = (data: ResponseFormDataI) => {
        const newResponse: ResponseDefinitionI = {
            contentType: data.contentType || undefined,
            description: data.description,
            schema: data.schema || undefined,
            statusCode: data.statusCode,
        };

        if (editingResponse) {
            const index = responsesWithIds.findIndex((response) => response.id === editingResponse.id);

            if (index !== -1) {
                const updatedResponses = [...responses];

                updatedResponses[index] = newResponse;

                onChange(updatedResponses);
            }
        } else {
            onChange([...responses, newResponse]);
        }

        setIsDialogOpen(false);
        reset(defaultResponseValues);
    };

    const handleRemoveResponse = (id: string) => {
        const index = responsesWithIds.findIndex((response) => response.id === id);

        if (index !== -1) {
            const updatedResponses = responses.filter((_, responseIndex) => responseIndex !== index);

            onChange(updatedResponses);
        }
    };

    const getStatusCodeColor = (statusCode: string) => {
        const code = parseInt(statusCode, 10);

        if (code >= 200 && code < 300) {
            return 'bg-green-100 text-green-800';
        }

        if (code >= 300 && code < 400) {
            return 'bg-blue-100 text-blue-800';
        }

        if (code >= 400 && code < 500) {
            return 'bg-yellow-100 text-yellow-800';
        }

        if (code >= 500) {
            return 'bg-red-100 text-red-800';
        }

        return 'bg-gray-100 text-gray-800';
    };

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Responses</span>

                <Button
                    icon={<PlusIcon className="size-3" />}
                    onClick={openAddDialog}
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
                            onClick={() => openEditDialog(response)}
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

            <Dialog onOpenChange={setIsDialogOpen} open={isDialogOpen}>
                <DialogContent className="max-w-lg">
                    <Form {...form}>
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
                    </Form>
                </DialogContent>
            </Dialog>
        </div>
    );
};

export default ResponseEditor;
