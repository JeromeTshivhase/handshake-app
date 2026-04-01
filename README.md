# handshake-app

## Required Environment Variables

The following environment variables must be set for production deployments:

- `DB_URL` - JDBC URL for the database
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - Secret key for JWT signing (32+ chars, random)
- `MAIL_HOST` - SMTP host (default: smtp.gmail.com)
- `MAIL_PORT` - SMTP port (default: 587)
- `MAIL_USERNAME` - SMTP username
- `MAIL_PASSWORD` - SMTP password
- `MAIL_FROM` - Email address for sending notifications (default: trustymarkets@gmail.com)
- `PAYFAST_MERCHANT_ID` - Payfast merchant ID
- `PAYFAST_MERCHANT_KEY` - Payfast merchant key
- `PAYFAST_PASSPHRASE` - Payfast passphrase
- `PAYFAST_MODE` - Payfast mode (default: sandbox)
- `PAYFAST_RETURN_URL` - Payfast return URL
- `PAYFAST_CANCEL_URL` - Payfast cancel URL
- `PAYFAST_NOTIFY_URL` - Payfast notify URL
- `CLOUDINARY_CLOUD_NAME` - Cloudinary cloud name
- `CLOUDINARY_API_KEY` - Cloudinary API key
- `CLOUDINARY_API_SECRET` - Cloudinary API secret
- `CLOUDINARY_URL` - Optional single-value Cloudinary credential (`cloudinary://<api_key>:<api_secret>@<cloud_name>`)
- `CLOUDINARY_FOLDER` - Optional Cloudinary folder (default: `handshake-app/uploads`)
- `CORS_MAX_AGE_SECONDS` - Browser preflight cache duration (default: `86400`)
- `APP_HTTP_CACHE_PUBLIC_MAX_AGE_SECONDS` - Public API response cache duration (default: `60`)
- `HIBERNATE_FORMAT_SQL` - SQL formatting toggle (default: `false`)
- `DB_POOL_MAX_SIZE` - Hikari max pool size (default: `20`)
- `DB_POOL_MIN_IDLE` - Hikari min idle connections (default: `5`)

Set these variables in your deployment environment or in a `.env` file (if supported by your deployment tooling).

## Deploying to Render

This project is ready for deployment on [Render](https://render.com/):

1. Push your code to a Git repository (GitHub, GitLab, Bitbucket).
2. Create a new Web Service on Render, connecting your repo.
3. Render will detect the `render.yaml` and Dockerfile and build the app automatically.
4. Set all required environment variables in the Render dashboard (see above).
5. The app will be available at the Render-provided URL.

Uploads are stored in Cloudinary. Save `url` and `public_id` in your database for image display/deletion.
