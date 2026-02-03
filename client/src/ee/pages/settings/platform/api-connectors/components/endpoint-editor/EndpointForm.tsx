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
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {Textarea} from '@/components/ui/textarea';
import {HttpMethod} from '@/shared/middleware/graphql';
import {CodeIcon, FormInputIcon} from 'lucide-react';

import {EndpointDefinitionI} from '../../types/api-connector-wizard.types';
import EndpointYamlEditor from './EndpointYamlEditor';
import ParameterList from './ParameterList';
import RequestBodyEditor from './RequestBodyEditor';
import ResponseEditor from './ResponseEditor';
import useEndpointForm from './hooks/useEndpointForm';

interface EndpointFormProps {
    endpoint?: EndpointDefinitionI;
    onClose: () => void;
    onSave: (endpoint: EndpointDefinitionI) => void;
    open: boolean;
}

const EndpointForm = ({endpoint, onClose, onSave, open}: EndpointFormProps) => {
    const {
        control,
        editorMode,
        handleModeChange,
        handleSaveEndpoint,
        handleSetParameters,
        handleSetRequestBody,
        handleSetResponses,
        handleSetYamlValue,
        handleSubmit,
        parameters,
        requestBody,
        responses,
        yamlValue,
    } = useEndpointForm({endpoint, onClose, onSave, open});

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className="max-h-[90vh] max-w-2xl overflow-hidden">
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
                                                <Textarea placeholder="Detailed description..." rows={2} {...field} />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <div className="border-t pt-4">
                                    <ParameterList onChange={handleSetParameters} parameters={parameters} />
                                </div>

                                <div className="border-t pt-4">
                                    <RequestBodyEditor onChange={handleSetRequestBody} requestBody={requestBody} />
                                </div>

                                <div className="border-t pt-4">
                                    <ResponseEditor onChange={handleSetResponses} responses={responses} />
                                </div>
                            </div>
                        </TabsContent>

                        <TabsContent className="mt-4" value="yaml">
                            <EndpointYamlEditor onChange={handleSetYamlValue} value={yamlValue} />
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
            </DialogContent>
        </Dialog>
    );
};

export default EndpointForm;
