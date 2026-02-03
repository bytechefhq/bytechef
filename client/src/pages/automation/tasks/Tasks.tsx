'use client';

import TaskDetail from './components/TaskDetail';
import TaskList from './components/TaskList';
import {useTasks} from './hooks/useTasks';

export default function Tasks() {
    useTasks();

    return (
        <div className="flex size-full bg-background">
            <TaskList />

            <div className="h-full flex-1">
                <TaskDetail />
            </div>
        </div>
    );
}
