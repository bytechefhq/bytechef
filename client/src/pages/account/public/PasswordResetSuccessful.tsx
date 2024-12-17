import {Button} from '@/components/ui/button';
import {Card, CardDescription, CardHeader, CardTitle} from '@/components/ui/card';
import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';
import {CircleCheckBig} from 'lucide-react';
import {Link} from 'react-router-dom';

const PasswordResetSuccessful = () => {
    return (
        <PublicLayoutContainer>
            <Card className="mx-auto flex max-w-sm flex-col gap-6 rounded-xl p-6 text-center shadow-none">
                <CircleCheckBig className="mx-auto size-12 text-success" />

                <CardHeader className="p-0">
                    <CardTitle className="self-center text-xl font-bold text-content-neutral-primary">
                        Password reset
                    </CardTitle>

                    <CardDescription className="self-center text-center text-content-neutral-secondary">
                        Your password has been successfully reset. <br /> Click below to log in
                    </CardDescription>
                </CardHeader>

                <Link to="/login">
                    <Button className="h-10 w-fit space-x-2 bg-surface-brand-primary hover:bg-surface-brand-primary-hover active:bg-surface-brand-primary-pressed">
                        Continue to Log in
                    </Button>
                </Link>
            </Card>
        </PublicLayoutContainer>
    );
};

export default PasswordResetSuccessful;
