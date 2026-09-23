import Button from '@/components/Button/Button';
import CreatableSelect, {SelectOptionType} from '@/components/CreatableSelect/CreatableSelect';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Switch} from '@/components/ui/switch';
import {Textarea} from '@/components/ui/textarea';
import DescriptionFormField from '@/shared/components/entity-form/DescriptionFormField';
import NameFormField from '@/shared/components/entity-form/NameFormField';
import {
    AutomationWorkflowProjectCategoriesQuery,
    AutomationWorkflowProjectTagsQuery,
    AutomationWorkflowProjectsQuery,
} from '@/shared/middleware/graphql';
import {useForm} from 'react-hook-form';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type EmbeddedCategoryType = AutomationWorkflowProjectCategoriesQuery['automationWorkflowProjectCategories'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

export interface AutomationWorkflowProjectFormValuesI {
    automationHubVisible: boolean;
    category?: string;
    description: string;
    name: string;
    permissionExpression: string;
    tags: Array<string>;
}

interface AutomationWorkflowProjectFormI {
    automationHubVisible: boolean;
    category?: SelectOptionType;
    description: string;
    name: string;
    permissionExpression: string;
    tags: Array<SelectOptionType>;
}

interface AutomationWorkflowProjectDialogProps {
    categories: EmbeddedCategoryType[] | undefined;
    onClose: () => void;
    onSubmit: (values: AutomationWorkflowProjectFormValuesI) => void;
    project?: AutomationWorkflowProjectType;
    tags: EmbeddedTagType[] | undefined;
}

const AutomationWorkflowProjectDialog = ({
    categories,
    onClose,
    onSubmit,
    project,
    tags,
}: AutomationWorkflowProjectDialogProps) => {
    const categoryOptions: Array<SelectOptionType> = (categories || []).map((category: EmbeddedCategoryType) => ({
        label: category.name,
        value: category.name,
    }));

    const tagOptions: Array<SelectOptionType> = (tags || []).map((tag: EmbeddedTagType) => ({
        label: tag.name,
        value: tag.name,
    }));

    const existingCategoryName = project?.categoryId
        ? (categories || []).find((category: EmbeddedCategoryType) => category.id === project.categoryId)?.name
        : undefined;

    const existingTagNames = project?.tagIds
        ? (tags || [])
              .filter((tag: EmbeddedTagType) => project.tagIds.includes(tag.id))
              .map((tag: EmbeddedTagType) => tag.name)
        : [];

    const form = useForm<AutomationWorkflowProjectFormI>({
        defaultValues: {
            automationHubVisible: project?.automationHubVisible ?? true,
            category: existingCategoryName ? {label: existingCategoryName, value: existingCategoryName} : undefined,
            description: project?.description || '',
            name: project?.name || '',
            permissionExpression: project?.permissionExpression ?? '',
            tags: existingTagNames.map((name) => ({label: name, value: name})),
        },
    });

    const {control, handleSubmit, setValue} = form;

    const saveProject = (formValues: AutomationWorkflowProjectFormI) => {
        onSubmit({
            automationHubVisible: formValues.automationHubVisible,
            category: formValues.category?.value || undefined,
            description: formValues.description,
            name: formValues.name,
            permissionExpression: formValues.permissionExpression,
            tags: (formValues.tags || []).map((tag) => tag.value),
        });
    };

    return (
        <Dialog
            onOpenChange={(open) => {
                if (!open) {
                    onClose();
                }
            }}
            open
        >
            <DialogContent aria-label="Project Dialog" onInteractOutside={(event) => event.preventDefault()}>
                <DialogMain>
                    <Form {...form}>
                        <form className="flex min-h-0 flex-1 flex-col" onSubmit={handleSubmit(saveProject)}>
                            <DialogHeader
                                description={`Use this to ${project ? 'edit' : 'create'} a project which will contain workflows`}
                                title={`${project ? 'Edit' : 'Create'} Project`}
                            />

                            <DialogBody>
                                <NameFormField placeholder="My CRM Project" />

                                <DescriptionFormField placeholder="Cute description of your project" />

                                <FormField
                                    control={control}
                                    name="category"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Category</FormLabel>

                                            <FormControl>
                                                <CreatableSelect
                                                    field={field}
                                                    isClearable
                                                    isMulti={false}
                                                    onCreateOption={(inputValue: string) => {
                                                        setValue('category', {label: inputValue, value: inputValue});
                                                    }}
                                                    options={categoryOptions}
                                                    placeholder="Marketing, Sales, Social Media..."
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={control}
                                    name="tags"
                                    render={({field}) => (
                                        <FormItem>
                                            <FormLabel>Tags</FormLabel>

                                            <FormControl>
                                                <CreatableSelect
                                                    field={field}
                                                    isMulti
                                                    onCreateOption={(inputValue: string) => {
                                                        setValue('tags', [
                                                            ...(form.getValues().tags || []),
                                                            {label: inputValue, value: inputValue},
                                                        ]);
                                                    }}
                                                    options={tagOptions}
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

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
                                                />
                                            </FormControl>

                                            <FormMessage />
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={control}
                                    name="automationHubVisible"
                                    render={({field}) => (
                                        <FormItem className="flex items-center justify-between gap-4">
                                            <div>
                                                <FormLabel htmlFor="automation-hub-visible">
                                                    Show in Automation Hub
                                                </FormLabel>

                                                <p className="text-sm text-muted-foreground">
                                                    Turn off for flows you only activate through the API.
                                                </p>
                                            </div>

                                            <FormControl>
                                                <Switch
                                                    checked={field.value}
                                                    id="automation-hub-visible"
                                                    onCheckedChange={field.onChange}
                                                />
                                            </FormControl>
                                        </FormItem>
                                    )}
                                />
                            </DialogBody>

                            <DialogFooter>
                                <DialogCancelButton />

                                <Button label="Save" type="submit" />
                            </DialogFooter>
                        </form>
                    </Form>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AutomationWorkflowProjectDialog;
