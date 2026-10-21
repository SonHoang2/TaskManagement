#!/usr/bin/env bash
# One-time migration: hand resources created earlier by `kubectl apply` over to Helm, without downtime.
# Helm refuses to install over objects it does not own, so stamp its ownership metadata on them first.
# Usage: ./k8s/helm-adopt.sh [release] [namespace] [helm upgrade args...]   (no-op once the release exists)
set -euo pipefail
cd "$(dirname "$0")/.."
REL=${1:-taskmgmt}; NS=${2:-taskmgmt}; shift 2 2>/dev/null || true

if helm status "$REL" -n "$NS" >/dev/null 2>&1; then exit 0; fi

# --ignore-not-found equivalent: only touch objects that already exist
helm template "$REL" k8s/helm/taskmgmt -n "$NS" "$@" | kubectl get -f - -o name 2>/dev/null | while read -r obj; do
  kubectl label --overwrite "$obj" -n "$NS" app.kubernetes.io/managed-by=Helm >/dev/null
  kubectl annotate --overwrite "$obj" -n "$NS" meta.helm.sh/release-name="$REL" meta.helm.sh/release-namespace="$NS" >/dev/null
  echo "adopted $obj"
done || true
