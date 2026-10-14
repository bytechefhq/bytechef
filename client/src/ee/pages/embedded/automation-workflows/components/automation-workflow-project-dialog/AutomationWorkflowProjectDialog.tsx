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
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/Select/Select';
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from '@/components/ui/form';
import {Textarea} from '@/components/ui/textarea';
import DescriptionFormField from '@/shared/components/entity-form/DescriptionFormField';
import NameFormField from '@/shared/components/entity-form/NameFormField';
import {
    AutomationWorkflowProjectCategoriesQuery,
    AutomationWorkflowProjectTagsQuery,
    AutomationWorkflowProjectsQuery,
    CodeWorkflowLanguage,
} from '@/shared/middleware/graphql';
import {useForm} from 'react-hook-form';

type AutomationWorkflowProjectType = AutomationWorkflowProjectsQuery['automationWorkflowProjects'][number];
type EmbeddedCategoryType = AutomationWorkflowProjectCategoriesQuery['automationWorkflowProjectCategories'][number];
type EmbeddedTagType = AutomationWorkflowProjectTagsQuery['automationWorkflowProjectTags'][number];

export interface AutomationWorkflowProjectFormValuesI {
    category?: string;
    description: string;
    language?: CodeWorkflowLanguage;
    name: string;
    permissionExpression: string;
    tags: Array<string>;
}

interface AutomationWorkflowProjectFormI {
    category?: SelectOptionType;
    description: string;
    language?: CodeWorkflowLanguage;
    name: string;
    permissionExpression: string;
    tags: Array<SelectOptionType>;
}

interface AutomationWorkflowProjectDialogProps {
    categories: EmbeddedCategoryType[] | undefined;
    codeWorkflow?: boolean;
    onClose: () => void;
    onSubmit: (values: AutomationWorkflowProjectFormValuesI) => void;
    project?: AutomationWorkflowProjectType;
    tags: EmbeddedTagType[] | undefined;
}

const AutomationWorkflowProjectDialog = ({
    categories,
    codeWorkflow,
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
            category: existingCategoryName ? {label: existingCategoryName, value: existingCategoryName} : undefined,
            description: project?.description || '',
            language: codeWorkflow ? CodeWorkflowLanguage.Javascript : undefined,
            name: project?.name || '',
            permissionExpression: project?.permissionExpression ?? '',
            tags: existingTagNames.map((name) => ({label: name, value: name})),
        },
    });

    const {control, handleSubmit, setValue} = form;

    const saveProject = (formValues: AutomationWorkflowProjectFormI) => {
        onSubmit({
            category: formValues.category?.value || undefined,
            description: formValues.description,
            language: codeWorkflow ? formValues.language : undefined,
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
                                description={
                                    codeWorkflow
                                        ? 'Create a code-backed project and start editing its source.'
                                        : `Use this to ${project ? 'edit' : 'create'} a project which will contain workflows`
                                }
                                title={codeWorkflow ? 'New Code Project' : `${project ? 'Edit' : 'Create'} Project`}
                            />

                            <DialogBody>
                                {codeWorkflow && (
                                    <FormField
                                        control={control}
                                        name="language"
                                        render={({field}) => (
                                            <FormItem>
                                                <FormLabel>Language</FormLabel>

                                                <Select onValueChange={field.onChange} value={field.value}>
                                                    <FormControl>
                                                        <SelectTrigger>
                                                            <SelectValue />
                                                        </SelectTrigger>
                                                    </FormControl>

                                                    <SelectContent>
                                                        <SelectItem value={CodeWorkflowLanguage.Javascript}>
                                                            JavaScript
                                                        </SelectItem>

                                                        <SelectItem value={CodeWorkflowLanguage.Python}>
                                                            Python
                                                        </SelectItem>
                                                    </SelectContent>
                                                </Select>

                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />
                                )}

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
                                                    placeholder="Select or type to create, e.g. Marketing..."
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
                                                    placeholder="Select or type to create tags..."
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
