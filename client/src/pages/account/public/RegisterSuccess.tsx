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
    const {activate, activationFailure, loading} = useActivateStore(
        useShallow((state) => ({
            activate: state.activate,
            activationFailure: state.activationFailure,
            loading: state.loading,
        }))
    );

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const key = searchParams.get('key');

    useEffect(() => {
        if (key) {
            activate(key);
        }

        if (key && activationFailure) {
            navigate('/account-error', {state: {fromInternalFlow: true}});
        }
    }, [activate, activationFailure, key, navigate]);

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-center shadow-none">
                <CircleCheckBig className="mx-auto size-12 text-success" />

                <CardHeader className="p-0">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                        Account created successfully
                    </CardTitle>

                    <CardDescription className="self-center text-center text-content-neutral-secondary">
                        You&apos;re ready to start using ByteChef.
                    </CardDescription>
                </CardHeader>

                <Link to="/login">
                    <Button disabled={loading} icon={loading ? <LoadingIcon /> : undefined} label="Start" size="lg" />
                </Link>
            </Card>
        </PublicLayoutContainer>
    );
};

export default RegisterSuccess;
