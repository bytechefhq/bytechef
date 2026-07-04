// The nested workflow definition as stored by ByteChef (triggers + tasks, dispatcher
// children nested inside parameters). The graph pipeline treats it as opaque JSON and
// flattens it itself (flattenDefinitionTasks).
export type WorkflowDefinitionType = Readonly<Record<string, unknown>>;
