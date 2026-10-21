{{- define "taskmgmt.labels" -}}
app.kubernetes.io/part-of: taskmgmt
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}

{{/* image for service name: <registry>/<prefix><name>:<tag> */}}
{{- define "taskmgmt.image" -}}
{{ .root.Values.image.registry }}/{{ .root.Values.image.prefix }}{{ .name }}:{{ .root.Values.image.tag }}
{{- end }}
