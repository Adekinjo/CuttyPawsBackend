#!/usr/bin/env bash
set -u

BACKEND_DIR="$(pwd)"
FRONTEND_DIR="$HOME/CuttyPawsFrontend"
REPORT_DIR="$BACKEND_DIR/full-security-report"
APP_URL="https://www.cuttypaws.com"
API_URL="https://cuttypawsbackend.onrender.com"
DOCKER_IMAGE=""

mkdir -p "$REPORT_DIR"

echo "===== CUTTYPAWS SECURITY AUDIT START ====="
echo ""

echo "1) Checking website headers..."
curl -s -D "$REPORT_DIR/app_headers.txt" -o /dev/null "$APP_URL"

echo "2) Checking API headers..."
curl -s -D "$REPORT_DIR/api_headers.txt" -o /dev/null "$API_URL"

echo "3) Checking TLS certificate for website..."
echo | openssl s_client -connect www.cuttypaws.com:443 -servername www.cuttypaws.com 2>/dev/null | openssl x509 -noout -issuer -subject -dates > "$REPORT_DIR/app_tls.txt"

echo "4) Checking TLS certificate for API..."
echo | openssl s_client -connect cuttypawsbackend.onrender.com:443 -servername cuttypawsbackend.onrender.com 2>/dev/null | openssl x509 -noout -issuer -subject -dates > "$REPORT_DIR/api_tls.txt"

echo "5) Scanning backend code with Trivy..."
trivy fs "$BACKEND_DIR" > "$REPORT_DIR/backend_trivy.txt" || true

if [ -d "$FRONTEND_DIR" ]; then
  echo "6) Scanning frontend code with Trivy..."
  trivy fs "$FRONTEND_DIR" > "$REPORT_DIR/frontend_trivy.txt" || true

  if [ -f "$FRONTEND_DIR/package.json" ]; then
    echo "7) Running npm audit for frontend..."
    cd "$FRONTEND_DIR" || exit
    npm audit --json > "$REPORT_DIR/frontend_npm_audit.json" || true
    cd "$BACKEND_DIR" || exit
  fi
else
  echo "6) Frontend folder not found, skipping frontend scan..."
fi

if [ -f "$BACKEND_DIR/pom.xml" ]; then
  echo "8) Checking backend dependency tree..."
  ./mvnw dependency:tree > "$REPORT_DIR/backend_dependency_tree.txt" 2>/dev/null || true
fi

if [ -n "$DOCKER_IMAGE" ]; then
  echo "9) Scanning Docker image..."
  trivy image "$DOCKER_IMAGE" > "$REPORT_DIR/docker_trivy.txt" || true
else
  echo "9) Docker image not set, skipping Docker image scan..."
fi

echo "10) Building quick summary..."
{
  echo "===== SECURITY SUMMARY ====="
  echo ""
  echo "Website headers file: $REPORT_DIR/app_headers.txt"
  echo "API headers file: $REPORT_DIR/api_headers.txt"
  echo "Website TLS file: $REPORT_DIR/app_tls.txt"
  echo "API TLS file: $REPORT_DIR/api_tls.txt"
  echo "Backend Trivy file: $REPORT_DIR/backend_trivy.txt"
  echo "Frontend Trivy file: $REPORT_DIR/frontend_trivy.txt"
  echo "Frontend npm audit file: $REPORT_DIR/frontend_npm_audit.json"
  echo "Backend dependency tree: $REPORT_DIR/backend_dependency_tree.txt"
  echo "Docker image scan file: $REPORT_DIR/docker_trivy.txt"
  echo ""
  echo "Check these first:"
  echo "1. backend_trivy.txt"
  echo "2. frontend_npm_audit.json"
  echo "3. app_headers.txt"
  echo "4. api_headers.txt"
} > "$REPORT_DIR/README.txt"

echo ""
echo "===== AUDIT COMPLETE ====="
echo "Reports saved in:"
echo "$REPORT_DIR"
