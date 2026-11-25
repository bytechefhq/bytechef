import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import LoadingIcon from '@/components/LoadingIcon';
import {Alert, AlertDescription, AlertTitle} from '@/components/ui/alert';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {ENVIRONMENT_CONFIGS} from '@/shared/constants/environmentConfigs';
import {PromotionResourceType} from '@/shared/middleware/graphql';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {AlertTriangleIcon} from 'lucide-react';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {toast} from 'sonner';

import EnvironmentPromotionConnectionRow from './EnvironmentPromotionConnectionRow';
import {useEnvironmentPromotionDialog} from './hooks/useEnvironmentPromotionDialog';

export interface EnvironmentPromotionDialogProps {
    onClose: () => void;
    onPromoted?: (result: {created: boolean; targetEnvironmentId: number; targetId: string}) => void;
    resourceType: PromotionResourceType;
    sourceEnvironmentId: number;
    sourceId: string;
    sourceName: string;
    workspaceId: number;
}

const TARGET_LIST_PATHS: Record<PromotionResourceType, string> = {
    [PromotionResourceType.A2AServer]: '/automation/a2a-servers',
    [PromotionResourceType.ApiCollection]: '/automation/api-platform/api-collections',
    [PromotionResourceType.McpServer]: '/automation/mcp-servers',
    [PromotionResourceType.ProjectDeployment]: '/automation/deployments',
};

const EnvironmentPromotionDialog = ({
    onClose,
    onPromoted,
    resourceType,
    sourceEnvironmentId,
    sourceId,
    sourceName,
    workspaceId,
}: EnvironmentPromotionDialogProps) => {
    const [open, setOpen] = useState(true);

    const {
        handlePromote,
        handleTargetConnectionIdChange,
        isPreviewLoading,
        isPromotePending,
        mappings,
        preview,
        setTargetEnvironmentId,
        targetEnvironmentId,
        targetEnvironmentOptions,
        unresolvedConnectionCount,
    } = useEnvironmentPromotionDialog({resourceType, sourceEnvironmentId, sourceId});

    const navigate = useNavigate();

    const closeDialog = () => {
        setOpen(false);

        onClose();
    };

    const willCreateTarget = preview ? preview.existingTargetId == null : false;

    const handlePromoteClick = async () => {
        let result: Awaited<ReturnType<typeof handlePromote>>;

        try {
            result = await handlePromote();
        } catch {
            return;
        }

        if (!result || targetEnvironmentId == null) {
            return;
        }

        const targetConfig = ENVIRONMENT_CONFIGS[targetEnvironmentId];
        const targetLabel = targetConfig ? targetConfig.label : `environment ${targetEnvironmentId}`;

        const descriptionLines = [
            result.created
                ? `A new "${sourceName}" counterpart was created disabled in ${targetLabel}; enable it after reviewing its connections.`
                : `The existing "${sourceName}" counterpart in ${targetLabel} was updated.`,
        ];

        if (result.created && result.targetUrl) {
            descriptionLines.push(`New URL: ${result.targetUrl}`);
        }

        if (result.unresolvedConnectionIds.length > 0) {
            descriptionLines.push(`${result.unresolvedConnectionIds.length} connections still need a binding.`);
        }

        const projectId = preview?.projects[0]?.projectId;

        const targetListPath =
            resourceType === PromotionResourceType.ProjectDeployment && projectId
                ? `${TARGET_LIST_PATHS[resourceType]}?projectId=${projectId}`
                : TARGET_LIST_PATHS[resourceType];

        const targetId = result.targetId;

        toast.success(`Promoted to ${targetLabel}`, {
            action: {
                label: `View in ${targetLabel}`,
                onClick: () => {
                    environmentStore.getState().setCurrentEnvironmentId(targetEnvironmentId);

                    navigate(targetListPath, {
                        state:
                            resourceType === PromotionResourceType.ProjectDeployment
                                ? {projectDeploymentId: +targetId}
                                : undefined,
                    });
                },
            },
            description: descriptionLines.join(' '),
        });

        onPromoted?.({created: result.created, targetEnvironmentId, targetId: result.targetId});

        closeDialog();
    };

    return (
        <Dialog
            onOpenChange={(nextOpen) => {
                if (!nextOpen) {
                    closeDialog();
                }
            }}
            open={open}
        >
            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader title={`Promote "${sourceName}"`} />

                    <DialogBody>
                        <div className="space-y-2">
                            <label className="text-sm font-medium" htmlFor="environment-promotion-target-select">
                                Target environment
                            </label>

                            <Select
                                onValueChange={(value) => setTargetEnvironmentId(+value)}
                                value={targetEnvironmentId != null ? targetEnvironmentId.toString() : undefined}
                            >
                                <SelectTrigger id="environment-promotion-target-select">
                                    <SelectValue placeholder="Choose target environment..." />
                                </SelectTrigger>

                                <SelectContent>
                                    {targetEnvironmentOptions.map((option) => (
                                        <SelectItem key={option.id} value={option.id.toString()}>
                                            {option.label}
                                        </SelectItem>
                                    ))}
                                </SelectContent>
                            </Select>
                        </div>

                        {isPreviewLoading && (
                            <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                <LoadingIcon />

                                <span>Loading preview…</span>
                            </div>
                        )}

                        {preview && (
                            <div className="space-y-3">
                                <Badge
                                    className="border-transparent"
                                    label={
                                        willCreateTarget ? 'Will create' : `Will update ${preview.existingTargetName}`
                                    }
                                    styleType={willCreateTarget ? 'success-outline' : 'warning-outline'}
                                />

                                {preview.projects.length > 0 && (
                                    <ul className="space-y-1 text-sm text-content-neutral-secondary">
                                        {preview.projects.map((project) => (
                                            <li key={project.projectId}>
                                                {`${project.projectName}: v${project.sourceProjectVersion} → ${
                                                    project.targetProjectVersion != null
                                                        ? `v${project.targetProjectVersion}`
                                                        : 'new'
                                                }`}
                                            </li>
                                        ))}
                                    </ul>
                                )}

                                {preview.warnings.map((warning) => (
                                    <Alert key={warning} variant="warning">
                                        <AlertTriangleIcon />

                                        <AlertDescription>{warning}</AlertDescription>
                                    </Alert>
                                ))}

                                {unresolvedConnectionCount > 0 && (
                                    <Alert variant="warning">
                                        <AlertTriangleIcon />

                                        <AlertTitle>Unbound connections</AlertTitle>

                                        <AlertDescription>
                                            {willCreateTarget
                                                ? `${unresolvedConnectionCount} connection(s) are unresolved; bind them in the new target before enabling it.`
                                                : `${unresolvedConnectionCount} connection(s) are unresolved, so the affected nodes in the existing target are published without a connection.`}
                                        </AlertDescription>
                                    </Alert>
                                )}

                                {preview.connections.length > 0 && (
                                    <fieldset className="space-y-1 border-0">
                                        <legend className="text-sm font-medium">Connections</legend>

                                        {preview.connections.map((connection) => (
                                            <EnvironmentPromotionConnectionRow
                                                connection={connection}
                                                key={connection.sourceConnectionId}
                                                onTargetConnectionIdChange={handleTargetConnectionIdChange}
                                                targetConnectionId={mappings[connection.sourceConnectionId]}
                                                targetEnvironmentId={targetEnvironmentId ?? sourceEnvironmentId}
                                                workspaceId={workspaceId}
                                            />
                                        ))}
                                    </fieldset>
                                )}
                            </div>
                        )}
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={isPreviewLoading || isPromotePending || !preview}
                            label={isPromotePending ? 'Promoting…' : 'Promote'}
                            onClick={handlePromoteClick}
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default EnvironmentPromotionDialog;
