import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {VersionWorkflowChange} from '@/shared/middleware/graphql';
import {Suspense, lazy} from 'react';

import WorkflowChangeCanvasTab from './WorkflowChangeCanvasTab';
import WorkflowDefinitionChangesList from './WorkflowDefinitionChangesList';

const MonacoDiffEditorWrapper = lazy(() => import('@/shared/components/MonacoDiffEditorWrapper'));

interface WorkflowVersionChangesDialogProps {
    baseVersionLabel?: string;
    change?: VersionWorkflowChange;
    isLoading: boolean;
    onClose: () => void;
    open: boolean;
    versionLabel: string;
}

const WorkflowVersionChangesDialog = ({
    baseVersionLabel,
    change,
    isLoading,
    onClose,
    open,
    versionLabel,
}: WorkflowVersionChangesDialogProps) => {
    const range = baseVersionLabel ? `${baseVersionLabel} → ${versionLabel}` : versionLabel;

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent size="xl">
                <DialogMain>
                    <DialogHeader
                        description={range}
                        title={change ? change.label || change.workflowUuid : 'Workflow changes'}
                    />

                    <DialogBody>
                        {isLoading || !change ? (
                            <div className="flex justify-center py-4">
                                <LoadingIcon />
                            </div>
                        ) : (
                            <Tabs defaultValue="changes">
                                <TabsList>
                                    <TabsTrigger value="changes">Changes</TabsTrigger>

                                    <TabsTrigger value="definition">Definition</TabsTrigger>

                                    <TabsTrigger value="canvas">Canvas</TabsTrigger>
                                </TabsList>

                                <TabsContent value="changes">
                                    <WorkflowDefinitionChangesList changes={change.changes} />
                                </TabsContent>

                                <TabsContent value="definition">
                                    <div className="h-[60vh]">
                                        <Suspense fallback={<MonacoEditorLoader />}>
                                            <MonacoDiffEditorWrapper
                                                language="json"
                                                modified={change.newDefinition ?? ''}
                                                original={change.oldDefinition ?? ''}
                                            />
                                        </Suspense>
                                    </div>
                                </TabsContent>

                                <TabsContent value="canvas">
                                    <WorkflowChangeCanvasTab
                                        baseVersionLabel={baseVersionLabel}
                                        change={change}
                                        versionLabel={versionLabel}
                                    />
                                </TabsContent>
                            </Tabs>
                        )}
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowVersionChangesDialog;
