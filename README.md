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

Set these variables in your deployment environment or in a `.env` file (if supported by your deployment tooling).

## Deploying to Render

This project is ready for deployment on [Render](https://render.com/):

1. Push your code to a Git repository (GitHub, GitLab, Bitbucket).
2. Create a new Web Service on Render, connecting your repo.
3. Render will detect the `render.yaml` and Dockerfile and build the app automatically.
4. Set all required environment variables in the Render dashboard (see above).
5. The app will be available at the Render-provided URL.

Uploads are stored in the `/uploads` directory. For persistent uploads, use a Render Disk or external storage.
