import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
    DialogTrigger,
} from '@/components/Dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Label} from '@/components/ui/label';
import {RadioGroup, RadioGroupItem} from '@/components/ui/radio-group';
import {Textarea} from '@/components/ui/textarea';
import {Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {
    useCreateIntegrationMutation,
    useUpdateIntegrationMutation,
} from '@/ee/shared/mutations/embedded/integrations.mutations';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {
    IntegrationCategoryKeys,
    useGetIntegrationCategoriesQuery,
} from '@/ee/shared/queries/embedded/integrationCategories.queries';
import {IntegrationTagKeys, useGetIntegrationTagsQuery} from '@/ee/shared/queries/embedded/integrationTags.quries';
import {IntegrationKeys} from '@/ee/shared/queries/embedded/integrations.queries';
import CategoryFormField from '@/shared/components/entity-form/CategoryFormField';
import DescriptionFormField from '@/shared/components/entity-form/DescriptionFormField';
import NameFormField from '@/shared/components/entity-form/NameFormField';
import TagsFormField from '@/shared/components/entity-form/TagsFormField';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useQueryClient} from '@tanstack/react-query';
import {ReactNode, useState} from 'react';
import {useForm} from 'react-hook-form';

export type IntegrationUsageType = 'connectionOnly' | 'workflows';

interface IntegrationDialogProps {
    integration: Integration | undefined;
    onClose?: (integration?: Integration) => void;
    onSuccess?: (integrationId: number | void, usage: IntegrationUsageType) => void;
    triggerNode?: ReactNode;
}

type IntegrationFormValuesType = Integration & {permissionExpression?: string | null};

const IntegrationDialog = ({integration, onClose, onSuccess, triggerNode}: IntegrationDialogProps) => {
    const [isOpen, setIsOpen] = useState(!triggerNode);
    const [usage, setUsage] = useState<IntegrationUsageType>('workflows');

    const {captureIntegrationCreated} = useAnalytics();

    const form = useForm<IntegrationFormValuesType>({
        defaultValues: {
            category: integration?.category
                ? {
                      label: integration?.category?.name,
                      ...integration?.category,
                  }
                : undefined,
            componentName: integration?.componentName || '',
            description: integration?.description || '',
            multipleInstances: false,
            name: integration?.name || '',
            permissionExpression: integration?.permissionExpression ?? '',
            tags:
                integration?.tags?.map((tag: Tag) => ({
                    ...tag,
                    label: tag.name,
                })) || [],
        } as IntegrationFormValuesType,
    });

    const {control, getValues, handleSubmit, reset, setValue} = form;

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery({connectionDefinitions: true});

    const {data: categories, error: categoriesError, isLoading: categoriesLoading} = useGetIntegrationCategoriesQuery();

    const {data: tags, error: tagsError, isLoading: tagsLoading} = useGetIntegrationTagsQuery();

    const queryClient = useQueryClient();

    const onSuccessHandler = (integrationId: number | void) => {
        captureIntegrationCreated();

        const id = integrationId || integration?.id;

        if (id) {
            void queryClient.invalidateQueries({
                queryKey: IntegrationKeys.integration(id),
            });
        }

        void queryClient.invalidateQueries({
            queryKey: IntegrationCategoryKeys.integrationCategories,
        });
        void queryClient.invalidateQueries({
            queryKey: IntegrationKeys.integrations,
        });
        void queryClient.invalidateQueries({
            queryKey: IntegrationTagKeys.integrationTags,
        });
        void queryClient.invalidateQueries({queryKey: ['integrationCodeWorkflowSource']});

        if (onSuccess) {
            onSuccess(integrationId, usage);
        }

        closeDialog();
    };

    const createIntegrationMutation = useCreateIntegrationMutation({
        onSuccess: onSuccessHandler,
    });

    const updateIntegrationMutation = useUpdateIntegrationMutation({
        onSuccess: onSuccessHandler,
    });

    const tagNames = integration?.tags?.map((tag) => tag.name);

    const remainingTags = tags?.filter((tag) => !tagNames?.includes(tag.name));

    function closeDialog() {
        reset();

        setUsage('workflows');

        setIsOpen(false);

        if (onClose) {
            onClose();
        }
    }

    function saveIntegration() {
        const formData = getValues();

        if (!formData) {
            return;
        }

        const tagValues = formData.tags?.map((tag: Tag) => {
            return {id: tag.id, name: tag.name, version: tag.version};
        });

        const category = formData?.category?.name ? formData?.category : undefined;

        const permissionExpression = formData.permissionExpression?.trim() || undefined;

        if (integration?.id) {
            updateIntegrationMutation.mutate({
                ...integration,
                ...formData,
                category,
                permissionExpression,
            } as Integration);
        } else {
            createIntegrationMutation.mutate({
                ...formData,
                category,
                permissionExpression,
                tags: tagValues,
            } as Integration);
        }
    }

    return (
        <Dialog
            onOpenChange={(isOpen) => {
                if (isOpen) {
                    setIsOpen(isOpen);
                } else {
                    closeDialog();
                }
            }}
            open={isOpen}
        >
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent>
                <DialogMain>
                    <Form {...form}>
                        <DialogHeader
                            description={
                                integration?.id
                                    ? 'Use this to edit your integration'
                                    : 'Use this to create an integration your users connect to'
                            }
                            title={`${integration?.id ? 'Edit' : 'Create'} Integration`}
                        />

                        <DialogBody>
                            {categoriesError &&
                                !categoriesLoading &&
                                `An error has occurred: ${categoriesError.message}`}

                            {tagsError && !tagsLoading && `An error has occurred: ${tagsError.message}`}

                            {!integration?.id && (
                                <fieldset className="border-0">
                                    <legend className="mb-2 text-sm font-medium">How will you use it?</legend>

                                    <RadioGroup
                                        aria-label="Integration usage"
                                        className="flex flex-col gap-3"
                                        onValueChange={(value) => setUsage(value as IntegrationUsageType)}
                                        value={usage}
                                    >
                                        <div className="flex items-start gap-2">
                                            <RadioGroupItem
                                                aria-label="With workflows"
                                                id="integration-usage-workflows"
                                                value="workflows"
                                            />

                                            <Label
                                                className="flex flex-col items-start gap-1"
                                                htmlFor="integration-usage-workflows"
                                            >
                                                <span>With workflows</span>

                                                <span className="text-xs font-normal text-content-neutral-secondary">
                                                    Build workflows your users enable and configure.
                                                </span>
                                            </Label>
                                        </div>

                                        <div className="flex items-start gap-2">
                                            <RadioGroupItem
                                                aria-label="Actions, tools & MCP"
                                                id="integration-usage-connection-only"
                                                value="connectionOnly"
                                            />

                                            <Label
                                                className="flex flex-col items-start gap-1"
                                                htmlFor="integration-usage-connection-only"
                                            >
                                                <span>Actions, tools & MCP</span>

                                                <span className="text-xs font-normal text-content-neutral-secondary">
                                                    Use the connection from your code through the Actions and Tools
                                                    APIs, or expose its tools through an MCP server.
                                                </span>
                                            </Label>
                                        </div>
                                    </RadioGroup>
                                </fieldset>
                            )}

                            <FormField
                                control={control}
                                name="componentName"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Component</FormLabel>

                                        <FormControl>
                                            {componentDefinitions && (
                                                <ComboBox
                                                    disabled={!!integration?.id}
                                                    items={componentDefinitions.map((componentDefinition) => ({
                                                        componentDefinition,
                                                        icon: componentDefinition.icon,
                                                        label: componentDefinition.title!,
                                                        value: componentDefinition.name,
                                                    }))}
                                                    maxHeight={true}
                                                    name="component"
                                                    onBlur={field.onBlur}
                                                    onChange={(item) => {
                                                        const componentName = (
                                                            item?.componentDefinition as ComponentDefinitionBasic
                                                        ).name;
                                                        const title = (
                                                            item?.componentDefinition as ComponentDefinitionBasic
                                                        ).title;

                                                        setValue('componentName', componentName);
                                                        setValue('name', title);
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

                            <NameFormField />

                            <CategoryFormField categories={categories} categoriesLoading={categoriesLoading} />

                            <DescriptionFormField placeholder="Cute description of your integration" />

                            <FormField
                                control={control}
                                name="permissionExpression"
                                render={({field}) => (
                                    <FormItem>
                                        <FormLabel>Permission Expression</FormLabel>

                                        <FormControl>
                                            <Textarea
                                                placeholder="e.g. metadata['plan'] == 'pro'"
                                                rows={3}
                                                {...field}
                                                value={field.value ?? ''}
                                            />
                                        </FormControl>

                                        <FormMessage />
                                    </FormItem>
                                )}
                            />

                            <TagsFormField remainingTags={remainingTags} />
                        </DialogBody>

                        <DialogFooter>
                            <DialogCancelButton />

                            <Button label="Save" onClick={handleSubmit(saveIntegration)} type="submit" />
                        </DialogFooter>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default IntegrationDialog;
