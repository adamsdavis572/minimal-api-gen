# Quickstart: TDD Workflow for Feature 004

**Feature**: 004-minimal-api-refactoring  
**TDD Cycle**: RED → GREEN iterations (target: <10 cycles)  
**Test Baseline**: Feature 003 (7 integration tests - Golden Standard)

---

## Prerequisites

✅ **devbox** installed and configured  
✅ **Java 11** available via devbox  
✅ **Maven 3.8.9+** available via devbox  
✅ **.NET 8.0 SDK** available via devbox  
✅ **Feature 003** merged into branch `004-minimal-api-refactoring`

**Verify**:
```bash
devbox run java -version   # Should show Java 11
devbox run mvn -version    # Should show Maven 3.8.9+
devbox run dotnet --version # Should show 8.0.x
git branch --show-current   # Should show 004-minimal-api-refactoring
```

---

## TDD Workflow

### Phase A: Initial RED State (Expected)

**Goal**: Confirm Feature 003 tests fail after initial refactoring.

1. **Modify Java Generator**:
   ```bash
   cd generator/src/main/java/org/openapitools/codegen/languages
   # Edit MinimalApiServerCodegen.java
   # Add operationsByTag logic to postProcessOperationsWithModels()
   ```

2. **Create New Templates**:
   ```bash
   cd generator/src/main/resources/minimal-api-server
   # Create: TagEndpoints.cs.mustache
   # Create: EndpointMapper.cs.mustache
   ```

3. **Modify Existing Templates**:
   ```bash
   # Modify: program.mustache (remove FastEndpoints, add Minimal API)
   # Modify: project.csproj.mustache (remove FastEndpoints packages, add Swashbuckle + FluentValidation)
   ```

4. **Build Generator**:
   ```bash
   cd generator
   devbox run mvn clean package
   ```

   Expected: ✅ Build succeeded → `target/minimal-api-server-openapi-generator-1.0.0.jar`

5. **Regenerate Project**:
   ```bash
   cd test-output
   rm -rf src/ # Clean old FastEndpoints code
   
   devbox run java -cp ../generator/target/minimal-api-server-openapi-generator-1.0.0.jar:../generator/target/lib/* \
     org.openapitools.codegen.OpenAPIGenerator generate \
     -g minimal-api-server \
     -i ../specs/petstore.yaml \
     -o . \
     --package-name PetstoreApi
   ```

   Expected: New files in `src/PetstoreApi/`:
   - `Program.cs` (Minimal API setup)
   - `PetstoreApi.csproj` (Swashbuckle + FluentValidation)
   - `Endpoints/PetEndpoints.cs` (route group)
   - `Extensions/EndpointMapper.cs` (MapAllEndpoints)

6. **Build C# Project**:
   ```bash
   cd test-output/src/PetstoreApi
   devbox run dotnet restore
   devbox run dotnet build
   ```

   Expected Outcomes:
   - ❌ **FAIL** (compilation errors) → Fix Java/templates, go to step 4
   - ✅ **SUCCESS** → Proceed to step 7

7. **Run Tests (First RED)**:
   ```bash
   cd test-output/tests/PetstoreApi.Tests
   devbox run dotnet test --logger "console;verbosity=detailed"
   ```

   Expected: ❌ **RED** (0 passed, 7 failed)

   Typical Failures:
   - `404 Not Found` → Endpoint routes incorrect
   - `405 Method Not Allowed` → HTTP method mismatch
   - `422 Unprocessable Entity` → Validation not wired up
   - `NullReferenceException` → Missing PetStore logic injection

---

### Phase B: First GREEN Iteration

**Goal**: Fix endpoint routing and basic responses.

8. **Analyze Test Failures**:
   ```bash
   # Read test output carefully
   # Example: "Expected StatusCode: 201, Actual: 404"
   # → Route not registered or path mismatch
   ```

9. **Fix Templates**:
   - **Route Path Issue**: Check `TagEndpoints.cs.mustache` → ensure `{{path}}` variable correct
   - **HTTP Method Issue**: Check `Map{{httpMethod}}` → verify case (MapPost not MapPOST)
   - **Registration Issue**: Check `EndpointMapper.cs` → ensure `Map{Tag}Endpoints()` called

10. **Regenerate + Rebuild**:
    ```bash
    cd generator
    devbox run mvn clean package
    
    cd ../test-output
    rm -rf src/
    devbox run java -cp ... # (same as step 5)
    
    cd src/PetstoreApi
    devbox run dotnet build
    ```

11. **Rerun Tests**:
    ```bash
    cd ../../tests/PetstoreApi.Tests
    devbox run dotnet test
    ```

    Expected: ✅ **GREEN** (1-2 tests passing, 5-6 still failing)

---

### Phase C: Validation GREEN

**Goal**: Fix FluentValidation wiring.

12. **Inject Validators**:
    - Edit `TagEndpoints.cs.mustache`:
      ```mustache
      group.MapPost("{{path}}", async ([FromBody] {{bodyParam.dataType}} request, IValidator<{{bodyParam.dataType}}> validator) => 
      {
          var validationResult = await validator.ValidateAsync(request);
          if (!validationResult.IsValid)
          {
              return Results.ValidationProblem(validationResult.ToDictionary());
          }
          // ... operation logic
      })
      ```

13. **Regenerate + Test**:
    ```bash
    # (Repeat steps 10-11)
    ```

    Expected: ✅ **GREEN** (3-4 tests passing, 3-4 still failing)

---

### Phase D: Logic Injection GREEN

**Goal**: Inject PetStore CRUD logic into endpoints.

14. **Add PetStore Logic**:
    - Option A: Manual injection (in template) - **TEMPORARY**
      ```csharp
      // In AddPet endpoint
      var createdPet = PetStore.AddPet(pet);
      return Results.Created($"/v2/pet/{createdPet.Id}", createdPet);
      ```

    - Option B: Generator logic injection (Java) - **FUTURE**
      ```java
      // In postProcessOperationsWithModels()
      op.vendorExtensions.put("operationLogic", generatePetStoreLogic(op));
      ```

15. **Regenerate + Test**:
    ```bash
    # (Repeat steps 10-11)
    ```

    Expected: ✅ **GREEN** (7 tests passing, 0 failing) → **DONE**

---

## Key Commands Reference

### Build Generator
```bash
cd generator
devbox run mvn clean package
```

### Generate C# Code
```bash
cd test-output
devbox run java -cp ../generator/target/minimal-api-server-openapi-generator-1.0.0.jar:../generator/target/lib/* \
  org.openapitools.codegen.OpenAPIGenerator generate \
  -g minimal-api-server \
  -i ../specs/petstore.yaml \
  -o . \
  --package-name PetstoreApi
```

### Build C# Project
```bash
cd test-output/src/PetstoreApi
devbox run dotnet restore
devbox run dotnet build
```

### Run Tests
```bash
cd test-output/tests/PetstoreApi.Tests
devbox run dotnet test --logger "console;verbosity=detailed"
```

### Run Specific Test
```bash
devbox run dotnet test --filter "FullyQualifiedName~AddPet_ReturnsCreated"
```

### Watch Mode (Auto-rerun tests on file changes)
```bash
devbox run dotnet watch test
```

---

## Debugging Tips

### 1. Compilation Errors
**Symptom**: `dotnet build` fails with CS errors

**Fix**:
- Check using statements in templates
- Verify package references in `project.csproj.mustache`
- Ensure namespaces match `{{packageName}}`

**Command**:
```bash
devbox run dotnet build --verbosity diagnostic
```

### 2. 404 Not Found
**Symptom**: Tests fail with 404

**Fix**:
- Check `EndpointMapper.cs` → ensure `Map{Tag}Endpoints()` called
- Check `program.mustache` → ensure `app.MapAllEndpoints()` called
- Check route prefix in `TagEndpoints.cs` → match test URLs

**Command**:
```bash
# Start server and check routes
cd test-output/src/PetstoreApi
devbox run dotnet run --urls "http://localhost:5002"

# In another terminal, test manually
curl -v http://localhost:5002/v2/pet/1
```

### 3. 422 Validation Errors
**Symptom**: Tests fail with validation errors

**Fix**:
- Check `Program.cs` → ensure `AddValidatorsFromAssemblyContaining<Program>()`
- Check validator classes generated
- Check `[FromBody]` attribute on body parameters

**Command**:
```bash
# Check if validators registered
cd test-output/src/PetstoreApi
devbox run dotnet run --verbosity diagnostic | grep "Validator"
```

### 4. NullReferenceException
**Symptom**: Tests fail with null reference

**Fix**:
- Inject PetStore logic (see Phase D)
- Ensure PetStore class generated in `Features/` folder
- Check static initialization: `private static readonly Dictionary<long, Pet> _pets = new();`

**Command**:
```bash
# Check generated files
ls -la src/PetstoreApi/Features/
cat src/PetstoreApi/Features/PetStore.cs
```

---

## TDD Cycle Tracking

Create a tracking file to monitor progress:

**File**: `specs/004-minimal-api-refactoring/tdd-cycles.md`

```markdown
# TDD Cycles - Feature 004

## Cycle 1: Initial RED
- Date: 2024-XX-XX
- Changes: Created TagEndpoints.cs.mustache, EndpointMapper.cs.mustache
- Build: ✅ SUCCESS
- Tests: ❌ RED (0/7 passed)
- Failures: 404 Not Found on all endpoints

## Cycle 2: Route Registration
- Date: 2024-XX-XX
- Changes: Fixed EndpointMapper to call Map{Tag}Endpoints()
- Build: ✅ SUCCESS
- Tests: ✅ GREEN (2/7 passed) - GetPetById, DeletePet
- Failures: AddPet/UpdatePet validation errors

## Cycle 3: Validation Wiring
- Date: 2024-XX-XX
- Changes: Added IValidator<T> injection to POST/PUT endpoints
- Build: ✅ SUCCESS
- Tests: ✅ GREEN (4/7 passed)
- Failures: 500 errors on CRUD operations (no logic)

## Cycle 4: Logic Injection
- Date: 2024-XX-XX
- Changes: Injected PetStore CRUD logic into TagEndpoints template
- Build: ✅ SUCCESS
- Tests: ✅ GREEN (7/7 passed) - **DONE**
```

---

## Success Criteria

✅ **SC-004**: All Feature 003 tests pass (7/7 GREEN)  
✅ **SC-001**: No FastEndpoints references in generated code  
✅ **SC-002**: Route groups by OpenAPI tag working  
✅ **SC-003**: FluentValidation manual pattern working  
✅ **SC-005**: Build with zero warnings  
✅ **SC-006**: Swagger UI accessible at `/swagger`  
✅ **SC-007**: Live server CRUD operations working (curl validation)

---

## Next Steps

After all tests GREEN:
1. **Run curl validation** (see Feature 003 session for commands)
2. **Commit changes**:
   ```bash
   git add generator/ test-output/ specs/004-minimal-api-refactoring/
   git commit -m "feat(004): Implement Minimal API generator with TDD"
   ```
3. **Update plan.md** with completion status
4. **Generate summary** (via speckit or manual)
5. **Proceed to Feature 005** (next refactoring target)
