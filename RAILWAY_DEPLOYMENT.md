# Deploy to Railway

The application is ready to run on Railway from the repository root. Railway
will detect the root `Dockerfile`, build the Spring Boot JAR and provide the
`PORT` used by the application.

## 1. Create the services

1. Push this repository to GitHub.
2. In Railway, choose **New Project → Deploy from GitHub repo** and select the
   repository.
3. On the project canvas, add a **PostgreSQL** database service.
4. Wait until PostgreSQL is available before deploying the backend.

Do not expose PostgreSQL publicly. The backend uses Railway's private network.

## 2. Configure backend variables

Open the backend service, select **Variables → RAW Editor**, and add the block
below. It assumes the database service is named `Postgres`; update that part if
you gave it another name.

```dotenv
SPRING_PROFILES_ACTIVE=prod
PGHOST=${{Postgres.PGHOST}}
PGPORT=${{Postgres.PGPORT}}
PGDATABASE=${{Postgres.PGDATABASE}}
PGUSER=${{Postgres.PGUSER}}
PGPASSWORD=${{Postgres.PGPASSWORD}}
JWT_SECRET=PASTE_A_RANDOM_SECRET_HERE
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=https://your-frontend.example
CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app
```

Generate `JWT_SECRET` locally and paste the result into Railway. Keep this value
stable after the first deployment, because changing it invalidates every issued
JWT:

```bash
openssl rand -hex 32
```

If Google login is used, also add the same OAuth Web client ID used by the
frontend:

```dotenv
GOOGLE_CLIENT_ID=your-google-web-client-id
```

Do not set `PORT`; Railway injects it automatically. Do not copy the localhost
database values from `.env.example` into Railway.

## 3. Configure and deploy the service

1. In the backend service settings, set **Healthcheck Path** to
   `/actuator/health` and keep the timeout at `300` seconds.
2. Apply the staged changes and deploy. Liquibase creates and seeds the schema
   during application startup.
3. In **Settings → Networking**, generate a public Railway domain.

No custom build or start command is required. Railway automatically detects the
root `Dockerfile`, whose entry point starts the generated JAR.

## 4. Verify the deployment

Replace `<backend-domain>` with the generated domain:

```bash
curl https://<backend-domain>/actuator/health
curl https://<backend-domain>/api/categories
```

The health endpoint must return HTTP `200` with `{"status":"UP"}`. Swagger is
available at:

```text
https://<backend-domain>/swagger-ui/index.html
```

Finally, configure the frontend API base URL to use the generated backend
domain. If the browser reports a CORS error, set `CORS_ALLOWED_ORIGINS` to the
frontend's exact origin (scheme and hostname, without a trailing slash) and
redeploy.

## CLI alternative

After creating a Railway project and PostgreSQL service, the backend can also
be uploaded directly from this directory:

```bash
railway login
railway link
railway up
```

The `.railwayignore` file excludes local build output, IDE metadata and secrets
from CLI uploads.
