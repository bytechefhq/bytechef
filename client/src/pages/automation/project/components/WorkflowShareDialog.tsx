import Button from '@/components/Button/Button';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {Dialog, DialogCloseButton, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Switch} from '@/components/ui/switch';
import {Textarea} from '@/components/ui/textarea';
import {
    useDeleteSharedWorkflowMutation,
    useExportSharedWorkflowMutation,
    useSharedWorkflowQuery,
} from '@/shared/middleware/graphql';
import {CheckIcon, LinkIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface WorkflowShareDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    projectVersion: number;
    workflowId: string;
    workflowUuid: string;
}

export function WorkflowShareDialog({
    onOpenChange,
    open,
    projectVersion,
    workflowId,
    workflowUuid,
}: WorkflowShareDialogProps) {
    const [description, setDescription] = useState<string | undefined>();
    const [shareState, setShareState] = useState<'not-shared' | 'exported' | 'disabled'>('not-shared');
    const [templateUrl, setTemplateUrl] = useState<string | undefined>();
    const [isCopied, setIsCopied] = useState(false);

    const {data: {sharedWorkflow} = {}, refetch} = useSharedWorkflowQuery({
        workflowUuid,
    });

    const deleteSharedWorkflowMutation = useDeleteSharedWorkflowMutation();
    const exportSharedWorkflowMutation = useExportSharedWorkflowMutation();

    const handleExport = () => {
        exportSharedWorkflowMutation.mutate(
            {
                description,
                workflowId,
            },
            {
                onError: (err) => {
                    console.log('error', err);
                },
                onSuccess: () => {
                    refetch();
                    setShareState('exported');
                },
            }
        );
    };

    const handleToggleCheckedChange = () => {
        if (shareState === 'disabled') {
            exportSharedWorkflowMutation.mutate(
                {
                    description,
                    workflowId,
                },
                {
                    onSuccess: () => {
                        refetch();
                        setShareState('exported');
                    },
                }
            );
        } else if (shareState === 'exported') {
            deleteSharedWorkflowMutation.mutate(
                {
                    workflowId,
                },
                {
                    onSuccess: () => setShareState('disabled'),
                }
            );
        }
    };

    const handleCopyLinkClick = async () => {
        try {
            await navigator.clipboard.writeText(templateUrl!);

            setIsCopied(true);
            setTimeout(() => setIsCopied(false), 2000);
        } catch (err) {
            console.error('Failed to copy link:', err);
        }
    };

    useEffect(() => {
        if (sharedWorkflow?.description) {
            setDescription(sharedWorkflow?.description);
        }

        if (sharedWorkflow?.exported === false) {
            setShareState('disabled');
        } else if (sharedWorkflow?.exported === true) {
            setShareState('exported');
        }

        if (sharedWorkflow?.publicUrl) {
            setTemplateUrl(sharedWorkflow?.publicUrl + '/import/shared/workflows/' + workflowUuid);
        }
    }, [sharedWorkflow, workflowUuid]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="flex flex-col">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <DialogTitle className="text-center text-lg font-semibold">Share workflow as template</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4">
                    <Alert
                        className={twMerge(shareState === 'not-shared' && 'bg-muted')}
                        variant={
                            shareState === 'exported' && !exportSharedWorkflowMutation.isPending
                                ? sharedWorkflow?.projectVersion === projectVersion
                                    ? 'success'
                                    : 'destructive'
                                : 'default'
                        }
                    >
                        <AlertDescription>
                            {shareState === 'not-shared' ? (
                                <div>
                                    <p className="mb-2 text-sm font-medium">This workflow has not been shared</p>

                                    <p className="mb-2 text-sm leading-relaxed text-muted-foreground">
                                        Exporting this workflow as a template will let others import it through a unique
                                        link that you can share. You can disable (and re-enable) the template at any
                                        time.
                                    </p>

                                    <Button className="h-auto p-0" label="Learn more" variant="link" />
                                </div>
                            ) : shareState === 'exported' ? (
                                <>
                                    <div className="flex items-center justify-between pb-4 font-semibold text-primary">
                                        {sharedWorkflow?.projectVersion === projectVersion ? (
                                            <span className="text-sm font-medium text-primary">
                                                This workflow has been exported
                                            </span>
                                        ) : (
                                            <span>An older version of this workflow is shared as a template</span>
                                        )}

                                        <Switch checked={true} onCheckedChange={handleToggleCheckedChange} />
                                    </div>

                                    {sharedWorkflow?.projectVersion === projectVersion ? (
                                        <p className="mb-3 text-sm text-muted-foreground">
                                            Please test the link and import experience before sharing.
                                        </p>
                                    ) : (
                                        <p className="mb-3 text-sm text-muted-foreground">
                                            To update the template based on the latest version of your workflow, click
                                            the Update button below.
                                        </p>
                                    )}

                                    <div className="flex items-center gap-2">
                                        <Input className="text-primary" readOnly value={templateUrl!} />

                                        {isCopied ? (
                                            <div className="flex items-center text-sm font-medium">
                                                <CheckIcon className="mr-1 size-4" />

                                                <span>Copied</span>
                                            </div>
                                        ) : (
                                            <Button
                                                icon={<LinkIcon />}
                                                label="Copy link"
                                                onClick={handleCopyLinkClick}
                                                size="sm"
                                                variant="secondary"
                                            />
                                        )}
                                    </div>
                                </>
                            ) : (
                                <div className="space-y-3">
                                    <div className="flex items-center justify-between">
                                        <span className="text-sm font-medium">
                                            This workflow is not currently shared
                                        </span>

                                        <Switch onCheckedChange={handleToggleCheckedChange} />
                                    </div>

                                    <div>
                                        <p className="mb-2 text-sm leading-relaxed text-muted-foreground">
                                            The link that was generated previously will not work any more. Use the
                                            toggle above to export the current version of your workflow and re-enable
                                            the link.
                                        </p>

                                        <Button className="h-auto p-0" label="Learn more" variant="link" />
                                    </div>
                                </div>
                            )}
                        </AlertDescription>
                    </Alert>

                    <div className="space-y-2">
                        <div className="flex items-center justify-between">
                            <Label className="text-sm font-medium" htmlFor="description">
                                Description <span className="text-red-500">*</span>
                            </Label>

                            {/*<Button*/}

                            {/*    className="h-auto p-1 text-purple-600 hover:bg-purple-50 hover:text-purple-700"*/}

                            {/*    size="sm"*/}

                            {/*    variant="ghost"*/}

                            {/*>*/}

                            {/*    <SparklesIcon className="mr-1 size-3" />*/}

                            {/*    <span className="text-xs">Generate with AI</span>*/}

                            {/*</Button>*/}
                        </div>

                        <Textarea
                            className="min-h-[80px] text-sm"
                            id="description"
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Describe what this workflow does..."
                            value={description}
                        />
                    </div>

                    {shareState === 'not-shared' && (
                        <Button
                            className="w-full"
                            disabled={!description}
                            label="Export and generate template link"
                            onClick={handleExport}
                        />
                    )}

                    {shareState === 'exported' && sharedWorkflow?.projectVersion !== projectVersion && (
                        <Button
                            className="w-full"
                            disabled={!description}
                            label="Update template based on the current version"
                            onClick={handleExport}
                        />
                    )}

                    <p className="space-x-1 text-xs leading-relaxed text-muted-foreground">
                        <span>
                            Template links will not be distributed by ByteChef app. You decide with whom and where to
                            share, and you may disable and re-enable them at any time.
                        </span>

                        <Button className="h-auto p-0" label="Learn more" size="sm" variant="link" />
                    </p>
                </div>
            </DialogContent>
        </Dialog>
    );
}
