import {Tabs, TabsList, TabsTrigger} from '@/components/ui/tabs';
import {CodeIcon, FormInputIcon} from 'lucide-react';

export type EditorModeType = 'form' | 'yaml';

interface EndpointEditorToggleProps {
    mode: EditorModeType;
    onModeChange: (mode: EditorModeType) => void;
}

const EndpointEditorToggle = ({mode, onModeChange}: EndpointEditorToggleProps) => {
    return (
        <Tabs onValueChange={(value) => onModeChange(value as EditorModeType)} value={mode}>
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
        </Tabs>
    );
};

export default EndpointEditorToggle;
