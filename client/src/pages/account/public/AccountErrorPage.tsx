import {Button} from '@/components/ui/button';
import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {XCircleIcon} from 'lucide-react';
import {Link, useLocation} from 'react-router-dom';

const AccountErrorPage = () => {
    const location = useLocation();

    return (
        <PublicLayoutContainer>
            <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-center shadow-none">
                <XCircleIcon className="mx-auto size-12 text-destructive" />

                <CardHeader className="p-0">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">Error</CardTitle>

                    <CardDescription className="self-center text-center text-content-neutral-secondary">
                        {location.state.error ? `${location.state.error}` : 'Something went wrong. Try again.'}
                    </CardDescription>
                </CardHeader>

                <Link to="/">
                    <Button className="h-10 w-fit space-x-2 bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed">
                        Go back
                    </Button>
                </Link>
            </Card>
        </PublicLayoutContainer>
    );
};

export default AccountErrorPage;
