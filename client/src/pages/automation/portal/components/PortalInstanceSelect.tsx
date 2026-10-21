import {Dialog, DialogBody, DialogContent, DialogHeader, DialogMain} from '@/components/Dialog';
import {PortalAppInstanceType, PortalAppType, isLaunchableUrl} from '@/pages/automation/portal/utils';
import {ExternalLinkIcon} from 'lucide-react';

interface PortalInstanceSelectProps {
    onLaunch: (portalAppInstance: PortalAppInstanceType) => void;
    onOpenChange: (open: boolean) => void;
    portalApp: PortalAppType;
}

const PortalInstanceSelect = ({onLaunch, onOpenChange, portalApp}: PortalInstanceSelectProps) => (
    <Dialog onOpenChange={onOpenChange} open={true}>
        <DialogContent aria-describedby={undefined} size="sm">
            <DialogMain>
                <DialogHeader title={`Open ${portalApp.name}`} />

                <DialogBody>
                    <ul className="space-y-2">
                        {portalApp.instances.map((portalAppInstance) => {
                            const launchable = isLaunchableUrl(portalAppInstance.url);

                            return (
                                <li key={portalAppInstance.id}>
                                    <button
                                        className="flex w-full items-center justify-between rounded-md border border-stroke-neutral-secondary px-3 py-2 text-left hover:bg-surface-neutral-primary-hover disabled:cursor-not-allowed disabled:opacity-60"
                                        disabled={!launchable}
                                        onClick={() => {
                                            onLaunch(portalAppInstance);

                                            onOpenChange(false);
                                        }}
                                        type="button"
                                    >
                                        <span className="flex flex-col">
                                            <span className="text-sm font-semibold">{portalAppInstance.name}</span>

                                            <span className="text-xs text-content-neutral-secondary">
                                                {portalAppInstance.workspaceName}
                                            </span>

                                            {!launchable && (
                                                <span className="text-xs text-content-destructive">
                                                    No address that can be opened.
                                                </span>
                                            )}
                                        </span>

                                        {launchable && <ExternalLinkIcon className="size-4 shrink-0" />}
                                    </button>
                                </li>
                            );
                        })}
                    </ul>
                </DialogBody>
            </DialogMain>
        </DialogContent>
    </Dialog>
);

export default PortalInstanceSelect;
