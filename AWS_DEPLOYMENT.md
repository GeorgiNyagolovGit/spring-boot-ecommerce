# AWS deployment

The recommended setup is:

- Elastic Beanstalk, Docker on 64-bit Amazon Linux 2023
- Application Load Balancer managed by Elastic Beanstalk
- Amazon RDS for PostgreSQL
- HTTPS certificate in AWS Certificate Manager (ACM)
- Route 53 for a custom domain, if needed

The application listens on port `8080`. Elastic Beanstalk checks
`/actuator/health`, and the `prod` Spring profile reads its PostgreSQL and
security configuration from environment variables.

## 1. Create the PostgreSQL database

In **RDS > Create database** choose PostgreSQL and create a database named
`javacommerce`. For a real production environment, disable public access.

Place RDS and Elastic Beanstalk in the same VPC. On the RDS security group,
allow inbound TCP `5432` **from the Elastic Beanstalk instance security
group**, not from the whole internet.

Save these values:

- RDS endpoint (host only, without `jdbc:postgresql://`)
- port (normally `5432`)
- database name
- master/application username
- password

Liquibase creates and updates the tables when the application starts.

## 2. Create the Elastic Beanstalk application

Install and configure the AWS and EB CLIs, then authenticate with an IAM user
or role that can manage Elastic Beanstalk and its supporting resources.

From the repository root:

```bash
aws configure
eb init
```

Select the intended AWS region, create/select an application, and choose the
**Docker running on 64-bit Amazon Linux 2023** platform. Then create an
environment:

```bash
eb create marketplace-backend-prod --elb-type application
```

The first environment creation can take several minutes. The repository's
`.ebextensions/01-application.config` configures port `8080`, the production
profile, and the health check.

## 3. Configure environment variables

Generate a JWT key containing at least 32 random bytes and encode it as Base64:

```bash
openssl rand -base64 48
```

Set the variables in **Elastic Beanstalk > Environments > Configuration >
Updates, monitoring, and logging > Runtime environment variables**:

| Variable | Value |
| --- | --- |
| `DB_HOST` | RDS endpoint |
| `DB_PORT` | `5432` |
| `DB_NAME` | `javacommerce` |
| `DB_USERNAME` | database username |
| `DB_PASSWORD` | database password |
| `JWT_SECRET` | generated Base64 value |
| `GOOGLE_CLIENT_ID` | Google Web client ID, if Google login is used |
| `CORS_ALLOWED_ORIGINS` | exact frontend URL, for example `https://app.example.com` |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | optional wildcard pattern |

`DB_PASSWORD` and `JWT_SECRET` must not be committed to Git. For stronger
secret management, store them in AWS Secrets Manager and inject them into the
environment through your deployment pipeline.

The same settings can be applied with the EB CLI (be aware that values can be
saved in shell history):

```bash
eb setenv DB_HOST=YOUR_RDS_ENDPOINT DB_PORT=5432 DB_NAME=javacommerce \
  DB_USERNAME=YOUR_DB_USER DB_PASSWORD=YOUR_DB_PASSWORD \
  JWT_SECRET=YOUR_BASE64_SECRET \
  CORS_ALLOWED_ORIGINS=https://YOUR_FRONTEND_DOMAIN
```

## 4. Deploy and verify

Run the tests locally, then deploy:

```bash
./gradlew test
eb deploy
eb health
eb open
```

Verify:

```text
https://YOUR_ENVIRONMENT_URL/actuator/health
https://YOUR_ENVIRONMENT_URL/swagger-ui/index.html
```

The health endpoint must return HTTP `200` with `{"status":"UP"}`. If the
environment is unhealthy, inspect logs with `eb logs` and verify the RDS
security groups and all database variables.

## 5. HTTPS and domain

Request a certificate in ACM in the same region as the load balancer. Add an
HTTPS `443` listener to the Elastic Beanstalk load balancer, attach the
certificate, and redirect HTTP to HTTPS. Point a Route 53 alias record at the
load balancer. Finally, update `CORS_ALLOWED_ORIGINS` to the frontend's HTTPS
origin.

## Updating the application

After committing a tested version, deploy it with:

```bash
eb deploy
```

Use separate Elastic Beanstalk environments and databases for staging and
production. Enable RDS automated backups and deletion protection before using
the service with real data.
