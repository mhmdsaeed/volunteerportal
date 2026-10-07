# Deploying Volunteer Portal (free, on Oracle Cloud)

One small server runs everything with Docker Compose:

```
Internet ──HTTPS──► Caddy (free Let's Encrypt certificate, renewed automatically)
                      └──► Volunteer Portal app (Java 21, internal port 8080)
                              └──► MySQL 8.4 (internal only, data in a Docker volume)
```

Only ports 22 (SSH), 80 and 443 are open; MySQL and the app are never reachable from outside. Cost: $0 on Oracle Cloud's *Always Free* tier (free-tier terms change; check them when you sign up), plus optionally about $10/year for your own domain.

## 1. Create the server (Oracle Cloud)

1. Sign up at <https://www.oracle.com/cloud/free/>. A card is asked for to verify you; Always Free resources aren't charged. Tip: upgrading the account to *Pay As You Go* stops Oracle from reclaiming idle free VMs and still costs nothing within the free limits. Set a budget alert of $1 to be safe.
2. **Compute → Instances → Create instance**
   - Image: **Ubuntu 24.04**. Shape: **Ampere A1 (VM.Standard.A1.Flex)**, 2 OCPU, 12 GB RAM (Always Free allows up to 4 OCPU / 24 GB).
   - Add your SSH public key. If you get "Out of host capacity", try another availability domain or try again later.
3. **Reserve the public IP** (Networking → Reserved public IPs) and attach it, so the address never changes.
4. **Open ports 80 and 443** in the instance's subnet: *Networking → Virtual cloud networks → your VCN → Security lists → Default → Add ingress rules*: source `0.0.0.0/0`, TCP, destination ports `80` and `443`.
5. Oracle's Ubuntu images also block them in the VM's own firewall. SSH in (`ssh ubuntu@<ip>`) and run:

   ```bash
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
   sudo netfilter-persistent save
   ```

## 2. Get a domain name

Free: create a subdomain at <https://www.duckdns.org> (e.g. `volunteerportal.duckdns.org`) and set it to the server's public IP. Or point an `A` record of your own domain at the IP. Check with `ping volunteerportal.duckdns.org` before continuing: Caddy can only get a certificate once the name reaches the server.

## 3. Install and start

On the server:

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && newgrp docker

sudo git clone https://github.com/mhmdsaeed/volunteerportal.git /opt/volunteerportal
sudo chown -R ubuntu: /opt/volunteerportal
cd /opt/volunteerportal/deploy
cp .env.example .env
nano .env        # set DOMAIN, ACME_EMAIL and the secrets (openssl rand -base64 32 for each); MAIL_* optional
chmod 600 .env

docker compose up -d --build     # first build takes a few minutes
docker compose logs -f app       # wait for "Started VolunteerPortalApplication", Ctrl+C to stop following
```

**Email (optional).** "Forgot your password?" emails a reset link only if `MAIL_HOST` is set in `.env`, with `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` and `MAIL_FROM` (an address the mail service lets you send as). Use your email provider's SMTP settings or a transactional service (Brevo, Mailgun, Amazon SES, ...); Oracle Cloud blocks outgoing port 25, so use port 587. Without it, users who forget their password ask an admin, who sets a new one under Admin → Users → **Roles and password**. To check it works, use "Forgot your password?" with your own account and watch `docker compose logs app` for mail errors.

If the GitHub repository is private, clone with a [deploy key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/managing-deploy-keys) or a read-only access token instead.

Open `https://<your-domain>`. On the first start Flyway creates the database tables and the admin account from `.env` is created.

## 4. First login

1. Log in as `ADMIN_USERNAME` with `ADMIN_DEFAULT_PASSWORD`.
2. Change the password straight away on My Profile → **Change password** (the account password is only the initial one).
3. Optionally set `SEED_ADMIN=false` in `.env` and run `docker compose up -d`.
4. Never set `SPRING_PROFILES_ACTIVE=dev` on the server: it creates demo accounts with a known password.

## Backups

`deploy/backup.sh` writes a compressed database dump to `/var/backups/volunteerportal` and keeps 14 days. Schedule it nightly:

```bash
sudo mkdir -p /var/backups/volunteerportal && sudo chown ubuntu: /var/backups/volunteerportal
crontab -e   # add:
30 2 * * * /opt/volunteerportal/deploy/backup.sh >> /var/backups/volunteerportal/backup.log 2>&1
```

Copy the backups off the server too (e.g. Oracle Object Storage, which has a free tier, or download them now and then) - a backup on the same disk doesn't help if the VM is lost.

Restore a backup (this replaces the current data):

```bash
cd /opt/volunteerportal/deploy
gunzip -c /var/backups/volunteerportal/volunteerportal-YYYYMMDD-HHMMSS.sql.gz \
  | docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot volunteerportal'
```

## Updating

Run the tests on your PC first (`./mvnw verify`), push, then on the server:

```bash
cd /opt/volunteerportal && git pull
cd deploy && docker compose up -d --build
```

Database changes are applied automatically by Flyway when the new version starts.

## Everyday commands

| Task | Command (in `/opt/volunteerportal/deploy`) |
|---|---|
| Status | `docker compose ps` |
| App logs | `docker compose logs -f --tail=200 app` |
| Restart the app | `docker compose restart app` |
| Stop everything | `docker compose down` (data is kept in the volumes) |
| MySQL shell | `docker compose exec mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot volunteerportal'` |

## Monitoring

Add a free check at <https://uptimerobot.com> for `https://<your-domain>/login` (every 5 minutes, email alert).

## Security checklist

- [ ] Secrets in `.env` are long random values; `.env` is `chmod 600` and never committed.
- [ ] Admin password changed after the first login.
- [ ] Only ports 22, 80 and 443 open; SSH with keys only (Oracle's default).
- [ ] Nightly backups running and copied off the server.
- [ ] Not yet in the app: a limit on repeated login attempts (website and `/api/auth/login`).

## Mobile app

Point the app at `https://<your-domain>`. Android APKs can be installed directly for free (Play Store: one-time $25). iPhone needs an Apple Developer account ($99/year) and a Mac.

## Trying it on your PC first

With Docker Desktop: in `deploy/.env` set `DOMAIN=localhost` (Caddy then uses a self-signed certificate) and any `ACME_EMAIL`, run `docker compose up -d --build`, and open <https://localhost> (accept the certificate warning). Ports 80 and 443 must be free, and this uses its own database volume, separate from your development database.
