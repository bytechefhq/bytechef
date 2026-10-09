import Button from '@/components/Button/Button';
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
import {ScrollArea} from '@/components/ui/scroll-area';
import {McpComponent} from '@/shared/middleware/graphql';
import {ReactNode, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';

export interface McpComponentDialogProps {
    mcpComponent?: McpComponent;
    mcpServerId: string;
    onOpenChange?: (open: boolean) => void;
    open?: boolean;
    triggerNode?: ReactNode;
}

interface McpComponentDialogShellProps {
    children: ReactNode;
    currentStep: 'components' | 'tools';
    editing: boolean;
    onBack: () => void;
    onOpenChange: (open: boolean) => void;
    onSave: () => void;
    open?: boolean;
    selectedComponent?: {name?: string | null; title?: string | null} | null;
    selectedToolCount: number;
    triggerNode?: ReactNode;
}

const McpComponentDialogShell = ({
    children,
    currentStep,
    editing,
    onBack,
    onOpenChange,
    onSave,
    open,
    selectedComponent,
    selectedToolCount,
    triggerNode,
}: McpComponentDialogShellProps) => {
    const {description, title} = useMemo(() => {
        if (currentStep === 'components') {
            return {
                description: 'Choose a component to add to your MCP server.',
                title: 'Select Component',
            };
        }

        const componentLabel = selectedComponent?.title || selectedComponent?.name;

        if (editing) {
            return {
                description: 'Modify the tools enabled for this component.',
                title: `Edit Tools for ${componentLabel}`,
            };
        }

        return {
            description: 'Select the tools you want to enable for this component.',
            title: `Select Tools from ${componentLabel}`,
        };
    }, [currentStep, editing, selectedComponent]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            {triggerNode && <DialogTrigger asChild>{triggerNode}</DialogTrigger>}

            <DialogContent size="lg">
                <DialogMain>
                    <DialogHeader description={description} title={title} />

                    <DialogBody>
                        <ScrollArea className={twMerge('max-h-[60vh]', currentStep === 'components' && 'h-[60vh]')}>
                            {children}
                        </ScrollArea>
                    </DialogBody>

                    <DialogFooter>
                        {currentStep === 'tools' && (
                            <div className="flex w-full justify-between">
                                <div className="text-sm text-muted-foreground">
                                    {selectedToolCount} tool{selectedToolCount !== 1 ? 's' : ''} selected
                                </div>

                                <div className="flex space-x-2">
                                    <DialogCancelButton />

                                    {!editing && <Button label="Back" onClick={onBack} variant="outline" />}

                                    <Button
                                        disabled={selectedToolCount === 0}
                                        label={editing ? 'Update' : 'Save'}
                                        onClick={onSave}
                                    />
                                </div>
                            </div>
                        )}
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default McpComponentDialogShell;
