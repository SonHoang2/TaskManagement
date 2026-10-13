#!/usr/bin/env bash
# Deploys the metrics stack. Run from anywhere; needs kubectl pointed at the cluster.
# Used by build-and-deploy.sh and .github/workflows/cd.yml.
set -euo pipefail
cd "$(dirname "$0")/.."
O=observability

kubectl create configmap prometheus-rules -n taskmgmt --from-file=$O/prometheus/rules --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap alertmanager-config -n taskmgmt --from-file=alertmanager.yml=$O/alertmanager/alertmanager.yml --dry-run=client -o yaml | kubectl apply -f -
kubectl create configmap grafana-dashboards -n taskmgmt --from-file=$O/grafana/dashboards --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f k8s/observability.yaml

# ConfigMap changes are not picked up by running pods; stamp a content hash on the pod template so a
# change rolls the pod and an unchanged deploy does not.
stamp() { # deployment, files...
  local d=$1; shift
  local sum; sum=$(cat "$@" | sha256sum | cut -c1-16)
  kubectl patch deployment/"$d" -n taskmgmt -p \
    "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"checksum/config\":\"$sum\"}}}}}"
}
stamp prometheus k8s/observability.yaml $O/prometheus/rules/*.yml
stamp alertmanager $O/alertmanager/alertmanager.yml
stamp grafana k8s/observability.yaml $O/grafana/dashboards/*.json

for d in prometheus alertmanager grafana; do
  kubectl rollout status deployment/$d -n taskmgmt --timeout=180s
done
