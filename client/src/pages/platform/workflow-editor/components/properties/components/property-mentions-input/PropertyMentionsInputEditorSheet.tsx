import {Button} from '@/components/ui/button';
import {Sheet, SheetCloseButton, SheetContent, SheetHeader, SheetTitle, SheetTrigger} from '@/components/ui/sheet';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/properties/components/property-mentions-input/PropertyMentionsInputEditor';
import {ComponentDefinitionBasic, Workflow} from '@/shared/middleware/platform/configuration';
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
    placeholder,
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
            <Button className="size-auto p-0.5" size="icon" variant="ghost">
                <MaximizeIcon className="size-4" />
            </Button>
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
                    placeholder={placeholder}
                    type={type}
                    value={value}
                    workflow={workflow}
                />
            </div>
        </SheetContent>
    </Sheet>
);

export default PropertyMentionsInputEditorSheet;
