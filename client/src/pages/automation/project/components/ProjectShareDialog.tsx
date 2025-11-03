import Button from '@/components/Button/Button';
import {Alert, AlertDescription} from '@/components/ui/alert';
import {Dialog, DialogCloseButton, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {Switch} from '@/components/ui/switch';
import {Textarea} from '@/components/ui/textarea';
import {
    useDeleteSharedProjectMutation,
    useExportSharedProjectMutation,
    useSharedProjectQuery,
} from '@/shared/middleware/graphql';
import {CheckIcon, LinkIcon} from 'lucide-react';
import {useEffect, useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface ProjectShareDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    projectVersion: number;
    projectId: number;
    projectUuid: string;
}

export function ProjectShareDialog({
    onOpenChange,
    open,
    projectId,
    projectUuid,
    projectVersion,
}: ProjectShareDialogProps) {
    const [description, setDescription] = useState<string | undefined>();
    const [shareState, setShareState] = useState<'not-shared' | 'exported' | 'disabled'>('not-shared');
    const [templateUrl, setTemplateUrl] = useState<string | undefined>();
    const [isCopied, setIsCopied] = useState(false);

    const {data: {sharedProject} = {}, refetch} = useSharedProjectQuery({
        projectUuid,
    });

    const deleteSharedProjectMutation = useDeleteSharedProjectMutation();
    const exportSharedProjectMutation = useExportSharedProjectMutation();

    const handleExport = () => {
        exportSharedProjectMutation.mutate(
            {
                description,
                id: projectId.toString(),
            },
            {
                onError: (err) => {
                    console.error('error', err);
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
            exportSharedProjectMutation.mutate(
                {
                    description,
                    id: projectId.toString(),
                },
                {
                    onSuccess: () => {
                        refetch();
                        setShareState('exported');
                    },
                }
            );
        } else if (shareState === 'exported') {
            deleteSharedProjectMutation.mutate(
                {
                    id: projectId.toString(),
                },
                {
                    onSuccess: () => {
                        setShareState('disabled');
                    },
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
        if (sharedProject?.description) {
            setDescription(sharedProject?.description);
        }

        if (sharedProject?.exported === false) {
            setShareState('disabled');
        } else if (sharedProject?.exported === true) {
            setShareState('exported');
        }

        if (sharedProject?.publicUrl) {
            setTemplateUrl(sharedProject?.publicUrl + '/import/shared/projects/' + projectUuid);
        }
    }, [sharedProject, projectUuid]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="flex flex-col">
                <DialogHeader className="flex flex-row items-center justify-between space-y-0">
                    <DialogTitle className="text-center text-lg font-semibold">Share project as template</DialogTitle>

                    <DialogCloseButton />
                </DialogHeader>

                <div className="space-y-4">
                    <Alert
                        className={twMerge(shareState === 'not-shared' && 'bg-muted')}
                        variant={
                            shareState === 'exported' && !exportSharedProjectMutation.isPending
                                ? sharedProject?.projectVersion === projectVersion
                                    ? 'success'
                                    : 'destructive'
                                : 'default'
                        }
                    >
                        <AlertDescription>
                            {shareState === 'not-shared' && (
                                <div>
                                    <p className="mb-2 text-sm font-medium">This project has not been shared</p>

                                    <p className="mb-2 text-sm leading-relaxed text-muted-foreground">
                                        Exporting this project as a template will let others import it through a unique
                                        link that you can share. You can disable (and re-enable) the template at any
                                        time.
                                    </p>

                                    <Button className="h-auto p-0" label="Learn more" variant="link" />
                                </div>
                            )}

                            {shareState === 'exported' && (
                                <>
                                    <div className="flex items-center justify-between pb-4 font-semibold text-primary">
                                        {sharedProject?.projectVersion === projectVersion ? (
                                            <span className="text-sm font-medium text-primary">
                                                This project has been exported
                                            </span>
                                        ) : (
                                            <span>An older version of this project is shared as a template</span>
                                        )}

                                        <Switch checked={true} onCheckedChange={handleToggleCheckedChange} />
                                    </div>

                                    {sharedProject?.projectVersion === projectVersion ? (
                                        <p className="mb-3 text-sm text-muted-foreground">
                                            Please test the link and import experience before sharing.
                                        </p>
                                    ) : (
                                        <p className="mb-3 text-sm text-muted-foreground">
                                            To update the template based on the latest version of your project, click
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
                            )}

                            {shareState !== 'not-shared' && shareState !== 'exported' && (
                                <div className="space-y-3">
                                    <div className="flex items-center justify-between">
                                        <span className="text-sm font-medium">
                                            This project is not currently shared
                                        </span>

                                        <Switch onCheckedChange={handleToggleCheckedChange} />
                                    </div>

                                    <div>
                                        <p className="mb-2 text-sm leading-relaxed text-muted-foreground">
                                            The link that was generated previously will not work any more. Use the
                                            toggle above to export the current version of your project and re-enable the
                                            link.
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
                        </div>

                        <Textarea
                            className="min-h-[80px] text-sm"
                            id="description"
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Describe what this project does..."
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

                    {shareState === 'exported' && sharedProject?.projectVersion !== projectVersion && (
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
