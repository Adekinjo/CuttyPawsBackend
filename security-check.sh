#!/usr/bin/env bash

APP_URL="https://www.cuttypaws.com"
API_URL="https://cuttypawsbackend.onrender.com"

mkdir -p security-reports

echo "Checking website headers..."
curl -I $APP_URL > security-reports/app_headers.txt

echo "Checking API headers..."
curl -I $API_URL > security-reports/api_headers.txt

echo "Running OWASP ZAP passive scan (website)..."
docker run --rm -v $(pwd)/security-reports:/zap/wrk ghcr.io/zaproxy/zaproxy zap-baseline.py \
-t $APP_URL \
-r zap_app_report.html

echo "Running OWASP ZAP passive scan (API)..."
docker run --rm -v $(pwd)/security-reports:/zap/wrk ghcr.io/zaproxy/zaproxy zap-baseline.py \
-t $API_URL \
-r zap_api_report.html

echo "Running filesystem vulnerability scan..."
trivy fs . > security-reports/trivy_report.txt

echo "Done. Reports saved in security-reports folder."
