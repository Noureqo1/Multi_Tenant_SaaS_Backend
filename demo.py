import urllib.request
import json
import time
import sys

BASE = "http://localhost:8082"

def print_step(title):
    print(f"\n=== {title} ===")

def post_json(url, payload, headers=None):
    if headers is None:
        headers = {}
    headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers, method='POST')
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8')), response.status
    except urllib.error.HTTPError as e:
        print(f"Error {e.code}: {e.read().decode('utf-8')}")
        sys.exit(1)

def get_json(url, headers=None):
    if headers is None:
        headers = {}
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            return json.loads(response.read().decode('utf-8')), response.status
    except urllib.error.HTTPError as e:
        print(f"Error {e.code}: {e.read().decode('utf-8')}")
        sys.exit(1)

def main():
    print_step("1. Login")
    resp, status = post_json(f"{BASE}/auth/login", {"email": "admin@tenant-a.com", "password": "password"})
    token = resp.get("token")
    print(f"Token: {token}")

    print_step("2. Create Project")
    auth_headers = {"Authorization": f"Bearer {token}"}
    proj_resp, status = post_json(f"{BASE}/projects", {"name": "Q3 Report Project"}, headers=auth_headers)
    project_id = proj_resp.get("id")
    print(f"Project ID: {project_id}")

    print_step("3. Enqueue Report Job (expect 202)")
    headers = {**auth_headers, "X-Correlation-ID": "demo-corr-001"}
    job_resp, status = post_json(f"{BASE}/projects/{project_id}/generate-report", {}, headers=headers)
    job_id = job_resp.get("jobId")
    print(f"Response Status: {status}")
    print(f"Job ID: {job_id}")

    print_step("4. Poll Job Status (wait for COMPLETED)")
    for i in range(1, 11):
        print(f"Attempt {i}: ", end="", flush=True)
        poll_resp, status = get_json(f"{BASE}/jobs/{job_id}", headers=auth_headers)
        job_status = poll_resp.get("status")
        print(job_status)
        if job_status == "COMPLETED":
            break
        time.sleep(2)

    print_step("5. Actuator Health")
    health, _ = get_json(f"{BASE}/actuator/health")
    print(f"Status: {health.get('status')}")

    print_step("6. Custom Metrics")
    metrics, _ = get_json(f"{BASE}/actuator/metrics/workhub.jobs.published")
    print(f"Published Jobs: {metrics.get('measurements', [{}])[0].get('value', 0)}")
    
    print("\n=== Done. Check logs for [demo-corr-001] correlation ID ===")

if __name__ == "__main__":
    main()
