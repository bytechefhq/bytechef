# Tasks Hooks Split Design

## Overview

Split `useTasks.ts` (1120 lines) into component-mirroring hooks, passing tasks as props.

## Directory Structure

```
client/src/pages/automation/tasks/
├── Tasks.tsx                    # Main page (uses all hooks)
├── hooks/
│   ├── useTasks.ts             # Core: tasks state, selectedTask, API queries/mutations
│   ├── useTaskList.ts          # TaskList: filters, sort, search, suggestions
│   └── useTaskDetail.ts        # TaskDetail: editing, comments, attachments
├── components/
│   └── dialogs/
│       └── useTaskCreateDialog.ts  # Already exists, stays here
├── types.ts
└── components/...
```

## Data Flow

```
Tasks.tsx
  ├── useTasks() → { tasks, selectedTask, selectedTaskObject, ... }
  │     ↓ passes tasks
  ├── useTaskList(tasks) → { filters, sort, search, ... }
  │     ↓ passes tasks, selectedTaskObject
  └── useTaskDetail(tasks, selectedTaskObject) → { editing, comments, ... }
```

## Hook Interfaces

### useTasks.ts (Core Hook)

```typescript
interface UseTasksReturnI {
    // Task data
    tasks: TaskI[];

    // Selection
    selectedTask: string | null;
    selectedTaskObject: TaskI | null;
    setSelectedTask: (taskId: string | null) => void;

    // API mutations
    createTask: (task: Omit<TaskI, 'id' | 'createdAt' | 'comments' | 'attachments'>) => void;
    updateTask: (task: TaskI) => void;

    // Task modifications (local state updates)
    addCommentToTask: (taskId: string, comment: TaskCommentI) => void;
    addAttachmentToTask: (taskId: string, attachment: TaskAttachmentI) => void;
    removeAttachmentFromTask: (taskId: string, attachmentId: string) => void;
    updateTaskStatus: (taskId: string, status: TaskI['status']) => void;

    // Constants
    availableAssignees: string[];
    taskTemplates: TaskTemplateI[];

    // Utility functions (shared across components)
    formatDate: (dateString: string) => string;
    formatTimestamp: (timestamp: string) => string;
    getInitials: (name: string) => string;
    isTaskOverdue: (task: TaskI) => boolean;
    getStatusIcon: (status: string) => 'completed' | 'in-progress' | 'open';
    getPriorityColor: (priority: string) => string;
}
```

### useTaskList.ts

```typescript
interface UseTaskListParamsI {
    tasks: TaskI[];
}

interface UseTaskListReturnI {
    // Filtered/sorted results
    filteredTasks: TaskI[];

    // Search
    searchQuery: string;
    setSearchQuery: (query: string) => void;
    suggestions: string[];
    handleSearchChange: (value: string) => void;
    handleSuggestionClick: (suggestion: string) => void;
    handleKeyDown: (event: React.KeyboardEvent) => void;
    showSuggestions: boolean;
    selectedSuggestionIndex: number;

    // Filters
    filters: FiltersI;
    setFilters: React.Dispatch<React.SetStateAction<FiltersI>>;
    resetFilters: () => void;
    hasActiveFilters: boolean;

    // Sort
    sortBy: SortOptionType;
    sortDirection: SortDirectionType;
    setSortBy: (sort: SortOptionType) => void;
    setSortDirection: (direction: SortDirectionType) => void;

    // Computed counts
    taskCounts: TaskCountsI;
    priorityCounts: PriorityCountsI;
    assignees: string[];
    assigneeCounts: Record<string, number>;

    // Display helpers
    getHeaderText: () => string;
    highlightText: (text: string, query: string) => React.ReactNode;
}
```

### useTaskDetail.ts

```typescript
interface UseTaskDetailParamsI {
    tasks: TaskI[];
    selectedTask: TaskI | null;
    onUpdateTask: (task: TaskI) => void;
    onAddComment: (taskId: string, comment: TaskCommentI) => void;
    onAddAttachment: (taskId: string, attachment: TaskAttachmentI) => void;
    onRemoveAttachment: (taskId: string, attachmentId: string) => void;
}

interface UseTaskDetailReturnI {
    // Editing state
    isEditing: boolean;
    editingTask: TaskI | null;
    handleEditTask: () => void;
    handleSaveTask: () => void;
    handleCancelEdit: () => void;
    handleTaskFieldChange: (field: keyof TaskI, value: string | string[]) => void;

    // Comments
    newComment: string;
    setNewComment: (comment: string) => void;
    handleAddComment: () => void;

    // Attachments
    fileInputRef: React.RefObject<HTMLInputElement | null>;
    handleFileUpload: (event: React.ChangeEvent<HTMLInputElement>) => void;
    handleRemoveAttachment: (attachmentId: string) => void;
}
```

## Tasks.tsx After Refactoring

```typescript
export default function Tasks() {
    const tasksHook = useTasks();
    const taskList = useTaskList({ tasks: tasksHook.tasks });
    const taskDetail = useTaskDetail({
        tasks: tasksHook.tasks,
        selectedTask: tasksHook.selectedTaskObject,
        onUpdateTask: tasksHook.updateTask,
        onAddComment: tasksHook.addCommentToTask,
        onAddAttachment: tasksHook.addAttachmentToTask,
        onRemoveAttachment: tasksHook.removeAttachmentFromTask,
    });
    const dialogHook = useTaskCreateDialog();

    return (
        <div className="flex h-screen bg-background">
            <TaskList {...taskList} {...tasksHook} onOpenCreateDialog={dialogHook.openDialog} />
            <TaskDetail {...taskDetail} {...tasksHook} />
            <TaskCreateDialog {...dialogHook} {...tasksHook} />
        </div>
    );
}
```

## Migration Steps

1. Create `hooks/` directory
2. Create `hooks/useTasks.ts` (core hook)
3. Create `hooks/useTaskList.ts`
4. Create `hooks/useTaskDetail.ts`
5. Update `Tasks.tsx` to use new hooks
6. Update component props interfaces to accept spread props
7. Delete old `useTasks.ts`
8. Run lint/typecheck/format
