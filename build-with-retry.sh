#!/bin/bash
# Build with exponential backoff retry for network errors

for i in 1 2 3 4; do
  echo "=== Build Attempt $i of 4 ==="
  if mvn clean install -DskipTests 2>&1 | tee build_attempt_$i.log; then
    echo "✓ Build successful on attempt $i"
    exit 0
  else
    if [ $i -lt 4 ]; then
      delay=$((2 ** $i))
      echo "✗ Build failed, waiting ${delay}s before retry..."
      sleep $delay
    fi
  fi
done

echo "✗ Build failed after 4 attempts"
exit 1
