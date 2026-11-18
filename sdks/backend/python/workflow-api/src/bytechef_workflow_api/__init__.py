"""Type definitions for ByteChef script code workflows.

They describe the dict a Python code workflow file evaluates to and the context ByteChef passes to each task's
``perform``. The ByteChef runtime can't import this package: import it under ``typing.TYPE_CHECKING``.
"""

from typing import Any, Callable, Dict, List, Literal, Protocol, TypedDict, Union, overload

__all__ = [
    "ClusterElement",
    "ClusterElements",
    "ConnectionRequirement",
    "ConnectionSpec",
    "ForkJoinTaskDefinition",
    "InputDefinition",
    "InputType",
    "IntegrationDefinition",
    "LogLevel",
    "OutputDefinition",
    "ParallelTaskDefinition",
    "ProjectDefinition",
    "TaskContext",
    "TaskDefinition",
    "TaskOutputDefinition",
    "TriggerDefinition",
    "ValueOutputDefinition",
    "WorkflowDefinition",
    "WorkflowTask",
]

LogLevel = Literal["trace", "debug", "info", "warn", "error", "TRACE", "DEBUG", "INFO", "WARN", "ERROR"]

InputType = Literal["BOOLEAN", "DATE", "DATE_TIME", "FIELD_MAPPING", "INTEGER", "NUMBER", "STRING", "TIME"]


class _ClusterElementRequired(TypedDict):
    type: str


class ClusterElement(_ClusterElementRequired, total=False):
    """A cluster element composed at the call site, for actions that read cluster elements, such as the AI Agent's
    ``chat``. ``type`` is ``<componentName>/v<version>/<elementName>``, for example ``openAi/v1/model``;
    ``connection`` names one of the task's declared connections; for a tool, ``name`` is what the model calls it."""

    connection: str
    name: str
    parameters: Dict[str, Any]


ClusterElements = Dict[str, Union[ClusterElement, List[ClusterElement]]]
"""Cluster elements keyed by cluster element type (``model``, ``tools``, ``chatMemory``, ``rag``, ...)."""


class TaskContext(Protocol):
    """The context ByteChef passes to ``perform``.

    ``component`` calls a component action:
    ``context.component.<componentName>.<actionName>(input, connection_name, cluster_elements)``.
    """

    component: Any

    def connection(self, connection_name: str) -> Dict[str, Any]:
        """A declared connection's parameters, credentials included."""
        ...

    @overload
    def input(self) -> Dict[str, Any]:
        """The workflow inputs plus the output of every task that already ran, keyed by name."""
        ...

    @overload
    def input(self, name: str) -> Any:
        """One workflow input or task output. Fails on an unknown name."""
        ...

    def log(self, level: LogLevel, message: str) -> None:
        """Writes to the task's execution log."""
        ...

    def parameters(self) -> Dict[str, Any]:
        """The task's declared parameters, with any ``${...}`` already evaluated."""
        ...


class _ConnectionRequirementRequired(TypedDict):
    componentName: str
    name: str


class ConnectionRequirement(_ConnectionRequirementRequired, total=False):
    """A connection a task uses. ``name`` is what ``perform`` passes as the connection name. ``componentVersion``
    defaults to the component's latest version at save time."""

    componentVersion: int


class _ConnectionSpecRequired(TypedDict):
    componentName: str


class ConnectionSpec(_ConnectionSpecRequired, total=False):
    """A connection in the map form of ``connections``, keyed by connection name."""

    componentVersion: int


class _TaskDefinitionRequired(TypedDict):
    name: str
    perform: Callable[..., Any]


class TaskDefinition(_TaskDefinitionRequired, total=False):
    """A task: one step of a workflow. ByteChef calls ``perform(context)`` when the task runs, and its return value
    becomes the task's output. Task names must be unique across the workflow, nesting included."""

    connections: Union[List[ConnectionRequirement], Dict[str, ConnectionSpec]]
    description: str
    label: str
    parameters: Dict[str, Any]


class _ParallelTaskDefinitionRequired(TypedDict):
    name: str
    tasks: List[TaskDefinition]
    type: Literal["parallel"]


class ParallelTaskDefinition(_ParallelTaskDefinitionRequired, total=False):
    """A group whose tasks all start at the same time. They can't read each other's output."""

    description: str
    label: str


class _ForkJoinTaskDefinitionRequired(TypedDict):
    branches: List[List[TaskDefinition]]
    name: str
    type: Literal["forkJoin"]


class ForkJoinTaskDefinition(_ForkJoinTaskDefinitionRequired, total=False):
    """A group whose branches run at the same time. The tasks within a branch run in order."""

    description: str
    label: str


WorkflowTask = Union[TaskDefinition, ParallelTaskDefinition, ForkJoinTaskDefinition]
"""An entry of a workflow's ``tasks``. A group can't contain another group."""


class _InputDefinitionRequired(TypedDict):
    name: str


class InputDefinition(_InputDefinitionRequired, total=False):
    """A workflow input. At run time it arrives in ``context.input()`` under its own name. ``type`` defaults to
    ``STRING``."""

    label: str
    required: bool
    type: InputType


class TaskOutputDefinition(TypedDict):
    """A workflow output whose value is a task's output."""

    name: str
    task: str


class ValueOutputDefinition(TypedDict):
    """A workflow output whose value is a literal or a ``${...}`` expression."""

    name: str
    value: Any


OutputDefinition = Union[TaskOutputDefinition, ValueOutputDefinition]


class _TriggerDefinitionRequired(TypedDict):
    name: str
    type: str


class TriggerDefinition(_TriggerDefinitionRequired, total=False):
    """A component trigger that starts the workflow. ``type`` is ``<componentName>/v<version>/<triggerName>``, for
    example ``schedule/v1/interval``."""

    parameters: Dict[str, Any]


class _WorkflowDefinitionRequired(TypedDict):
    name: str


class WorkflowDefinition(_WorkflowDefinitionRequired, total=False):
    description: str
    inputs: List[InputDefinition]
    label: str
    outputs: List[OutputDefinition]
    tasks: List[WorkflowTask]
    triggers: List[TriggerDefinition]


class _ProjectDefinitionRequired(TypedDict):
    name: str


class ProjectDefinition(_ProjectDefinitionRequired, total=False):
    """An automation code project, or an embedded catalog code project. ``name`` is the project's identity: deploying
    again under the same name updates the project. ``version`` defaults to ``0.0.1``."""

    description: str
    version: str
    workflows: List[WorkflowDefinition]


class _IntegrationDefinitionRequired(TypedDict):
    componentName: str
    componentVersion: int


class IntegrationDefinition(_IntegrationDefinitionRequired, total=False):
    """An embedded code integration, identified by ``componentName`` and ``componentVersion``."""

    description: str
    version: str
    workflows: List[WorkflowDefinition]
