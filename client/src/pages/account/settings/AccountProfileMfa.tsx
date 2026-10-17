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
import {Input} from '@/components/Input/Input';
import LoadingIcon from '@/components/LoadingIcon';
import {ShieldCheckIcon, ShieldOffIcon} from 'lucide-react';

import {useAccountProfileMfa} from './hooks/useAccountProfileMfa';

const AccountProfileMfa = () => {
    const {
        disableCode,
        disablePassword,
        handleCancelSetup,
        handleDisable,
        handleEnable,
        handleSetup,
        loading,
        mfaState,
        qrCodeDataUrl,
        secret,
        setDisableCode,
        setDisablePassword,
        setShowDisableDialog,
        setVerifyCode,
        showDisableDialog,
        verifyCode,
    } = useAccountProfileMfa();

    return (
        <div className="py-12">
            <h2 className="text-base leading-7 font-semibold text-content-neutral-primary">
                Two-Factor Authentication
            </h2>

            <p className="mt-1 text-sm leading-6 text-content-neutral-secondary">
                Add an extra layer of security to your account using an authenticator app.
            </p>

            <div className="mt-6">
                {mfaState === 'disabled' && (
                    <Button
                        disabled={loading}
                        icon={loading ? <LoadingIcon /> : undefined}
                        label="Set up 2FA"
                        onClick={handleSetup}
                        variant="outline"
                    />
                )}

                {mfaState === 'setup' && (
                    <div className="space-y-4">
                        <p className="text-sm text-content-neutral-secondary">
                            Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.):
                        </p>

                        {qrCodeDataUrl && (
                            <div className="flex justify-center rounded-lg border p-4">
                                <img alt="TOTP QR Code" className="size-48" src={qrCodeDataUrl} />
                            </div>
                        )}

                        <div className="space-y-2">
                            <p className="text-xs text-muted-foreground">
                                Or enter this secret manually in your authenticator app:
                            </p>

                            <code className="block rounded bg-muted px-3 py-2 font-mono text-sm break-all">
                                {secret}
                            </code>
                        </div>

                        <fieldset className="space-y-2 border-0 p-0">
                            <label className="text-sm font-medium" htmlFor="verifyCode">
                                Verification Code
                            </label>

                            <Input
                                autoFocus
                                id="verifyCode"
                                inputMode="numeric"
                                maxLength={6}
                                onChange={(event) => setVerifyCode(event.target.value)}
                                pattern="[0-9]*"
                                placeholder="Enter 6-digit code"
                                value={verifyCode}
                            />
                        </fieldset>

                        <div className="flex gap-2">
                            <Button
                                disabled={verifyCode.length !== 6 || loading}
                                icon={loading ? <LoadingIcon /> : undefined}
                                label="Verify & Enable"
                                onClick={handleEnable}
                            />

                            <Button disabled={loading} label="Cancel" onClick={handleCancelSetup} variant="outline" />
                        </div>
                    </div>
                )}

                {mfaState === 'enabled' && (
                    <div className="space-y-4">
                        <div className="flex items-center gap-2 rounded-lg border border-stroke-success-secondary bg-surface-success-secondary p-3">
                            <ShieldCheckIcon className="size-4 text-content-success-primary" />

                            <p className="text-sm text-content-success-primary">
                                Two-factor authentication is enabled.
                            </p>
                        </div>

                        <Button
                            icon={<ShieldOffIcon className="size-4" />}
                            label="Disable 2FA"
                            onClick={() => setShowDisableDialog(true)}
                            variant="outline"
                        />

                        <Dialog
                            onOpenChange={(open) => {
                                if (!open) {
                                    setShowDisableDialog(false);

                                    setDisableCode('');
                                    setDisablePassword('');
                                }
                            }}
                            open={showDisableDialog}
                        >
                            <DialogContent>
                                <DialogMain>
                                    <DialogHeader
                                        description="Enter your current password and a TOTP code to disable two-factor authentication."
                                        title="Disable Two-Factor Authentication"
                                    />

                                    <DialogBody className="space-y-3">
                                        <fieldset className="space-y-2 border-0 p-0">
                                            <label className="text-sm font-medium" htmlFor="disablePassword">
                                                Password
                                            </label>

                                            <Input
                                                id="disablePassword"
                                                onChange={(event) => setDisablePassword(event.target.value)}
                                                placeholder="Enter your password"
                                                type="password"
                                                value={disablePassword}
                                            />
                                        </fieldset>

                                        <fieldset className="space-y-2 border-0 p-0">
                                            <label className="text-sm font-medium" htmlFor="disableCode">
                                                Authentication Code
                                            </label>

                                            <Input
                                                id="disableCode"
                                                inputMode="numeric"
                                                maxLength={6}
                                                onChange={(event) => setDisableCode(event.target.value)}
                                                pattern="[0-9]*"
                                                placeholder="Enter 6-digit code"
                                                value={disableCode}
                                            />
                                        </fieldset>
                                    </DialogBody>

                                    <DialogFooter>
                                        <DialogCancelButton />

                                        <Button
                                            disabled={disableCode.length !== 6 || !disablePassword || loading}
                                            icon={loading ? <LoadingIcon /> : undefined}
                                            label="Disable 2FA"
                                            onClick={handleDisable}
                                        />
                                    </DialogFooter>
                                </DialogMain>
                            </DialogContent>
                        </Dialog>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AccountProfileMfa;
