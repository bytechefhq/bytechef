/* eslint-disable sort-keys */
/* eslint-disable  @typescript-eslint/naming-convention */
/* eslint-disable  react/no-unescaped-entities */

import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Textarea} from '@/components/ui/textarea';
import {FileText, Mail, MessageSquare, Plus, Search, Settings, X} from 'lucide-react';
import {useState} from 'react';

interface NewAgentDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

interface TemplateData {
    id: string;
    name: string;
    description: string;
    trigger: string;
    triggerIcon: string;
    workflow: {
        title: string;
        steps: string[];
    }[];
    zapierIntegration?: boolean;
    requiredActions: {
        name: string;
        icon: string;
        color: string;
    }[];
}

export function NewAgentDialog({onOpenChange, open}: NewAgentDialogProps) {
    const [selectedOption, setSelectedOption] = useState<string | null>('custom');
    const [currentStep, setCurrentStep] = useState<'create' | 'connect'>('create');
    const [agentDescription, setAgentDescription] = useState('');

    const templateData: Record<string, TemplateData> = {
        'call-follow-up-proposal': {
            id: 'call-follow-up-proposal',
            name: 'Call Follow-Up: Add Proposal Requests to CRM and Send FYI on Slack',
            description:
                'Automatically update CRM, notify the team, and track leads when a proposal is requested after a meeting.',
            trigger: 'Fireflies.ai: New Meeting',
            triggerIcon: '🔥',
            zapierIntegration: true,
            workflow: [
                {
                    title: 'Trigger',
                    steps: ['[Meeting Platform]: New Meeting'],
                },
                {
                    title: 'Process Overview',
                    steps: ['After a call, check if the other party requested a proposal. If yes, follow these steps:'],
                },
                {
                    title: '1. CRM Update',
                    steps: [
                        "Add contact to 'Prospective Clients' sphere",
                        "Add contact to 'Needs Proposal' sphere",
                        'Update relevant contact information',
                    ],
                },
                {
                    title: '2. Team Notification',
                    steps: [
                        'Post notification to [team channel]:',
                        'Include requested services',
                        'Format: "FYI - proposal requested for [service types]"',
                        'Add "(posted by [automation platform])" at end',
                    ],
                },
                {
                    title: '3. Lead Tracking',
                    steps: [
                        'Add new row to Leads Tracking Document with:',
                        'Contact information',
                        'Requested services',
                        'Meeting date',
                        'Other relevant details',
                    ],
                },
            ],
            requiredActions: [
                {name: 'Slack: Send Channel Message', icon: '#', color: 'bg-purple-600'},
                {name: 'Relatable: Create or Update a Contact', icon: 'R', color: 'bg-blue-500'},
                {name: 'Fireflies.ai: Find Meeting by Call Details', icon: 'F', color: 'bg-pink-500'},
                {name: 'Google Sheets: Create Spreadsheet Row', icon: '📊', color: 'bg-green-500'},
            ],
        },
        'call-follow-up-email': {
            id: 'call-follow-up-email',
            name: 'Call Follow-Up Email Assistant',
            description:
                'Automatically send personalized follow-up emails after meetings with relevant attachments and next steps.',
            trigger: 'Calendar: Meeting Ended',
            triggerIcon: '📅',
            workflow: [
                {
                    title: 'Trigger',
                    steps: ['Meeting ends in calendar'],
                },
                {
                    title: 'Process',
                    steps: [
                        'Extract meeting notes and action items',
                        'Generate personalized follow-up email',
                        'Attach relevant documents',
                        'Send to meeting participants',
                    ],
                },
            ],
            requiredActions: [
                {name: 'Gmail: Send Email', icon: '✉️', color: 'bg-red-500'},
                {name: 'Calendar: Get Meeting Details', icon: '📅', color: 'bg-blue-500'},
            ],
        },
        'lead-gen-research-product': {
            id: 'lead-gen-research-product',
            name: 'Lead Gen Research: Product Pages',
            description:
                'Research and compile information about product pages and competitive analysis for lead generation.',
            trigger: 'Manual Trigger or Schedule',
            triggerIcon: '🔍',
            workflow: [
                {
                    title: 'Research Process',
                    steps: [
                        'Analyze target company websites',
                        'Extract product information',
                        'Compile competitive insights',
                        'Generate lead scoring data',
                    ],
                },
            ],
            requiredActions: [
                {name: 'Web Scraping: Extract Data', icon: '🌐', color: 'bg-gray-600'},
                {name: 'Google Sheets: Update Research', icon: '📊', color: 'bg-green-500'},
            ],
        },
        'daily-expense-summary': {
            id: 'daily-expense-summary',
            name: 'Daily Expense Summary Email',
            description: 'Automatically compile and send daily expense summaries to relevant stakeholders.',
            trigger: 'Daily Schedule at 6 PM',
            triggerIcon: '⏰',
            workflow: [
                {
                    title: 'Daily Process',
                    steps: [
                        'Collect expense data from various sources',
                        'Calculate totals and categorize expenses',
                        'Generate summary report',
                        'Email to finance team and managers',
                    ],
                },
            ],
            requiredActions: [
                {name: 'Expense App: Get Daily Data', icon: '💰', color: 'bg-orange-500'},
                {name: 'Gmail: Send Summary', icon: '✉️', color: 'bg-red-500'},
            ],
        },
        'support-email-agent': {
            id: 'support-email-agent',
            name: 'Support Email Agent',
            description: 'Automatically categorize, prioritize, and route support emails to appropriate team members.',
            trigger: 'New Email in Support Inbox',
            triggerIcon: '📧',
            workflow: [
                {
                    title: 'Email Processing',
                    steps: [
                        'Analyze email content and sentiment',
                        'Categorize by issue type and priority',
                        'Route to appropriate team member',
                        'Send acknowledgment to customer',
                    ],
                },
            ],
            requiredActions: [
                {name: 'Gmail: Monitor Inbox', icon: '✉️', color: 'bg-red-500'},
                {name: 'Slack: Notify Team', icon: '#', color: 'bg-purple-600'},
            ],
        },
    };

    const templates = [
        {
            id: 'custom',
            name: 'Create a custom agent',
            category: 'start',
            icons: [<Plus className="size-3 text-gray-600" key="plus" />],
        },
        {
            id: 'call-follow-up-proposal',
            name: 'Call Follow-Up: Add Proposal',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-pink-500" key="calendar">
                    <span className="text-[8px] font-bold text-white">F</span>
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500" key="plus">
                    <Plus className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-500" key="doc">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'call-follow-up-email',
            name: 'Call Follow-Up Email Assistant',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-pink-500" key="calendar">
                    <span className="text-[8px] font-bold text-white">F</span>
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500" key="mail">
                    <Mail className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'lead-gen-research-product',
            name: 'Lead Gen Research: Product Pages',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500" key="doc1">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-teal-500" key="doc2">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'lead-gen-research-companies',
            name: 'Lead Gen Research: Companies',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500" key="doc1">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-teal-500" key="doc2">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'daily-expense-summary',
            name: 'Daily Expense Summary Email',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-orange-500" key="doc">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500" key="mail">
                    <Mail className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'lead-gen-research-companies-2',
            name: 'Lead Gen Research: Companies',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500" key="doc1">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-teal-500" key="doc2">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'biz-dev-call-briefer',
            name: 'Biz Dev Call Briefer',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-400" key="doc1">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500" key="plus">
                    <Plus className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-500" key="doc2">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'support-email-agent',
            name: 'Support Email Agent',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500" key="mail">
                    <Mail className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'outreach-agent',
            name: 'Outreach Agent',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500" key="mail">
                    <Mail className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-orange-500" key="zapier">
                    <span className="text-[8px] font-bold text-white">Z</span>
                </div>,
            ],
        },
        {
            id: 'lead-enrichment-agent',
            name: 'Lead Enrichment Agent',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-orange-500" key="hubspot">
                    <span className="text-[8px] font-bold text-white">H</span>
                </div>,
            ],
        },
        {
            id: 'sales-prep-agent',
            name: 'Sales Prep Agent',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-orange-400" key="doc1">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-400" key="doc2">
                    <FileText className="size-2 text-white" />
                </div>,
                <div className="flex size-4 items-center justify-center rounded-sm bg-green-500" key="plus">
                    <Plus className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'customer-sentiment-analysis',
            name: 'Customer Sentiment Analysis',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-500" key="doc">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'seo-optimized-article-generator',
            name: 'SEO Optimized Article Generator',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-blue-500" key="doc">
                    <FileText className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'seo-analysis',
            name: 'SEO Analysis',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-full bg-gray-500" key="search">
                    <Search className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'inbox-categorizer',
            name: 'Inbox Categorizer',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-sm bg-red-500" key="mail">
                    <Mail className="size-2 text-white" />
                </div>,
            ],
        },
        {
            id: 'criteria-based-resume-screening',
            name: 'Criteria-Based Resume Screening',
            category: 'template',
            icons: [
                <div className="flex size-4 items-center justify-center rounded-full bg-gray-500" key="search">
                    <Search className="size-2 text-white" />
                </div>,
            ],
        },
    ];

    const handleCreateAgent = () => {
        setCurrentStep('connect');
    };

    const handleUseTemplate = () => {
        setCurrentStep('connect');
    };

    const handleSkipConnection = () => {
        onOpenChange(false);
        setCurrentStep('create');
    };

    const handleFinalCreate = () => {
        onOpenChange(false);
        setCurrentStep('create');
    };

    const handleClose = () => {
        onOpenChange(false);
        setCurrentStep('create');
    };

    const selectedTemplate = selectedOption && templateData[selectedOption] ? templateData[selectedOption] : null;

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="gap-0 p-0 sm:max-w-[900px]">
                <DialogHeader className="p-6 pb-2">
                    <div className="flex items-center justify-between">
                        <DialogTitle>New agent</DialogTitle>

                        <Button onClick={handleClose} size="icon" variant="ghost">
                            <X className="size-4" />
                        </Button>
                    </div>
                </DialogHeader>

                <div className="flex h-[601px]">
                    {/* Left sidebar with options */}

                    <div className="w-[350px] border-r border-gray-200 bg-gray-50">
                        <ScrollArea className="h-full">
                            <div className="p-4">
                                <div className="mb-4">
                                    <h3 className="mb-2 text-sm text-gray-500">Start from scratch</h3>

                                    {templates
                                        .filter((t) => t.category === 'start')
                                        .map((template) => (
                                            <div
                                                className={`flex cursor-pointer items-center justify-between rounded-md p-3 ${
                                                    selectedOption === template.id ? 'bg-gray-200' : 'hover:bg-gray-100'
                                                }`}
                                                key={template.id}
                                                onClick={() => setSelectedOption(template.id)}
                                            >
                                                <span className="text-sm">{template.name}</span>

                                                <div className="flex items-center gap-1">
                                                    {template.icons.map((icon, i) => (
                                                        <div key={i}>{icon}</div>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                </div>

                                <div>
                                    <h3 className="mb-2 text-sm text-gray-500">Use a template</h3>

                                    {templates
                                        .filter((t) => t.category === 'template')
                                        .map((template) => (
                                            <div
                                                className={`flex cursor-pointer items-center justify-between rounded-md p-3 ${
                                                    selectedOption === template.id ? 'bg-gray-200' : 'hover:bg-gray-100'
                                                }`}
                                                key={template.id}
                                                onClick={() => setSelectedOption(template.id)}
                                            >
                                                <span className="text-sm">{template.name}</span>

                                                <div className="flex items-center gap-1">
                                                    {template.icons.map((icon, i) => (
                                                        <div key={i}>{icon}</div>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                </div>
                            </div>
                        </ScrollArea>
                    </div>

                    {/* Right content area */}

                    <div className="flex flex-1 flex-col p-6">
                        {currentStep === 'create' ? (
                            selectedTemplate ? (
                                // Template Preview
                                <>
                                    <ScrollArea className="mb-4 flex-1">
                                        <div className="pr-4">
                                            <div className="mb-4 flex items-start gap-3">
                                                <div className="flex size-8 shrink-0 items-center justify-center rounded bg-orange-500">
                                                    <FileText className="size-4 text-white" />
                                                </div>

                                                <div className="flex-1">
                                                    <div className="mb-2 flex items-center gap-2">
                                                        <h2 className="text-lg font-medium">{selectedTemplate.name}</h2>

                                                        {selectedTemplate.zapierIntegration && (
                                                            <Badge className="text-xs" variant="secondary">
                                                                Zapier
                                                            </Badge>
                                                        )}
                                                    </div>

                                                    <p className="mb-4 text-sm text-gray-600">
                                                        {selectedTemplate.description}
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="space-y-4">
                                                <div>
                                                    <h3 className="mb-2 text-sm font-medium">Trigger:</h3>

                                                    <div className="flex items-center gap-2 rounded bg-gray-50 p-2">
                                                        <span className="text-sm">{selectedTemplate.triggerIcon}</span>

                                                        <span className="text-sm">{selectedTemplate.trigger}</span>
                                                    </div>
                                                </div>

                                                <div className="space-y-3">
                                                    {selectedTemplate.workflow.map((section, index) => (
                                                        <div key={index}>
                                                            <h4 className="mb-1 text-sm font-medium">
                                                                {section.title}
                                                            </h4>

                                                            <div className="space-y-1 text-sm text-gray-600">
                                                                {section.steps.map((step, stepIndex) => (
                                                                    <div className="ml-2" key={stepIndex}>
                                                                        {step}
                                                                    </div>
                                                                ))}
                                                            </div>
                                                        </div>
                                                    ))}
                                                </div>

                                                {selectedTemplate.requiredActions &&
                                                    selectedTemplate.requiredActions.length > 0 && (
                                                        <div>
                                                            <h3 className="mb-3 text-sm font-medium">
                                                                Required Actions
                                                            </h3>

                                                            <div className="space-y-2">
                                                                {selectedTemplate.requiredActions.map(
                                                                    (action, index) => (
                                                                        <div
                                                                            className="flex items-center gap-3 text-sm"
                                                                            key={index}
                                                                        >
                                                                            <div
                                                                                className={`size-4 ${action.color} flex shrink-0 items-center justify-center rounded-sm`}
                                                                            >
                                                                                <span className="text-xs text-white">
                                                                                    {action.icon}
                                                                                </span>
                                                                            </div>

                                                                            <span className="text-gray-700">
                                                                                {action.name}
                                                                            </span>
                                                                        </div>
                                                                    )
                                                                )}
                                                            </div>
                                                        </div>
                                                    )}
                                            </div>
                                        </div>
                                    </ScrollArea>
                                    <div className="mt-auto">
                                        <Button className="w-full" onClick={handleUseTemplate}>
                                            Use this template
                                        </Button>
                                    </div>
                                </>
                            ) : (
                                // Custom Agent Creation
                                <>
                                    <div className="mb-6">
                                        <h2 className="mb-4 text-lg font-medium">Create a custom agent</h2>

                                        <div className="mb-2 flex items-center justify-between">
                                            <h3 className="text-base font-medium">
                                                What would you like this agent to do?
                                            </h3>

                                            <Button className="h-auto p-0 text-sm text-indigo-600" variant="link">
                                                Skip this step
                                            </Button>
                                        </div>

                                        <Textarea
                                            className="min-h-[300px] text-sm"
                                            onChange={(e) => setAgentDescription(e.target.value)}
                                            placeholder="Example: Every morning at 8am, take a look at my tasks in Asana. Find all overdue tasks that have dependencies, and send a Slack message to the product team with a list of the overdue tasks, who they're assigned to, and the dependencies. Include a link to the tasks in the message."
                                            value={agentDescription}
                                        />

                                        <div className="mt-2 flex justify-end gap-2">
                                            <div className="flex size-6 items-center justify-center rounded-full bg-green-500">
                                                <MessageSquare className="size-3 text-white" />
                                            </div>

                                            <div className="flex size-6 items-center justify-center rounded-full bg-green-600">
                                                <MessageSquare className="size-3 text-white" />
                                            </div>
                                        </div>
                                    </div>
                                    <div className="mt-auto">
                                        <Button className="w-full" onClick={handleCreateAgent}>
                                            Create agent
                                        </Button>
                                    </div>
                                </>
                            )
                        ) : (
                            // Connection Step
                            <>
                                <div className="mb-6">
                                    <h2 className="mb-2 text-lg font-medium">Let's start by connecting your apps</h2>

                                    <p className="mb-6 text-sm text-gray-600">
                                        Connecting your apps allows this agent to run actions and access your data.
                                    </p>

                                    <div className="space-y-4">
                                        <div className="flex items-center justify-between">
                                            <div className="flex items-center gap-2">
                                                <Settings className="size-4 text-gray-400" />

                                                <span className="text-sm text-gray-600">Slack account</span>
                                            </div>
                                        </div>

                                        <div className="flex items-center justify-between rounded-lg border border-gray-200 p-4">
                                            <div className="flex items-center gap-3">
                                                <div className="flex size-8 items-center justify-center rounded bg-purple-600">
                                                    <span className="text-sm font-bold text-white">#</span>
                                                </div>

                                                <span className="font-medium">Slack</span>
                                            </div>

                                            <Button className="bg-indigo-600 hover:bg-indigo-700">Connect</Button>
                                        </div>

                                        <p className="text-sm text-gray-500">
                                            You can connect additional apps after creating the agent.
                                        </p>
                                    </div>
                                </div>

                                <div className="mt-auto flex gap-3">
                                    <Button onClick={handleSkipConnection} variant="outline">
                                        Skip
                                    </Button>

                                    <Button className="flex-1" onClick={handleFinalCreate}>
                                        Create
                                    </Button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
