1. **Fix DAO Queries**
   - The master branch merge brought in a new `Transaction` model that fundamentally breaks the properties expected by the newly created DAOs (`TransactionEntity`).
   - Specifically, `confidence` was split to `categoryConfidence` and `parseConfidence`, and `merchant` was split to `merchantRaw` and `merchantNormalised`.
   - Update `TransactionDao` to match these new property names in its SQL queries.
2. **Pre-commit Steps**
   - Verify formatting and detekt checks.
   - Compile, run tests and submit.
