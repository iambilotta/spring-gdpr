# Requirements — spring-gdpr-processor

Auto-generated from test sources by tracegate. Do NOT edit by hand: edit the test javadoc / docstring instead and rerun. Single source of truth is the test code.

**Convention**: category from the test name (`*Test`=FR, `*NfrTest`=NFR, `*InvariantTest`=INV, `*ContractTest`=CON; Python file markers `*invariant*`/`*nfr*`/`*contract*` map the same way; Playwright E2E tests join as **E2E**). Spec from doc-comment tags `@spec.given` / `@spec.when` / `@spec.then` (plus optional `@spec.adr` / `@spec.us`). Tests without a complete spec are listed with `(spec missing)` so they're visible and lintable.

## Coverage

- Total tests scanned: **3**
- With complete spec javadoc: **3** (100%)
- FR: 3

## Module `(root)`

### Functional Requirements

#### `FR-(root).ProcessingRecordCategory#csvRowListsTheDistinctTouchedCategoriesSortedAndPipeJoined`

- **Given**: a ROPA record that touched an IDENTITY field and a CONTACT field
- **When**: the CSV row is rendered
- **Then**: the categories column lists the distinct categories, sorted and pipe-joined
- **User Story**: REQ-GDPR-015
- **File**: `spring-gdpr-processor/src/test/java/com/iambilotta/gdpr/processor/ProcessingRecordCategoryTest.java`

#### `FR-(root).ProcessingRecordCategory#markdownRowSurfacesTheCategory`

- **Given**: a ROPA record that touched a FINANCIAL field
- **When**: the markdown row is rendered
- **Then**: the category appears in the row
- **User Story**: REQ-GDPR-015
- **File**: `spring-gdpr-processor/src/test/java/com/iambilotta/gdpr/processor/ProcessingRecordCategoryTest.java`

#### `FR-(root).ProcessingRecordCategory#uncategorisedFieldsLeaveTheCategoriesColumnEmpty`

- **Given**: a ROPA record whose personal-data fields declared no category
- **When**: the CSV row is rendered
- **Then**: the categories column is empty (backward-compatible: no category is not an error)
- **User Story**: REQ-GDPR-015
- **File**: `spring-gdpr-processor/src/test/java/com/iambilotta/gdpr/processor/ProcessingRecordCategoryTest.java`
