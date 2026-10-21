import Button from '@/components/Button/Button';
import ComboBox from '@/components/ComboBox/ComboBox';
import {
    Dialog,
    DialogBody,
    DialogCancelButton,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogMain,
} from '@/components/Dialog';
import {Label} from '@/components/ui/label';
import useInvalidateAppDeploymentQueries from '@/pages/automation/apps/hooks/useInvalidateAppDeploymentQueries';
import {AppVersion, useDeployAppInstanceMutation} from '@/shared/middleware/graphql';
import {useEffect, useMemo, useState} from 'react';

interface AppDeployDialogProps {
    appInstanceId: string;
    appVersions: Pick<AppVersion, 'createdDate' | 'gitCommitSha' | 'id' | 'name'>[];
    onOpenChange: (open: boolean) => void;
    open: boolean;
}

const AppDeployDialog = ({appInstanceId, appVersions, onOpenChange, open}: AppDeployDialogProps) => {
    const [selectedAppVersionId, setSelectedAppVersionId] = useState<string>('');

    const invalidateAppDeploymentQueries = useInvalidateAppDeploymentQueries();

    const deployAppInstanceMutation = useDeployAppInstanceMutation({
        onSuccess: () => {
            invalidateAppDeploymentQueries();

            onOpenChange(false);
        },
    });

    const versionItems = useMemo(
        () =>
            appVersions.map((appVersion) => ({
                label: appVersion.gitCommitSha
                    ? `${appVersion.name} (${appVersion.gitCommitSha.substring(0, 7)})`
                    : appVersion.name,
                value: appVersion.id,
            })),
        [appVersions]
    );

    const newestAppVersionId = appVersions[0]?.id || '';

    useEffect(() => {
        if (open) {
            setSelectedAppVersionId(newestAppVersionId);
        }
    }, [newestAppVersionId, open]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent size="md">
                <DialogMain>
                    <DialogHeader
                        description="The hosting provider builds and publishes the chosen version. The list shows the newest versions first."
                        title="Deploy a version"
                    />

                    <DialogBody>
                        <fieldset className="space-y-2 border-0 p-0">
                            <Label>Version</Label>

                            <ComboBox
                                aria-label="Version"
                                emptyMessage="This app has no versions."
                                items={versionItems}
                                onChange={(item) => setSelectedAppVersionId(item?.value ?? '')}
                                value={selectedAppVersionId}
                            />
                        </fieldset>
                    </DialogBody>

                    <DialogFooter>
                        <DialogCancelButton />

                        <Button
                            disabled={!selectedAppVersionId || deployAppInstanceMutation.isPending}
                            label="Deploy"
                            onClick={() =>
                                deployAppInstanceMutation.mutate({appInstanceId, appVersionId: selectedAppVersionId})
                            }
                        />
                    </DialogFooter>
                </DialogMain>
            </DialogContent>
        </Dialog>
    );
};

export default AppDeployDialog;
