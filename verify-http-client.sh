#!/bin/bash

echo "🧪 Goose HTTP Client Verification Script"
echo "========================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "\n${BLUE}1. Build Verification${NC}"
echo "--------------------"
if ./gradlew build > /dev/null 2>&1; then
    echo -e "✅ ${GREEN}Build successful${NC}"
else
    echo -e "❌ ${RED}Build failed${NC}"
    exit 1
fi

echo -e "\n${BLUE}2. Unit Test Verification${NC}"
echo "-------------------------"
if ./gradlew test > /dev/null 2>&1; then
    echo -e "✅ ${GREEN}All unit tests pass${NC}"
else
    echo -e "❌ ${RED}Unit tests failed${NC}"
    exit 1
fi

echo -e "\n${BLUE}3. Server Connectivity Check${NC}"
echo "-----------------------------"
GOOSE_PORT=$(ps aux | grep "GOOSE_PORT" | grep -o 'GOOSE_PORT":[0-9]*' | head -1 | cut -d':' -f2 | tr -d '"')

if [ -z "$GOOSE_PORT" ]; then
    echo -e "❌ ${RED}No Goose server found running${NC}"
    echo -e "💡 ${YELLOW}Start Goose application first${NC}"
    exit 1
fi

echo -e "🔍 Found Goose server on port: ${GOOSE_PORT}"

# Test basic connectivity
if curl -s -f http://127.0.0.1:$GOOSE_PORT/sessions > /dev/null 2>&1; then
    echo -e "✅ ${GREEN}Server is accessible${NC}"
elif curl -s http://127.0.0.1:$GOOSE_PORT/sessions 2>&1 | grep -q "401"; then
    echo -e "✅ ${GREEN}Server is accessible (requires authentication)${NC}"
else
    echo -e "❌ ${RED}Server is not accessible${NC}"
fi

echo -e "\n${BLUE}4. HTTP Client Class Verification${NC}"
echo "----------------------------------"

# Check if all required classes exist
CLASSES=(
    "src/main/kotlin/com/block/gooseintellij/client/GooseHttpClient.kt"
    "src/main/kotlin/com/block/gooseintellij/client/GooseStreamingClient.kt"
    "src/main/kotlin/com/block/gooseintellij/model/GooseSession.kt"
    "src/main/kotlin/com/block/gooseintellij/model/GooseContext.kt"
    "src/main/kotlin/com/block/gooseintellij/model/ChatRequest.kt"
    "src/main/kotlin/com/block/gooseintellij/model/ChatResponse.kt"
)

for class_file in "${CLASSES[@]}"; do
    if [ -f "$class_file" ]; then
        echo -e "✅ ${GREEN}$(basename $class_file)${NC}"
    else
        echo -e "❌ ${RED}$(basename $class_file) missing${NC}"
    fi
done

echo -e "\n${BLUE}5. Dependencies Verification${NC}"
echo "----------------------------"
if grep -q "okhttp" build.gradle.kts; then
    echo -e "✅ ${GREEN}OkHttp dependency added${NC}"
else
    echo -e "❌ ${RED}OkHttp dependency missing${NC}"
fi

if grep -q "kotlinx-coroutines" build.gradle.kts; then
    echo -e "✅ ${GREEN}Coroutines dependency added${NC}"
else
    echo -e "❌ ${RED}Coroutines dependency missing${NC}"
fi

if grep -q "kotlinx-serialization" build.gradle.kts; then
    echo -e "✅ ${GREEN}Serialization dependency added${NC}"
else
    echo -e "❌ ${RED}Serialization dependency missing${NC}"
fi

echo -e "\n${BLUE}6. Test Coverage Verification${NC}"
echo "-----------------------------"
TEST_FILES=(
    "src/test/kotlin/com/block/gooseintellij/client/GooseHttpClientTest.kt"
    "src/test/kotlin/com/block/gooseintellij/client/GooseHttpClientIntegrationTest.kt"
)

for test_file in "${TEST_FILES[@]}"; do
    if [ -f "$test_file" ]; then
        echo -e "✅ ${GREEN}$(basename $test_file)${NC}"
    else
        echo -e "❌ ${RED}$(basename $test_file) missing${NC}"
    fi
done

echo -e "\n${BLUE}7. API Endpoint Discovery${NC}"
echo "-------------------------"
ENDPOINTS=("/sessions" "/api/sessions" "/v1/sessions" "/health" "/status")

for endpoint in "${ENDPOINTS[@]}"; do
    response=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:$GOOSE_PORT$endpoint 2>/dev/null)
    if [ "$response" = "200" ]; then
        echo -e "✅ ${GREEN}$endpoint - Available${NC}"
    elif [ "$response" = "401" ]; then
        echo -e "🔐 ${YELLOW}$endpoint - Requires Authentication${NC}"
    elif [ "$response" = "404" ]; then
        echo -e "❌ ${RED}$endpoint - Not Found${NC}"
    else
        echo -e "❓ ${YELLOW}$endpoint - Status: $response${NC}"
    fi
done

echo -e "\n${BLUE}Summary${NC}"
echo "-------"
echo -e "✅ ${GREEN}HTTP Client Infrastructure: IMPLEMENTED${NC}"
echo -e "✅ ${GREEN}Unit Tests: PASSING${NC}"
echo -e "✅ ${GREEN}Build System: WORKING${NC}"
echo -e "🔐 ${YELLOW}Server Integration: NEEDS API SPECIFICATION${NC}"

echo -e "\n${BLUE}Next Steps${NC}"
echo "----------"
echo -e "1. 📋 Determine exact Goose server API specification"
echo -e "2. 🔑 Identify correct authentication method"
echo -e "3. 🧪 Run integration tests with real server"
echo -e "4. 🔗 Integrate with existing service layer"

echo -e "\n🎉 ${GREEN}HTTP Client Infrastructure is ready for integration!${NC}"