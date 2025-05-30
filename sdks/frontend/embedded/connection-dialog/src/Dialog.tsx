import {useState} from 'react';
import Logo from './assets/logo.svg';
import {XIcon} from 'lucide-react';

interface DialogProps {
    closeDialog: () => void;
    handleConnect: () => void;
    isOpen: boolean;
}

export default function Dialog({closeDialog, handleConnect, isOpen}: DialogProps) {
    const [connectionName, setConnectionName] = useState('');
    const [connectionType, setConnectionType] = useState('api');

    if (!isOpen) {
        return null;
    }

    return (
        <div className="fixed inset-0 z-50 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0">
            <div className="fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 sm:rounded-lg">
                <header className="flex items-center justify-between">
                    <h2 className="text-lg font-semibold">Create Connection</h2>

                    <button className="rounded-md p-2 transition-all hover:bg-slate-200" onClick={closeDialog}>
                        <XIcon className="ml-auto size-4" />

                        <span className="sr-only">Close</span>
                    </button>
                </header>

                <div className="flex flex-col space-y-1.5 text-center sm:text-left">
                    <div className="space-y-4">
                        <fieldset className="space-y-2">
                            <label htmlFor="connection-name" className="text-sm font-medium">
                                Connection Name
                            </label>

                            <input
                                id="connection-name"
                                type="text"
                                value={connectionName}
                                onChange={(event) => setConnectionName(event.target.value)}
                                className="flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                                placeholder="My Connection"
                            />
                        </fieldset>

                        <fieldset className="space-y-2">
                            <label htmlFor="connection-type" className="text-sm font-medium">
                                Connection Type
                            </label>

                            <select
                                id="connection-type"
                                value={connectionType}
                                onChange={(event) => setConnectionType(event.target.value)}
                                className="flex h-10 w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <option value="api">API Connection</option>
                                <option value="database">Database</option>
                                <option value="oauth">OAuth</option>
                            </select>
                        </fieldset>

                        <div className="flex items-center justify-end gap-2">
                            <button
                                className="inline-flex h-10 items-center justify-center rounded-md border border-input bg-background px-4 py-2 text-sm font-medium ring-offset-background transition-colors hover:bg-accent hover:text-accent-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
                                onClick={closeDialog}
                            >
                                Cancel
                            </button>

                            <button
                                onClick={handleConnect}
                                className="inline-flex h-10 items-center justify-center rounded-md bg-[#0951c4] px-4 py-2 text-sm font-medium text-primary-foreground ring-offset-background transition-colors hover:bg-[#0951c4]/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
                            >
                                Connect
                            </button>
                        </div>
                    </div>
                </div>

                <footer className="flex items-center justify-center">
                    <img src={Logo} alt="ByteChef Logo" className="mr-2 size-6" />

                    <span className="text-xs text-muted-foreground">
                        Powered by

                        <a
                            className="text-blue-600 pl-1 hover:underline"
                            href="https://bytechef.io"
                            target="_blank"
                            rel="noopener noreferrer"
                        >
                            ByteChef
                        </a>
                    </span>
                </footer>
            </div>
        </div>
    );
}
