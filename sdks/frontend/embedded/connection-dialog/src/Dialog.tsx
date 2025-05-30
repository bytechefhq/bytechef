import React, {useState} from 'react';
import Logo from './assets/logo.svg';

interface DialogProps {
    handleConnect: () => void;
    isOpen: boolean;
    onClose: () => void;
}

export default function Dialog({handleConnect, isOpen, onClose}: DialogProps) {
    const [connectionName, setConnectionName] = useState('');
    const [connectionType, setConnectionType] = useState('api');

    if (!isOpen) {
        return null;
    }

    return (
        <div className="fixed inset-0 z-50 bg-black/50 data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0">
            <div className="fixed left-[50%] top-[50%] z-50 grid w-full max-w-lg translate-x-[-50%] translate-y-[-50%] gap-4 border bg-background p-6 shadow-lg duration-200 sm:rounded-lg">
                <div className="flex items-center space-x-2 border-b border-gray-200 pb-2">
                    <img src={Logo} alt="ByteChef Logo" className="h-8 w-8" />

                    <h2 className="text-xl font-semibold">ByteChef</h2>
                </div>

                <div className="flex flex-col space-y-1.5 text-center sm:text-left">
                    <h2 className="mb-4 text-xl font-semibold">Create Connection</h2>

                    <div className="space-y-4">
                        <div className="space-y-2">
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
                        </div>

                        <div className="space-y-2">
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
                        </div>

                        <button
                            onClick={handleConnect}
                            className="mt-4 inline-flex h-10 w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground ring-offset-background transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
                        >
                            Connect
                        </button>
                    </div>
                </div>

                <div className="flex flex-col-reverse sm:flex-row sm:justify-end sm:space-x-2">
                    <button
                        onClick={onClose}
                        className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground ring-offset-background transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}
