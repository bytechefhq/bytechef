import Button from '@/components/Button/Button';
import {
    PortalAppInstanceType,
    PortalAppType,
    getPortalAppIconUrl,
    isLaunchableUrl,
} from '@/pages/automation/portal/utils';
import {AppWindowIcon, StarIcon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

interface PortalAppIconProps {
    portalApp: PortalAppType;
}

const PortalAppIcon = ({portalApp}: PortalAppIconProps) => {
    const [failed, setFailed] = useState(false);

    if (portalApp.hasIcon && !failed) {
        return (
            <img
                alt=""
                className="size-10 shrink-0 rounded-md object-contain"
                data-testid={`portal-app-icon-${portalApp.id}`}
                onError={() => setFailed(true)}
                src={getPortalAppIconUrl(portalApp.id)}
            />
        );
    }

    return (
        <span
            className="flex size-10 shrink-0 items-center justify-center rounded-md bg-surface-neutral-secondary"
            data-testid={`portal-app-icon-${portalApp.id}`}
        >
            <AppWindowIcon className="size-5 text-content-neutral-secondary" />
        </span>
    );
};

interface PortalAppCardProps {
    onLaunch: (portalAppInstance: PortalAppInstanceType) => void;
    onSelectInstance: (portalApp: PortalAppType) => void;
    onToggleFavorite: (portalApp: PortalAppType) => void;
    portalApp: PortalAppType;
}

const PortalAppCard = ({onLaunch, onSelectInstance, onToggleFavorite, portalApp}: PortalAppCardProps) => {
    const singleInstance = portalApp.instances.length === 1 ? portalApp.instances[0] : undefined;

    const launchable = singleInstance
        ? isLaunchableUrl(singleInstance.url)
        : portalApp.instances.some((portalAppInstance) => isLaunchableUrl(portalAppInstance.url));

    let statusText = 'This app has no address that can be opened.';

    if (launchable) {
        statusText = singleInstance ? singleInstance.workspaceName : `${portalApp.instances.length} instances`;
    }

    const handleOpen = () => {
        if (singleInstance) {
            onLaunch(singleInstance);
        } else {
            onSelectInstance(portalApp);
        }
    };

    return (
        <article
            className="flex h-full flex-col gap-3 rounded-md border border-stroke-neutral-secondary p-4"
            data-testid={`portal-app-card-${portalApp.id}`}
        >
            <div className="flex items-start gap-3">
                <PortalAppIcon key={portalApp.id} portalApp={portalApp} />

                <div className="min-w-0 flex-1">
                    <h3 className="truncate text-base font-semibold">{portalApp.name}</h3>

                    {portalApp.categoryName && (
                        <p className="text-xs text-content-neutral-secondary">{portalApp.categoryName}</p>
                    )}

                    {portalApp.description && (
                        <p className="line-clamp-2 text-sm text-content-neutral-secondary">{portalApp.description}</p>
                    )}
                </div>

                <Button
                    aria-label={
                        portalApp.favorite
                            ? `Remove ${portalApp.name} from favorites`
                            : `Add ${portalApp.name} to favorites`
                    }
                    aria-pressed={portalApp.favorite}
                    icon={
                        <StarIcon
                            className={twMerge(portalApp.favorite && 'fill-current text-content-warning-primary')}
                        />
                    }
                    onClick={() => onToggleFavorite(portalApp)}
                    size="iconSm"
                    variant="ghost"
                />
            </div>

            <div className="mt-auto flex items-center justify-between gap-2">
                <span className="text-xs text-content-neutral-secondary">{statusText}</span>

                <Button
                    aria-label={`Open ${portalApp.name}`}
                    disabled={!launchable}
                    label="Open"
                    onClick={handleOpen}
                    size="sm"
                />
            </div>
        </article>
    );
};

export default PortalAppCard;
