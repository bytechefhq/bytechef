type Data = {
    id: number;
    date: string;
    status: string;
    project: string;
    workflow: string;
    instance: string;
};
export const DATA: Data[] = [
    {
        id: 1,
        project: 'Project one',
        workflow: 'Workflow 1',
        instance: 'Instance 1',
        date: '01/01/2023',
        status: 'Completed',
    },
    {
        id: 2,
        project: 'Project two',
        workflow: 'Workflow 2',
        instance: 'Instance 2',
        date: '02/01/2023',
        status: 'Failed',
    },
    {
        id: 3,
        project: 'Project three',
        workflow: 'Workflow 3',
        instance: 'Instance 3',
        date: '03/01/2023',
        status: 'Running',
    },
];
