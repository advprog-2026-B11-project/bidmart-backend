# Auth Module — Custom Business Metrics

Custom Micrometer metrics for the authentication module. They are exposed on the
existing Actuator endpoint `/actuator/prometheus` (no extra infrastructure) and are
scraped by the same Prometheus instance that feeds Grafana.

> Micrometer converts dotted metric names to Prometheus names: dots become
> underscores and counters get a `_total` suffix. Tags become labels.
> e.g. `bidmart.auth.login` (counter) → `bidmart_auth_login_total{result="..."}`.

## Metrics

| Micrometer name | Type | Tags (labels) | Emitted by | Meaning |
|---|---|---|---|---|
| `bidmart.auth.login` | counter | `result` = `success` / `failure` / `mfa_required` | `AuthServiceImpl.login` | Login attempts by outcome |
| `bidmart.auth.login.duration` | timer | `result` | `AuthServiceImpl.login` | Login latency distribution |
| `bidmart.auth.register` | counter | `result` = `success` / `failure` | `AuthServiceImpl.register` | Registration attempts by outcome |
| `bidmart.auth.mfa.verify` | counter | `result`, `method` = `totp` / `email` | `AuthServiceImpl.verifyMfaLogin` | MFA verification attempts |
| `bidmart.auth.email.verify` | counter | `result` | `AuthServiceImpl.verifyEmail` | Email verification link clicks |
| `bidmart.auth.token.refresh` | counter | `result` | `AuthServiceImpl.refreshToken` | Refresh-token rotations |
| `bidmart.auth.ratelimit.blocked` | counter | `endpoint` = `login` / `register` / `verify-mfa` | `AuthRateLimitFilter` | Requests rejected by rate limiter |

## Prometheus / Grafana queries (PromQL)

```promql
# Login success rate per second (5m window)
rate(bidmart_auth_login_total{result="success"}[5m])

# Login failure count
bidmart_auth_login_total{result="failure"}

# Login p95 latency (seconds)
histogram_quantile(0.95, sum(rate(bidmart_auth_login_duration_seconds_bucket[5m])) by (le))

# Average login duration
rate(bidmart_auth_login_duration_seconds_sum[5m])
  / rate(bidmart_auth_login_duration_seconds_count[5m])

# Registrations (success vs failure)
sum by (result) (bidmart_auth_register_total)

# MFA verifications split by method
sum by (method, result) (bidmart_auth_mfa_verify_total)

# Rate-limit blocks per endpoint
sum by (endpoint) (rate(bidmart_auth_ratelimit_blocked_total[5m]))
```

## Verifying after deploy

```bash
curl -s https://bidmart-backend-wn6p.onrender.com/actuator/prometheus | grep bidmart_auth_
# expect lines like: bidmart_auth_login_total, bidmart_auth_register_total,
# bidmart_auth_login_duration_seconds_count, bidmart_auth_ratelimit_blocked_total, ...
```

## Notes for profiling demo
`bidmart_auth_login_duration_seconds` is a good before/after signal for optimising the
login path — the main cost is `JwtAuthenticationFilter` reloading the user (with
`User.role` / `Role.permissions` `FetchType.EAGER`) on the authenticated request that
follows login.
