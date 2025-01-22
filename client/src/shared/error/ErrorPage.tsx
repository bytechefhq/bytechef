export default function ErrorPage() {
    return (
        <div className="flex size-full flex-col items-center justify-center px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-md text-center">
                <div className="mx-auto size-12 text-primary" />

                <h1 className="mt-4 text-3xl font-bold tracking-tight text-foreground sm:text-4xl">
                    Oops, something went wrong!
                </h1>

                <p className="mt-4 text-muted-foreground">
                    Wea are sorry, but an unexpected error has occurred. Please try again later or contact support if
                    the issue persists.
                </p>

                <div className="mt-6">
                    <a
                        className="inline-flex items-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                        href="/"
                    >
                        Go to Homepage
                    </a>
                </div>
            </div>
        </div>
    );
}
