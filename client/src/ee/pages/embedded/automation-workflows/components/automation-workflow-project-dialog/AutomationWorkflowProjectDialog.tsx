import Button from '@/components/Button/Button';
import CreatableSelect, {SelectOptionType} from '@/components/CreatableSelect/CreatableSelect';
import {Input} from '@/components/Input/Input';
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
import {Textarea} from '@/components/ui/textarea';
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
    category?: string;
    description: string;
    name: string;
    tags: Array<string>;
}

interface AutomationWorkflowProjectFormI {
    category?: SelectOptionType;
    description: string;
    name: string;
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
            category: existingCategoryName ? {label: existingCategoryName, value: existingCategoryName} : undefined,
            description: project?.description || '',
            name: project?.name || '',
            tags: existingTagNames.map((name) => ({label: name, value: name})),
        },
    });

    const {control, handleSubmit, setValue} = form;

    const saveProject = (formValues: AutomationWorkflowProjectFormI) => {
        onSubmit({
            category: formValues.category?.value || undefined,
            description: formValues.description,
            name: formValues.name,
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
                <Form {...form}>
                    <form className="flex flex-col gap-4" onSubmit={handleSubmit(saveProject)}>
                        <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                            <div className="flex flex-col space-y-1">
                                <DialogTitle>{`${project ? 'Edit' : 'Create'} Project`}</DialogTitle>

                                <DialogDescription>
                                    {`Use this to ${project ? 'edit' : 'create'} a project which will contain workflows`}
                                </DialogDescription>
                            </div>

                            <DialogCloseButton />
                        </DialogHeader>

                        <FormField
                            control={control}
                            name="name"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Name</FormLabel>

                                    <FormControl>
                                        <Input placeholder="My CRM Project" {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                            rules={{required: true}}
                        />

                        <FormField
                            control={control}
                            name="description"
                            render={({field}) => (
                                <FormItem>
                                    <FormLabel>Description</FormLabel>

                                    <FormControl>
                                        <Textarea placeholder="Cute description of your project" rows={5} {...field} />
                                    </FormControl>

                                    <FormMessage />
                                </FormItem>
                            )}
                        />

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

export default AutomationWorkflowProjectDialog;
