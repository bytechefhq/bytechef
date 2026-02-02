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
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Textarea} from '@/components/ui/textarea';
import {toast} from '@/hooks/use-toast';
import {HttpMethod} from '@/shared/middleware/graphql';
import {CodeIcon, FormInputIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {v4 as uuidv4} from 'uuid';
import {parse as yamlParse, stringify as yamlStringify} from 'yaml';

import {
    EndpointDefinitionI,
    ParameterDefinitionI,
    ParameterLocationType,
    ParameterTypeType,
    RequestBodyDefinitionI,
    ResponseDefinitionI,
} from '../../types/api-connector-wizard.types';
import {safeJsonParse} from '../../utils/jsonUtils';
import EndpointYamlEditor from './EndpointYamlEditor';
import ParameterList from './ParameterList';
import RequestBodyEditor from './RequestBodyEditor';
import ResponseEditor from './ResponseEditor';

interface EndpointFormProps {
    endpoint?: EndpointDefinitionI;
    onClose: () => void;
    onSave: (endpoint: EndpointDefinitionI) => void;
    open: boolean;
}

interface EndpointFormDataI {
    description: string;
    httpMethod: HttpMethod;
    operationId: string;
    path: string;
    summary: string;
}

const DEFAULT_RESPONSE: ResponseDefinitionI = {
    description: 'Successful response',
    statusCode: '200',
};

const defaultEndpointValues: EndpointFormDataI = {
    description: '',
    httpMethod: HttpMethod.Get,
    operationId: '',
    path: '',
    summary: '',
};

const EndpointForm = ({endpoint, onClose, onSave, open}: EndpointFormProps) => {
    const [editorMode, setEditorMode] = useState<'form' | 'yaml'>('form');
    const [parameters, setParameters] = useState<ParameterDefinitionI[]>([]);
    const [requestBody, setRequestBody] = useState<RequestBodyDefinitionI | undefined>(undefined);
    const [responses, setResponses] = useState<ResponseDefinitionI[]>([]);
    const [yamlValue, setYamlValue] = useState('');

    const form = useForm<EndpointFormDataI>({
        defaultValues: defaultEndpointValues,
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    useEffect(() => {
        if (open) {
            if (endpoint) {
                reset({
                    description: endpoint.description || '',
                    httpMethod: endpoint.httpMethod,
                    operationId: endpoint.operationId,
                    path: endpoint.path,
                    summary: endpoint.summary || '',
                });

                setParameters(endpoint.parameters || []);
                setRequestBody(endpoint.requestBody);
                setResponses(endpoint.responses || []);
            } else {
                reset(defaultEndpointValues);
                setParameters([]);
                setRequestBody(undefined);
                setResponses([DEFAULT_RESPONSE]);
            }

            setEditorMode('form');
        }
    }, [endpoint, open, reset]);

    const generateYamlFromForm = useCallback(() => {
        const formData = getValues();
        const method = formData.httpMethod.toLowerCase();

        const operation: Record<string, unknown> = {
            operationId: formData.operationId,
            responses: {},
        };

        if (formData.summary) {
            operation.summary = formData.summary;
        }

        if (formData.description) {
            operation.description = formData.description;
        }

        if (parameters.length > 0) {
            operation.parameters = parameters.map((param) => ({
                description: param.description,
                example: param.example,
                in: param.in,
                name: param.name,
                required: param.required,
                schema: {type: param.type},
            }));
        }

        if (requestBody) {
            const {data: parsedSchema} = safeJsonParse(requestBody.schema, 'request body schema');

            operation.requestBody = {
                content: {
                    [requestBody.contentType]: {
                        schema: parsedSchema,
                    },
                },
                description: requestBody.description,
                required: requestBody.required,
            };
        }

        responses.forEach((response) => {
            const responseObj: Record<string, unknown> = {
                description: response.description,
            };

            if (response.contentType && response.schema) {
                const {data: parsedResponseSchema} = safeJsonParse(
                    response.schema,
                    `response schema for status ${response.statusCode}`
                );

                responseObj.content = {
                    [response.contentType]: {
                        schema: parsedResponseSchema,
                    },
                };
            }

            (operation.responses as Record<string, unknown>)[response.statusCode] = responseObj;
        });

        const spec = {
            paths: {
                [formData.path]: {
                    [method]: operation,
                },
            },
        };

        return yamlStringify(spec);
    }, [getValues, parameters, requestBody, responses]);

    const parseYamlToForm = useCallback(
        (yaml: string) => {
            try {
                const parsed = yamlParse(yaml);

                if (!parsed.paths) {
                    toast({
                        description: 'The YAML must contain a "paths" section with endpoint definitions.',
                        title: 'Invalid OpenAPI structure',
                        variant: 'destructive',
                    });

                    return;
                }

                const pathEntries = Object.entries(parsed.paths);

                if (pathEntries.length === 0) {
                    toast({
                        description: 'The "paths" section is empty. Add at least one endpoint path.',
                        title: 'No endpoints found',
                        variant: 'destructive',
                    });

                    return;
                }

                const [path, methods] = pathEntries[0];
                const methodEntries = Object.entries(methods as Record<string, Record<string, unknown>>);

                if (methodEntries.length === 0) {
                    toast({
                        description: `The path "${path}" has no HTTP methods defined. Add at least one method (get, post, etc.).`,
                        title: 'No methods found',
                        variant: 'destructive',
                    });

                    return;
                }

                const [method, operation] = methodEntries[0];

                setValue('path', path);
                setValue('httpMethod', method.toUpperCase() as HttpMethod);
                setValue('operationId', (operation.operationId as string) || '');
                setValue('summary', (operation.summary as string) || '');
                setValue('description', (operation.description as string) || '');

                if (Array.isArray(operation.parameters)) {
                    const validLocations: ParameterLocationType[] = ['path', 'query', 'header'];
                    const validTypes: ParameterTypeType[] = ['string', 'number', 'integer', 'boolean', 'array'];

                    const parsedParams: ParameterDefinitionI[] = operation.parameters.map(
                        (param: Record<string, unknown>) => {
                            const rawLocation = param.in as string;
                            const location: ParameterLocationType = validLocations.includes(
                                rawLocation as ParameterLocationType
                            )
                                ? (rawLocation as ParameterLocationType)
                                : 'query';

                            const rawType = ((param.schema as Record<string, unknown>)?.type as string) || 'string';
                            const type: ParameterTypeType = validTypes.includes(rawType as ParameterTypeType)
                                ? (rawType as ParameterTypeType)
                                : 'string';

                            return {
                                description: (param.description as string) || '',
                                example: (param.example as string) || '',
                                id: uuidv4(),
                                in: location,
                                name: (param.name as string) || '',
                                required: !!param.required,
                                type,
                            };
                        }
                    );

                    setParameters(parsedParams);
                }

                if (operation.requestBody) {
                    const reqBody = operation.requestBody as Record<string, unknown>;
                    const content = reqBody.content as Record<string, Record<string, unknown>>;

                    if (content) {
                        const contentType = Object.keys(content)[0];

                        setRequestBody({
                            contentType,
                            description: (reqBody.description as string) || '',
                            required: !!reqBody.required,
                            schema: JSON.stringify(content[contentType]?.schema || {}, null, 2),
                        });
                    }
                }

                if (operation.responses) {
                    const parsedResponses: ResponseDefinitionI[] = Object.entries(
                        operation.responses as Record<string, Record<string, unknown>>
                    ).map(([statusCode, responseData]) => {
                        const content = responseData.content as Record<string, Record<string, unknown>> | undefined;
                        const contentType = content ? Object.keys(content)[0] : undefined;

                        return {
                            contentType,
                            description: (responseData.description as string) || '',
                            schema:
                                contentType && content
                                    ? JSON.stringify(content[contentType]?.schema, null, 2)
                                    : undefined,
                            statusCode,
                        };
                    });

                    setResponses(parsedResponses);
                }
            } catch (error) {
                console.error('Failed to parse YAML in EndpointForm.parseYamlToForm:', error);

                toast({
                    description: 'Unable to parse the YAML. Please check that your YAML syntax is valid.',
                    title: 'Invalid YAML',
                    variant: 'destructive',
                });
            }
        },
        [setValue]
    );

    const handleModeChange = (mode: string) => {
        if (mode === 'yaml' && editorMode === 'form') {
            setYamlValue(generateYamlFromForm());
            setEditorMode('yaml');
        } else if (mode === 'form' && editorMode === 'yaml') {
            // Validate YAML before switching to prevent data loss
            let parsed;

            try {
                parsed = yamlParse(yamlValue);
            } catch {
                toast({
                    description:
                        'Fix the YAML syntax errors before switching to form view, or your changes may be lost.',
                    title: 'Invalid YAML',
                    variant: 'destructive',
                });

                return;
            }

            // Validate OpenAPI structure before switching modes
            if (!parsed.paths) {
                toast({
                    description: 'The YAML must contain a "paths" section to switch to form view.',
                    title: 'Invalid OpenAPI structure',
                    variant: 'destructive',
                });

                return;
            }

            parseYamlToForm(yamlValue);
            setEditorMode('form');
        }
    };

    const handleSaveEndpoint = (data: EndpointFormDataI) => {
        if (editorMode === 'yaml') {
            try {
                yamlParse(yamlValue);
            } catch {
                toast({
                    description: 'Please fix the YAML syntax errors before saving.',
                    title: 'Invalid YAML',
                    variant: 'destructive',
                });

                return;
            }

            parseYamlToForm(yamlValue);
        }

        const newEndpoint: EndpointDefinitionI = {
            ...data,
            id: endpoint?.id || uuidv4(),
            parameters,
            requestBody,
            responses,
        };

        onSave(newEndpoint);
        onClose();
    };

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className="max-h-[90vh] max-w-2xl overflow-hidden">
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(handleSaveEndpoint)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <DialogTitle>{endpoint ? 'Edit' : 'Add'} Endpoint</DialogTitle>

                            <DialogCloseButton />
                        </DialogHeader>

                        <Tabs className="w-full" onValueChange={handleModeChange} value={editorMode}>
                            <TabsList className="grid h-8 w-full grid-cols-2">
                                <TabsTrigger className="flex items-center gap-1.5 text-xs" value="form">
                                    <FormInputIcon className="size-3" />
                                    Form
                                </TabsTrigger>

                                <TabsTrigger className="flex items-center gap-1.5 text-xs" value="yaml">
                                    <CodeIcon className="size-3" />
                                    YAML
                                </TabsTrigger>
                            </TabsList>

                            <TabsContent className="max-h-[60vh] overflow-y-auto" value="form">
                                <div className="flex flex-col gap-4 py-2">
                                    <div className="grid grid-cols-3 gap-4">
                                        <FormField
                                            control={control}
                                            name="httpMethod"
                                            render={({field}) => (
                                                <FormItem>
                                                    <FormLabel>Method</FormLabel>

                                                    <Select onValueChange={field.onChange} value={field.value}>
                                                        <FormControl>
                                                            <SelectTrigger>
                                                                <SelectValue />
                                                            </SelectTrigger>
                                                        </FormControl>

                                                        <SelectContent>
                                                            <SelectItem value={HttpMethod.Get}>GET</SelectItem>

                                                            <SelectItem value={HttpMethod.Post}>POST</SelectItem>

                                                            <SelectItem value={HttpMethod.Put}>PUT</SelectItem>

                                                            <SelectItem value={HttpMethod.Patch}>PATCH</SelectItem>

                                                            <SelectItem value={HttpMethod.Delete}>DELETE</SelectItem>
                                                        </SelectContent>
                                                    </Select>

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={control}
                                            name="path"
                                            render={({field}) => (
                                                <FormItem className="col-span-2">
                                                    <FormLabel>Path</FormLabel>

                                                    <FormControl>
                                                        <Input placeholder="/users/{id}" {...field} />
                                                    </FormControl>

                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                            rules={{required: 'Path is required'}}
                                        />
                                    </div>

                                    <FormField
                                        control={control}
                                        name="operationId"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Operation ID</FormLabel>

                                                <FormControl>
                                                    <Input placeholder="getUserById" {...field} />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                        rules={{required: 'Operation ID is required'}}
                                    />

                                    <FormField
                                        control={control}
                                        name="summary"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Summary</FormLabel>

                                                <FormControl>
                                                    <Input placeholder="Get a user by ID" {...field} />
                                                </FormControl>

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
                                                    <Textarea
                                                        placeholder="Detailed description..."
                                                        rows={2}
                                                        {...field}
                                                    />
                                                </FormControl>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <div className="border-t pt-4">
                                        <ParameterList onChange={setParameters} parameters={parameters} />
                                    </div>

                                    <div className="border-t pt-4">
                                        <RequestBodyEditor onChange={setRequestBody} requestBody={requestBody} />
                                    </div>

                                    <div className="border-t pt-4">
                                        <ResponseEditor onChange={setResponses} responses={responses} />
                                    </div>
                                </div>
                            </TabsContent>

                            <TabsContent className="mt-4" value="yaml">
                                <EndpointYamlEditor onChange={setYamlValue} value={yamlValue} />
                            </TabsContent>
                        </Tabs>

                        <DialogFooter>
                            <DialogClose asChild>
                                <Button type="button" variant="outline">
                                    Cancel
                                </Button>
                            </DialogClose>

                            <Button type="submit">{endpoint ? 'Update' : 'Add'}</Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
};

export default EndpointForm;
