import {Button} from '@/components/ui/button';
import {Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger} from '@/components/ui/sheet';
import PropertyMentionsInputEditor from '@/pages/platform/workflow-editor/components/Properties/components/PropertyMentionsInput/PropertyMentionsInputEditor';
import {ComponentDefinitionBasic, Workflow} from '@/shared/middleware/platform/configuration';
import {DataPillType} from '@/shared/types';
import {Editor} from '@tiptap/react';
import {MaximizeIcon} from 'lucide-react';
import {Dispatch, SetStateAction, useState} from 'react';

export interface PropertyMentionsInputEditorSheetProps {
    componentDefinitions: ComponentDefinitionBasic[];
    controlType?: string;
    dataPills: DataPillType[];
    path?: string;
    onChange: (value: string) => void;
    onClose?: () => void;
    onFocus?: (editor: Editor) => void;
    placeholder?: string;
    setInitialized: Dispatch<SetStateAction<boolean>>;
    type: string;
    value?: string;
    title: string;
    workflow: Workflow;
}

const PropertyMentionsInputEditorSheet = ({
    componentDefinitions,
    controlType,
    dataPills,
    onChange,
    onClose,
    placeholder,
    title,
    type,
    value,
    workflow,
}: PropertyMentionsInputEditorSheetProps) => {
    const [initialized, setInitialized] = useState(false);

    return (
        <Sheet
            onOpenChange={() => {
                setInitialized(false);

                if (onClose) {
                    onClose();
                }
            }}
        >
            <SheetTrigger asChild>
                <Button className="size-auto p-0.5" size="icon" variant="ghost">
                    <MaximizeIcon className="h-4" />
                </Button>
            </SheetTrigger>

            <SheetContent className="flex w-11/12 flex-col gap-0 p-4 sm:max-w-screen-md">
                <SheetHeader>
                    <SheetTitle>{title}</SheetTitle>
                </SheetHeader>

                <div className="size-full pt-3">
                    <div className="property-mentions-editor flex size-full overflow-y-auto rounded-md bg-white">
                        <PropertyMentionsInputEditor
                            componentDefinitions={componentDefinitions}
                            controlType={controlType}
                            dataPills={dataPills}
                            initialized={initialized}
                            onChange={onChange}
                            placeholder={placeholder}
                            setInitialized={setInitialized}
                            type={type}
                            value={value}
                            workflow={workflow}
                        />
                    </div>
                </div>
            </SheetContent>
        </Sheet>
    );
};

export default PropertyMentionsInputEditorSheet;
