---
name: bruno-testing
description: Use this skill to run integration tests, perform regression suites, or debug specific API endpoints using the Bruno CLI for the Minimal API project.
---

# Bruno Integration Testing & Debugging

You are an expert at testing .NET Minimal APIs using the Bruno CLI. 

## ⛔ CRITICAL RULE
ALL commands must be executed via devbox. NEVER run `bru` or `task` directly. 
Always use: `devbox run task <task-name>`

## 1. Running Regression Tests
To run full suites, use the provided tasks. The API must be running, so use the lifecycle tasks:
- **Full Suite:** `devbox run task test:petstore-integration SUITE="all-suites"`
- **With Auth:** `devbox run task test:petstore-integration SUITE="all-suites-with-auth"`

## 2. Debugging a Specific Failure
If a regression test fails, or the user asks to debug a specific endpoint (e.g., `pet/add-pet.bru`), follow this exact workflow:
1. Ensure the background API is running: `devbox run task api:start` followed by `devbox run task api:wait`.
2. Run the specific debug task: `devbox run task bruno:debug-single TEST="path/to/test.bru"`.
3. Silently read the output file generated at `test-output/bruno-debug.json`.
4. Analyse the JSON to extract the exact HTTP Request Payload, Headers, and the HTTP Response Body.
5. Present the user with a summary of why the test failed, showing the actual response received from the .NET API versus the expected assertion.
6. When finished, clean up: `devbox run task api:stop`.

## Context
Generated code is located in `test-output/src/PetstoreApi/` and test handlers are copied from `petstore-tests/TestHandlers/`. If the bug is in the API logic, look in these directories.