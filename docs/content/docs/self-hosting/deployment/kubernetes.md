---
description: "ByteChef Kubernetes Deployment. Learn how to deploy and manage ByteChef clusters on Kubernetes platforms."
title: Kubernetes
---

Using [Kubernetes](https://kubernetes.io) (K8s) is a great way to run ByteChef in production self-hosted environments. It delivers automatic scaling, self-healing capabilities, and streamlines database and configuration handling.

While ByteChef also supports [Helm](https://helm.sh/) for simplified deployments, we recommend using Kubernetes for production environments. Refer to the [Helm chart values](https://github.com/bytechefhq/bytechef/blob/master/kubernetes/helm/bytechef-monolith/values.yaml) in the ByteChef GitHub repository.

### Prerequisites
Ensure Kubernetes and Helm are installed on your system:
- **kubectl** - Kubernetes command-line tool.
- **Helm 3.x** - Package manager for Kubernetes.
- **Kubernetes Cluster** - A running cluster (Docker Desktop, Minikube, or cloud providers like EKS, GKE, AKS).

### Getting Started with ByteChef
