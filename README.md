# Nanobot Backend Documentation

## Introduction

Nanobot Backend is a Spring Boot application designed to process requests for various database collections.

## Prerequisites

Before running the Nanobot Backend application, please ensure that the following prerequisites are met:

1. **Docker**:

   - Docker engine installed and running on your system
   - https://docs.docker.com/engine/install/ubuntu/
   - Docker network has been created with `docker network create backend-net`

2. **Hosts File**:

   - Update the Host Machines Hosts File with `sudo nano /etc/hosts`
   - Add `127.0.0.1 nanobot-api`

3. **Nano Node**:

   - Running with necessary docker network.
   - Fully Synchronized with the Nano network.

4. **Banano Node**:

   - Running with necessary docker network.
   - Fully Synchronized with the Banano network.

5. **Work Server**:

   - Running with necessary docker network.
   - Ready to process work requests.
   - Only Nano forks need this; Monero and Bitcoin have no proof of work to
     generate.

Each additional currency needs whatever node it runs on, reachable from the API
container. Only the currencies with a document in the `currencies` collection
are processed, so a node that is not deployed simply has no currency pointing at
it. See `CHAINS.md` for how a currency is wired to its node.

6. (OPTIONAL) **Monero**:

   - `monerod` fully synchronized, reachable for daemon RPC.
   - A `monero-wallet-rpc` hot wallet, unlocked, one per environment.

7. (OPTIONAL) **Bitcoin**:

   - `bitcoind` fully synchronized and not pruned.
   - A dedicated spend-capable descriptor wallet, selected through the
     `/wallet/<name>` path on the RPC URL. A watch-only wallet can track
     deposits but cannot sign withdrawals.

8. **Replica Set Mongo Database**:

   - Running with necessary docker network.
   - Created and imported collections, data and indexes.

9. **.env.template Modifications**

   - Copy the template file with `sudo cp .env.template .env`
   - Edit the .env file with `sudo vi .env`

10. (OPTIONAL) **HTTP Requests**

    - Port Forward
    - `ssh -L <port>:localhost:<port> <user>@<server-ip>`
    - Health Check
    - `curl -X GET http://localhost:<port>/health -H "Content-Type: application/json"`

11. **File and Former Permissions Updated**:

   - Due to docker-compose specifying user: "1000:1000" you may need to:
   - `sudo chown -R 1000:1000 nanobot-api/`

## Instructions

To run the application, navigate to the root of the project directory and execute `docker compose up -d`

## Additional Documentation

- Transfer flow and invariants: `TRANSFERS.md`
- Nodes, deposits, withdrawals and representatives: `CHAINS.md`