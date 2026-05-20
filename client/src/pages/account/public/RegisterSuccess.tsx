import Button from '@/components/Button/Button';
import LoadingIcon from '@/components/LoadingIcon';
import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {CircleCheckBig} from 'lucide-react';
import {useEffect} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';
import {useShallow} from 'zustand/react/shallow';

import {useActivateStore} from './stores/useActivateStore';

const RegisterSuccess = () => {
    const {activate, activationFailure, activationSuccess} = useActivateStore(
        useShallow((state) => ({
            activate: state.activate,
            activationFailure: state.activationFailure,
            activationSuccess: state.activationSuccess,
        }))
    );

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const key = searchParams.get('key');

    useEffect(() => {
        if (key && !activationSuccess) {
            activate(key);
        }
    }, [key, activate, activationSuccess]);

    useEffect(() => {
        if (activationFailure) {
            navigate('/account-error', {state: {fromInternalFlow: true}});
        }
    }, [activationFailure, navigate]);

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-center shadow-none">
                {activationSuccess ? (
                    <CircleCheckBig className="mx-auto size-12 text-success" />
                ) : (
                    <LoadingIcon className="mx-auto size-12 text-content-neutral-secondary" />
                )}

                <CardHeader className="p-0">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                        {activationSuccess ? 'Account created successfully' : 'Account is preparing...'}
                    </CardTitle>

                    <CardDescription className="self-center text-center text-content-neutral-secondary">
                        {activationSuccess
                            ? "You're ready to start using ByteChef."
                            : 'Please wait until the account is ready'}
                    </CardDescription>
                </CardHeader>

                {activationSuccess ? (
                    <Link to="/login">
                        <Button label="Start" size="lg" />
                    </Link>
                ) : (
                    <div>
                        <Button disabled label="Start" size="lg" />
                    </div>
                )}
            </Card>
        </PublicLayoutContainer>
    );
};

export default RegisterSuccess;
