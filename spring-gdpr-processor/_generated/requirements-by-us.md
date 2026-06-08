# Requirements — spring-gdpr-processor, grouped by User Story

Auto-generated companion to `requirements.md`. Tests link to a User Story via the javadoc tag `@spec.us=US-NNN-slug` (the slug points to a User Story defined in `PRODUCT.md`). Implementation-detail tests with no `@spec.us` are collected at the bottom; declared User Stories in PRODUCT.md with zero linked tests are listed as **not implemented yet**.

## Coverage

- Total tests scanned: **3**
- Tests linked to a User Story: **3**
- Tests without `@spec.us` (implementation detail): **0**
- User Stories declared in PRODUCT.md: **0**
- User Stories with at least one linked test: **0**
- User Stories declared but **not yet implemented**: **0**

## `REQ-GDPR-015`  _(unknown to PRODUCT.md)_

- `FR-(root).ProcessingRecordCategory#csvRowListsTheDistinctTouchedCategoriesSortedAndPipeJoined`
  - **Then**: the categories column lists the distinct categories, sorted and pipe-joined
- `FR-(root).ProcessingRecordCategory#markdownRowSurfacesTheCategory`
  - **Then**: the category appears in the row
- `FR-(root).ProcessingRecordCategory#uncategorisedFieldsLeaveTheCategoriesColumnEmpty`
  - **Then**: the categories column is empty (backward-compatible: no category is not an error)
