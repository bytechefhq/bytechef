import PublicLayoutContainer from '@/shared/layout/PublicLayoutContainer';

const VerifyEmail = () => {
    return (
        <PublicLayoutContainer>
            <div className="space-y-4 text-center">
                <h1 className="text-lg font-semibold">Welcome to ByteChef!</h1>

                <div className="text-3xl font-semibold">Let&apos;s verify your email address.</div>

                <p>An email has been sent with a link to verify your email address.</p>
            </div>
        </PublicLayoutContainer>
    );
};

export default VerifyEmail;
