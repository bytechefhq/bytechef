import Button from '@/components/Button/Button';
import {APP_ICON_ACCEPTED_TYPES, getAppIconPreviewUrl, validateAppIconFile} from '@/pages/automation/apps/appIcon';
import {AppWindowIcon} from 'lucide-react';
import {ChangeEvent, useEffect, useRef, useState} from 'react';

interface PendingIconPreviewI {
    file: File;
    url: string;
}

interface AppIconFieldProps {
    appId?: string;
    hasIcon: boolean;
    onPendingFileChange: (file: File | undefined) => void;
    onRemoveChange: (remove: boolean) => void;
    pendingFile?: File;
    remove: boolean;
}

const AppIconField = ({
    appId,
    hasIcon,
    onPendingFileChange,
    onRemoveChange,
    pendingFile,
    remove,
}: AppIconFieldProps) => {
    const [failedIconUrl, setFailedIconUrl] = useState<string | undefined>();
    const [pendingIconPreview, setPendingIconPreview] = useState<PendingIconPreviewI | undefined>();
    const [validationError, setValidationError] = useState<string | undefined>();

    const fileInputRef = useRef<HTMLInputElement>(null);

    const pendingIconUrl = pendingFile && pendingIconPreview?.file === pendingFile ? pendingIconPreview.url : undefined;

    const iconUrl = appId && hasIcon && !remove && !pendingFile ? getAppIconPreviewUrl(appId) : undefined;

    const currentIconUrl = iconUrl && iconUrl !== failedIconUrl ? iconUrl : undefined;

    const hasAnyIcon = !!pendingFile || (hasIcon && !remove);

    const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];

        event.target.value = '';

        if (!file) {
            return;
        }

        const error = validateAppIconFile(file);

        setValidationError(error);

        if (error) {
            return;
        }

        onPendingFileChange(file);
        onRemoveChange(false);
    };

    const handleRemove = () => {
        setValidationError(undefined);

        onPendingFileChange(undefined);
        onRemoveChange(hasIcon);
    };

    useEffect(() => {
        if (!pendingFile) {
            setPendingIconPreview(undefined);

            return undefined;
        }

        const objectUrl = URL.createObjectURL(pendingFile);

        setPendingIconPreview({file: pendingFile, url: objectUrl});

        return () => URL.revokeObjectURL(objectUrl);
    }, [pendingFile]);

    return (
        <fieldset className="space-y-2 border-0 p-0">
            <legend className="text-sm font-medium">Icon</legend>

            <div className="flex items-center gap-3">
                {pendingIconUrl && (
                    <img alt="Selected icon" className="size-10 rounded-md object-contain" src={pendingIconUrl} />
                )}

                {!pendingFile && currentIconUrl && (
                    <img
                        alt="Current icon"
                        className="size-10 rounded-md object-contain"
                        onError={() => setFailedIconUrl(currentIconUrl)}
                        src={currentIconUrl}
                    />
                )}

                {!pendingIconUrl && !currentIconUrl && (
                    <span className="flex size-10 items-center justify-center rounded-md bg-surface-neutral-secondary">
                        <AppWindowIcon className="size-5 text-content-neutral-secondary" />
                    </span>
                )}

                <input
                    accept={APP_ICON_ACCEPTED_TYPES.join(',')}
                    aria-label="Icon file"
                    className="hidden"
                    onChange={handleFileChange}
                    ref={fileInputRef}
                    type="file"
                />

                <Button
                    label={hasAnyIcon ? 'Replace icon' : 'Upload icon'}
                    onClick={() => fileInputRef.current?.click()}
                    size="sm"
                    type="button"
                    variant="outline"
                />

                {hasAnyIcon && (
                    <Button label="Remove icon" onClick={handleRemove} size="sm" type="button" variant="ghost" />
                )}
            </div>

            {pendingFile && <p className="text-xs text-content-neutral-secondary">{`Selected: ${pendingFile.name}`}</p>}

            {validationError ? (
                <p className="text-xs text-content-destructive" role="alert">
                    {validationError}
                </p>
            ) : (
                <p className="text-xs text-content-neutral-secondary">PNG, JPEG or WebP, up to 256 KB.</p>
            )}
        </fieldset>
    );
};

export default AppIconField;
