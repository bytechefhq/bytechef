# Tasks Page Refactoring Design

## Overview

Refactor `tasks.tsx` (~2400 lines) into smaller, maintainable components following the patterns established in `datatable/` directory.

## Directory Structure

```
client/src/pages/automation/tasks/
├── Tasks.tsx                    # Main page (thin, ~80 lines)
├── useTasks.ts                  # Main hook with all state/logic
├── types.ts                     # Shared interfaces
├── components/
│   ├── TaskList.tsx             # Sidebar with task cards
│   ├── TaskDetail.tsx           # Right panel task details
│   ├── TaskSearch.tsx           # Search with autocomplete
│   ├── TaskFilters.tsx          # Filter dropdown menu
│   ├── TaskSortMenu.tsx         # Sort dropdown menu
│   ├── TaskCard.tsx             # Individual task card in list
│   ├── TaskComments.tsx         # Comments section
│   ├── TaskAttachments.tsx      # Attachments section
│   ├── TaskDependencies.tsx     # Dependencies section
│   └── dialogs/
│       ├── TaskCreateDialog.tsx # Create task modal
│       └── useTaskCreateDialog.ts # Dialog state hook
└── tests/
    ├── Tasks.test.tsx
    ├── useTasks.test.ts
    └── components/
        └── TaskList.test.tsx
```

## Main Hook Interface (`useTasks.ts`)

```typescript
interface UseTasksReturn {
    // Data
    tasks: TaskI[];
    filteredTasks: TaskI[];
    selectedTask: TaskI | null;

    // Selection & Editing
    setSelectedTask: (id: string | null) => void;
    isEditing: boolean;
    editingTask: TaskI | null;
    handleEditTask: () => void;
    handleSaveTask: () => void;
    handleCancelEdit: () => void;
    handleTaskFieldChange: (field: keyof TaskI, value: string | string[]) => void;

    // Search & Filters
    searchQuery: string;
    setSearchQuery: (query: string) => void;
    filters: FiltersI;
    setFilters: (filters: FiltersI) => void;
    sortBy: SortOptionType;
    setSortBy: (sort: SortOptionType) => void;
    sortDirection: SortDirectionType;
    setSortDirection: (dir: SortDirectionType) => void;
    hasActiveFilters: boolean;
    resetFilters: () => void;

    // Comments & Attachments
    handleAddComment: (content: string) => void;
    handleFileUpload: (files: FileList) => void;
    handleRemoveAttachment: (id: string) => void;

    // Status
    handleStatusToggle: (taskId: string) => void;

    // Counts & Metadata
    taskCounts: TaskCountsI;
    priorityCounts: PriorityCountsI;
    assignees: string[];
    availableAssignees: string[];
}
```

## Component Responsibilities

### `Tasks.tsx` (~80 lines)

- Calls `useTasks()` hook
- Composes layout: sidebar + main content
- Passes props to child components
- No direct state management

### `TaskList.tsx` (~150 lines)

- Receives: `tasks`, `selectedTask`, `onSelectTask`, `onStatusToggle`, `searchQuery`
- Renders: ScrollArea with TaskCard list, empty state
- Handles: Task selection clicks

### `TaskCard.tsx` (~80 lines)

- Receives: `task`, `isSelected`, `onSelect`, `onStatusToggle`, `searchQuery`
- Renders: Single task card with status icon, title, badges, metadata
- Handles: Status toggle click, highlight matching text

### `TaskSearch.tsx` (~120 lines)

- Receives: `searchQuery`, `onSearchChange`, `tasks`, `filters`
- Renders: Input with suggestions dropdown, keyboard shortcut badge
- Handles: Autocomplete, keyboard navigation, Cmd+K shortcut

### `TaskFilters.tsx` (~180 lines)

- Receives: `filters`, `onFiltersChange`, `taskCounts`, `priorityCounts`, `assignees`
- Renders: DropdownMenu with status/priority/assignee sections
- Handles: Filter selection, clear all

### `TaskSortMenu.tsx` (~60 lines)

- Receives: `sortBy`, `sortDirection`, `onSortChange`
- Renders: DropdownMenu with sort options
- Handles: Sort selection, direction toggle

### `TaskDetail.tsx` (~200 lines)

- Receives: `task`, `isEditing`, `editingTask`, `onEdit`, `onSave`, `onCancel`, `onFieldChange`, `searchQuery`, `availableAssignees`
- Renders: Header with edit/save buttons, description, status/assignee grid
- Composes: TaskComments, TaskAttachments, TaskDependencies
- Handles: Edit mode toggle, field updates

### `TaskComments.tsx` (~100 lines)

- Receives: `comments`, `onAddComment`
- Renders: Comment list with avatars, timestamps; add comment form
- Handles: New comment submission

### `TaskAttachments.tsx` (~80 lines)

- Receives: `attachments`, `onUpload`, `onRemove`
- Renders: Attachment cards with download/remove buttons, upload button
- Handles: File input trigger, remove click

### `TaskDependencies.tsx` (~60 lines)

- Receives: `dependencies`, `allTasks`
- Renders: Linked task cards with status/priority badges

### `dialogs/TaskCreateDialog.tsx` (~250 lines)

- Uses: `useTaskCreateDialog()` hook
- Renders: Dialog with form fields
- Handles: Form validation, template application, submission

### `dialogs/useTaskCreateDialog.ts` (~80 lines)

- Manages: Dialog open/close state, form state, validation errors
- Returns: `isOpen`, `openDialog`, `closeDialog`, `form`, `errors`, `handleSubmit`, `applyTemplate`

## Types (`types.ts`)

```typescript
// Filter & Sort Types
export type StatusFilterType = 'all' | 'open' | 'in-progress' | 'completed';
export type PriorityFilterType = 'all' | 'high' | 'medium' | 'low';
export type AssigneeFilterType = 'all' | string;
export type SortOptionType = 'created' | 'title' | 'priority' | 'status' | 'assignee' | 'dueDate';
export type SortDirectionType = 'asc' | 'desc';

// Data Interfaces
export interface TaskCommentI {
    id: string;
    author: string;
    content: string;
    timestamp: string;
}

export interface TaskAttachmentI {
    id: string;
    name: string;
    size: string;
    type: string;
    uploadedBy: string;
    uploadedAt: string;
}

export interface TaskI {
    id: string;
    title: string;
    description: string;
    status: 'open' | 'in-progress' | 'completed';
    priority: 'high' | 'medium' | 'low';
    assignee: string;
    createdAt: string;
    dueDate?: string;
    comments: TaskCommentI[];
    attachments: TaskAttachmentI[];
    dependencies: string[];
    version?: number;
}

export interface FiltersI {
    status: StatusFilterType;
    priority: PriorityFilterType;
    assignee: AssigneeFilterType;
}

export interface TaskCountsI {
    all: number;
    open: number;
    'in-progress': number;
    completed: number;
}

export interface PriorityCountsI {
    all: number;
    high: number;
    medium: number;
    low: number;
}

export interface TaskTemplateI {
    id: string;
    name: string;
    description: string;
    defaultStatus: 'open' | 'in-progress' | 'completed';
    defaultPriority: 'high' | 'medium' | 'low';
    defaultAssignee?: string;
    estimatedDuration?: string;
    checklist: string[];
}
```

## Utilities (inside `useTasks.ts`)

```typescript
// Status/Priority helpers
const cycleTaskStatus = (status: TaskI['status']): TaskI['status'] => {...};
const getStatusIcon = (status: string) => {...};
const getPriorityColor = (priority: string) => {...};

// Date formatting
const formatDate = (dateString: string) => {...};
const formatTimestamp = (timestamp: string) => {...};
const isTaskOverdue = (task: TaskI) => {...};

// ID generation
const generateTaskId = (tasks: TaskI[]) => {...};
const generateCommentId = (tasks: TaskI[]) => {...};
const generateAttachmentId = (tasks: TaskI[]) => {...};

// Text utilities
const getInitials = (name: string) => {...};
const highlightText = (text: string, query: string) => {...};
```

## Constants (inside `useTasks.ts`)

```typescript
const AVAILABLE_ASSIGNEES = [
  'John Doe', 'Jane Smith', 'Mike Johnson', 'Sarah Wilson',
  'Tom Brown', 'Lisa Davis', 'Alex Chen', 'Emma Wilson',
];

const TASK_TEMPLATES: TaskTemplateI[] = [...];
```

## Migration Steps

1. Create `types.ts` with all interfaces
2. Create `useTasks.ts` extracting logic from current file
3. Create components one by one, importing from `useTasks`
4. Replace `tasks.tsx` content with thin `Tasks.tsx`
5. Delete old `tasks.tsx` (rename handled by git)

## File Summary

| File                                        | Lines (est.) | Purpose                  |
| ------------------------------------------- | ------------ | ------------------------ |
| `Tasks.tsx`                                 | ~80          | Main page layout         |
| `useTasks.ts`                               | ~400         | All state/logic          |
| `types.ts`                                  | ~80          | Shared interfaces        |
| `components/TaskList.tsx`                   | ~150         | Sidebar task list        |
| `components/TaskCard.tsx`                   | ~80          | Individual task card     |
| `components/TaskSearch.tsx`                 | ~120         | Search with autocomplete |
| `components/TaskFilters.tsx`                | ~180         | Filter dropdown          |
| `components/TaskSortMenu.tsx`               | ~60          | Sort dropdown            |
| `components/TaskDetail.tsx`                 | ~200         | Task detail panel        |
| `components/TaskComments.tsx`               | ~100         | Comments section         |
| `components/TaskAttachments.tsx`            | ~80          | Attachments section      |
| `components/TaskDependencies.tsx`           | ~60          | Dependencies section     |
| `components/dialogs/TaskCreateDialog.tsx`   | ~250         | Create task modal        |
| `components/dialogs/useTaskCreateDialog.ts` | ~80          | Dialog hook              |
