# WooGit V1 — Requirement Traceability

This matrix connects product requirements to implementation areas and evidence.

| Requirement | Contract | Execution area | Required evidence |
|---|---|---|---|
| Direct WooCommerce connection | V1_API_CONTRACT | E5/E6 | API integration tests |
| No WooGit server | V1_DATA_CONTRACTS / V1_NOTIFICATION_SPEC | Entire V1 | Architecture review |
| Local-first reads/writes | V1_DATA_CONTRACTS | E7/E8 | Repository + sync tests |
| Persistent pending queue | V1_SCHEMA | E3/E8 | Restart/recovery tests |
| Retry/backoff | V1_DATA_CONTRACTS | E8 | Network resilience tests |
| No silent overwrite | V1_DATA_CONTRACTS | E8 | Conflict tests |
| Conflict resolution | V1_DATA_CONTRACTS | E8/E12 | UI + domain tests |
| `date_modified_gmt` versioning | V1_DATA_CONTRACTS | E1/E8 | Version tests |
| Order management | V1_API_CONTRACT | E6/E12 | API + UI tests |
| Product management | V1_API_CONTRACT | E6/E12 | API + UI tests |
| Variable products/variations | V1_API_CONTRACT | E6/E12 | Integration tests |
| Product images | V1_API_CONTRACT | E6/E12 | Image operation tests |
| New-order notification | V1_NOTIFICATION_SPEC | E9/E12 | Background/notification tests |
| Notification deduplication | V1_NOTIFICATION_SPEC | E9 | Restart/dedup tests |
| Secure credentials | V1_PERMISSION_SECURITY | E4/E5 | Security tests |
| RTL/LTR-ready UI | V1_DESIGN_SPEC | E10/E11 | UI/accessibility review |
| Accessibility | V1_DESIGN_SPEC | E11/E13 | UI tests/manual audit |
| Error recovery | V1_ERROR_CATALOG | E5/E8/E12 | Error matrix tests |
| Database migrations | V1_SCHEMA | E3 | Migration tests |
| CI quality gate | Execution Plan E0 | E0/E13 | GitHub Actions evidence |

## Rule

A requirement is not considered complete until its implementation area and required evidence are both present. Documentation alone cannot mark a runtime requirement Done.
