# Tasks Page Refactoring Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Refactor `tasks.tsx` (~2400 lines) into 14 smaller, focused files following datatable/ patterns.

**Architecture:** Extract all types to `types.ts`, all logic to `useTasks.ts` hook, and split UI into presentational components in `components/` directory. Main `Tasks.tsx` becomes thin orchestrator.

**Tech Stack:** React 19, TypeScript 5.9, TailwindCSS, Lucide icons, shadcn/ui components

---

## Task 1: Create types.ts

**Files:**

- Create: `client/src/pages/automation/tasks/types.ts`

**Step 1: Create the types file**

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

export interface NewTaskFormI {
    title: string;
    description: string;
    status: 'open' | 'in-progress' | 'completed';
    priority: 'high' | 'medium' | 'low';
    assignee: string;
    dueDate: string;
    dependencies: string[];
    templateId?: string;
}
```

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/types.ts
git commit -m "feat(tasks): add types.ts with shared interfaces"
```

---

## Task 2: Create useTasks.ts hook

**Files:**

- Create: `client/src/pages/automation/tasks/useTasks.ts`

**Step 1: Create the main hook file**

Extract all state, logic, and handlers from tasks.tsx into a custom hook. The hook will:

- Import types from `./types`
- Import mutations from `@/shared/mutations/automation/tasks.mutations`
- Manage all useState hooks
- Contain all utility functions (cycleTaskStatus, formatDate, etc.)
- Contain all handler functions
- Return a clean interface

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/useTasks.ts
git commit -m "feat(tasks): add useTasks hook with all state and logic"
```

---

## Task 3: Create components directory structure

**Files:**

- Create: `client/src/pages/automation/tasks/components/` directory
- Create: `client/src/pages/automation/tasks/components/dialogs/` directory

**Step 1: Create directories**

```bash
mkdir -p client/src/pages/automation/tasks/components/dialogs
```

**Step 2: Commit**

```bash
git commit --allow-empty -m "chore(tasks): create components directory structure"
```

---

## Task 4: Create TaskCard.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskCard.tsx`

**Step 1: Create TaskCard component**

Extract the task card rendering logic (lines 1838-1938 from original). Component receives:

- `task: TaskI`
- `isSelected: boolean`
- `onSelect: () => void`
- `onStatusToggle: (e: React.MouseEvent) => void`
- `searchQuery: string`
- `getStatusIcon: (status: string) => JSX.Element`
- `getPriorityColor: (priority: string) => string`
- `isTaskOverdue: (task: TaskI) => boolean`
- `highlightText: (text: string, query: string) => React.ReactNode`
- `formatDate: (date: string) => string`

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskCard.tsx
git commit -m "feat(tasks): add TaskCard component"
```

---

## Task 5: Create TaskSearch.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskSearch.tsx`

**Step 1: Create TaskSearch component**

Extract search input with autocomplete (lines 1770-1828 from original). Component receives:

- `searchQuery: string`
- `onSearchChange: (value: string) => void`
- `suggestions: string[]`
- `highlightText: (text: string, query: string) => React.ReactNode`

Manages internally:

- `showSuggestions` state
- `selectedSuggestionIndex` state
- `showKeyboardShortcutTooltip` state
- Keyboard shortcut (Cmd+K) effect
- Suggestion navigation

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskSearch.tsx
git commit -m "feat(tasks): add TaskSearch component with autocomplete"
```

---

## Task 6: Create TaskFilters.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskFilters.tsx`

**Step 1: Create TaskFilters component**

Extract filter dropdown menu (lines 1487-1712 from original). Component receives:

- `filters: FiltersI`
- `onFiltersChange: (filters: FiltersI) => void`
- `taskCounts: TaskCountsI`
- `priorityCounts: PriorityCountsI`
- `assignees: string[]`
- `assigneeCounts: Record<string, number>`
- `hasActiveFilters: boolean`
- `onResetFilters: () => void`

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskFilters.tsx
git commit -m "feat(tasks): add TaskFilters dropdown component"
```

---

## Task 7: Create TaskSortMenu.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskSortMenu.tsx`

**Step 1: Create TaskSortMenu component**

Extract sort dropdown menu (lines 1441-1485 from original). Component receives:

- `sortBy: SortOptionType`
- `sortDirection: SortDirectionType`
- `onSortChange: (sortBy: SortOptionType, direction: SortDirectionType) => void`

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskSortMenu.tsx
git commit -m "feat(tasks): add TaskSortMenu dropdown component"
```

---

## Task 8: Create TaskList.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskList.tsx`

**Step 1: Create TaskList component**

Compose sidebar with TaskCard list. Component receives all props needed by TaskSearch, TaskFilters, TaskSortMenu, and TaskCard. Renders:

- Header with title and task count
- Create task button (triggers dialog)
- TaskSortMenu
- TaskFilters
- Active filter badges
- TaskSearch
- ScrollArea with TaskCard list
- Footer with total count

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskList.tsx
git commit -m "feat(tasks): add TaskList sidebar component"
```

---

## Task 9: Create TaskComments.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskComments.tsx`

**Step 1: Create TaskComments component**

Extract comments section (lines 2210-2292 from original). Component receives:

- `comments: TaskCommentI[]`
- `onAddComment: (content: string) => void`
- `formatTimestamp: (timestamp: string) => string`
- `getInitials: (name: string) => string`

Manages internally:

- `newComment` state for the input

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskComments.tsx
git commit -m "feat(tasks): add TaskComments component"
```

---

## Task 10: Create TaskAttachments.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskAttachments.tsx`

**Step 1: Create TaskAttachments component**

Extract attachments section (lines 2295-2413 from original). Component receives:

- `attachments: TaskAttachmentI[]`
- `onUpload: (event: React.ChangeEvent<HTMLInputElement>) => void`
- `onRemove: (attachmentId: string) => void`
- `formatTimestamp: (timestamp: string) => string`

Manages internally:

- `fileInputRef` for file upload

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskAttachments.tsx
git commit -m "feat(tasks): add TaskAttachments component"
```

---

## Task 11: Create TaskDependencies.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskDependencies.tsx`

**Step 1: Create TaskDependencies component**

Extract dependencies section (lines 2346-2388 from original). Component receives:

- `dependencies: string[]`
- `allTasks: TaskI[]`
- `getStatusIcon: (status: string) => JSX.Element`
- `getPriorityColor: (priority: string) => string`

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskDependencies.tsx
git commit -m "feat(tasks): add TaskDependencies component"
```

---

## Task 12: Create TaskDetail.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/TaskDetail.tsx`

**Step 1: Create TaskDetail component**

Extract main content panel (lines 1971-2418 from original). Component receives:

- `task: TaskI | null`
- `isEditing: boolean`
- `editingTask: TaskI | null`
- `onEdit: () => void`
- `onSave: () => void`
- `onCancel: () => void`
- `onFieldChange: (field: keyof TaskI, value: string | string[]) => void`
- `onAddComment: (content: string) => void`
- `onFileUpload: (event: React.ChangeEvent<HTMLInputElement>) => void`
- `onRemoveAttachment: (id: string) => void`
- `searchQuery: string`
- `availableAssignees: string[]`
- `allTasks: TaskI[]`
- Utility functions (getStatusIcon, getPriorityColor, etc.)

Composes: TaskComments, TaskAttachments, TaskDependencies

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/TaskDetail.tsx
git commit -m "feat(tasks): add TaskDetail panel component"
```

---

## Task 13: Create useTaskCreateDialog.ts

**Files:**

- Create: `client/src/pages/automation/tasks/components/dialogs/useTaskCreateDialog.ts`

**Step 1: Create dialog hook**

Extract dialog state management. Hook manages:

- `isOpen` state
- `form` state (NewTaskFormI)
- `errors` state
- `openDialog` / `closeDialog` handlers
- `handleFormChange` handler
- `applyTemplate` handler
- `validateForm` function
- `handleSubmit` handler

Returns interface for TaskCreateDialog to consume.

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/dialogs/useTaskCreateDialog.ts
git commit -m "feat(tasks): add useTaskCreateDialog hook"
```

---

## Task 14: Create TaskCreateDialog.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/components/dialogs/TaskCreateDialog.tsx`

**Step 1: Create TaskCreateDialog component**

Extract create task dialog (lines 1138-1437 from original). Component receives:

- `isOpen: boolean`
- `onOpenChange: (open: boolean) => void`
- `form: NewTaskFormI`
- `errors: Partial<NewTaskFormI>`
- `onFormChange: (field: keyof NewTaskFormI, value: string | string[]) => void`
- `onSubmit: () => void`
- `onClose: () => void`
- `onApplyTemplate: (templateId: string) => void`
- `templates: TaskTemplateI[]`
- `availableAssignees: string[]`
- `availableTasks: TaskI[]` (for dependencies)

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/components/dialogs/TaskCreateDialog.tsx
git commit -m "feat(tasks): add TaskCreateDialog component"
```

---

## Task 15: Create main Tasks.tsx

**Files:**

- Create: `client/src/pages/automation/tasks/Tasks.tsx`

**Step 1: Create thin main component**

```typescript
'use client';

import {useTasks} from './useTasks';
import {TaskList} from './components/TaskList';
import {TaskDetail} from './components/TaskDetail';
import {TaskCreateDialog} from './components/dialogs/TaskCreateDialog';

export default function Tasks() {
    const {
        // Data
        tasks,
        filteredTasks,
        selectedTask,
        // ... all other props from useTasks
    } = useTasks();

    return (
        <div className="flex h-screen bg-background">
            <TaskList
                // ... props
            />
            <TaskDetail
                // ... props
            />
            <TaskCreateDialog
                // ... props
            />
        </div>
    );
}
```

**Step 2: Commit**

```bash
git add client/src/pages/automation/tasks/Tasks.tsx
git commit -m "feat(tasks): add thin Tasks.tsx main component"
```

---

## Task 16: Delete old tasks.tsx

**Files:**

- Delete: `client/src/pages/automation/tasks/tasks.tsx`

**Step 1: Remove old file**

```bash
git rm client/src/pages/automation/tasks/tasks.tsx
```

**Step 2: Commit**

```bash
git commit -m "refactor(tasks): remove old monolithic tasks.tsx"
```

---

## Task 17: Verify and format

**Step 1: Run format check**

```bash
cd client && npm run format
```

**Step 2: Run lint**

```bash
npm run lint
```

**Step 3: Run typecheck**

```bash
npm run typecheck
```

**Step 4: Fix any issues found**

Address any lint or type errors.

**Step 5: Commit fixes**

```bash
git add -A
git commit -m "fix(tasks): address lint and type issues"
```

---

## Task 18: Final verification

**Step 1: Run full check**

```bash
npm run check
```

**Step 2: Manual test**

Start dev server and verify:

- Task list renders correctly
- Task selection works
- Filters work
- Search with autocomplete works
- Create task dialog works
- Edit task works
- Comments work
- Status toggle works

**Step 3: Final commit if needed**

```bash
git add -A
git commit -m "fix(tasks): final adjustments after testing"
```
