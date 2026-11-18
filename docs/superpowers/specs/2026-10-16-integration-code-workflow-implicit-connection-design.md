# Implicit integration connection for code integration tasks

## Problem

A task in an embedded code integration that calls the integration's own component has to declare that connection
and pass its name, although inside an integration there is only ever one such connection - the connected user's:

```js
connections: [{componentName: "googleMail", name: "gmail"}],
perform: function (context) {
    return context.component.googleMail.searchEmail({maxResults: 10}, "gmail");
}
```

Without the declaration the call fails with "Connection with name null does not exist", and the editor's Workflow
Test Configuration offers no Gmail connection for the task.

## Goal

In an integration code workflow, a task may call the integration's own component with no declaration and no
connection name:

```js
perform: function (context) {
    return context.component.googleMail.searchEmail({maxResults: 10});
}
```

Scope is integrations only. Automation code workflows keep explicit declarations: they have no "own component", and
provisioning binds connected-user connections from the declared slots.

## Design

1. **Implicit declaration at integration deploy.** Where an integration code workflow's task definitions are turned
   into workflow nodes (the integration-specific path in `IntegrationCodeWorkflowFacadeImpl`, which already knows
   the integration's `componentName`), every code task that declares no connection on the integration's own component
   gets one added: name = the component name (e.g. `googleMail`), componentName = the integration component, latest
   version. It is language-neutral (JavaScript, Python, Java), and it lands in the same `connections` extension a
   hand-written declaration would, so the existing layers pick it up unchanged:
   - the embedded coordinator maps a declared own-component connection to the integration instance connection
     (`IntegrationTaskDispatcherPreSendProcessor.getDeclaredIntegrationConnectionIdMap`);
   - the admin instance configuration already skips own-component slots;
   - the editor's Workflow Test Configuration shows one Gmail slot per code task.
   A task that already declares an own-component connection under any name is left alone, so existing sources keep
   working. If a declared name equals the component name but points at another component, the implicit one is not
   added (the explicit declaration wins).
2. **No-name fallback on the worker.** In `CodeWorkflowTaskContext`, when a call passes no connection name, resolve
   the connection whose declared name equals the called component's name. The implicit declaration uses exactly that
   name, so the fallback is naturally scoped to the integration's own component; an automation task only reaches it
   if it declared a connection literally named after the component. When nothing matches, keep the current failure
   but with a message that names the component instead of "null".

## Not changing

- Automation provisioning, `WorkflowConnectionSlot`, `ConnectedUserWorkflowConnectionResolver`.
- Explicit declarations and named calls keep their current meaning.
- Cluster-element `connection` references still need explicit names.

## Tests

- Integration deploy: an undeclared task gets the implicit own-component connection; a task that declares one (any
  name) gets no second one; a task declaring other components keeps them and gains the implicit one.
- `CodeWorkflowTaskContext`: a no-name call resolves the connection named after the component; a no-name call with no
  such connection fails with the component named in the message; a named call is unchanged.

## Samples

The integration starter in the embedded sample app and the three integration sample repos drop the Gmail
`connections` line and the `"gmail"` argument.
