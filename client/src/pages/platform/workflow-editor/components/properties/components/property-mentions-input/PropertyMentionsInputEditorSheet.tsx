import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetTitle, SheetTrigger} from '@/components/ui/sheet';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import {MaximizeIcon, TextIcon} from 'lucide-react';
import {VisuallyHidden} from 'radix-ui';

export interface PropertyMentionsInputEditorSheetProps {
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    onClose?: () => void;
    path?: string;
    placeholder?: string;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    title: string;
    type: string;
    value?: string;
    workflow: Workflow;
}

const PropertyMentionsInputEditorSheet = ({
    componentDefinitions,
    controlType,
    dataPills,
    onClose,
    path,
    placeholder,
    taskDispatcherDefinitions,
    title,
    type,
    value,
    workflow,
}: PropertyMentionsInputEditorSheetProps) => (
    <Sheet
        onOpenChange={(open) => {
            if (!open && onClose) {
                onClose();
            }
        }}
    >
        <SheetTrigger asChild>
            <Button icon={<MaximizeIcon />} size="iconXs" variant="secondary" />
        </SheetTrigger>

        <SheetContent
            className="absolute bottom-4 right-4 top-3 flex h-auto w-11/12 flex-col gap-0 rounded-md bg-surface-neutral-secondary p-0 sm:max-w-screen-md"
            onFocusOutside={(event) => event.preventDefault()}
            onPointerDownOutside={(event) => event.preventDefault()}
        >
            <VisuallyHidden.Root>
                <SheetTitle>{title}</SheetTitle>
            </VisuallyHidden.Root>

            <header className="flex w-full shrink-0 items-center justify-between gap-x-3 rounded-t-md bg-surface-neutral-primary p-3">
                <div className="flex items-center gap-x-2">
                    <TextIcon />

                    <span className="text-base font-medium text-content-neutral-primary">{title}</span>
                </div>

                <SheetCloseButton />
            </header>

            <div className="property-mentions-editor flex min-h-0 flex-1 p-3">
                <div className="flex flex-1 overflow-y-auto rounded-md bg-surface-neutral-primary p-3">
                    <PropertyMentionsInputEditor
                        className="size-full"
                        componentDefinitions={componentDefinitions}
                        controlType={controlType}
                        dataPills={dataPills}
                        path={path}
                        placeholder={placeholder}
                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                        type={type}
                        value={value}
                        workflow={workflow}
                    />
                </div>
            </div>
        </SheetContent>
    </Sheet>
);

export default PropertyMentionsInputEditorSheet;
