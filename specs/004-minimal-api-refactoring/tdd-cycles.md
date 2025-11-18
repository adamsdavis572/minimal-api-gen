# TDD Cycles - Feature 004: Minimal API Refactoring

**Purpose**: Track RED → GREEN iterations during Test-Driven refactoring from FastEndpoints to Minimal API

**Target**: Under 10 cycles (SC-005)

**Baseline**: Feature 003 - 7 integration tests all passing (GREEN)

---

## Cycle 0: Baseline Verification (GREEN ✅)

**Date**: 2025-11-17  
**Phase**: Pre-refactoring baseline

**Changes**: None (verification only)

**Build Status**: ✅ SUCCESS
- Generator: Built successfully
- Generated Code: Compiled with warnings (CS8618 nullable, CS1998 async)

**Test Results**: ✅ **7/7 PASSED** (GREEN)
- AddPet_ReturnsCreated ✅
- GetPetById_ReturnsOk ✅
- GetPetById_NotFound ✅
- UpdatePet_ReturnsOk ✅
- UpdatePet_NotFound ✅
- DeletePet_WithApiKey_ReturnsNoContent ✅
- DeletePet_NotFound ✅

---

## Cycle 1: Infrastructure Templates Modified (RED ❌)

**Date**: 2025-11-17  
**Phase**: Phase 2 - Foundational Infrastructure (T004-T010)

**Changes**:
- Modified `project.csproj.mustache`: Removed FastEndpoints packages, added Swashbuckle + FluentValidation
- Modified `program.mustache`: Removed FastEndpoints setup, added Minimal API setup (AddEndpointsApiExplorer, AddSwaggerGen, MapAllEndpoints)
- Regenerated test-output with `devbox run java -cp "target/*.jar:~/.m2/.../cli.jar" org.openapitools.codegen.OpenAPIGenerator generate -g aspnetcore-minimalapi`

**Build Status**: ❌ FAILED (57 errors)
- Generator: ✅ Built successfully (2.9s)
- Generated Code: ❌ Compilation failed

**Primary Errors**:
1. `Program.cs(2,19): error CS0234: The type or namespace name 'Extensions' does not exist` - Missing EndpointMapper class
2. `Features/*Endpoint.cs: error CS0246: 'FastEndpoints' could not be found` (56 occurrences) - Endpoint templates still use FastEndpoints base classes

**Root Cause**: Endpoint/Request templates not yet refactored - still generating FastEndpoints code

**Test Results**: ⚠️ **Cannot run** (compilation blocked)

**Next Step**: Phase 3 (US1) - Refactor Java generator logic to group operations by tag

**Test Execution Time**: 38ms

**Analysis**: Baseline confirmed - all FastEndpoints tests pass. Ready to begin refactoring.

**Next Actions**: 
1. Modify infrastructure templates (program.mustache, project.csproj.mustache)
2. Refactor Java generator logic (operationsByTag grouping)
3. Create Minimal API templates (TagEndpoints, EndpointMapper)
4. Begin TDD RED → GREEN cycles

---

## Cycle 1: Infrastructure Changes (Expected: RED ❌)

**Date**: TBD  
**Phase**: Phase 2 (Foundational)

**Changes**: 
- Modified project.csproj.mustache: Removed FastEndpoints packages, added Minimal API packages
- Modified program.mustache: Removed FastEndpoints setup, added Minimal API setup

**Build Status**: TBD

**Test Results**: TBD

**Failures**: TBD

**Next Actions**: TBD

---

## Cycle 2: Java Logic Refactoring (Expected: RED ❌)

**Date**: TBD  
**Phase**: Phase 3 (US1)

**Changes**: 
- Refactored postProcessOperationsWithModels(): Implemented operationsByTag grouping
- Added computed fields: tagPascalCase, operationIdPascalCase, returnType, successCode
- Modified apiTemplateFiles(): Removed endpoint.mustache mapping
- Modified supportingFiles(): Registered TagEndpoints.cs.mustache per tag

**Build Status**: TBD

**Test Results**: TBD

**Failures**: TBD

**Next Actions**: TBD

---

## Cycle 3: Template Creation (Expected: RED ❌ → Approaching GREEN)

**Date**: TBD  
**Phase**: Phase 4 (US2)

**Changes**: 
- Created TagEndpoints.cs.mustache with route group pattern
- Created EndpointMapper.cs.mustache with MapAllEndpoints() method
- Deleted FastEndpoints templates (endpoint.mustache, request.mustache, etc.)

**Build Status**: TBD

**Test Results**: TBD

**Failures**: TBD

**Next Actions**: TBD

---

## Cycle 4+: Iterative Fixes (Target: GREEN ✅)

**Date**: TBD  
**Phase**: Phase 5 (US3)

**Changes**: TBD (iterative fixes based on test failures)

**Build Status**: TBD

**Test Results**: Target: **7/7 PASSED** (GREEN)

**Failures**: TBD

**Next Actions**: TBD

---

## Summary

**Total Cycles**: TBD (Target: <10)  
**Final Status**: TBD (Target: GREEN - 7/7 tests pass)  
**Time to GREEN**: TBD  
**Success Criteria Met**: TBD

**Key Learnings**: TBD
