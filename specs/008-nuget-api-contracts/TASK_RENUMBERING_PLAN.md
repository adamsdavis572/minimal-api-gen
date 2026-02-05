# Task Renumbering Plan for Early Testing Infrastructure

## Changes Applied
✅ Task summary updated (126 → 130 tasks)
✅ Phase 2.5 inserted (T008-T014: Testing Infrastructure)
✅ Phase 3 header updated with test strategy

## Remaining Renumbering Required

### Phase 3.2: Template Creation
- OLD T011-T013 → NEW T018-T020 (templates)

### Phase 3.3: Generator Logic
- OLD T014-T019 → NEW T021-T026 (generator methods)

### Phase 3.4: Extension Methods
- OLD T020-T023 → NEW T027-T030 (extension templates)

### Phase 3.5: Modify Templates
- OLD T024-T028 → NEW T031-T035 (file paths, README)

### Phase 3.6: Testing (NEW SECTION - replaces old T029-T039)
- T033: Build generator
- T034: Generate with useNugetPackaging
- T035-T043: Verification tasks
- T044: Copy test stubs
- T045: Build Implementation

### Phase 3.7: Unit Tests (NEW SECTION)
- T046: GeneratedProjectStructureTests (NuGetPackaging category)
- T047: CsprojMetadataTests (NuGetPackaging category)
- T048: ProjectReferenceTests (NuGetPackaging category)

### Phase 3.8: Integration Test (NEW SECTION)
- T049: Create test:integration:with-nuget task
- T050: **RUN test:integration:with-nuget** (immediate validation)
- T051: Fix any issues
- T052: Run dotnet pack

### Phase 4: User Story 2 (renumber from old T040+)
- OLD T040-T042 → NEW T053-T055 (extension methods)
- OLD T043-T047 → NEW T056-T060 (Program.cs, README, docs)

### Phase 4.4: Unit Tests (NEW SECTION)
- T061: GeneratedProjectStructureTests (Baseline category)
- T062: CsprojMetadataTests (Baseline category)

### Phase 4.5: Integration Test (NEW SECTION)
- T063: Create test:integration:baseline task
- T064: Update Program.cs
- T065: **RUN test:integration:baseline** (immediate validation)
- T066: Verify validators NOT invoked

### Phase 5: User Story 3 (versioning - renumber from old T054+)
- OLD T054-T071 → NEW T067-T084 (versioning docs and tests)

### Phase 6: User Story 4 (metadata - renumber from old T072+)
- OLD T072-T097 → NEW T085-T100 (CLI options, metadata)

### Phase 6.4: Unit Tests (NEW SECTION - replaces old T092-T097)
- T101: Validator-specific tests (WithValidators category)

### Phase 6.5: Integration Test (NEW SECTION)
- T102: Create test:integration:with-validators task
- T103: **RUN test:integration:with-validators** (immediate validation)

### Phase 7: User Story 5 (symbols - renumber from old T098+)
- OLD T098-T109 → NEW T104-T115 (symbol package support)

### Phase 8: Polish & Integration (SIMPLIFIED - renumber from old T110+)
- OLD T110-T112 → NEW T116-T118 (Taskfile tasks)
- OLD T113-T116 → NEW T119-T122 (documentation)

### Phase 8.3: Regression Testing (NEW SECTION - replaces old Phase 8.3)
- T123: Create test:integration:all task
- T124: **RUN test:integration:all** (final regression)

### Phase 8.4: Final Metrics (SIMPLIFIED)
- T125-T127: Size, build time, completion checklist (from old T122-T124)

## Key Structural Changes

1. **Phase 2.5 Added**: Testing infrastructure setup BEFORE implementation
2. **Early Testing**: Each user story now tests immediately after implementation
3. **Integration Tests**: Taskfile-based, not xUnit wrappers
4. **Unit Tests**: Fast file/XML validation, triggered by integration tests
5. **Phase 8**: Simplified to regression + metrics only

## Total Tasks
- Before: 126
- After: 130 (added 7 testing tasks, removed 3 redundant wrapper tests)

## Next Steps
1. Apply systematic renumbering to tasks.md
2. Add new testing sections to each phase
3. Update cross-references in Implementation Strategy section
4. Update Appendix references
