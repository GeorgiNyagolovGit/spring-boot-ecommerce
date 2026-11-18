# Deploy to Render

The repository contains a Render Blueprint that creates:

- an always-on Docker Web Service using the `0.5c-512mb` plan;
- a paid PostgreSQL database using the `0.1c-256mb` plan;
- private database networking;
- a generated Base64 JWT secret;
- automatic deployments from the connected GitHub branch;
- a health check at `/actuator/health`.

At the current listed prices, the compute plans cost $7/month for the web
service and $6/month for PostgreSQL. The database is configured with 1 GB of
storage to avoid unnecessary storage cost for a demonstration project. Usage
beyond included platform allowances can still add charges.

## Before creating the Blueprint

Push the complete repository to GitHub. Do not commit `.env` or real secrets.

## Create the Blueprint

1. Sign in to <https://dashboard.render.com/>.
2. Select **New > Blueprint**.
3. Connect GitHub if needed and select this repository.
4. Keep the Blueprint path as `render.yaml`.
5. Give the Blueprint a name such as `marketplace-production`.
6. Enter the prompted environment values:
   - `CORS_ALLOWED_ORIGINS`: the exact frontend HTTPS origin. If the included
     frontend is served by this backend, use the final Render service URL,
     such as `https://marketplace-backend.onrender.com`.
   - `CORS_ALLOWED_ORIGIN_PATTERNS`: leave empty unless wildcard frontend
     preview domains are required.
   - `GOOGLE_CLIENT_ID`: the Google OAuth Web client ID, or leave empty if
     Google login is not being used yet.
7. Review the two paid resources and select **Apply**.

Render obtains the database host, port, name, username and password directly
from the managed PostgreSQL instance. Do not add those values manually.

## Verify the deployment

Wait until the database is available and the web service shows **Live**. Then
open:

```text
https://YOUR-SERVICE.onrender.com/actuator/health
https://YOUR-SERVICE.onrender.com/swagger-ui/index.html
```

The health endpoint should return:

```json
{"status":"UP"}
```

The first application start runs all Liquibase migrations and can take longer
than later restarts. If deployment fails, open the Web Service and inspect the
**Logs** tab.

## Later updates

Push a commit to the GitHub branch connected to the Blueprint. The
`autoDeployTrigger: commit` setting starts a new Render deployment
automatically.

## Billing safety

Open **Workspace Settings > Billing** after creation and verify that only the
expected Web Service and PostgreSQL database exist. Suspend or delete both
resources when they are no longer needed; deleting only the web service does
not delete the database.
