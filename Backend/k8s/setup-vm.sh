#!/usr/bin/env bash
# One-time setup of the Ubuntu Server VM: k3s (+ingress-nginx) and the GitHub Actions self-hosted runner.
# No Docker needed: CI builds images and pushes them to GHCR, k3s pulls them.
# Run INSIDE the VM as your normal user (not root):
#   RUNNER_TOKEN=<token from GitHub> ./setup-vm.sh
# Token: repo -> Settings -> Actions -> Runners -> New self-hosted runner (valid ~1h).
set -euo pipefail

REPO_URL=${REPO_URL:-https://github.com/SonHoang2/TaskManagementAPI}
: "${RUNNER_TOKEN:?set RUNNER_TOKEN (see header)}"
[ "$(id -u)" -ne 0 ] || { echo "run as a normal user, not root"; exit 1; }

echo "==> Grow root LV to the full disk"
sudo lvextend -r -l +100%FREE /dev/ubuntu-vg/ubuntu-lv || true

echo "==> k3s (traefik off: the manifests use ingress-nginx)"
if ! command -v k3s >/dev/null; then
  curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="server --disable traefik --write-kubeconfig-mode 644" sh -
fi
mkdir -p ~/.kube && cp /etc/rancher/k3s/k3s.yaml ~/.kube/config && chmod 600 ~/.kube/config
export KUBECONFIG=~/.kube/config
kubectl wait --for=condition=Ready node --all --timeout=180s

echo "==> ingress-nginx (k3s servicelb exposes it on the VM's port 80)"
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.11.2/deploy/static/provider/cloud/deploy.yaml

echo "==> GitHub Actions runner"
mkdir -p ~/actions-runner && cd ~/actions-runner
# each step checks its own result, so a re-run resumes where a failed run stopped
if [ ! -f config.sh ]; then
  # .../releases/latest redirects to .../releases/tag/vX.Y.Z (no pipe: grep -m1 + pipefail kills curl with error 23)
  LATEST=$(curl -fsSLI -o /dev/null -w '%{url_effective}' https://github.com/actions/runner/releases/latest)
  VER=${LATEST##*/v}
  curl -fsSL -o runner.tar.gz "https://github.com/actions/runner/releases/download/v${VER}/actions-runner-linux-x64-${VER}.tar.gz"
  tar xzf runner.tar.gz && rm runner.tar.gz
  sudo ./bin/installdependencies.sh
fi
if [ ! -f .runner ]; then
  ./config.sh --unattended --url "$REPO_URL" --token "$RUNNER_TOKEN" --labels taskmgmt-local --name "$(hostname)" --replace
fi
if [ ! -f .service ]; then
  sudo ./svc.sh install "$USER"
fi
sudo ./svc.sh start

echo "Done. VM IP: $(hostname -I | awk '{print $1}')  -> add '<ip> taskmgmt.local' to /etc/hosts on your host."
