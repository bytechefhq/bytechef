import Button from '@/components/Button/Button';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle, SheetTrigger} from '@/components/ui/sheet';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {MaximizeIcon} from 'lucide-react';

export interface PropertyMentionsInputEditorSheetProps {
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    path?: string;
    onClose?: () => void;
    onFocus?: (editor: Editor) => void;
    placeholder?: string;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
    type: string;
    value?: string;
    title: string;
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
        onOpenChange={() => {
            if (onClose) {
                onClose();
            }
        }}
    >
        <SheetTrigger asChild>
            <Button icon={<MaximizeIcon />} size="iconXs" variant="secondary" />
        </SheetTrigger>

        <SheetContent className="flex w-11/12 flex-col gap-0 p-4 sm:max-w-screen-md">
            <SheetHeader className="flex flex-row items-center justify-between space-y-0">
                <SheetTitle>{title}</SheetTitle>

                <SheetCloseButton />
            </SheetHeader>

            <div className="property-mentions-editor flex size-full overflow-y-auto rounded-md pt-3">
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
        </SheetContent>
    </Sheet>
);

export default PropertyMentionsInputEditorSheet;
