    ### **Project: Minimal API Generator (via Inheritance)**

**Goal:** To create a new, standalone OpenAPI Generator (`aspnetcore-minimalapi`) by *extending* the base `AspNetCoreServerCodegen` class. This new generator will initially *mimic* the FastEndpoints library, be validated by a test suite, and then be refactored to produce modern Minimal API code.

**Methodology:** This plan uses a "Test-Driven Refactoring" workflow based on your inheritance model.

**Environment and Development**
1. The core openapi-generator projecct is here: ~/scratch/git/openapi-generator3 This has the jar files necessary to run the unmodified generator, run the meta command etc 
2. all build commands java, maven or dotnet should be run with devbox using 'devbox run <command>'

-----

### **Phase 1: Analysis (As-Is) - The `AspNetCoreServerCodegen.java`**

**Goal:** Understand the *override points* and *templates* in the base `AspNetCoreServerCodegen.java` class that are used to implement the `fastendpoints` library.

1.  **Locate the Base Class:** We will analyse the source code of `AspNetCoreServerCodegen.java` from the official `openapi-generator` repository.

2.  **Identify `fastendpoints` Logic Blocks:** We will search for all logic blocks that are conditional on the `fastendpoints` library, i.e., `if ("fastendpoints".equals(library))`.

3.  **Map the Override Methods:** We will create a map of which methods we need to override and what logic they contain:

      * **`processOpts()`:** Contains the `fastendpoints`-specific `CliOption`s (like `useMediatR`).
      * **`postProcessOperationsWithModels()`:** Contains logic to add specific imports or properties for FastEndpoints.
      * **`apiTemplateFiles()`:** Contains the crucial line: `apiTemplateFiles.put("endpoint.mustache", ".cs");`. This tells the generator to use `endpoint.mustache` for each operation.
      * **`supportingFiles()`:** Contains logic to add the FastEndpoints-specific `program.cs.mustache`, `csproj.mustache`, `validator.mustache`, etc.

4.  **Identify Core Templates:** We will analyse all `.mustache` templates used by the `fastendpoints` library (located in the `resources/aspnetcore` directory).

      * **Operation Templates (`endpoint.mustache`, `validator.mustache`):**
          * `endpoint.mustache`: The core operation class. This is the **primary target for refactoring**.
          * `validator.mustache`: The FluentValidation class. This is **highly reusable** as Minimal APIs can use FluentValidation.
      * **Supporting Files (`program.cs.mustache`, `csproj.mustache`):**
          * These define the project setup, dependencies, and startup logic. They are **primary targets for refactoring**.
      * **Model Templates (`model.mustache`, `modelEnum.mustache`, etc.):**
          * **How they are used:** The base class uses the `modelTemplateFiles()` method to map these templates. They loop through a schema's properties (`{{vars}}`) to generate the C\# POCO (Plain Old C\# Object) classes that act as our Data Transfer Objects (DTOs).
          * **Connection:** These models are the "data contract". They are referenced by:
            1.  `endpoint.mustache` (as the generic types in `Endpoint<TRequest, TResponse>`).
            2.  `validator.mustache` (as the generic type in `Validator<TRequest>`).
          * **Refactoring Impact:** This is a key finding. These templates are **99-100% framework-agnostic**. A C\# class is a C\# class. This is a major benefit, as they **will not need to be modified**. Our new Minimal API endpoints will consume these exact same models, just in a different method signature (e.g., `(MyRequest request)` instead of `HandleAsync(MyRequest request)`).

-----

### **Phase 2: Scaffolding (Baseline via Inheritance)**

**Goal:** Create the new `aspnetcore-minimalapi` generator. This generator will `extend AspNetCoreServerCodegen` and *override* methods to perfectly replicate the `fastendpoints` library behaviour.

1.  **Use the `meta` command:** Create the new generator's project structure.

    ```bash
    java -jar openapi-generator-cli.jar meta -n aspnetcore-minimalapi -p org.openapitools.codegen.minimalapi
    ```

2.  **Create the Generator Class:**

      * Create the class `MinimalApiServerCodegen.java` in the new project.
      * This class **must extend** `org.openapitools.codegen.v3.generators.dotnet.AspNetCoreServerCodegen`.
      * Add the main `openapi-generator-cli` as a `provided` dependency in your `pom.xml` to access the base class.

3.  **Override Methods (to mimic FastEndpoints):**

      * We will now override the methods identified in Phase 1.
      * **Constructor:** Call `super()` and set any default values needed.
      * **`processOpts()`:** Override and re-implement the logic that adds the `fastendpoints` `CliOption`s.
      * **`apiTemplateFiles()`:** Override and re-implement the logic to put `endpoint.mustache` into the template map.
      * Repeat this pattern for `postProcessOperationsWithModels` and `supportingFiles`, copying *only* the FastEndpoints-related logic into our overridden methods.

4.  **Copy Templates:**

      * Create the template directory: `src/main/resources/aspnetcore-minimalapi`.
      * In `MinimalApiServerCodegen.java`, override `templateDir()`:
        `public String templateDir() { return "aspnetcore-minimalapi"; }`
      * Copy *all* templates identified in Phase 1 (e.g., `endpoint.mustache`, `validator.mustache`, `model.mustache`, etc.) from the original `resources/aspnetcore` directory into our new `aspnetcore-minimalapi` directory.

5.  **Register the Generator:**

      * Create `src/main/resources/META-INF/services/org.openapitools.codegen.CodegenConfig`.
      * The content of this file must be our new class's name:
        `org.openapitools.codegen.minimalapi.MinimalApiServerCodegen`
      * Override `getName()`:
        `public String getName() { return "aspnetcore-minimalapi"; }`

**Outcome:** We now have a clean, inheritance-based generator (`-g aspnetcore-minimalapi`). When built and run, it will produce a perfect **FastEndpoints project**.

-----

### **Phase 3: Baseline Validation & Test Framework (The "Golden Standard")**

**Goal:** Build a complete xUnit test suite that *proves* our new generator's output is a 100% correct and functional FastEndpoints application.

1.  **Build and Run Generator:**

      * Build your new generator (`mvn clean package`).
      * Run your generator against `petstore.oas`:
        ```bash
        java -jar target/my-generator.jar generate -g aspnetcore-minimalapi -i petstore.oas -o ./petstore-fastendpoints
        ```
      * This produces a **FastEndpoints project** in the `petstore-fastendpoints` folder.

2.  **Create the Test Project:**

      * Create a new **xUnit Test Project**.
      * Add a project reference to the newly generated `petstore-fastendpoints` project.

3.  **Implement Test Framework:**

      * Add `Microsoft.AspNetCore.Mvc.Testing` and `FluentAssertions`.
      * Create `CustomWebApplicationFactory.cs` pointing to the generated `Program` class.
      * Create test classes (`PetApiTests.cs`, etc.) and an `HttpClient`.

4.  **Write the "Golden Standard" Test Suite:**

      * For each operation (e.g., `AddPet`), write tests for the "happy path" (validating success) and the "unhappy path" (validating the FluentValidation 400 response).
      * **Crucially,** to test the "happy path", you must fill in the stubbed `HandleAsync` logic in the *generated code* to return a valid response.
      * Run all tests until they pass.

**Outcome:** You now have a complete, passing test suite. This suite **proves** the FastEndpoints logic (generated by *your* new generator) is correct. It is now our "contract".

-----

### **Phase 4: Test-Driven Refactoring (Generator Development)**

**Goal:** Iteratively refactor our `MinimalApiServerCodegen.java` class and its templates until the generator's output (now a Minimal API project) passes the *exact same* test suite from Phase 3.

This is the "Red-Green-Refactor" loop:

1.  **Start Refactoring (RED):**

      * We are now editing our *own* clean class: `MinimalApiServerCodegen.java`.
      * **Refactor Java Logic:**
          * `processOpts()`: Remove the FastEndpoints options (e.g., `useMediatR`). Add new `CliOption`s (e.g., `useRouteGroups`, `useGlobalExceptionHandler`).
          * `postProcessOperationsWithModels()`: Change the logic. Instead of FastEndpoints imports, group operations by `tag` and add this `operationsByTag` map to the template model.
          * `apiTemplateFiles()`: This is a major change. We no longer generate one file per operation.
            ```java
            @Override
            public Map<String, String> apiTemplateFiles() {
                Map<String, String> templates = super.apiTemplateFiles();
                // REMOVE the FastEndpoints logic:
                templates.remove("endpoint.mustache"); 
                // We will generate our new files via supportingFiles
                return templates; 
            }
            ```
          * `supportingFiles()`: This is where we add the new templates. We must update the Java logic to iterate over our `operationsByTag` map and, for each tag, add a `SupportingFile` entry for `TagEndpoints.cs.mustache`. This will tell the generator to run that template once for "Pet", once for "Store", etc. We also update the entries for `program.cs.mustache` etc.
      * **Refactor Templates:**
          * `csproj.mustache`: Remove `FastEndpoints`, add `FluentValidation.DependencyInjectionExtensions`.
          * `program.cs.mustache`: Remove `UseFastEndpoints()`, add `AddValidatorsFromAssemblyContaining()`, `AddExceptionHandler()`, and `app.MapAllEndpoints()`.
          * **Delete `endpoint.mustache`**.
          * **Create `EndpointMapper.cs.mustache`:** Loops over `operationsByTag` to generate `app.MapPetEndpoints()`, etc.
          * **Create `TagEndpoints.cs.mustache`:** This is our new "core" template. It generates the `MapPetEndpoints` class, loops through that tag's operations, and creates the `group.MapPost(...)` stubs, referencing the **unchanged models** from `model.mustache`.

2.  **Re-Generate and Test (RED -\> GREEN):**

      * **Re-build** your generator (`mvn clean package`).
      * **Re-run** your generator. The output in `petstore-fastendpoints` will now be a **Minimal API project**.
      * Point your xUnit Test Project (from Phase 3) to this *newly generated* project.
      * **Run the tests.** They will fail (this is **RED**).
      * **Iterate:** Fix your generator's templates (`.mustache`) and Java logic (`.java`). Re-build. Re-generate. Re-run tests.
      * **Migrate Logic:** As the tests fail, you will copy the business logic from your *test stubs* (from Phase 3) into the new `TagEndpoints.cs` generated file, replacing `Results.NotImplemented()` with `TypedResults.Ok(...)`.
      * Repeat this loop until all tests pass (this is **GREEN**).

**Outcome:** The *same* test suite that proved the FastEndpoints logic was correct *now proves* your new Minimal API logic is correct.

-----

### **Phase 5: Finalisation & Documentation**

**Goal:** Clean up and document the new generator.

1.  **Cleanup:** Delete any old, unused `.mustache` templates (like `endpoint.mustache`).
2.  **Documentation:** Write a new `README.md` for the `aspnetcore-minimalapi` generator, explaining its inheritance model.
3.  **Document Options:** Clearly document all the new `CliOption`s (`useRouteGroups`, etc.) for your users.