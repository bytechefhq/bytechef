/* eslint-disable sort-keys */
/* eslint-disable react/no-unescaped-entities */
/* eslint-disable  @typescript-eslint/no-unused-vars */

import {Button} from '@/components/ui/button';
import {
    Activity,
    AlertCircle,
    ArrowLeft,
    BarChart3,
    ChevronDown,
    ChevronUp,
    Chrome,
    FileText,
    Grid3X3,
    HelpCircle,
    MoreHorizontal,
    Play,
    Plus,
    Settings,
    Sparkles,
    Zap,
} from 'lucide-react';
import {useRef, useState} from 'react';

import {FirefliesTriggerModal} from './components/fireflies-trigger-modal';
import {InsertToolsPopup} from './components/insert-tools-popup';
import {ToolConfigDialog} from './components/tool-config-dialog';

import type React from 'react';

interface AgentEditPageProps {
    agentId?: string;
    onBack?: () => void;
}

export default function AiAgentEditor({agentId, onBack}: AgentEditPageProps) {
    const [instructions, setInstructions] = useState(`Trigger
[Meeting Platform]: New Meeting

Process Overview
After a call, check if the other party requested a proposal. If yes, follow these steps:
1. CRM Update
Add contact to 'Prospective Clients' sphere
Add contact to 'Needs Proposal' sphere
Update relevant contact information
2. Team Notification
Post notification to [team channel]:
Include requested services
Format: "FYI - proposal requested for [service types]"
Add "(posted by [automation platform])" at end
3. Lead Tracking
Add new row to Leads Tracking Document with:
Contact information
Requested services
Meeting date
Other relevant details

Required Actions
Slack: Send Private Channel Message
[CRM]: Create or Update Contact
[Meeting Platform]: Find Meeting by Call Details
Google Sheets: Create Spreadsheet Row`);

    const integrations = [
        {
            name: 'Fireflies.ai: New Meeting',
            icon: 'F',
            color: 'bg-pink-500',
        },
        {
            name: 'Google Sheets: Create Spreadsheet',
            icon: '📊',
            color: 'bg-green-500',
        },
        {
            name: 'Fireflies.ai: Find Meeting by Call Details',
            icon: 'F',
            color: 'bg-pink-500',
        },
        {
            name: 'Relatable: Create or Update a Contact',
            icon: 'R',
            color: 'bg-blue-500',
        },
        {
            name: 'Slack: Send Channel Message',
            icon: '#',
            color: 'bg-purple-600',
        },
    ];

    const bottomIntegrations = [
        {
            name: 'Slack: Send Channel Message',
            icon: '#',
            color: 'bg-purple-600',
        },
        {
            name: 'Relatable: Create or Update a Contact',
            icon: 'R',
            color: 'bg-blue-500',
        },
        {
            name: 'Fireflies.ai: Find Meeting by Call Details',
            icon: 'F',
            color: 'bg-pink-500',
        },
    ];

    const [isTestingAgent, setIsTestingAgent] = useState(false);
    const [isTriggerExpanded, setIsTriggerExpanded] = useState(false);
    const [isActionCompleteExpanded, setIsActionCompleteExpanded] = useState(true);
    const [firefliesModalOpen, setFirefliesModalOpen] = useState(false);
    const [insertToolsPopupOpen, setInsertToolsPopupOpen] = useState(false);
    const [toolConfigDialogOpen, setToolConfigDialogOpen] = useState(false);
    const [selectedConfigTool, setSelectedConfigTool] = useState<{
        id: string;
        name: string;
        icon: React.ReactNode;
    } | null>(null);

    const insertToolsButtonRef = useRef<HTMLButtonElement>(null);

    const handleConfigureIntegration = (toolId: string, toolName: string, toolIcon: React.ReactNode) => {
        setSelectedConfigTool({
            id: toolId,
            name: toolName,
            icon: toolIcon,
        });
        setToolConfigDialogOpen(true);
    };

    return (
        <div className="flex h-screen bg-gray-50">
            {/* Main Content */}

            <div className="flex flex-1 flex-col bg-white">
                {/* Header */}

                <div className="border-b border-gray-200 px-6 py-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <Button onClick={onBack} variant="outline">
                                <ArrowLeft className="size-4" />
                            </Button>

                            <div className="flex items-center gap-2">
                                <div className="flex size-6 items-center justify-center rounded bg-pink-500">
                                    <FileText className="size-3 text-white" />
                                </div>

                                <h1 className="text-lg font-medium">Call Follow-Up: Add Proposal Re...</h1>
                            </div>
                        </div>

                        <div className="flex items-center gap-4">
                            <div className="flex items-center">
                                <Button
                                    className="rounded-none border-b-2 border-indigo-600 text-sm font-medium"
                                    variant="ghost"
                                >
                                    <Settings className="mr-2 size-4" />
                                    Configure
                                </Button>

                                <Button className="rounded-none text-sm font-medium text-gray-500" variant="ghost">
                                    <Activity className="mr-2 size-4" />
                                    Activity
                                </Button>
                            </div>

                            <div className="flex items-center gap-1">
                                <div className="flex size-6 items-center justify-center rounded-sm bg-pink-500">
                                    <span className="text-xs font-bold text-white">F</span>
                                </div>

                                <div className="flex size-6 items-center justify-center rounded-sm bg-blue-500">
                                    <span className="text-xs font-bold text-white">D</span>
                                </div>

                                <div className="flex size-6 items-center justify-center rounded-sm bg-blue-500">
                                    <span className="text-xs font-bold text-white">R</span>
                                </div>

                                <Button size="sm" variant="outline">
                                    <MoreHorizontal className="size-4" />
                                </Button>

                                <div className="size-6 rounded-full bg-gray-300"></div>

                                <Button size="sm" variant="outline">
                                    <HelpCircle className="size-4" />
                                </Button>

                                <Button size="sm" variant="outline">
                                    <MoreHorizontal className="size-4" />
                                </Button>
                            </div>

                            <Button className="bg-indigo-600 hover:bg-indigo-700">Share</Button>
                        </div>
                    </div>
                </div>

                {/* Content */}

                <div className="flex-1 bg-gray-50 p-6">
                    <div className="grid h-full grid-cols-3 gap-6">
                        {/* Left Column - Instructions */}

                        <div className="col-span-2 space-y-6">
                            {/* Trigger Section */}

                            <div className="rounded-lg bg-gray-50 p-4">
                                <div className="flex items-center justify-between">
                                    <h2 className="text-lg font-medium">Trigger</h2>

                                    <div
                                        className="flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-sm transition-shadow hover:shadow-md"
                                        onClick={() => setFirefliesModalOpen(true)}
                                    >
                                        <AlertCircle className="size-4 shrink-0 text-red-500" />

                                        <div className="flex size-4 items-center justify-center rounded-sm bg-pink-500">
                                            <span className="text-xs font-bold text-white">F</span>
                                        </div>

                                        <span className="text-sm font-medium">Fireflies.ai: New Meeting</span>

                                        <Button size="sm" variant="outline">
                                            <Settings className="size-4 text-gray-400" />
                                        </Button>
                                    </div>
                                </div>
                            </div>

                            {/* Instructions Section */}

                            <div className="rounded-lg border border-gray-200 bg-white">
                                <div className="flex items-center justify-between border-b border-gray-200 p-4">
                                    <h2 className="text-lg font-medium">Instructions:</h2>

                                    <div className="flex items-center gap-2">
                                        <button
                                            className="flex items-center gap-2 rounded-lg border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
                                            onClick={() => setInsertToolsPopupOpen(!insertToolsPopupOpen)}
                                            ref={insertToolsButtonRef}
                                        >
                                            <Grid3X3 className="size-4" />

                                            <span>Insert tools</span>

                                            <div className="flex size-5 items-center justify-center rounded-sm bg-red-500">
                                                <span className="text-xs font-bold text-white">M</span>
                                            </div>

                                            <div className="flex size-5 items-center justify-center rounded-sm bg-green-500">
                                                <FileText className="size-3 text-white" />
                                            </div>

                                            <div className="flex size-5 items-center justify-center rounded-sm bg-pink-500">
                                                <span className="text-xs font-bold text-white">F</span>
                                            </div>

                                            <button
                                                className="flex size-5 items-center justify-center rounded-sm border border-gray-300 hover:bg-gray-100"
                                                onClick={() => {}}
                                            >
                                                <MoreHorizontal className="size-3" />
                                            </button>
                                        </button>
                                    </div>
                                </div>

                                <div className="p-6">
                                    <div className="space-y-4 font-mono text-sm leading-relaxed text-gray-900">
                                        <div>[Meeting Platform]: New Meeting</div>

                                        <div className="space-y-2">
                                            <div className="font-medium">Process Overview</div>

                                            <div>
                                                After a call, check if the other party requested a proposal. If yes,
                                                follow these steps:
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <div className="font-medium">1. CRM Update</div>

                                            <div className="ml-0 space-y-1">
                                                <div>Add contact to 'Prospective Clients' sphere</div>

                                                <div>Add contact to 'Needs Proposal' sphere</div>

                                                <div>Update relevant contact information</div>
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <div className="font-medium">2. Team Notification</div>

                                            <div className="ml-0 space-y-1">
                                                <div>Post notification to [team channel]:</div>

                                                <div>Include requested services</div>

                                                <div>Format: "FYI - proposal requested for [service types]"</div>

                                                <div>Add "(posted by [automation platform])" at end</div>
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <div className="font-medium">3. Lead Tracking</div>

                                            <div className="ml-0 space-y-1">
                                                <div>Add new row to Leads Tracking Document with:</div>

                                                <div>Contact information</div>

                                                <div>Requested services</div>

                                                <div>Meeting date</div>

                                                <div>Other relevant details</div>
                                            </div>
                                        </div>

                                        <div className="space-y-2 pt-4">
                                            <div className="font-medium">Required Actions</div>

                                            <div className="ml-0 space-y-1">
                                                <div>Slack: Send Private Channel Message</div>

                                                <div>[CRM]: Create or Update Contact</div>

                                                <div>[Meeting Platform]: Find Meeting by Call Details</div>

                                                <div>Google Sheets: Create Spreadsheet Row</div>
                                            </div>
                                        </div>
                                    </div>

                                    {/* Integration Items - moved from bottom */}

                                    <div className="mt-6 border-t border-gray-200 pt-6">
                                        <div className="space-y-2">
                                            <div className="flex items-center gap-2 rounded border border-red-200 bg-red-50 p-2">
                                                <AlertCircle className="size-3 shrink-0 text-red-500" />

                                                <div className="flex size-3 items-center justify-center rounded-sm bg-purple-600">
                                                    <span className="text-[8px] font-bold text-white">#</span>
                                                </div>

                                                <span className="flex-1 text-xs font-medium">
                                                    Slack: Send Channel Message
                                                </span>

                                                <Button
                                                    className="h-6"
                                                    onClick={() =>
                                                        handleConfigureIntegration(
                                                            'slack-message',
                                                            'Slack: Send Channel Message',
                                                            <div className="flex size-4 items-center justify-center rounded-sm bg-purple-600">
                                                                <span className="text-xs font-bold text-white">#</span>
                                                            </div>
                                                        )
                                                    }
                                                    size="sm"
                                                    variant="outline"
                                                >
                                                    <Settings className="size-3 text-gray-400" />
                                                </Button>
                                            </div>

                                            <div className="flex items-center gap-2 rounded border border-red-200 bg-red-50 p-2">
                                                <AlertCircle className="size-3 shrink-0 text-red-500" />

                                                <div className="flex size-3 items-center justify-center rounded-sm bg-blue-500">
                                                    <span className="text-[8px] font-bold text-white">R</span>
                                                </div>

                                                <span className="flex-1 text-xs font-medium">
                                                    Relatable: Create or Update a Contact
                                                </span>

                                                <Button
                                                    className="h-6"
                                                    onClick={() =>
                                                        handleConfigureIntegration(
                                                            'relatable-contact',
                                                            'Relatable: Create or Update a Contact',
                                                            <div className="flex size-4 items-center justify-center rounded-sm bg-blue-500">
                                                                <span className="text-xs font-bold text-white">R</span>
                                                            </div>
                                                        )
                                                    }
                                                    size="sm"
                                                    variant="outline"
                                                >
                                                    <Settings className="size-3 text-gray-400" />
                                                </Button>
                                            </div>

                                            <div className="flex items-center gap-2 rounded border border-red-200 bg-red-50 p-2">
                                                <AlertCircle className="size-3 shrink-0 text-red-500" />

                                                <div className="flex size-3 items-center justify-center rounded-sm bg-pink-500">
                                                    <span className="text-[8px] font-bold text-white">F</span>
                                                </div>

                                                <span className="flex-1 text-xs font-medium">
                                                    Fireflies.ai: Find Meeting by Call Details
                                                </span>

                                                <Button
                                                    className="h-6"
                                                    onClick={() =>
                                                        handleConfigureIntegration(
                                                            'fireflies-meeting',
                                                            'Fireflies.ai: Find Meeting by Call Details',
                                                            <div className="flex size-4 items-center justify-center rounded-sm bg-pink-500">
                                                                <span className="text-xs font-bold text-white">F</span>
                                                            </div>
                                                        )
                                                    }
                                                    size="sm"
                                                    variant="outline"
                                                >
                                                    <Settings className="size-3 text-gray-400" />
                                                </Button>
                                            </div>

                                            <div className="flex items-center gap-2 rounded border border-green-200 bg-green-50 p-2">
                                                <AlertCircle className="size-3 shrink-0 text-red-500" />

                                                <div className="flex size-3 items-center justify-center rounded-sm bg-green-500">
                                                    <FileText className="size-1.5 text-white" />
                                                </div>

                                                <span className="flex-1 text-xs font-medium">
                                                    Google Sheets: Create Spreadsheet Row
                                                </span>

                                                <Button
                                                    className="h-6"
                                                    onClick={() =>
                                                        handleConfigureIntegration(
                                                            'google-sheets',
                                                            'Google Sheets: Create Spreadsheet Row',
                                                            <div className="flex size-4 items-center justify-center rounded-sm bg-green-500">
                                                                <span className="text-xs text-white">📊</span>
                                                            </div>
                                                        )
                                                    }
                                                    size="sm"
                                                    variant="outline"
                                                >
                                                    <Settings className="size-3 text-gray-400" />
                                                </Button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* Bottom Integration Issues */}
                        </div>

                        {/* Right Column - Agent Preview */}

                        <div className="space-y-6">
                            {/* Single Agent Preview Container */}

                            <div className="rounded-lg border border-gray-200 bg-white p-6">
                                {!isTestingAgent ? (
                                    <>
                                        <h3 className="mb-6 text-lg font-medium">Agent preview</h3>

                                        {/* Large Preview Area */}
                                        <div className="mb-8 flex h-48 w-full items-center justify-center rounded-lg bg-gray-50">
                                            <div className="size-12 text-gray-300">
                                                <svg className="size-full" fill="currentColor" viewBox="0 0 24 24">
                                                    <path d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                                                </svg>
                                            </div>
                                        </div>

                                        {/* Test This Agent Section */}
                                        <div>
                                            <h4 className="mb-2 text-lg font-medium">Test this agent</h4>

                                            <p className="mb-6 text-sm text-gray-500">
                                                Test your agent and chat with your agent to make sure it's running
                                                properly.
                                            </p>

                                            {/* Integration List */}

                                            <div className="mb-6 space-y-3">
                                                <div className="flex items-center justify-between rounded-lg border border-red-200 bg-red-50 p-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex size-5 items-center justify-center rounded-sm bg-pink-500">
                                                            <span className="text-xs font-bold text-white">F</span>
                                                        </div>

                                                        <span className="text-sm font-medium">
                                                            Fireflies.ai: New Meeting
                                                        </span>
                                                    </div>

                                                    <Button
                                                        className="bg-indigo-600 text-white hover:bg-indigo-700"
                                                        size="sm"
                                                    >
                                                        Finish setup
                                                    </Button>
                                                </div>

                                                <div className="flex items-center justify-between rounded-lg border border-green-200 bg-green-50 p-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex size-5 items-center justify-center rounded-sm bg-green-500">
                                                            <span className="text-xs text-white">📊</span>
                                                        </div>

                                                        <span className="text-sm font-medium">
                                                            Google Sheets: Create Spreadsheet
                                                        </span>
                                                    </div>

                                                    <Button
                                                        className="bg-indigo-600 text-white hover:bg-indigo-700"
                                                        size="sm"
                                                    >
                                                        Finish setup
                                                    </Button>
                                                </div>

                                                <div className="flex items-center justify-between rounded-lg border border-red-200 bg-red-50 p-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex size-5 items-center justify-center rounded-sm bg-pink-500">
                                                            <span className="text-xs font-bold text-white">F</span>
                                                        </div>

                                                        <span className="text-sm font-medium">
                                                            Fireflies.ai: Find Meeting by C...
                                                        </span>
                                                    </div>

                                                    <Button
                                                        className="bg-indigo-600 text-white hover:bg-indigo-700"
                                                        size="sm"
                                                    >
                                                        Finish setup
                                                    </Button>
                                                </div>

                                                <div className="flex items-center justify-between rounded-lg border border-blue-200 bg-blue-50 p-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex size-5 items-center justify-center rounded-sm bg-blue-500">
                                                            <span className="text-xs font-bold text-white">R</span>
                                                        </div>

                                                        <span className="text-sm font-medium">
                                                            Relatable: Create or Update a ...
                                                        </span>
                                                    </div>

                                                    <Button
                                                        className="bg-indigo-600 text-white hover:bg-indigo-700"
                                                        size="sm"
                                                    >
                                                        Finish setup
                                                    </Button>
                                                </div>

                                                <div className="flex items-center justify-between rounded-lg border border-purple-200 bg-purple-50 p-3">
                                                    <div className="flex items-center gap-3">
                                                        <div className="flex size-5 items-center justify-center rounded-sm bg-purple-600">
                                                            <span className="text-xs font-bold text-white">#</span>
                                                        </div>

                                                        <span className="text-sm font-medium">
                                                            Slack: Send Channel Message
                                                        </span>
                                                    </div>

                                                    <Button
                                                        className="bg-indigo-600 text-white hover:bg-indigo-700"
                                                        size="sm"
                                                    >
                                                        Finish setup
                                                    </Button>
                                                </div>
                                            </div>

                                            {/* Test Agent Button */}

                                            <Button
                                                className="w-full justify-start gap-2"
                                                onClick={() => setIsTestingAgent(true)}
                                                variant="outline"
                                            >
                                                <Play className="size-4" />
                                                Test agent
                                            </Button>
                                        </div>
                                    </>
                                ) : (
                                    <>
                                        {/* Testing Interface */}
                                        <div className="mb-6 flex items-center justify-between">
                                            <h3 className="text-lg font-medium">Agent preview</h3>

                                            <Button
                                                className="bg-indigo-600 hover:bg-indigo-700"
                                                onClick={() => setIsTestingAgent(false)}
                                            >
                                                Retest agent
                                            </Button>
                                        </div>

                                        {/* Agent Info */}
                                        <div className="mb-4 flex items-center gap-3">
                                            <div className="flex size-8 items-center justify-center rounded bg-orange-500">
                                                <span className="text-sm font-bold text-white">G</span>
                                            </div>

                                            <div>
                                                <div className="text-sm font-medium">Gmail Event Creator</div>

                                                <div className="text-xs text-gray-500">Today at 5:09am</div>
                                            </div>
                                        </div>

                                        {/* Testing Status */}
                                        <div className="mb-6 flex items-center gap-2">
                                            <div className="flex size-5 items-center justify-center rounded-full bg-indigo-600">
                                                <svg
                                                    className="size-3 text-white"
                                                    fill="currentColor"
                                                    viewBox="0 0 20 20"
                                                >
                                                    <path
                                                        clipRule="evenodd"
                                                        d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                                        fillRule="evenodd"
                                                    />
                                                </svg>
                                            </div>

                                            <span className="text-sm font-medium">
                                                Testing behavior: Gmail Event Creator
                                            </span>
                                        </div>

                                        {/* Trigger Section */}
                                        <div className="rounded-lg bg-gray-50 p-4">
                                            <div className="flex items-center justify-between">
                                                <h2 className="text-lg font-medium">Trigger</h2>

                                                <div className="flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-3 py-2 shadow-sm">
                                                    <AlertCircle className="size-4 shrink-0 text-red-500" />

                                                    <div className="flex size-4 items-center justify-center rounded-sm bg-pink-500">
                                                        <span className="text-xs font-bold text-white">F</span>
                                                    </div>

                                                    <span className="text-sm font-medium">
                                                        Fireflies.ai: New Meeting
                                                    </span>

                                                    <Button size="sm" variant="outline">
                                                        <Settings className="size-4 text-gray-400" />
                                                    </Button>
                                                </div>
                                            </div>
                                        </div>

                                        {/* Agent Activity */}
                                        <div className="mb-6 space-y-4">
                                            <div className="flex items-center gap-3">
                                                <div className="flex size-8 items-center justify-center rounded bg-orange-500">
                                                    <span className="text-sm font-bold text-white">G</span>
                                                </div>

                                                <div>
                                                    <div className="text-sm font-medium">Gmail Event Creator</div>

                                                    <div className="text-xs text-gray-500">Today at 5:09am</div>
                                                </div>
                                            </div>

                                            <div className="text-sm leading-relaxed text-gray-700">
                                                Retrieving all emails from your inbox received in the past 24 hours to
                                                begin the review process.
                                            </div>
                                        </div>

                                        {/* Action Complete Section */}
                                        <div className="mb-6 rounded-lg bg-gray-50 p-4">
                                            <div
                                                className="mb-3 flex cursor-pointer items-center justify-between"
                                                onClick={() => setIsActionCompleteExpanded(!isActionCompleteExpanded)}
                                            >
                                                <div className="flex items-center gap-2">
                                                    <span className="text-sm font-medium text-gray-600">
                                                        Action Complete
                                                    </span>
                                                </div>

                                                <div className="flex items-center gap-2">
                                                    <div className="flex size-5 items-center justify-center rounded-full bg-indigo-600">
                                                        <svg
                                                            className="size-3 text-white"
                                                            fill="currentColor"
                                                            viewBox="0 0 20 20"
                                                        >
                                                            <path
                                                                clipRule="evenodd"
                                                                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                                                                fillRule="evenodd"
                                                            />
                                                        </svg>
                                                    </div>

                                                    <button
                                                        className="flex items-center rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
                                                        onClick={() =>
                                                            setIsActionCompleteExpanded(!isActionCompleteExpanded)
                                                        }
                                                    >
                                                        {isActionCompleteExpanded ? (
                                                            <ChevronUp className="size-4 text-gray-400" />
                                                        ) : (
                                                            <ChevronDown className="size-4 text-gray-400" />
                                                        )}
                                                    </button>
                                                </div>
                                            </div>

                                            <div
                                                className="flex cursor-pointer items-center gap-2"
                                                onClick={() => setIsActionCompleteExpanded(!isActionCompleteExpanded)}
                                            >
                                                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500">
                                                    <span className="text-xs font-bold text-white">G</span>
                                                </div>

                                                <span className="text-sm font-medium">Gmail: Find Email</span>
                                            </div>

                                            {/* Expanded Content */}

                                            {isActionCompleteExpanded && (
                                                <div className="mt-4 transition-all duration-200 ease-in-out">
                                                    {/* Resolved Fields */}

                                                    <div>
                                                        <div className="mb-2 flex items-center justify-between">
                                                            <span className="text-sm font-medium">
                                                                Resolved fields:
                                                            </span>

                                                            <button
                                                                className="flex items-center rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
                                                                onClick={() => {}}
                                                            >
                                                                <ChevronUp className="size-4 text-gray-400" />
                                                            </button>
                                                        </div>

                                                        <div className="grid grid-cols-2 gap-2 text-sm">
                                                            <div className="rounded border bg-white px-3 py-2 text-gray-600">
                                                                Search String
                                                            </div>

                                                            <div className="rounded border bg-white px-3 py-2">
                                                                in:inbox after:2025-06-16
                                                            </div>
                                                        </div>

                                                        {/* Final Output */}

                                                        <div className="mt-4">
                                                            <div className="mb-2 text-sm font-medium">
                                                                Final output:
                                                            </div>

                                                            <div className="mb-2 text-sm text-gray-600">
                                                                The output is longer than 50,000 characters. Rendering
                                                                this could slow down your browser.
                                                            </div>

                                                            <button
                                                                className="flex items-center rounded border border-gray-300 px-3 py-2 text-sm hover:bg-gray-100"
                                                                onClick={() => {}}
                                                            >
                                                                Click to show
                                                                <ChevronUp className="ml-1 size-3 rotate-180" />
                                                            </button>
                                                        </div>
                                                    </div>
                                                </div>
                                            )}
                                        </div>

                                        {/* Final Message */}
                                        <div className="mb-6 text-sm leading-relaxed text-gray-700">
                                            All emails from the past 24 hours have been retrieved. The next step is to
                                            review each email to see if any require scheduling an event. I will now
                                            assess each email for event-related information and proceed accordingly.
                                        </div>

                                        {/* Reply Input */}
                                        <div className="flex items-center gap-2">
                                            <input
                                                className="flex-1 rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-indigo-500"
                                                placeholder="Reply"
                                                type="text"
                                            />

                                            <Button className="bg-indigo-600 p-2 hover:bg-indigo-700" size="sm">
                                                <svg
                                                    className="size-4"
                                                    fill="none"
                                                    stroke="currentColor"
                                                    viewBox="0 0 24 24"
                                                >
                                                    <path
                                                        d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
                                                        strokeLinecap="round"
                                                        strokeLinejoin="round"
                                                        strokeWidth={2}
                                                    />
                                                </svg>
                                            </Button>
                                        </div>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Modals and Popups */}

            <FirefliesTriggerModal onOpenChange={setFirefliesModalOpen} open={firefliesModalOpen} />

            <InsertToolsPopup
                anchorRef={insertToolsButtonRef}
                onOpenChange={setInsertToolsPopupOpen}
                open={insertToolsPopupOpen}
            />

            {/* Tool Configuration Dialog */}

            {selectedConfigTool && (
                <ToolConfigDialog
                    onOpenChange={(open) => {
                        setToolConfigDialogOpen(open);
                        if (!open) {
                            setSelectedConfigTool(null);
                        }
                    }}
                    open={toolConfigDialogOpen}
                    toolIcon={selectedConfigTool.icon}
                    toolId={selectedConfigTool.id}
                    toolName={selectedConfigTool.name}
                />
            )}
        </div>
    );
}
