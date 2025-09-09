/* eslint-disable sort-keys */
/* eslint-disable @typescript-eslint/naming-convention */

import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {AlertCircle, Globe, Grid3X3, MoreHorizontal, Plus, Search, Settings, Trash2, X} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';

import {ToolConfigDialog} from './tool-config-dialog';

import type React from 'react';

interface InsertToolsPopupProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    anchorRef: React.RefObject<HTMLElement>;
}

interface Tool {
    id: string;
    name: string;
    action: string;
    icon: React.ReactNode;
    needsSetup?: boolean;
    category?: string;
}

export function InsertToolsPopup({anchorRef, onOpenChange, open}: InsertToolsPopupProps) {
    const popupRef = useRef<HTMLDivElement>(null);
    const [position, setPosition] = useState({top: 0, left: 0});
    const [configDialogOpen, setConfigDialogOpen] = useState(false);
    const [selectedTool, setSelectedTool] = useState<Tool | null>(null);

    const tools: Tool[] = [
        {
            id: 'visit-website',
            name: 'Visit website',
            action: 'Visit website',
            icon: <Globe className="size-4 text-blue-500" />,
            category: 'general',
        },
        {
            id: 'google-search',
            name: 'Google search',
            action: 'Google search',
            icon: <Search className="size-4 text-blue-500" />,
            category: 'general',
        },
        {
            id: 'gmail-label',
            name: 'Gmail: Add Label to Email',
            action: 'Gmail',
            icon: (
                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500">
                    <span className="text-xs font-bold text-white">M</span>
                </div>
            ),
            category: 'email',
        },
        {
            id: 'google-sheets',
            name: 'Google Sheets: Create Spreadsheet Row',
            action: 'Google Sheets',
            icon: (
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500">
                    <span className="text-xs text-white">📊</span>
                </div>
            ),
            needsSetup: true,
            category: 'productivity',
        },
        {
            id: 'fireflies-meeting',
            name: 'Fireflies.ai: Find Meeting by Call Details',
            action: 'Fireflies.ai',
            icon: (
                <div className="flex size-4 items-center justify-center rounded-sm bg-pink-500">
                    <span className="text-xs font-bold text-white">F</span>
                </div>
            ),
            needsSetup: true,
            category: 'meetings',
        },
        {
            id: 'relatable-contact',
            name: 'Relatable: Create or Update a Contact',
            action: 'Relatable',
            icon: (
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-500">
                    <span className="text-xs font-bold text-white">R</span>
                </div>
            ),
            needsSetup: true,
            category: 'crm',
        },
        {
            id: 'slack-message',
            name: 'Slack: Send Channel Message',
            action: 'Send Channel Message',
            icon: (
                <div className="flex size-4 items-center justify-center rounded-sm bg-purple-600">
                    <span className="text-xs font-bold text-white">#</span>
                </div>
            ),
            needsSetup: true,
            category: 'communication',
        },
    ];

    // Calculate popup position
    useEffect(() => {
        if (open && anchorRef.current && popupRef.current) {
            const anchorRect = anchorRef.current.getBoundingClientRect();
            const popupRect = popupRef.current.getBoundingClientRect();

            let top = anchorRect.bottom + 8;
            let left = anchorRect.right - 400; // Align to right edge of anchor

            // Ensure popup doesn't go off screen
            if (left < 8) left = 8;
            if (left + 400 > window.innerWidth - 8) {
                left = window.innerWidth - 408;
            }

            if (top + popupRect.height > window.innerHeight - 8) {
                top = anchorRect.top - popupRect.height - 8;
            }

            setPosition({top, left});
        }
    }, [open, anchorRef]);

    // Handle click outside
    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (
                popupRef.current &&
                !popupRef.current.contains(event.target as Node) &&
                anchorRef.current &&
                !anchorRef.current.contains(event.target as Node)
            ) {
                onOpenChange(false);
            }
        }

        if (open) {
            document.addEventListener('mousedown', handleClickOutside);
            return () => document.removeEventListener('mousedown', handleClickOutside);
        }
    }, [open, onOpenChange, anchorRef]);

    // Handle escape key
    useEffect(() => {
        function handleEscape(event: KeyboardEvent) {
            if (event.key === 'Escape') {
                onOpenChange(false);
            }
        }

        if (open) {
            document.addEventListener('keydown', handleEscape);
            return () => document.removeEventListener('keydown', handleEscape);
        }
    }, [open, onOpenChange]);

    const handleToolClick = (tool: Tool) => {
        console.log(`Selected tool: ${tool.name} (${tool.action})`);
        // Here you would implement the logic to insert the tool into the instructions
        onOpenChange(false);
    };

    const handleFinishSetup = (tool: Tool, event: React.MouseEvent) => {
        event.stopPropagation();
        console.log(`Finish setup for: ${tool.name}`);
        // Here you would implement the setup logic for the tool
    };

    const handleConfigureTool = (tool: Tool, event: React.MouseEvent) => {
        event.stopPropagation();
        setSelectedTool(tool);
        setConfigDialogOpen(true);
        // Close the tools popup when opening config dialog
        onOpenChange(false);
    };

    const handleRemoveFromAgent = (tool: Tool, event: React.MouseEvent) => {
        event.stopPropagation();
        console.log(`Remove from agent: ${tool.name}`);
        // Here you would implement the removal logic for the tool
    };

    if (!open) return null;

    return (
        <>
            {/* Backdrop */}
            <div className="fixed inset-0 z-40" />

            {/* Popup */}
            <div
                className="fixed z-50 w-96 rounded-lg border border-gray-200 bg-white shadow-lg"
                ref={popupRef}
                style={{
                    top: position.top,
                    left: position.left,
                }}
            >
                {/* Header */}

                <div className="flex items-center justify-between border-b border-gray-200 p-4">
                    <div className="flex items-center gap-2">
                        <Grid3X3 className="size-5 text-blue-600" />

                        <h3 className="text-lg font-medium">Tools</h3>
                    </div>

                    <div className="flex items-center gap-2">
                        <Button
                            className="h-auto p-0 font-medium text-blue-600 hover:text-blue-700"
                            onClick={() => console.log('Add tool clicked')}
                            size="sm"
                            variant="ghost"
                        >
                            <Plus className="mr-1 size-4" />
                            Add tool
                        </Button>

                        <Button className="size-6" onClick={() => onOpenChange(false)} size="icon" variant="ghost">
                            <X className="size-4" />
                        </Button>
                    </div>
                </div>

                {/* Tools List */}

                <div className="max-h-96 overflow-y-auto">
                    <div className="space-y-1 p-2">
                        {tools.map((tool) => (
                            <div
                                className={`flex cursor-pointer items-center gap-3 rounded-lg p-3 transition-colors ${
                                    tool.needsSetup
                                        ? 'border border-red-200 bg-red-50 hover:bg-red-100'
                                        : 'bg-gray-50 hover:bg-gray-100'
                                }`}
                                key={tool.id}
                                onClick={() => handleToolClick(tool)}
                            >
                                {/* Tool Icon */}

                                <div className="shrink-0">{tool.icon}</div>

                                {/* Tool Name */}

                                <div className="min-w-0 flex-1">
                                    <span className="block truncate text-sm font-medium text-gray-900">
                                        {tool.name}
                                    </span>
                                </div>

                                {/* Warning Icon for tools that need setup */}

                                {tool.needsSetup && <AlertCircle className="size-4 shrink-0 text-red-500" />}

                                {/* Finish Setup Button */}

                                {tool.needsSetup && (
                                    <Button
                                        className="h-7 bg-indigo-600 px-3 py-1 text-xs text-white hover:bg-indigo-700"
                                        onClick={(e) => handleFinishSetup(tool, e)}
                                        size="sm"
                                    >
                                        Finish setup
                                    </Button>
                                )}

                                {/* More Options Dropdown */}

                                <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                        <Button
                                            className="size-6 shrink-0"
                                            onClick={(e) => e.stopPropagation()}
                                            size="icon"
                                            variant="ghost"
                                        >
                                            <MoreHorizontal className="size-3" />
                                        </Button>
                                    </DropdownMenuTrigger>

                                    <DropdownMenuContent align="end" className="w-48">
                                        <DropdownMenuItem
                                            className="flex items-center gap-2"
                                            onClick={(e) => handleConfigureTool(tool, e)}
                                        >
                                            <Settings className="size-4" />
                                            Configure
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center gap-2 text-red-600 focus:text-red-600"
                                            onClick={(e) => handleRemoveFromAgent(tool, e)}
                                        >
                                            <Trash2 className="size-4" />
                                            Remove from agent
                                        </DropdownMenuItem>
                                    </DropdownMenuContent>
                                </DropdownMenu>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Configuration Dialog */}
            {selectedTool && (
                <ToolConfigDialog
                    onOpenChange={(open) => {
                        setConfigDialogOpen(open);
                        if (!open) {
                            setSelectedTool(null);
                        }
                    }}
                    open={configDialogOpen}
                    toolIcon={selectedTool.icon}
                    toolId={selectedTool.id}
                    toolName={selectedTool.name}
                />
            )}
        </>
    );
}
