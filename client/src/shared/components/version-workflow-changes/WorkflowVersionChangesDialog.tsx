import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {Tabs, TabsContent, TabsList, TabsTrigger} from '@/components/ui/tabs';
import MonacoEditorLoader from '@/shared/components/MonacoEditorLoader';
import {VersionWorkflowChange} from '@/shared/middleware/graphql';
import {Suspense, lazy} from 'react';

import WorkflowDefinitionChangesList from './WorkflowDefinitionChangesList';

const MonacoDiffEditorWrapper = lazy(() => import('@/shared/components/MonacoDiffEditorWrapper'));

interface WorkflowVersionChangesTabsProps {
    change: VersionWorkflowChange;
}

const WorkflowVersionChangesTabs = ({change}: WorkflowVersionChangesTabsProps) => (
    <Tabs className="gap-4" defaultValue="changes">
        <TabsList>
            <TabsTrigger value="changes">Changes</TabsTrigger>

            <TabsTrigger value="definition">Definition</TabsTrigger>
        </TabsList>

        <TabsContent value="changes">
            <div className="h-[70vh] overflow-y-auto">
                <WorkflowDefinitionChangesList changeType={change.changeType} changes={change.changes} />
            </div>
        </TabsContent>

        <TabsContent value="definition">
            <div className="h-[70vh]">
                <Suspense fallback={<MonacoEditorLoader />}>
                    <MonacoDiffEditorWrapper
                        language="json"
                        modified={change.newDefinition ?? ''}
                        original={change.oldDefinition ?? ''}
                    />
                </Suspense>
            </div>
        </TabsContent>
    </Tabs>
);

interface WorkflowVersionChangesDialogProps {
    baseVersionLabel?: string;
    change?: VersionWorkflowChange;
    isError?: boolean;
    isLoading: boolean;
    onClose: () => void;
    open: boolean;
    versionLabel: string;
}

const WorkflowVersionChangesDialog = ({
    baseVersionLabel,
    change,
    isError,
    isLoading,
    onClose,
    open,
    versionLabel,
}: WorkflowVersionChangesDialogProps) => {
    const range = baseVersionLabel ? `${baseVersionLabel} → ${versionLabel}` : versionLabel;

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className="sm:w-[min(1400px,calc(100vw-4rem))]" size="xl">
                <DialogMain>
                    <DialogHeader
                        description={range}
                        title={change ? change.label || change.workflowUuid : 'Workflow changes'}
                    />

                    <DialogBody>
                        {isError ? (
                            <p className="py-4 text-sm text-content-destructive">
                                Couldn&apos;t load the workflow changes.
                            </p>
                        ) : isLoading || !change ? (
                            <div className="flex justify-center py-4">
                                <LoadingIcon />
                            </div>
                        ) : (
                            <WorkflowVersionChangesTabs change={change} />
                        )}
                    </DialogBody>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default WorkflowVersionChangesDialog;
