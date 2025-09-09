/* eslint-disable sort-keys */
/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable tailwindcss/no-unnecessary-arbitrary-value */

import {Button} from '@/components/ui/button';
import {Dialog, DialogContent, DialogHeader} from '@/components/ui/dialog';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {HelpCircle, Settings, X} from 'lucide-react';
import {useState} from 'react';

import type React from 'react';

interface ToolConfigDialogProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    toolId: string;
    toolName: string;
    toolIcon: React.ReactNode;
}

interface ConfigField {
    id: string;
    label: string;
    required: boolean;
    type: 'select' | 'text' | 'textarea';
    placeholder?: string;
    options?: {value: string; label: string}[];
    value?: string;
    hasInfo?: boolean;
}

export function ToolConfigDialog({onOpenChange, open, toolIcon, toolId, toolName}: ToolConfigDialogProps) {
    const [formData, setFormData] = useState<Record<string, string>>({});

    // Configuration fields for different tools
    const getConfigFields = (toolId: string): ConfigField[] => {
        switch (toolId) {
            case 'gmail-label':
                return [
                    {
                        id: 'account',
                        label: 'Gmail account',
                        required: true,
                        type: 'select',
                        options: [
                            {value: 'gmail-1.1.12', label: 'Gmail (1.1.12) ivica.cardic@gmail.com'},
                            {value: 'gmail-other', label: 'Add another Gmail account'},
                        ],
                        value: 'gmail-1.1.12',
                    },
                    {
                        id: 'label',
                        label: 'Label',
                        required: true,
                        type: 'select',
                        placeholder: 'Let your agent select a value for this field',
                        hasInfo: true,
                    },
                    {
                        id: 'message',
                        label: 'Message',
                        required: true,
                        type: 'select',
                        placeholder: 'Let your agent select a value for this field',
                        hasInfo: true,
                    },
                ];
            case 'google-sheets':
                return [
                    {
                        id: 'account',
                        label: 'Google Sheets account',
                        required: true,
                        type: 'select',
                        options: [{value: 'sheets-1.0.0', label: 'Google Sheets (1.0.0) ivica.cardic@gmail.com'}],
                        value: 'sheets-1.0.0',
                    },
                    {
                        id: 'spreadsheet',
                        label: 'Spreadsheet',
                        required: true,
                        type: 'select',
                        placeholder: 'Select a spreadsheet',
                        hasInfo: true,
                    },
                    {
                        id: 'worksheet',
                        label: 'Worksheet',
                        required: true,
                        type: 'select',
                        placeholder: 'Select a worksheet',
                        hasInfo: true,
                    },
                ];
            case 'slack-message':
                return [
                    {
                        id: 'account',
                        label: 'Slack account',
                        required: true,
                        type: 'select',
                        options: [{value: 'slack-1.0.0', label: 'Slack (1.0.0) workspace.slack.com'}],
                        value: 'slack-1.0.0',
                    },
                    {
                        id: 'channel',
                        label: 'Channel',
                        required: true,
                        type: 'select',
                        placeholder: 'Select a channel',
                        hasInfo: true,
                    },
                    {
                        id: 'message',
                        label: 'Message',
                        required: true,
                        type: 'textarea',
                        placeholder: 'Enter your message',
                        hasInfo: true,
                    },
                ];
            default:
                return [];
        }
    };

    const configFields = getConfigFields(toolId);

    const handleSave = () => {
        console.log(`Saving configuration for ${toolName}:`, formData);
        onOpenChange(false);
    };

    const handleCancel = () => {
        onOpenChange(false);
    };

    const handleFieldChange = (fieldId: string, value: string) => {
        setFormData((prev) => ({
            ...prev,
            [fieldId]: value,
        }));
    };

    const renderField = (field: ConfigField) => {
        switch (field.type) {
            case 'select':
                return (
                    <div className="space-y-2" key={field.id}>
                        <div className="flex items-center gap-2">
                            <label className="text-sm font-medium text-gray-700">
                                {field.label}

                                {field.required && <span className="ml-1 text-red-500">*</span>}
                            </label>

                            {field.hasInfo && (
                                <Button className="size-4 p-0" size="icon" variant="ghost">
                                    <HelpCircle className="size-3 text-gray-400" />
                                </Button>
                            )}

                            {field.id === 'account' && (
                                <Button className="ml-auto size-4 p-0" size="icon" variant="ghost">
                                    <Settings className="size-3 text-gray-400" />
                                </Button>
                            )}
                        </div>

                        <Select
                            onValueChange={(value) => handleFieldChange(field.id, value)}
                            value={formData[field.id] || field.value || ''}
                        >
                            <SelectTrigger className="w-full">
                                <SelectValue placeholder={field.placeholder} />
                            </SelectTrigger>

                            <SelectContent>
                                {field.options?.map((option) => (
                                    <SelectItem key={option.value} value={option.value}>
                                        {option.label}
                                    </SelectItem>
                                ))}

                                {!field.options && field.placeholder && (
                                    <SelectItem disabled value="agent-select">
                                        {field.placeholder}
                                    </SelectItem>
                                )}
                            </SelectContent>
                        </Select>
                    </div>
                );
            case 'textarea':
                return (
                    <div className="space-y-2" key={field.id}>
                        <div className="flex items-center gap-2">
                            <label className="text-sm font-medium text-gray-700">
                                {field.label}

                                {field.required && <span className="ml-1 text-red-500">*</span>}
                            </label>

                            {field.hasInfo && (
                                <Button className="size-4 p-0" size="icon" variant="ghost">
                                    <HelpCircle className="size-3 text-gray-400" />
                                </Button>
                            )}
                        </div>

                        <textarea
                            className="w-full resize-none rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-indigo-500"
                            onChange={(e) => handleFieldChange(field.id, e.target.value)}
                            placeholder={field.placeholder}
                            rows={3}
                            value={formData[field.id] || ''}
                        />
                    </div>
                );
            default:
                return (
                    <div className="space-y-2" key={field.id}>
                        <div className="flex items-center gap-2">
                            <label className="text-sm font-medium text-gray-700">
                                {field.label}

                                {field.required && <span className="ml-1 text-red-500">*</span>}
                            </label>

                            {field.hasInfo && (
                                <Button className="size-4 p-0" size="icon" variant="ghost">
                                    <HelpCircle className="size-3 text-gray-400" />
                                </Button>
                            )}
                        </div>

                        <input
                            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-transparent focus:outline-none focus:ring-2 focus:ring-indigo-500"
                            onChange={(e) => handleFieldChange(field.id, e.target.value)}
                            placeholder={field.placeholder}
                            type="text"
                            value={formData[field.id] || ''}
                        />
                    </div>
                );
        }
    };

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className="gap-0 p-0 sm:max-w-[600px]">
                {/* Header */}

                <DialogHeader className="border-b border-gray-200 p-6 pb-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            {toolIcon}

                            <h2 className="text-lg font-medium">{toolName}</h2>
                        </div>

                        <Button onClick={() => onOpenChange(false)} size="icon" variant="ghost">
                            <X className="size-4" />
                        </Button>
                    </div>
                </DialogHeader>

                {/* Content */}

                <div className="space-y-6 p-6">{configFields.map((field) => renderField(field))}</div>

                {/* Footer */}

                <div className="flex items-center justify-end gap-3 border-t border-gray-200 bg-gray-50 p-6">
                    <Button onClick={handleCancel} variant="outline">
                        Cancel
                    </Button>

                    <Button className="bg-indigo-600 text-white hover:bg-indigo-700" onClick={handleSave}>
                        Save
                    </Button>
                </div>
            </DialogContent>
        </Dialog>
    );
}
