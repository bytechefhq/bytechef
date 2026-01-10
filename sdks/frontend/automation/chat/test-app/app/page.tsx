import Link from 'next/link';

export default function Home() {
    return (
        <div className="flex min-h-screen flex-col items-center justify-center p-8">
            <div className="w-full max-w-2xl space-y-8">
                <div className="text-center">
                    <h1 className="text-4xl font-bold tracking-tight">ByteChef Chat SDK</h1>
                    <p className="mt-4 text-lg text-muted-foreground">Test Application</p>
                </div>

                <div className="grid gap-4 md:grid-cols-2">
                    <Link
                        href="/embedded"
                        className="flex flex-col gap-2 rounded-lg border border-border bg-card p-6 transition-colors hover:bg-accent"
                    >
                        <h2 className="text-2xl font-semibold">Embedded Chat</h2>
                        <p className="text-sm text-muted-foreground">
                            Full-page embedded chat widget integrated directly into your application layout
                        </p>
                    </Link>

                    <Link
                        href="/modal"
                        className="flex flex-col gap-2 rounded-lg border border-border bg-card p-6 transition-colors hover:bg-accent"
                    >
                        <h2 className="text-2xl font-semibold">Modal Chat</h2>
                        <p className="text-sm text-muted-foreground">
                            Floating chat modal that can be triggered from anywhere in your application
                        </p>
                    </Link>
                </div>

                <div className="rounded-lg border border-border bg-muted/50 p-6">
                    <h3 className="mb-4 text-lg font-semibold">Getting Started</h3>
                    <ol className="list-inside list-decimal space-y-2 text-sm">
                        <li>Update the webhookUrl in the demo pages</li>
                        <li>Test both embedded and modal implementations</li>
                        <li>Customize styling and behavior for your needs</li>
                    </ol>
                </div>

                <div className="text-center text-sm text-muted-foreground">
                    <a
                        href="https://github.com/bytechefhq/bytechef"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="underline underline-offset-4 hover:text-foreground"
                    >
                        Documentation & Source Code
                    </a>
                </div>
            </div>
        </div>
    );
}
