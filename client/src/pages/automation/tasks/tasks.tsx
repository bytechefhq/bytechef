'use client';

import {Avatar, AvatarFallback} from '@/components/ui/avatar';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {Card, CardContent} from '@/components/ui/card';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from '@/components/ui/dialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Input} from '@/components/ui/input';
import {Label} from '@/components/ui/label';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from '@/components/ui/select';
import {Textarea} from '@/components/ui/textarea';
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from '@/components/ui/tooltip';
import {
    TaskType,
    useCreateTaskMutation,
    useGetTasksQuery,
    useUpdateTaskMutation,
} from '@/shared/mutations/automation/tasks.mutations';
import {
    AlertTriangle,
    ArrowUpDown,
    Calendar,
    Check,
    CheckCircle2,
    Circle,
    Clock,
    Download,
    Edit,
    FileText,
    Keyboard,
    Link,
    MessageSquare,
    Paperclip,
    Plus,
    Save,
    Search,
    Send,
    SlidersHorizontal,
    User,
    X,
    XCircle,
} from 'lucide-react';
import {useEffect, useRef, useState} from 'react';

import type React from 'react';

type StatusFilterType = 'all' | 'open' | 'in-progress' | 'completed';
type PriorityFilterType = 'all' | 'high' | 'medium' | 'low';
type AssigneeFilterType = 'all' | string;
type SortOptionType = 'created' | 'title' | 'priority' | 'status' | 'assignee' | 'dueDate';
type SortDirectionType = 'asc' | 'desc';

interface FiltersI {
    status: StatusFilterType;
    priority: PriorityFilterType;
    assignee: AssigneeFilterType;
}

interface TaskCommentI {
    id: string;
    author: string;
    content: string;
    timestamp: string;
}

interface TaskAttachmentI {
    id: string;
    name: string;
    size: string;
    type: string;
    uploadedBy: string;
    uploadedAt: string;
}

interface TaskI {
    assignee: string;
    attachments: TaskAttachmentI[];
    comments: TaskCommentI[];
    createdAt: string;
    dependencies: string[];
    description: string;
    dueDate?: string;
    id: string;
    priority: 'high' | 'medium' | 'low';
    status: 'open' | 'in-progress' | 'completed';
    title: string;
    version?: number;
}

interface NewTaskFormI {
    title: string;
    description: string;
    status: 'open' | 'in-progress' | 'completed';
    priority: 'high' | 'medium' | 'low';
    assignee: string;
    dueDate: string;
    dependencies: string[];
    templateId?: string;
}

interface TaskTemplateI {
    id: string;
    name: string;
    description: string;
    defaultStatus: 'open' | 'in-progress' | 'completed';
    defaultPriority: 'high' | 'medium' | 'low';
    defaultAssignee?: string;
    estimatedDuration?: string;
    checklist: string[];
}

export default function Tasks() {
    const [autoSelect] = useState(false);
    const [selectedTask, setSelectedTask] = useState<string | null>(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
    const [showKeyboardShortcutTooltip, setShowKeyboardShortcutTooltip] = useState(false);
    const [isEditing, setIsEditing] = useState(false);
    const [editingTask, setEditingTask] = useState<TaskI | null>(null);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [newTaskForm, setNewTaskForm] = useState<NewTaskFormI>({
        assignee: '',
        dependencies: [],
        description: '',
        dueDate: '',
        priority: 'medium',
        status: 'open',
        templateId: 'none',
        title: '',
    });
    const [newComment, setNewComment] = useState('');
    const [formErrors, setFormErrors] = useState<Partial<NewTaskFormI>>({});
    const [filters, setFilters] = useState<FiltersI>({
        assignee: 'all',
        priority: 'all',
        status: 'all',
    });
    const [sortBy, setSortBy] = useState<SortOptionType>('created');
    const [sortDirection, setSortDirection] = useState<SortDirectionType>('desc');
    const searchInputRef = useRef<HTMLInputElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    // GraphQL mutations
    const createTaskMutation = useCreateTaskMutation({
        onError: (error) => {
            console.error('Error creating task:', error);
        },
        onSuccess: (createdTask) => {
            // Auto-select the new task if auto-select is enabled
            if (autoSelect) {
                setSelectedTask(createdTask.id);
            }
        },
    });

    const updateTaskMutation = useUpdateTaskMutation({
        onError: (error) => {
            console.error('Error updating task:', error);
        },
    });

    // Task templates
    const taskTemplates: TaskTemplateI[] = [
        {
            checklist: ['Reproduce the issue', 'Identify root cause', 'Implement fix', 'Test solution', 'Deploy fix'],
            defaultPriority: 'high',
            defaultStatus: 'open',
            description: 'Standard bug fix workflow',
            estimatedDuration: '2-4 hours',
            id: 'bug-fix',
            name: 'Bug Fix',
        },
        {
            checklist: ['Gather requirements', 'Design solution', 'Implement feature', 'Write tests', 'Documentation'],
            defaultPriority: 'medium',
            defaultStatus: 'open',
            description: 'New feature development',
            estimatedDuration: '1-2 weeks',
            id: 'feature-request',
            name: 'Feature Request',
        },
        {
            checklist: ['Review code changes', 'Check for best practices', 'Test functionality', 'Provide feedback'],
            defaultPriority: 'medium',
            defaultStatus: 'open',
            description: 'Code review task',
            estimatedDuration: '1-2 hours',
            id: 'code-review',
            name: 'Code Review',
        },
        {
            checklist: ['Research topic', 'Write content', 'Review for accuracy', 'Format and publish'],
            defaultPriority: 'low',
            defaultStatus: 'open',
            description: 'Documentation task',
            estimatedDuration: '4-6 hours',
            id: 'documentation',
            name: 'Documentation',
        },
    ];

    // Fetch tasks from GraphQL API
    const {data: apiTasks} = useGetTasksQuery();

    // Map API tasks to UI tasks with default values for missing fields
    const mapApiTaskToUiTask = (apiTask: TaskType): TaskI => ({
        assignee: apiTask.createdBy || 'Unassigned',
        attachments: [],
        comments: [],
        createdAt: apiTask.createdDate || new Date().toISOString(),
        dependencies: [],
        description: apiTask.description || '',
        dueDate: undefined,
        id: apiTask.id,
        priority: 'medium', // Default value
        status: 'open', // Default value
        title: apiTask.name,
    });

    // Update tasks when API data is loaded
    useEffect(() => {
        if (apiTasks) {
            const mappedTasks = apiTasks.map(mapApiTaskToUiTask);
            setTasks(mappedTasks);
        }
    }, [apiTasks]);

    // Sample task data with additional examples (used as fallback if API fails)
    const [tasks, setTasks] = useState<TaskI[]>([
        {
            assignee: 'John Doe',
            attachments: [
                {
                    id: 'a1',
                    name: 'error-logs.txt',
                    size: '2.3 KB',
                    type: 'text/plain',
                    uploadedAt: '2024-06-10T09:15:00Z',
                    uploadedBy: 'John Doe',
                },
            ],
            comments: [
                {
                    author: 'Jane Smith',
                    content: "I've reproduced this issue on staging. It seems to be related to the OAuth callback URL.",
                    id: 'c1',
                    timestamp: '2024-06-10T10:30:00Z',
                },
                {
                    author: 'John Doe',
                    content: "Thanks for the investigation. I'll check the callback configuration.",
                    id: 'c2',
                    timestamp: '2024-06-10T11:15:00Z',
                },
            ],
            createdAt: '2024-06-10',
            dependencies: [],
            description: 'Users are unable to log in with Google OAuth',
            dueDate: '2024-06-15',
            id: '1',
            priority: 'high',
            status: 'open',
            title: 'Fix authentication bug',
        },
        {
            assignee: 'Jane Smith',
            attachments: [],
            comments: [
                {
                    author: 'Jane Smith',
                    content: 'Started working on the wireframes. Should have initial designs ready by tomorrow.',
                    id: 'c3',
                    timestamp: '2024-06-09T14:20:00Z',
                },
            ],
            createdAt: '2024-06-09',
            dependencies: ['1'],
            description: 'Redesign the main dashboard with new metrics',
            dueDate: '2024-06-20',
            id: '2',
            priority: 'medium',
            status: 'in-progress',
            title: 'Update user dashboard',
        },
        {
            assignee: 'Mike Johnson',
            attachments: [],
            comments: [],
            createdAt: '2024-06-08',
            dependencies: [],
            description: 'Document all REST endpoints for the new API version',
            dueDate: '2024-06-25',
            id: '3',
            priority: 'low',
            status: 'open',
            title: 'Write API documentation',
        },
        {
            assignee: 'Sarah Wilson',
            attachments: [],
            comments: [
                {
                    author: 'Sarah Wilson',
                    content: 'Migration completed successfully. All data has been transferred to the new schema.',
                    id: 'c4',
                    timestamp: '2024-06-12T14:20:00Z',
                },
            ],
            createdAt: '2024-06-07',
            dependencies: [],
            description: 'Migrate user data to new schema structure',
            dueDate: '2024-06-12',
            id: '4',
            priority: 'high',
            status: 'completed',
            title: 'Database migration',
        },
        {
            assignee: 'Tom Brown',
            attachments: [],
            comments: [
                {
                    author: 'Tom Brown',
                    content: 'Identified several slow queries. Working on indexing improvements.',
                    id: 'c5',
                    timestamp: '2024-06-06T16:45:00Z',
                },
            ],
            createdAt: '2024-06-06',
            dependencies: ['4'],
            description: 'Optimize query performance for large datasets',
            dueDate: '2024-06-18',
            id: '5',
            priority: 'medium',
            status: 'in-progress',
            title: 'Performance optimization',
        },
        {
            assignee: 'Lisa Davis',
            attachments: [],
            comments: [],
            createdAt: '2024-06-05',
            dependencies: ['2'],
            description: 'Test new features on iOS and Android devices',
            dueDate: '2024-06-14',
            id: '6',
            priority: 'high',
            status: 'open',
            title: 'Mobile app testing',
        },
        {
            assignee: 'Alex Chen',
            attachments: [],
            comments: [
                {
                    author: 'Alex Chen',
                    content:
                        'Planning to use CSS custom properties for theme switching. Will start with the main components.',
                    id: 'c6',
                    timestamp: '2024-06-04T09:30:00Z',
                },
            ],
            createdAt: '2024-06-04',
            dependencies: [],
            description: 'Add dark mode support across the entire application',
            dueDate: '2024-06-22',
            id: '7',
            priority: 'medium',
            status: 'open',
            title: 'Implement dark mode',
        },
        {
            assignee: 'Emma Wilson',
            attachments: [
                {
                    id: 'a2',
                    name: 'security-checklist.pdf',
                    size: '156 KB',
                    type: 'application/pdf',
                    uploadedAt: '2024-06-03T12:00:00Z',
                    uploadedBy: 'Emma Wilson',
                },
            ],
            comments: [
                {
                    author: 'Emma Wilson',
                    content: 'Started with authentication flows. Found a few minor issues that need addressing.',
                    id: 'c7',
                    timestamp: '2024-06-03T13:15:00Z',
                },
                {
                    author: 'John Doe',
                    content: 'Let me know if you need any clarification on the auth implementation.',
                    id: 'c8',
                    timestamp: '2024-06-03T14:00:00Z',
                },
            ],
            createdAt: '2024-06-03',
            dependencies: [],
            description: 'Conduct comprehensive security review of the application',
            dueDate: '2024-06-17',
            id: '8',
            priority: 'high',
            status: 'in-progress',
            title: 'Security audit',
        },
        {
            assignee: 'Tom Brown',
            attachments: [],
            comments: [
                {
                    author: 'Tom Brown',
                    content: 'Pipeline is now live! All tests are running automatically on every commit.',
                    id: 'c9',
                    timestamp: '2024-06-10T10:00:00Z',
                },
            ],
            createdAt: '2024-06-02',
            dependencies: [],
            description: 'Configure automated testing and deployment pipeline',
            dueDate: '2024-06-10',
            id: '9',
            priority: 'medium',
            status: 'completed',
            title: 'Setup CI/CD pipeline',
        },
        {
            assignee: 'Lisa Davis',
            attachments: [],
            comments: [],
            createdAt: '2024-06-01',
            dependencies: [],
            description: 'Analyze recent user feedback and create improvement recommendations',
            dueDate: '2024-06-30',
            id: '10',
            priority: 'low',
            status: 'open',
            title: 'User feedback analysis',
        },
        {
            assignee: 'Mike Johnson',
            attachments: [],
            comments: [
                {
                    author: 'Mike Johnson',
                    content: 'Researching email service providers. Considering SendGrid vs AWS SES.',
                    id: 'c10',
                    timestamp: '2024-05-31T15:30:00Z',
                },
            ],
            createdAt: '2024-05-31',
            dependencies: [],
            description: 'Implement email notifications for task updates and mentions',
            dueDate: '2024-06-21',
            id: '11',
            priority: 'medium',
            status: 'open',
            title: 'Email notification system',
        },
    ]);

    // Available assignees for the select dropdown
    const availableAssignees = [
        'John Doe',
        'Jane Smith',
        'Mike Johnson',
        'Sarah Wilson',
        'Tom Brown',
        'Lisa Davis',
        'Alex Chen',
        'Emma Wilson',
    ];

    // Keyboard shortcut to focus search
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            // Check for Cmd+K (Mac) or Ctrl+K (Windows/Linux)
            if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
                e.preventDefault(); // Prevent browser's default behavior
                searchInputRef.current?.focus();

                // Show tooltip briefly when shortcut is used
                setShowKeyboardShortcutTooltip(true);
                setTimeout(() => setShowKeyboardShortcutTooltip(false), 2000);
            }
        };

        document.addEventListener('keydown', handleKeyDown);
        return () => document.removeEventListener('keydown', handleKeyDown);
    }, []);

    // Utility function to highlight matching text
    const highlightText = (text: string, query: string) => {
        if (!query.trim()) return text;

        const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
        const parts = text.split(regex);

        return parts.map((part, index) => {
            if (part.toLowerCase() === query.toLowerCase()) {
                return (
                    <mark className="rounded bg-yellow-200 px-0.5 text-yellow-900" key={index}>
                        {part}
                    </mark>
                );
            }
            return part;
        });
    };

    // First, let's add a function to cycle through task statuses
    // Add this function after the existing utility functions

    const cycleTaskStatus = (currentStatus: string) => {
        // Cycle through: open -> in-progress -> completed -> open
        switch (currentStatus) {
            case 'open':
                return 'in-progress';
            case 'in-progress':
                return 'completed';
            case 'completed':
                return 'open';
            default:
                return 'open';
        }
    };

    // Generate unique ID for new tasks
    const generateTaskId = () => {
        return (Math.max(...tasks.map((t) => Number.parseInt(t.id)), 0) + 1).toString();
    };

    // Generate unique ID for comments
    const generateCommentId = () => {
        const allComments = tasks.flatMap((t) => t.comments);
        const maxId = Math.max(...allComments.map((c) => Number.parseInt(c.id.replace('c', ''))), 0);
        return `c${maxId + 1}`;
    };

    // Generate unique ID for attachments
    const generateAttachmentId = () => {
        const allAttachments = tasks.flatMap((t) => t.attachments);
        const maxId = Math.max(...allAttachments.map((a) => Number.parseInt(a.id.replace('a', ''))), 0);
        return `a${maxId + 1}`;
    };

    // Get current date in YYYY-MM-DD format
    const getCurrentDate = () => {
        return new Date().toISOString().split('T')[0];
    };

    // Get current timestamp
    const getCurrentTimestamp = () => {
        return new Date().toISOString();
    };

    // Check if task is overdue
    const isTaskOverdue = (task: TaskI) => {
        if (!task.dueDate || task.status === 'completed') return false;
        return new Date(task.dueDate) < new Date();
    };

    // // Get days until due date
    // const getDaysUntilDue = (dueDate: string) => {
    //     const today = new Date();
    //     const due = new Date(dueDate);
    //     const diffTime = due.getTime() - today.getTime();
    //     return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    // };

    // Format date for display
    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-US', {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
        });
    };

    // Format timestamp for comments
    const formatTimestamp = (timestamp: string) => {
        return new Date(timestamp).toLocaleString('en-US', {
            day: 'numeric',
            hour: 'numeric',
            minute: '2-digit',
            month: 'short',
        });
    };

    // Get initials for avatar
    const getInitials = (name: string) => {
        return name
            .split(' ')
            .map((n) => n[0])
            .join('')
            .toUpperCase();
    };

    // Validate form fields
    const validateForm = (form: NewTaskFormI): Partial<NewTaskFormI> => {
        const errors: Partial<NewTaskFormI> = {};

        if (!form.title.trim()) {
            errors.title = 'Title is required';
        }

        if (!form.description.trim()) {
            errors.description = 'Description is required';
        }

        if (!form.assignee) {
            errors.assignee = 'Assignee is required';
        }

        return errors;
    };

    // Handle form field changes
    const handleNewTaskFormChange = (field: keyof NewTaskFormI, value: string | string[]) => {
        setNewTaskForm((prev) => ({
            ...prev,
            [field]: value,
        }));

        // Clear error for this field when user starts typing
        if (formErrors[field]) {
            setFormErrors((prev) => ({
                ...prev,
                [field]: undefined,
            }));
        }
    };

    // Apply template to form
    const applyTemplate = (templateId: string) => {
        const template = taskTemplates.find((t) => t.id === templateId);
        if (template) {
            setNewTaskForm((prev) => ({
                ...prev,
                assignee: template.defaultAssignee || prev.assignee,
                priority: template.defaultPriority,
                status: template.defaultStatus,
                templateId: templateId,
            }));
        }
    };

    // Handle form submission
    const handleCreateTask = () => {
        const errors = validateForm(newTaskForm);
        setFormErrors(errors);

        if (Object.keys(errors).length === 0) {
            // Create a local task for immediate UI feedback
            const newTask: TaskI = {
                assignee: newTaskForm.assignee,
                attachments: [],
                comments: [],
                createdAt: getCurrentDate(),
                dependencies: newTaskForm.dependencies,
                description: newTaskForm.description.trim(),
                dueDate: newTaskForm.dueDate || undefined,
                id: generateTaskId(), // Temporary ID, will be replaced by the server
                priority: newTaskForm.priority,
                status: newTaskForm.status,
                title: newTaskForm.title.trim(),
            };

            // Add to local state for immediate feedback
            setTasks((prev) => [newTask, ...prev]);

            // Create task in the backend using GraphQL mutation
            createTaskMutation.mutate({
                description: newTaskForm.description.trim(),
                name: newTaskForm.title.trim(),
            });

            // Reset form and close modal
            setNewTaskForm({
                assignee: '',
                dependencies: [],
                description: '',
                dueDate: '',
                priority: 'medium',
                status: 'open',
                templateId: 'none',
                title: '',
            });
            setFormErrors({});
            setIsCreateModalOpen(false);
        }
    };

    // Handle modal close
    const handleCloseModal = () => {
        setIsCreateModalOpen(false);
        setFormErrors({});
        // Optionally reset form when closing
        setNewTaskForm({
            assignee: '',
            dependencies: [],
            description: '',
            dueDate: '',
            priority: 'medium',
            status: 'open',
            templateId: 'none',
            title: '',
        });
    };

    // Sort tasks
    const sortTasks = (tasks: TaskI[]) => {
        return [...tasks].sort((a, b) => {
            let aValue: string | number;
            let bValue: string | number;

            // Define order maps outside the switch
            const priorityOrder = {high: 3, low: 1, medium: 2};
            const statusOrder = {completed: 3, 'in-progress': 2, open: 1};

            switch (sortBy) {
                case 'title':
                    aValue = a.title.toLowerCase();
                    bValue = b.title.toLowerCase();
                    break;
                case 'priority':
                    aValue = priorityOrder[a.priority];
                    bValue = priorityOrder[b.priority];
                    break;
                case 'status':
                    aValue = statusOrder[a.status];
                    bValue = statusOrder[b.status];
                    break;
                case 'assignee':
                    aValue = a.assignee.toLowerCase();
                    bValue = b.assignee.toLowerCase();
                    break;
                case 'dueDate':
                    aValue = a.dueDate ? new Date(a.dueDate).getTime() : Number.MAX_SAFE_INTEGER;
                    bValue = b.dueDate ? new Date(b.dueDate).getTime() : Number.MAX_SAFE_INTEGER;
                    break;
                case 'created':
                default:
                    aValue = new Date(a.createdAt).getTime();
                    bValue = new Date(b.createdAt).getTime();
                    break;
            }

            if (sortDirection === 'asc') {
                return aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
            } else {
                return aValue > bValue ? -1 : aValue < bValue ? 1 : 0;
            }
        });
    };

    // Filter tasks based on search query and filters
    const filteredTasks = sortTasks(
        tasks.filter((task) => {
            // First filter by search query (title, description, assignee)
            const matchesSearch =
                task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                task.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
                task.assignee.toLowerCase().includes(searchQuery.toLowerCase());

            // Then filter by status
            const matchesStatus = filters.status === 'all' || task.status === filters.status;

            // Then filter by priority
            const matchesPriority = filters.priority === 'all' || task.priority === filters.priority;

            // Then filter by assignee
            const matchesAssignee = filters.assignee === 'all' || task.assignee === filters.assignee;

            return matchesSearch && matchesStatus && matchesPriority && matchesAssignee;
        })
    );

    // Get search suggestions based on task titles and assignee names (filtered by status, priority, and assignee)
    const getSuggestions = () => {
        if (!searchQuery.trim()) return [];

        const filteredTasks = tasks.filter((task) => {
            const matchesStatus = filters.status === 'all' || task.status === filters.status;
            const matchesPriority = filters.priority === 'all' || task.priority === filters.priority;
            const matchesAssignee = filters.assignee === 'all' || task.assignee === filters.assignee;
            return matchesStatus && matchesPriority && matchesAssignee;
        });

        const titleSuggestions = filteredTasks
            .filter(
                (task) =>
                    task.title.toLowerCase().includes(searchQuery.toLowerCase()) &&
                    task.title.toLowerCase() !== searchQuery.toLowerCase()
            )
            .map((task) => task.title);

        const assigneeSuggestions = filteredTasks
            .filter(
                (task) =>
                    task.assignee.toLowerCase().includes(searchQuery.toLowerCase()) &&
                    task.assignee.toLowerCase() !== searchQuery.toLowerCase()
            )
            .map((task) => task.assignee);

        const allSuggestions = [...titleSuggestions, ...assigneeSuggestions].slice(0, 5);
        return [...new Set(allSuggestions)]; // Remove duplicates
    };

    const suggestions = getSuggestions();

    const handleSearchChange = (value: string) => {
        setSearchQuery(value);
        setShowSuggestions(value.length > 0);
        setSelectedSuggestionIndex(-1);
    };

    const handleSuggestionClick = (suggestion: string) => {
        setSearchQuery(suggestion);
        setShowSuggestions(false);
        setSelectedSuggestionIndex(-1);
        searchInputRef.current?.focus();
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (!showSuggestions || suggestions.length === 0) return;

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault();
                setSelectedSuggestionIndex((prev) => (prev < suggestions.length - 1 ? prev + 1 : prev));
                break;
            case 'ArrowUp':
                e.preventDefault();
                setSelectedSuggestionIndex((prev) => (prev > 0 ? prev - 1 : -1));
                break;
            case 'Enter':
                e.preventDefault();
                if (selectedSuggestionIndex >= 0) {
                    handleSuggestionClick(suggestions[selectedSuggestionIndex]);
                }
                break;
            case 'Escape':
                setShowSuggestions(false);
                setSelectedSuggestionIndex(-1);
                break;
        }
    };

    // Close suggestions when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (searchInputRef.current && !searchInputRef.current.contains(event.target as Node)) {
                setShowSuggestions(false);
                setSelectedSuggestionIndex(-1);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Task editing functions
    const handleEditTask = () => {
        const task = tasks.find((t) => t.id === selectedTask);
        if (task) {
            setEditingTask({...task});
            setIsEditing(true);
        }
    };

    const handleSaveTask = () => {
        if (editingTask) {
            // Update local state for immediate feedback
            setTasks((prevTasks) => prevTasks.map((task) => (task.id === editingTask.id ? editingTask : task)));

            // Update task in the backend using GraphQL mutation
            updateTaskMutation.mutate({
                description: editingTask.description,
                id: editingTask.id,
                name: editingTask.title,
                version: editingTask.version,
            });

            setIsEditing(false);
            setEditingTask(null);
        }
    };

    const handleCancelEdit = () => {
        setIsEditing(false);
        setEditingTask(null);
    };

    const handleTaskFieldChange = (field: keyof TaskI, value: string | string[]) => {
        if (editingTask) {
            setEditingTask({
                ...editingTask,
                [field]: value,
            });
        }
    };

    // Task status toggle function
    const handleStatusToggle = (taskId: string, e: React.MouseEvent) => {
        // Prevent the click from selecting the task
        e.stopPropagation();

        // Find the task and update its status
        setTasks((prevTasks) =>
            prevTasks.map((task) => {
                if (task.id === taskId) {
                    return {
                        ...task,
                        status: cycleTaskStatus(task.status),
                    };
                }
                return task;
            })
        );
    };

    // Add comment to task
    const handleAddComment = () => {
        if (!newComment.trim() || !selectedTask) return;

        const comment: TaskCommentI = {
            author: 'Current User', // In a real app, this would be the logged-in user
            content: newComment.trim(),
            id: generateCommentId(),
            timestamp: getCurrentTimestamp(),
        };

        setTasks((prevTasks) =>
            prevTasks.map((task) => {
                if (task.id === selectedTask) {
                    return {
                        ...task,
                        comments: [...task.comments, comment],
                    };
                }
                return task;
            })
        );

        setNewComment('');
    };

    // Handle file upload
    const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
        const files = event.target.files;
        if (!files || !selectedTask) return;

        const newAttachments: TaskAttachmentI[] = Array.from(files).map((file) => ({
            id: generateAttachmentId(),
            name: file.name,
            size: `${(file.size / 1024).toFixed(1)} KB`,
            type: file.type || 'application/octet-stream',
            uploadedAt: getCurrentTimestamp(),
            uploadedBy: 'Current User',
        }));

        setTasks((prevTasks) =>
            prevTasks.map((task) => {
                if (task.id === selectedTask) {
                    return {
                        ...task,
                        attachments: [...task.attachments, ...newAttachments],
                    };
                }
                return task;
            })
        );

        // Reset file input
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    // Remove attachment
    const handleRemoveAttachment = (attachmentId: string) => {
        if (!selectedTask) return;

        setTasks((prevTasks) =>
            prevTasks.map((task) => {
                if (task.id === selectedTask) {
                    return {
                        ...task,
                        attachments: task.attachments.filter((a) => a.id !== attachmentId),
                    };
                }
                return task;
            })
        );
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'completed':
                return <CheckCircle2 className="size-4 text-green-600" />;
            case 'in-progress':
                return <Clock className="size-4 text-blue-600" />;
            default:
                return <Circle className="size-4 text-gray-400" />;
        }
    };

    const getPriorityColor = (priority: string) => {
        switch (priority) {
            case 'high':
                return 'bg-red-100 text-red-800 border-red-200';
            case 'medium':
                return 'bg-yellow-100 text-yellow-800 border-yellow-200';
            case 'low':
                return 'bg-green-100 text-green-800 border-green-200';
            default:
                return 'bg-gray-100 text-gray-800 border-gray-200';
        }
    };

    // Get task counts for each status
    const getTaskCounts = () => {
        return {
            all: tasks.length,
            completed: tasks.filter((task) => task.status === 'completed').length,
            'in-progress': tasks.filter((task) => task.status === 'in-progress').length,
            open: tasks.filter((task) => task.status === 'open').length,
        };
    };

    // Get task counts for each priority
    const getPriorityCounts = () => {
        return {
            all: tasks.length,
            high: tasks.filter((task) => task.priority === 'high').length,
            low: tasks.filter((task) => task.priority === 'low').length,
            medium: tasks.filter((task) => task.priority === 'medium').length,
        };
    };

    // Get unique assignees and their task counts
    const getAssigneeCounts = () => {
        const uniqueAssignees = [...new Set(tasks.map((task) => task.assignee))];
        const counts: Record<string, number> = {
            all: tasks.length,
        };

        uniqueAssignees.forEach((assignee) => {
            counts[assignee] = tasks.filter((task) => task.assignee === assignee).length;
        });

        return {assignees: uniqueAssignees, counts};
    };

    const taskCounts = getTaskCounts();
    const priorityCounts = getPriorityCounts();
    const {assignees, counts: assigneeCounts} = getAssigneeCounts();

    // Check if any filters are active
    const hasActiveFilters = filters.status !== 'all' || filters.priority !== 'all' || filters.assignee !== 'all';

    // Reset all filters
    const resetFilters = () => {
        setFilters({
            assignee: 'all',
            priority: 'all',
            status: 'all',
        });
    };

    // Get header text based on active filters
    const getHeaderText = () => {
        const parts = [];

        if (filters.status !== 'all') {
            parts.push(filters.status.charAt(0).toUpperCase() + filters.status.slice(1).replace('-', ' '));
        }

        if (filters.priority !== 'all') {
            parts.push(filters.priority.charAt(0).toUpperCase() + filters.priority.slice(1) + ' priority');
        }

        if (filters.assignee !== 'all') {
            parts.push(`by ${filters.assignee}`);
        }

        if (parts.length === 0) {
            return 'All tasks';
        }

        return `${parts.join(', ')} tasks`;
    };

    // Determine the keyboard shortcut text based on platform
    const isMac = typeof navigator !== 'undefined' ? /Mac|iPod|iPhone|iPad/.test(navigator.platform) : false;
    const shortcutText = isMac ? '⌘K' : 'Ctrl+K';

    // Get available tasks for dependencies (exclude current task and completed tasks)
    const getAvailableTasksForDependencies = (currentTaskId?: string) => {
        return tasks.filter((task) => task.id !== currentTaskId && task.status !== 'completed');
    };

    return (
        <div className="flex h-screen bg-background">
            {/* Sidebar */}

            <div className="flex w-96 flex-col border-r border-border bg-background">
                {/* Fixed Header */}

                <div className="border-b border-border bg-background p-4">
                    <div className="mb-4 flex items-center justify-between">
                        <h1 className="truncate text-sm font-medium text-foreground">
                            {getHeaderText()} ({filteredTasks.length})
                        </h1>

                        <div className="flex items-center gap-2">
                            {/* Create Task Button */}

                            <Dialog onOpenChange={setIsCreateModalOpen} open={isCreateModalOpen}>
                                <DialogTrigger asChild>
                                    <Button className="h-6 px-2 text-xs" size="sm">
                                        <Plus className="mr-1 size-3" />
                                        New
                                    </Button>
                                </DialogTrigger>

                                <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-workflow-test-configuration-dialog-width">
                                    <DialogHeader>
                                        <DialogTitle>Create New Task</DialogTitle>

                                        <DialogDescription>
                                            Add a new task to your project. Fill in the details below to get started.
                                        </DialogDescription>
                                    </DialogHeader>

                                    <div className="space-y-4 py-4">
                                        {/* Template Selection */}

                                        <div className="space-y-2">
                                            <Label>Template (Optional)</Label>

                                            <Select
                                                onValueChange={(value) => {
                                                    handleNewTaskFormChange(
                                                        'templateId',
                                                        value === 'none' ? '' : value
                                                    );
                                                    if (value && value !== 'none') applyTemplate(value);
                                                }}
                                                value={newTaskForm.templateId || 'none'}
                                            >
                                                <SelectTrigger>
                                                    <SelectValue placeholder="Choose a template" />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    <SelectItem value="none">No template</SelectItem>

                                                    {taskTemplates.map((template) => (
                                                        <SelectItem key={template.id} value={template.id}>
                                                            <div className="flex flex-col">
                                                                <span className="font-medium">{template.name}</span>

                                                                <span className="text-xs text-muted-foreground">
                                                                    {template.description}
                                                                </span>
                                                            </div>
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        </div>

                                        <div className="space-y-2">
                                            <Label htmlFor="task-title">Title *</Label>

                                            <Input
                                                className={formErrors.title ? 'border-red-500' : ''}
                                                id="task-title"
                                                onChange={(e) => handleNewTaskFormChange('title', e.target.value)}
                                                placeholder="Enter task title"
                                                value={newTaskForm.title}
                                            />

                                            {formErrors.title && (
                                                <p className="text-sm text-red-500">{formErrors.title}</p>
                                            )}
                                        </div>

                                        <div className="space-y-2">
                                            <Label htmlFor="task-description">Description *</Label>

                                            <Textarea
                                                className={`min-h-[80px] ${formErrors.description ? 'border-red-500' : ''}`}
                                                id="task-description"
                                                onChange={(e) => handleNewTaskFormChange('description', e.target.value)}
                                                placeholder="Describe what needs to be done"
                                                value={newTaskForm.description}
                                            />

                                            {formErrors.description && (
                                                <p className="text-sm text-red-500">{formErrors.description}</p>
                                            )}
                                        </div>

                                        <div className="grid grid-cols-2 gap-4">
                                            <div className="space-y-2">
                                                <Label htmlFor="task-status">Status</Label>

                                                <Select
                                                    onValueChange={(value) => handleNewTaskFormChange('status', value)}
                                                    value={newTaskForm.status}
                                                >
                                                    <SelectTrigger>
                                                        <SelectValue placeholder="Select a status" />
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        <SelectItem value="open">
                                                            <div className="flex items-center gap-2">
                                                                <Circle className="size-3 text-gray-400" />
                                                                Open
                                                            </div>
                                                        </SelectItem>

                                                        <SelectItem value="in-progress">
                                                            <div className="flex items-center gap-2">
                                                                <Clock className="size-3 text-blue-600" />
                                                                In Progress
                                                            </div>
                                                        </SelectItem>

                                                        <SelectItem value="completed">
                                                            <div className="flex items-center gap-2">
                                                                <CheckCircle2 className="size-3 text-green-600" />
                                                                Completed
                                                            </div>
                                                        </SelectItem>
                                                    </SelectContent>
                                                </Select>
                                            </div>

                                            <div className="space-y-2">
                                                <Label htmlFor="task-priority">Priority</Label>

                                                <Select
                                                    onValueChange={(value) =>
                                                        handleNewTaskFormChange('priority', value)
                                                    }
                                                    value={newTaskForm.priority}
                                                >
                                                    <SelectTrigger>
                                                        <SelectValue placeholder="Select a priority" />
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        <SelectItem value="high">
                                                            <div className="flex items-center gap-2">
                                                                <Badge
                                                                    className="border-red-200 bg-red-100 text-red-800"
                                                                    variant="outline"
                                                                >
                                                                    High
                                                                </Badge>
                                                            </div>
                                                        </SelectItem>

                                                        <SelectItem value="medium">
                                                            <div className="flex items-center gap-2">
                                                                <Badge
                                                                    className="border-yellow-200 bg-yellow-100 text-yellow-800"
                                                                    variant="outline"
                                                                >
                                                                    Medium
                                                                </Badge>
                                                            </div>
                                                        </SelectItem>

                                                        <SelectItem value="low">
                                                            <div className="flex items-center gap-2">
                                                                <Badge
                                                                    className="border-green-200 bg-green-100 text-green-800"
                                                                    variant="outline"
                                                                >
                                                                    Low
                                                                </Badge>
                                                            </div>
                                                        </SelectItem>
                                                    </SelectContent>
                                                </Select>
                                            </div>
                                        </div>

                                        <div className="grid grid-cols-2 gap-4">
                                            <div className="space-y-2">
                                                <Label htmlFor="task-assignee">Assignee *</Label>

                                                <Select
                                                    onValueChange={(value) =>
                                                        handleNewTaskFormChange('assignee', value)
                                                    }
                                                    value={newTaskForm.assignee}
                                                >
                                                    <SelectTrigger
                                                        className={formErrors.assignee ? 'border-red-500' : ''}
                                                    >
                                                        <SelectValue placeholder="Select an assignee" />
                                                    </SelectTrigger>

                                                    <SelectContent>
                                                        {availableAssignees.map((assignee) => (
                                                            <SelectItem key={assignee} value={assignee}>
                                                                <div className="flex items-center gap-2">
                                                                    <User className="size-3 text-muted-foreground" />

                                                                    {assignee}
                                                                </div>
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>

                                                {formErrors.assignee && (
                                                    <p className="text-sm text-red-500">{formErrors.assignee}</p>
                                                )}
                                            </div>

                                            <div className="space-y-2">
                                                <Label htmlFor="task-due-date">Due Date</Label>

                                                <Input
                                                    id="task-due-date"
                                                    onChange={(e) => handleNewTaskFormChange('dueDate', e.target.value)}
                                                    type="date"
                                                    value={newTaskForm.dueDate}
                                                />
                                            </div>
                                        </div>

                                        <div className="space-y-2">
                                            <Label>Dependencies</Label>

                                            <Select
                                                onValueChange={(value) => {
                                                    if (
                                                        value &&
                                                        value !== 'add-dependency' &&
                                                        !newTaskForm.dependencies.includes(value)
                                                    ) {
                                                        handleNewTaskFormChange('dependencies', [
                                                            ...newTaskForm.dependencies,
                                                            value,
                                                        ]);
                                                    }
                                                }}
                                                value="add-dependency"
                                            >
                                                <SelectTrigger>
                                                    <SelectValue placeholder="Add task dependencies" />
                                                </SelectTrigger>

                                                <SelectContent>
                                                    <SelectItem disabled value="add-dependency">
                                                        Add task dependencies
                                                    </SelectItem>

                                                    {getAvailableTasksForDependencies().map((task) => (
                                                        <SelectItem key={task.id} value={task.id}>
                                                            <div className="flex items-center gap-2">
                                                                <Link className="size-3 text-muted-foreground" />

                                                                {task.title}
                                                            </div>
                                                        </SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>

                                            {newTaskForm.dependencies.length > 0 && (
                                                <div className="mt-2 flex flex-wrap gap-1">
                                                    {newTaskForm.dependencies.map((depId) => {
                                                        const depTask = tasks.find((t) => t.id === depId);
                                                        return (
                                                            <Badge
                                                                className="flex items-center gap-1"
                                                                key={depId}
                                                                variant="secondary"
                                                            >
                                                                {depTask?.title}

                                                                <Button
                                                                    className="size-3 p-0"
                                                                    onClick={() =>
                                                                        handleNewTaskFormChange(
                                                                            'dependencies',
                                                                            newTaskForm.dependencies.filter(
                                                                                (id) => id !== depId
                                                                            )
                                                                        )
                                                                    }
                                                                    size="icon"
                                                                    variant="ghost"
                                                                >
                                                                    <X className="size-2" />
                                                                </Button>
                                                            </Badge>
                                                        );
                                                    })}
                                                </div>
                                            )}
                                        </div>
                                    </div>

                                    <DialogFooter>
                                        <Button onClick={handleCloseModal} variant="outline">
                                            Cancel
                                        </Button>

                                        <Button onClick={handleCreateTask}>Create Task</Button>
                                    </DialogFooter>
                                </DialogContent>
                            </Dialog>

                            {/* Sort Button */}

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        aria-label="Sort options"
                                        className="size-6 shrink-0"
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <ArrowUpDown className="size-4" />
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end" className="w-48">
                                    <DropdownMenuLabel>Sort by</DropdownMenuLabel>

                                    <DropdownMenuSeparator />

                                    {[
                                        {label: 'Created Date', value: 'created'},
                                        {label: 'Title', value: 'title'},
                                        {label: 'Priority', value: 'priority'},
                                        {label: 'Status', value: 'status'},
                                        {label: 'Assignee', value: 'assignee'},
                                        {label: 'Due Date', value: 'dueDate'},
                                    ].map((option) => (
                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            key={option.value}
                                            onClick={() => {
                                                if (sortBy === option.value) {
                                                    setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
                                                } else {
                                                    setSortBy(option.value as SortOptionType);
                                                    setSortDirection('asc');
                                                }
                                            }}
                                        >
                                            <span>{option.label}</span>

                                            {sortBy === option.value && (
                                                <span className="text-xs">{sortDirection === 'asc' ? '↑' : '↓'}</span>
                                            )}
                                        </DropdownMenuItem>
                                    ))}
                                </DropdownMenuContent>
                            </DropdownMenu>

                            <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                    <Button
                                        aria-label="Filter options"
                                        className="size-6 shrink-0"
                                        size="icon"
                                        variant={hasActiveFilters ? 'default' : 'ghost'}
                                    >
                                        <SlidersHorizontal className="size-4" />
                                    </Button>
                                </DropdownMenuTrigger>

                                <DropdownMenuContent align="end" className="w-56">
                                    <DropdownMenuLabel>Filter Tasks</DropdownMenuLabel>

                                    <DropdownMenuSeparator />

                                    <DropdownMenuGroup>
                                        <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                                            Status
                                        </DropdownMenuLabel>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, status: 'all'}))}
                                        >
                                            <span>All</span>

                                            {filters.status === 'all' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({taskCounts.all})
                                            </span>
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, status: 'open'}))}
                                        >
                                            <div className="flex items-center">
                                                <Circle className="mr-2 size-3 text-gray-400" />

                                                <span>Open</span>
                                            </div>

                                            {filters.status === 'open' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({taskCounts.open})
                                            </span>
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, status: 'in-progress'}))}
                                        >
                                            <div className="flex items-center">
                                                <Clock className="mr-2 size-3 text-blue-600" />

                                                <span>In Progress</span>
                                            </div>

                                            {filters.status === 'in-progress' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({taskCounts['in-progress']})
                                            </span>
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, status: 'completed'}))}
                                        >
                                            <div className="flex items-center">
                                                <CheckCircle2 className="mr-2 size-3 text-green-600" />

                                                <span>Completed</span>
                                            </div>

                                            {filters.status === 'completed' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({taskCounts.completed})
                                            </span>
                                        </DropdownMenuItem>
                                    </DropdownMenuGroup>

                                    <DropdownMenuSeparator />

                                    <DropdownMenuGroup>
                                        <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                                            Priority
                                        </DropdownMenuLabel>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, priority: 'all'}))}
                                        >
                                            <span>All</span>

                                            {filters.priority === 'all' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({priorityCounts.all})
                                            </span>
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, priority: 'high'}))}
                                        >
                                            <div className="flex items-center">
                                                <Badge
                                                    className="mr-2 h-3 border-red-200 bg-red-100 px-1 text-red-800"
                                                    variant="outline"
                                                >
                                                    <span className="text-[10px]">High</span>
                                                </Badge>

                                                <span>High</span>
                                            </div>

                                            {filters.priority === 'high' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({priorityCounts.high})
                                            </span>
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, priority: 'medium'}))}
                                        >
                                            <div className="flex items-center">
                                                <Badge
                                                    className="mr-2 h-3 border-yellow-200 bg-yellow-100 px-1 text-yellow-800"
                                                    variant="outline"
                                                >
                                                    <span className="text-[10px]">Med</span>
                                                </Badge>

                                                <span>Medium</span>
                                            </div>

                                            {filters.priority === 'medium' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({priorityCounts.medium})
                                            </span>
                                        </DropdownMenuItem>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, priority: 'low'}))}
                                        >
                                            <div className="flex items-center">
                                                <Badge
                                                    className="mr-2 h-3 border-green-200 bg-green-100 px-1 text-green-800"
                                                    variant="outline"
                                                >
                                                    <span className="text-[10px]">Low</span>
                                                </Badge>

                                                <span>Low</span>
                                            </div>

                                            {filters.priority === 'low' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({priorityCounts.low})
                                            </span>
                                        </DropdownMenuItem>
                                    </DropdownMenuGroup>

                                    <DropdownMenuSeparator />

                                    <DropdownMenuGroup>
                                        <DropdownMenuLabel className="text-xs font-normal text-muted-foreground">
                                            Assignee
                                        </DropdownMenuLabel>

                                        <DropdownMenuItem
                                            className="flex items-center justify-between"
                                            onClick={() => setFilters((prev) => ({...prev, assignee: 'all'}))}
                                        >
                                            <span>All</span>

                                            {filters.assignee === 'all' && <Check className="size-4" />}

                                            <span className="ml-auto text-xs text-muted-foreground">
                                                ({assigneeCounts.all})
                                            </span>
                                        </DropdownMenuItem>

                                        {assignees.map((assignee) => (
                                            <DropdownMenuItem
                                                className="flex items-center justify-between"
                                                key={assignee}
                                                onClick={() => setFilters((prev) => ({...prev, assignee}))}
                                            >
                                                <div className="flex items-center">
                                                    <User className="mr-2 size-3 text-muted-foreground" />

                                                    <span className="truncate">{assignee}</span>
                                                </div>

                                                {filters.assignee === assignee && <Check className="size-4" />}

                                                <span className="ml-auto text-xs text-muted-foreground">
                                                    ({assigneeCounts[assignee]})
                                                </span>
                                            </DropdownMenuItem>
                                        ))}
                                    </DropdownMenuGroup>

                                    <DropdownMenuSeparator />

                                    <DropdownMenuItem
                                        className="text-primary focus:text-primary"
                                        disabled={!hasActiveFilters}
                                        onClick={resetFilters}
                                    >
                                        <X className="mr-2 size-4" />
                                        Clear all filters
                                    </DropdownMenuItem>
                                </DropdownMenuContent>
                            </DropdownMenu>
                        </div>
                    </div>

                    {/* Active Filters */}

                    {hasActiveFilters && (
                        <div className="mb-4 flex flex-wrap gap-1">
                            {filters.status !== 'all' && (
                                <Badge className="flex h-6 items-center gap-1" variant="secondary">
                                    {filters.status.charAt(0).toUpperCase() + filters.status.slice(1).replace('-', ' ')}

                                    <Button
                                        className="ml-1 size-4 p-0"
                                        onClick={() => setFilters((prev) => ({...prev, status: 'all'}))}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <X className="size-3" />

                                        <span className="sr-only">Remove status filter</span>
                                    </Button>
                                </Badge>
                            )}

                            {filters.priority !== 'all' && (
                                <Badge className="flex h-6 items-center gap-1" variant="secondary">
                                    {`Priority: ${filters.priority.charAt(0).toUpperCase()} ${filters.priority.slice(1)}`}

                                    <Button
                                        className="ml-1 size-4 p-0"
                                        onClick={() => setFilters((prev) => ({...prev, priority: 'all'}))}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <X className="size-3" />

                                        <span className="sr-only">Remove priority filter</span>
                                    </Button>
                                </Badge>
                            )}

                            {filters.assignee !== 'all' && (
                                <Badge className="flex h-6 items-center gap-1" variant="secondary">
                                    {`Assignee: ${filters.assignee}`}

                                    <Button
                                        className="ml-1 size-4 p-0"
                                        onClick={() => setFilters((prev) => ({...prev, assignee: 'all'}))}
                                        size="icon"
                                        variant="ghost"
                                    >
                                        <X className="size-3" />

                                        <span className="sr-only">Remove assignee filter</span>
                                    </Button>
                                </Badge>
                            )}

                            <Button className="h-6 text-xs" onClick={resetFilters} size="sm" variant="ghost">
                                Clear all
                            </Button>
                        </div>
                    )}

                    {/* Search Bar with Autocomplete */}

                    <div className="relative">
                        <TooltipProvider>
                            <Tooltip open={showKeyboardShortcutTooltip}>
                                <TooltipTrigger asChild>
                                    <div className="relative w-full">
                                        <Search className="absolute left-3 top-1/2 z-10 size-4 -translate-y-1/2 text-muted-foreground" />

                                        <Input
                                            className="px-10"
                                            onChange={(e) => handleSearchChange(e.target.value)}
                                            onFocus={() => searchQuery.length > 0 && setShowSuggestions(true)}
                                            onKeyDown={handleKeyDown}
                                            placeholder={`Search tasks... (${shortcutText})`}
                                            ref={searchInputRef}
                                            value={searchQuery}
                                        />

                                        <div className="absolute right-3 top-1/2 -translate-y-1/2">
                                            <kbd className="hidden h-5 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium text-muted-foreground opacity-100 sm:inline-flex">
                                                <span className="text-xs">{shortcutText}</span>
                                            </kbd>
                                        </div>
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent side="bottom">
                                    <div className="flex items-center gap-2">
                                        <Keyboard className="size-4" />

                                        <span>Search shortcut activated!</span>
                                    </div>
                                </TooltipContent>
                            </Tooltip>
                        </TooltipProvider>

                        {/* Suggestions Dropdown */}

                        {showSuggestions && suggestions.length > 0 && (
                            <div className="absolute inset-x-0 top-full z-50 mt-1 max-h-48 overflow-y-auto rounded-md border border-border bg-background shadow-lg">
                                {suggestions.map((suggestion, index) => (
                                    <div
                                        className={`cursor-pointer px-3 py-2 text-sm transition-colors ${
                                            index === selectedSuggestionIndex
                                                ? 'bg-muted text-foreground'
                                                : 'text-muted-foreground hover:bg-muted/50 hover:text-foreground'
                                        }`}
                                        key={suggestion}
                                        onClick={() => handleSuggestionClick(suggestion)}
                                    >
                                        <div className="flex items-center gap-2">
                                            <Search className="size-3 shrink-0" />

                                            <span className="truncate">{highlightText(suggestion, searchQuery)}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Scrollable Task List */}

                <ScrollArea className="flex-1 overflow-hidden">
                    <div className="p-4 pt-0">
                        <div className="space-y-2">
                            {filteredTasks.length > 0 ? (
                                filteredTasks.map((task) => (
                                    <div
                                        className={`cursor-pointer rounded-lg border p-3 transition-colors hover:bg-muted/50 ${
                                            selectedTask === task.id
                                                ? 'border-primary bg-muted'
                                                : 'border-border bg-background'
                                        }`}
                                        key={task.id}
                                        onClick={() => {
                                            setSelectedTask(task.id);
                                            // Cancel editing when switching tasks
                                            if (isEditing) {
                                                setIsEditing(false);
                                                setEditingTask(null);
                                            }
                                        }}
                                    >
                                        <div className="flex w-full items-start gap-3">
                                            <div
                                                className="mt-0.5 shrink-0 cursor-pointer rounded-full p-1 transition-colors hover:bg-muted"
                                                onClick={(e) => handleStatusToggle(task.id, e)}
                                                title={`Click to change status (currently: ${task.status.replace('-', ' ')})`}
                                            >
                                                {getStatusIcon(task.status)}
                                            </div>

                                            <div className="min-w-0 flex-1 overflow-hidden">
                                                <div className="mb-1 flex flex-wrap items-center gap-2">
                                                    <h3 className="min-w-0 flex-1 truncate text-sm font-medium text-foreground">
                                                        {searchQuery
                                                            ? highlightText(task.title, searchQuery)
                                                            : task.title}
                                                    </h3>

                                                    <div className="flex shrink-0 items-center gap-1">
                                                        <Badge
                                                            className={`text-xs ${getPriorityColor(task.priority)}`}
                                                            variant="outline"
                                                        >
                                                            {task.priority}
                                                        </Badge>

                                                        {isTaskOverdue(task) && (
                                                            <Badge className="text-xs" variant="destructive">
                                                                <AlertTriangle className="mr-1 size-3" />
                                                                Overdue
                                                            </Badge>
                                                        )}
                                                    </div>
                                                </div>

                                                <p className="mb-2 line-clamp-2 break-words text-xs text-muted-foreground">
                                                    {searchQuery
                                                        ? highlightText(task.description, searchQuery)
                                                        : task.description}
                                                </p>

                                                <div className="flex items-center justify-between text-xs text-muted-foreground">
                                                    <span className="mr-2 min-w-0 flex-1 truncate">
                                                        {searchQuery
                                                            ? highlightText(task.assignee, searchQuery)
                                                            : task.assignee}
                                                    </span>

                                                    <div className="flex shrink-0 items-center gap-2">
                                                        {task.dueDate && (
                                                            <div className="flex items-center gap-1">
                                                                <Calendar className="size-3" />

                                                                <span>{formatDate(task.dueDate)}</span>
                                                            </div>
                                                        )}

                                                        {task.comments.length > 0 && (
                                                            <div className="flex items-center gap-1">
                                                                <MessageSquare className="size-3" />

                                                                <span>{task.comments.length}</span>
                                                            </div>
                                                        )}

                                                        {task.attachments.length > 0 && (
                                                            <div className="flex items-center gap-1">
                                                                <Paperclip className="size-3" />

                                                                <span>{task.attachments.length}</span>
                                                            </div>
                                                        )}

                                                        {task.dependencies.length > 0 && (
                                                            <div className="flex items-center gap-1">
                                                                <Link className="size-3" />

                                                                <span>{task.dependencies.length}</span>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="py-8 text-center">
                                    <Search className="mx-auto mb-2 size-8 text-muted-foreground" />

                                    <p className="text-sm font-medium text-foreground">No tasks found</p>

                                    <p className="text-xs text-muted-foreground">
                                        {searchQuery || hasActiveFilters
                                            ? 'Try adjusting your search or filter criteria'
                                            : 'No tasks match your criteria'}
                                    </p>
                                </div>
                            )}
                        </div>
                    </div>
                </ScrollArea>

                {/* Sidebar Footer */}

                <div className="border-t border-border bg-background p-4">
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                        <span>{tasks.length} total tasks</span>

                        <span>Task Manager v1.0</span>
                    </div>
                </div>
            </div>

            {/* Main Content */}

            <div className="flex-1 bg-background">
                {selectedTask ? (
                    <div className="p-6">
                        {(() => {
                            const task = tasks.find((t) => t.id === selectedTask);
                            if (!task) return null;

                            const displayTask = isEditing ? editingTask : task;

                            return (
                                <div className="w-full">
                                    {/* Header with Edit/Save/Cancel buttons */}

                                    <div className="mb-6 flex items-center justify-between">
                                        <div className="flex items-center gap-3">
                                            {getStatusIcon(displayTask?.status || '')}

                                            {isEditing ? (
                                                <div className="space-y-4">
                                                    <Input
                                                        className="h-auto border-none p-0 text-2xl font-semibold focus-visible:ring-0 focus-visible:ring-offset-0"
                                                        onChange={(e) => handleTaskFieldChange('title', e.target.value)}
                                                        placeholder="Task title"
                                                        value={editingTask?.title || ''}
                                                    />

                                                    <div>
                                                        <Label htmlFor="edit-priority">Priority</Label>

                                                        <Select
                                                            onValueChange={(value) =>
                                                                handleTaskFieldChange('priority', value)
                                                            }
                                                            value={editingTask?.priority || ''}
                                                        >
                                                            <SelectTrigger>
                                                                <SelectValue placeholder="Select priority" />
                                                            </SelectTrigger>

                                                            <SelectContent>
                                                                <SelectItem value="high">High priority</SelectItem>

                                                                <SelectItem value="medium">Medium priority</SelectItem>

                                                                <SelectItem value="low">Low priority</SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    </div>
                                                </div>
                                            ) : (
                                                <h1 className="text-2xl font-semibold text-foreground">
                                                    {searchQuery ? highlightText(task.title, searchQuery) : task.title}
                                                </h1>
                                            )}

                                            {isTaskOverdue(task) && (
                                                <Badge variant="destructive">
                                                    <AlertTriangle className="mr-1 size-3" />
                                                    Overdue
                                                </Badge>
                                            )}
                                        </div>

                                        <div className="flex items-center gap-2">
                                            {isEditing ? (
                                                <>
                                                    <Button
                                                        className="flex items-center gap-2"
                                                        onClick={handleSaveTask}
                                                        size="sm"
                                                    >
                                                        <Save className="size-4" />
                                                        Save
                                                    </Button>
                                                    <Button
                                                        className="flex items-center gap-2"
                                                        onClick={handleCancelEdit}
                                                        size="sm"
                                                        variant="outline"
                                                    >
                                                        <XCircle className="size-4" />
                                                        Cancel
                                                    </Button>
                                                </>
                                            ) : (
                                                <Button
                                                    className="flex items-center gap-2"
                                                    onClick={handleEditTask}
                                                    size="sm"
                                                    variant="outline"
                                                >
                                                    <Edit className="size-4" />
                                                    Edit
                                                </Button>
                                            )}
                                        </div>
                                    </div>

                                    {/* Task Details in Clean Layout */}

                                    <div className="space-y-6">
                                        <div>
                                            <h2 className="mb-3 text-lg font-medium text-foreground">Description</h2>

                                            {isEditing ? (
                                                <Textarea
                                                    className="min-h-[100px]"
                                                    onChange={(e) =>
                                                        handleTaskFieldChange('description', e.target.value)
                                                    }
                                                    placeholder="Task description"
                                                    value={editingTask?.description || ''}
                                                />
                                            ) : (
                                                <p className="text-muted-foreground">
                                                    {searchQuery
                                                        ? highlightText(task.description, searchQuery)
                                                        : task.description}
                                                </p>
                                            )}
                                        </div>

                                        <div className="grid grid-cols-2 gap-8">
                                            <div className="space-y-6">
                                                <div>
                                                    <h3 className="mb-2 text-lg font-medium text-foreground">Status</h3>

                                                    {isEditing ? (
                                                        <Select
                                                            onValueChange={(value) =>
                                                                handleTaskFieldChange('status', value)
                                                            }
                                                            value={editingTask?.status || ''}
                                                        >
                                                            <SelectTrigger>
                                                                <SelectValue placeholder="Select status" />
                                                            </SelectTrigger>

                                                            <SelectContent>
                                                                <SelectItem value="open">
                                                                    <div className="flex items-center gap-2">
                                                                        <Circle className="size-3 text-gray-400" />
                                                                        Open
                                                                    </div>
                                                                </SelectItem>

                                                                <SelectItem value="in-progress">
                                                                    <div className="flex items-center gap-2">
                                                                        <Clock className="size-3 text-blue-600" />
                                                                        In Progress
                                                                    </div>
                                                                </SelectItem>

                                                                <SelectItem value="completed">
                                                                    <div className="flex items-center gap-2">
                                                                        <CheckCircle2 className="size-3 text-green-600" />
                                                                        Completed
                                                                    </div>
                                                                </SelectItem>
                                                            </SelectContent>
                                                        </Select>
                                                    ) : (
                                                        <div className="flex items-center gap-2">
                                                            {getStatusIcon(task.status)}

                                                            <span className="capitalize text-muted-foreground">
                                                                {task.status.replace('-', ' ')}
                                                            </span>
                                                        </div>
                                                    )}
                                                </div>

                                                <div>
                                                    <h3 className="mb-2 text-lg font-medium text-foreground">
                                                        Created
                                                    </h3>

                                                    <p className="text-muted-foreground">
                                                        {formatDate(task.createdAt)}
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="space-y-6">
                                                <div>
                                                    <h3 className="mb-2 text-lg font-medium text-foreground">
                                                        Assignee
                                                    </h3>

                                                    {isEditing ? (
                                                        <Select
                                                            onValueChange={(value) =>
                                                                handleTaskFieldChange('assignee', value)
                                                            }
                                                            value={editingTask?.assignee || ''}
                                                        >
                                                            <SelectTrigger>
                                                                <SelectValue placeholder="Select assignee" />
                                                            </SelectTrigger>

                                                            <SelectContent>
                                                                {availableAssignees.map((assignee) => (
                                                                    <SelectItem key={assignee} value={assignee}>
                                                                        <div className="flex items-center gap-2">
                                                                            <User className="size-3 text-muted-foreground" />

                                                                            {assignee}
                                                                        </div>
                                                                    </SelectItem>
                                                                ))}
                                                            </SelectContent>
                                                        </Select>
                                                    ) : (
                                                        <p className="text-muted-foreground">
                                                            {searchQuery
                                                                ? highlightText(task.assignee, searchQuery)
                                                                : task.assignee}
                                                        </p>
                                                    )}
                                                </div>

                                                <div>
                                                    <h3 className="mb-2 text-lg font-medium text-foreground">
                                                        Priority
                                                    </h3>

                                                    {!isEditing && (
                                                        <Badge
                                                            className={getPriorityColor(task.priority)}
                                                            variant="outline"
                                                        >
                                                            {task.priority}
                                                        </Badge>
                                                    )}
                                                </div>
                                            </div>
                                        </div>

                                        {/* Comments Section */}

                                        <div>
                                            <h3 className="mb-4 text-lg font-medium text-foreground">
                                                Comments ({task.comments.length})
                                            </h3>

                                            <div className="space-y-4">
                                                {task.comments.length > 0 ? (
                                                    task.comments.map((comment) => (
                                                        <Card key={comment.id}>
                                                            <CardContent className="p-4">
                                                                <div className="flex items-start gap-3">
                                                                    <Avatar className="size-8">
                                                                        <AvatarFallback className="text-xs">
                                                                            {getInitials(comment.author)}
                                                                        </AvatarFallback>
                                                                    </Avatar>

                                                                    <div className="flex-1">
                                                                        <div className="mb-1 flex items-center gap-2">
                                                                            <span className="text-sm font-medium">
                                                                                {comment.author}
                                                                            </span>

                                                                            <span className="text-xs text-muted-foreground">
                                                                                {formatTimestamp(comment.timestamp)}
                                                                            </span>
                                                                        </div>

                                                                        <p className="text-sm text-muted-foreground">
                                                                            {comment.content}
                                                                        </p>
                                                                    </div>
                                                                </div>
                                                            </CardContent>
                                                        </Card>
                                                    ))
                                                ) : (
                                                    <div className="py-8 text-center">
                                                        <MessageSquare className="mx-auto mb-2 size-8 text-muted-foreground" />

                                                        <p className="text-sm font-medium text-foreground">
                                                            No comments yet
                                                        </p>

                                                        <p className="text-xs text-muted-foreground">
                                                            Be the first to add a comment
                                                        </p>
                                                    </div>
                                                )}

                                                {/* Add Comment */}

                                                <Card>
                                                    <CardContent className="p-4">
                                                        <div className="flex items-start gap-3">
                                                            <Avatar className="size-8">
                                                                <AvatarFallback className="text-xs">CU</AvatarFallback>
                                                            </Avatar>

                                                            <div className="flex-1 space-y-2">
                                                                <Textarea
                                                                    className="min-h-[80px]"
                                                                    onChange={(e) => setNewComment(e.target.value)}
                                                                    placeholder="Add a comment..."
                                                                    value={newComment}
                                                                />

                                                                <div className="flex justify-end">
                                                                    <Button
                                                                        className="flex items-center gap-2"
                                                                        disabled={!newComment.trim()}
                                                                        onClick={handleAddComment}
                                                                        size="sm"
                                                                    >
                                                                        <Send className="size-3" />
                                                                        Add Comment
                                                                    </Button>
                                                                </div>
                                                            </div>
                                                        </div>
                                                    </CardContent>
                                                </Card>
                                            </div>
                                        </div>

                                        {/* Attachments Section */}

                                        {task.attachments.length > 0 && (
                                            <div>
                                                <h3 className="mb-4 text-lg font-medium text-foreground">
                                                    Attachments ({task.attachments.length})
                                                </h3>

                                                <div className="space-y-2">
                                                    {task.attachments.map((attachment) => (
                                                        <Card key={attachment.id}>
                                                            <CardContent className="p-4">
                                                                <div className="flex items-center justify-between">
                                                                    <div className="flex items-center gap-3">
                                                                        <FileText className="size-4 text-muted-foreground" />

                                                                        <div>
                                                                            <p className="text-sm font-medium">
                                                                                {attachment.name}
                                                                            </p>

                                                                            <p className="text-xs text-muted-foreground">
                                                                                $
                                                                                {`${attachment.size} • Uploaded by ${attachment.uploadedBy} ${formatTimestamp(attachment.uploadedAt)}`}
                                                                            </p>
                                                                        </div>
                                                                    </div>

                                                                    <div className="flex items-center gap-2">
                                                                        <Button size="sm" variant="ghost">
                                                                            <Download className="size-3" />
                                                                        </Button>

                                                                        <Button
                                                                            onClick={() =>
                                                                                handleRemoveAttachment(attachment.id)
                                                                            }
                                                                            size="sm"
                                                                            variant="ghost"
                                                                        >
                                                                            <XCircle className="size-3" />
                                                                        </Button>
                                                                    </div>
                                                                </div>
                                                            </CardContent>
                                                        </Card>
                                                    ))}
                                                </div>
                                            </div>
                                        )}

                                        {/* Dependencies Section */}

                                        {task.dependencies.length > 0 && (
                                            <div>
                                                <h3 className="mb-4 text-lg font-medium text-foreground">
                                                    Dependencies ({task.dependencies.length})
                                                </h3>

                                                <div className="space-y-2">
                                                    {task.dependencies.map((depId) => {
                                                        const depTask = tasks.find((t) => t.id === depId);
                                                        if (!depTask) return null;
                                                        return (
                                                            <Card key={depId}>
                                                                <CardContent className="p-4">
                                                                    <div className="flex items-center gap-3">
                                                                        {getStatusIcon(depTask.status)}

                                                                        <div className="flex-1">
                                                                            <p className="text-sm font-medium">
                                                                                {depTask.title}
                                                                            </p>

                                                                            <p className="text-xs text-muted-foreground">
                                                                                {`Assigned to • ${depTask.assignee} ${depTask.status.replace('-', ' ')}`}
                                                                            </p>
                                                                        </div>

                                                                        <Badge
                                                                            className={getPriorityColor(
                                                                                depTask.priority
                                                                            )}
                                                                            variant="outline"
                                                                        >
                                                                            {depTask.priority}
                                                                        </Badge>
                                                                    </div>
                                                                </CardContent>
                                                            </Card>
                                                        );
                                                    })}
                                                </div>
                                            </div>
                                        )}

                                        {/* Upload Attachments */}

                                        <Card>
                                            <CardContent className="p-4">
                                                <div className="flex items-center justify-center">
                                                    <input
                                                        className="hidden"
                                                        multiple
                                                        onChange={handleFileUpload}
                                                        ref={fileInputRef}
                                                        type="file"
                                                    />

                                                    <Button
                                                        className="flex items-center gap-2"
                                                        onClick={() => fileInputRef.current?.click()}
                                                        variant="outline"
                                                    >
                                                        <Paperclip className="size-4" />
                                                        Upload Files
                                                    </Button>
                                                </div>
                                            </CardContent>
                                        </Card>
                                    </div>
                                </div>
                            );
                        })()}
                    </div>
                ) : (
                    <div className="flex h-full items-center justify-center">
                        <div className="text-center">
                            <h2 className="mb-2 text-lg font-medium text-foreground">Select a task</h2>

                            <p className="text-sm text-muted-foreground">
                                Choose a task from the sidebar to view its details
                            </p>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}
