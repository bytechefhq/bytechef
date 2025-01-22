import LoadingIcon from '@/components/LoadingIcon';
import {Button} from '@/components/ui/button';
import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {CircleCheckBig} from 'lucide-react';
import {useEffect} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router-dom';

import {useActivateStore} from './stores/useActivateStore';

const RegisterSuccess = () => {
    const {activate, activationFailure, loading} = useActivateStore();

    const navigate = useNavigate();

    const [searchParams] = useSearchParams();

    const key = searchParams.get('key');

    useEffect(() => {
        if (key) {
            activate(key);
        }

        if (key && activationFailure) {
            navigate('/account-error');
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
                    <Button
                        className="w-fit space-x-2 bg-surface-brand-primary py-5 hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed"
                        disabled={loading}
                    >
                        {loading && <LoadingIcon />} Start
                    </Button>
                </Link>
            </Card>
        </PublicLayoutContainer>
    );
};

export default RegisterSuccess;
