import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import {Input} from '@/components/ui/input';
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
        setVerifyCode,
        verifyCode,
    } = useAccountProfileMfa();

    return (
        <div className="py-12">
            <h2 className="text-base font-semibold leading-7 text-gray-900">Two-Factor Authentication</h2>

            <p className="mt-1 text-sm leading-6 text-gray-500">
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
                        <p className="text-sm text-gray-700">
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

                            <code className="block break-all rounded bg-muted px-3 py-2 font-mono text-sm">
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
                        <div className="flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 p-3">
                            <ShieldCheckIcon className="size-4 text-green-600" />

                            <p className="text-sm text-green-700">Two-factor authentication is enabled.</p>
                        </div>

                        <AlertDialog>
                            <AlertDialogTrigger asChild>
                                <Button
                                    icon={<ShieldOffIcon className="size-4" />}
                                    label="Disable 2FA"
                                    variant="outline"
                                />
                            </AlertDialogTrigger>

                            <AlertDialogContent>
                                <AlertDialogHeader>
                                    <AlertDialogTitle>Disable Two-Factor Authentication</AlertDialogTitle>

                                    <AlertDialogDescription>
                                        Enter your current password and a TOTP code to disable two-factor
                                        authentication.
                                    </AlertDialogDescription>
                                </AlertDialogHeader>

                                <div className="space-y-3 py-2">
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
                                </div>

                                <AlertDialogFooter>
                                    <AlertDialogCancel
                                        onClick={() => {
                                            setDisableCode('');
                                            setDisablePassword('');
                                        }}
                                    >
                                        Cancel
                                    </AlertDialogCancel>

                                    <AlertDialogAction
                                        disabled={disableCode.length !== 6 || !disablePassword || loading}
                                        onClick={handleDisable}
                                    >
                                        Disable 2FA
                                    </AlertDialogAction>
                                </AlertDialogFooter>
                            </AlertDialogContent>
                        </AlertDialog>
                    </div>
                )}
            </div>
        </div>
    );
};

export default AccountProfileMfa;
