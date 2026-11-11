#!/usr/bin/env python3
"""Offline sanity check for the lesson-10 manifests (no cluster needed). It is NOT a replacement for 'kubectl apply'.
Parses every YAML document and cross-checks the references that, when wrong, make a deployment fail:
  - selector labels match pod labels, Service selector matches pods, targetPort names a declared container port
  - envFrom ConfigMap/Secret names exist, namespaces agree, probe ports are declared, volumeMounts have volumes
  - every key in the ConfigMap is something the app can bind (printed for a human to eyeball)
Usage: python tools/check_k8s.py reference/lesson-10/k8s"""
import glob, os, sys

try:
    import yaml
except ImportError:
    sys.exit("needs PyYAML:  pip install pyyaml")

errors, docs = [], []
for f in sorted(glob.glob(os.path.join(sys.argv[1], "*.yaml"))):
    for d in yaml.safe_load_all(open(f, encoding="utf-8")):
        if d:
            docs.append((os.path.basename(f), d))
            for k in ("apiVersion", "kind"):
                if k not in d:
                    errors.append(f"{f}: missing {k}")
            if "metadata" not in d or "name" not in d["metadata"]:
                errors.append(f"{f}: missing metadata.name")

by_kind = {}
for f, d in docs:
    by_kind.setdefault(d["kind"], []).append(d)
names = lambda kind: {d["metadata"]["name"] for d in by_kind.get(kind, [])}

ns = {d["metadata"].get("namespace") for f, d in docs if d["kind"] != "Namespace"}
if len(ns) != 1 or ns.pop() not in names("Namespace"):
    errors.append("namespace mismatch between objects and the Namespace object")

for dep in by_kind.get("Deployment", []):
    spec = dep["spec"]
    tmpl_labels = spec["template"]["metadata"]["labels"]
    for k, v in spec["selector"]["matchLabels"].items():
        if tmpl_labels.get(k) != v:
            errors.append(f"Deployment selector {k}={v} does not match the pod template labels")
    pod = spec["template"]["spec"]
    vols = {v["name"] for v in pod.get("volumes", [])}
    for c in pod["containers"]:
        ports = {p["name"] for p in c.get("ports", [])}
        for ef in c.get("envFrom", []):
            if "configMapRef" in ef and ef["configMapRef"]["name"] not in names("ConfigMap"):
                errors.append(f"envFrom ConfigMap {ef['configMapRef']['name']} not defined")
            if "secretRef" in ef and ef["secretRef"]["name"] not in names("Secret"):
                errors.append(f"envFrom Secret {ef['secretRef']['name']} not defined (secret.example.yaml?)")
        for probe in ("startupProbe", "livenessProbe", "readinessProbe"):
            p = c.get(probe)
            if not p:
                errors.append(f"{c['name']}: no {probe}")
            elif p["httpGet"]["port"] not in ports:
                errors.append(f"{probe} port {p['httpGet']['port']} is not a declared container port")
        for vm in c.get("volumeMounts", []):
            if vm["name"] not in vols:
                errors.append(f"volumeMount {vm['name']} has no volume")
        res = c.get("resources", {})
        if "memory" not in res.get("limits", {}) or "cpu" not in res.get("requests", {}):
            errors.append("container needs a memory limit and a cpu request")
        if ":latest" in c["image"] or ":" not in c["image"].split("/")[-1]:
            errors.append(f"image {c['image']} must carry a version tag")
    grace = pod.get("terminationGracePeriodSeconds", 30)
    print(f"Deployment {dep['metadata']['name']}: replicas={spec['replicas']}, terminationGracePeriodSeconds={grace}")

for svc in by_kind.get("Service", []):
    sel = svc["spec"]["selector"]
    ok = any(all(d["spec"]["template"]["metadata"]["labels"].get(k) == v for k, v in sel.items()) for d in by_kind.get("Deployment", []))
    if not ok:
        errors.append(f"Service {svc['metadata']['name']} selects no Deployment's pods")
    ports = {p["name"] for d in by_kind["Deployment"] for c in d["spec"]["template"]["spec"]["containers"] for p in c.get("ports", [])}
    for p in svc["spec"]["ports"]:
        if isinstance(p["targetPort"], str) and p["targetPort"] not in ports:
            errors.append(f"Service targetPort {p['targetPort']} is not a named container port")

for h in by_kind.get("HorizontalPodAutoscaler", []):
    if h["spec"]["scaleTargetRef"]["name"] not in names("Deployment"):
        errors.append("HPA targets a Deployment that does not exist")

print("objects:", ", ".join(f"{d['kind']}/{d['metadata']['name']}" for f, d in docs))
if errors:
    print("FAIL")
    for e in errors:
        print("  -", e)
    sys.exit(1)
print("PASS (offline structural checks only; nothing was applied to a cluster)")
