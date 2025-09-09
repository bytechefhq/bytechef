/* eslint-disable sort-keys */
/* eslint-disable tailwindcss/no-unnecessary-arbitrary-value */

import {Button} from '@/components/ui/button';
import {Dialog, DialogContent, DialogHeader} from '@/components/ui/dialog';
import {Settings, X} from 'lucide-react';
import {useState} from 'react';

interface FirefliesTriggerModalProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

export function FirefliesTriggerModal({onOpenChange, open}: FirefliesTriggerModalProps) {
    const [isConnected, setIsConnected] = useState(false);

    const handleConnect = () => {
        // Simulate connection process
        setIsConnected(true);
    };

    const handleSave = () => {
        onOpenChange(false);
    };

    const handleCancel = () => {
        onOpenChange(false);
    };

    const handleReplaceTrigger = () => {
        // Handle replace trigger functionality
        console.log('Replace trigger clicked');
    };

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="gap-0 p-0 sm:max-w-[600px]">
                {/* Header */}

                <DialogHeader className="border-b border-gray-200 p-6 pb-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="flex size-6 items-center justify-center rounded-sm bg-pink-500">
                                <span className="text-sm font-bold text-white">F</span>
                            </div>

                            <h2 className="text-lg font-medium">Fireflies.ai: New Meeting</h2>
                        </div>

                        <Button onClick={() => onOpenChange(false)} size="icon" variant="ghost">
                            <X className="size-4" />
                        </Button>
                    </div>
                </DialogHeader>

                {/* Content */}

                <div className="space-y-6 p-6">
                    {/* Account Section */}

                    <div>
                        <div className="mb-4 flex items-center justify-between">
                            <div className="flex items-center gap-2">
                                <h3 className="text-base font-medium text-gray-700">Fireflies.ai account</h3>

                                <span className="text-sm text-red-500">*</span>
                            </div>

                            <Button className="p-1" size="sm" variant="ghost">
                                <Settings className="size-4 text-gray-400" />
                            </Button>
                        </div>

                        {/* Connection Area */}

                        <div className="rounded-lg bg-gray-50 p-4">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-3">
                                    <div className="flex size-8 items-center justify-center rounded-sm bg-pink-500">
                                        <span className="text-sm font-bold text-white">F</span>
                                    </div>

                                    <div>
                                        <div className="text-sm font-medium">Fireflies.ai</div>

                                        {isConnected && (
                                            <div className="text-xs text-green-600">Connected successfully</div>
                                        )}
                                    </div>
                                </div>

                                <Button
                                    className="bg-indigo-600 px-6 py-2 text-white hover:bg-indigo-700"
                                    disabled={isConnected}
                                    onClick={handleConnect}
                                >
                                    {isConnected ? 'Connected' : 'Connect'}
                                </Button>
                            </div>
                        </div>

                        {!isConnected && (
                            <p className="mt-3 text-sm text-gray-500">
                                Connect your Fireflies.ai account to enable meeting triggers and access your meeting
                                data.
                            </p>
                        )}
                    </div>

                    {/* Trigger Configuration (shown when connected) */}

                    {isConnected && (
                        <div className="space-y-4">
                            <h3 className="text-base font-medium text-gray-700">Trigger Configuration</h3>

                            <div className="rounded-lg bg-gray-50 p-4">
                                <div className="space-y-3">
                                    <div>
                                        <label className="mb-1 block text-sm font-medium text-gray-600">
                                            Meeting Type
                                        </label>

                                        <select className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-indigo-500">
                                            <option>All meetings</option>

                                            <option>Scheduled meetings only</option>

                                            <option>Ad-hoc meetings only</option>
                                        </select>
                                    </div>

                                    <div>
                                        <label className="mb-1 block text-sm font-medium text-gray-600">
                                            Minimum Duration (minutes)
                                        </label>

                                        <input
                                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                            placeholder="5"
                                            type="number"
                                        />
                                    </div>

                                    <div>
                                        <label className="mb-1 block text-sm font-medium text-gray-600">
                                            Workspace
                                        </label>

                                        <select className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-indigo-500">
                                            <option>Default workspace</option>

                                            <option>Sales team workspace</option>

                                            <option>Marketing workspace</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer */}

                <div className="flex items-center justify-between border-t border-gray-200 bg-gray-50 p-6">
                    <Button
                        className="text-gray-600 hover:text-gray-800"
                        onClick={handleReplaceTrigger}
                        variant="ghost"
                    >
                        Replace trigger
                    </Button>

                    <div className="flex items-center gap-3">
                        <Button onClick={handleCancel} variant="outline">
                            Cancel
                        </Button>

                        <Button
                            className="bg-indigo-600 text-white hover:bg-indigo-700"
                            disabled={!isConnected}
                            onClick={handleSave}
                        >
                            Save
                        </Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
