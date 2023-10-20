# Contributing to ByteChef

Thanks for taking the time for contribution to ByteChef!
We're very welcoming community and while it's very much appreciated if you follow these guidelines it's not a requirement.

## Code of Conduct
This project and everyone participating in it is governed by the [ByteChef Code of Conduct](./CODE_OF_CONDUCT.md).
By participating, you are expected to uphold this code. Please report unacceptable behavior to support@bytechef.io.

## How can I contribute?

There are many ways in which you can contribute to ByteChef.

### Report a bug
Report all issues through GitHub Issues using the [Report a Bug](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=bug%2Cneeds+triaging&template=bug-report.yaml&title=%5Bbug%5D%3A+) template.
To help resolve your issue as quickly as possible, read the template and provide all the requested information.

### File a feature/component request
We welcome all feature requests, whether it's to add new functionality, improve existing connectors or to suggest a brand new connector.
File your feature request through GitHub Issues using the [Feature Request](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=enhancement&template=feature-request.yaml&title=%5Bfeature%5D%3A+) template for improvements or [Connector Request](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=nhancement,component&template=new-component-request.yaml&title=%5Bcomponent%5D%3A+) for improvements to the existing components or for the new ones.

### Improve the documentation
You can help by suggesting improvements to our documentation using the [Documentation Improvement](https://github.com/bytechefhq/bytechef/issues/new?assignees=&labels=documentation&template=documentation-improvement.yaml&title=%5Bdocs%5D%3A+) template or check [Step-by-step guide to contributing](#step-by-step-guide-to-contributing)!

### Close a Bug / Feature issue
Find [issues](https://github.com/bytechefhq/bytechef/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc) where we need help. Search for issues with either [`good first issue`](https://github.com/bytechefhq/bytechef/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc+label%3A%22good+first+issue%22+) and/or [`help wanted`](https://github.com/bytechefhq/bytechef/issues?q=is%3Aissue+is%3Aopen+sort%3Aupdated-desc+label%3A%22help+wanted%22) labels. Check out the following [Code Contribution Guide](#contributing-code-changes) to begin.

## Contributing Code Changes

Please review the following sections before proposing code changes.

### License
By contributing, you agree that your contributions will be licensed under the terms of the [ByteChef project licenses](https://github.com/bytechefhq/bytechef/blob/master/README.md#license).

[//]: # (### Developer Certificate of Origin &#40;DCO&#41;)

[//]: # ()
[//]: # (By contributing to ByteChef, Inc., You accept and agree to the terms and conditions in the [Developer Certificate of Origin]&#40;https://github.com/bytechefhq/bytechef/blob/master/DCO.md&#41; for Your present and future Contributions submitted to ByteChef, Inc. Your contribution includes any submissions to the [ByteChef repository]&#40;https://github.com/bytechefhq&#41; when you click on such buttons as `Propose changes` or `Create pull request`. Except for the licenses granted herein, You reserve all right, title, and interest in and to Your Contributions.)

### Step-by-step guide to contributing

#### We Use [GitHub Flow](https://guides.github.com/introduction/flow/index.html), So All Code Changes Happen Through Pull Requests
Pull requests are the best way to propose changes to the codebase (we use [Git-Flow](https://nvie.com/posts/a-successful-git-branching-model/)). We actively welcome your pull requests:

1. Fork the repo and create a new branch from the `develop` branch.
2. Branches are named as `bug/fix-name` or `feature/feature-name`
3. Please add tests for your changes. Client-side changes require Cypress/Jest tests while server-side changes require JUnit tests.
4. Once you are confident in your code changes, create a pull request in your fork to the `develop` branch in the bytechefhq/bytechef base repository.
5. If you've changed any APIs, please call this out in the pull request and ensure backward compatibility.
6. Link the issue of the base repository in your Pull request description. [Guide](https://docs.github.com/en/free-pro-team@latest/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue)
7. When you raise a pull request, we automatically run tests on our CI. Please ensure that all the tests are passing for your code change. We will not be able to accept your change if the test suite doesn't pass.
8. Documentation: When new features are added or there are changes to existing features that require updates to documentation, we encourage you to add/update any missing documentation in the [`/docs` folder](https://github.com/bytechefhq/bytechef/tree/master/docs). To update an existing documentation page, you can simply click on the `Edit this page` button on the bottom left corner of the documentation page.

# Setup for local development 

## Server Side

### Prerequisites

TODO

### Steps for setup

TODO

## Questions?
Contact us on [Discord](https://discord.gg/PybnUM3Y) or mail us at [support@bytechef.io](mailto:support@bytechef.io).
