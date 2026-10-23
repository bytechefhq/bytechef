import {Card, CardContent, CardHeader, CardTitle} from '@/components/ui/card';
import NewPasswordForm from '@/pages/account/public/components/NewPasswordForm';
import {usePasswordResetStore} from '@/pages/account/public/stores/usePasswordResetStore';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import PasswordResetSuccessful from './PasswordResetSuccessful';

const PasswordResetFinish = () => {
    const {resetPasswordFinish, resetPasswordSuccess} = usePasswordResetStore(
        useShallow((state) => ({
            resetPasswordFinish: state.resetPasswordFinish,
            resetPasswordSuccess: state.resetPasswordSuccess,
        }))
    );

    const [searchParams] = useSearchParams();
    const key = searchParams.get('key');

    if (resetPasswordSuccess) {
        return <PasswordResetSuccessful />;
    }

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto max-w-sm rounded-xl p-6 text-start shadow-none">
                <CardHeader className="p-0 pb-8 text-center">
                    <CardTitle className="text-xl font-bold text-content-neutral-primary">
                        Create a new password
                    </CardTitle>
                </CardHeader>

                <CardContent className="flex flex-col gap-6 p-0">
                    {key && !resetPasswordSuccess ? (
                        <NewPasswordForm
                            onSubmit={(newPassword) => resetPasswordFinish(key, newPassword)}
                            submitLabel="Reset password"
                        />
                    ) : null}
                </CardContent>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetFinish;
