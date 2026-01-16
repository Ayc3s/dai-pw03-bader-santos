# practical-work-3

`Author` : Gabriel Bader, Mauro Santos

# Introduction

In this document, we will see the installation of the project, including the setup of the Vm and how to work with our project.


# Overview

The Onsen Management App allows to manage clients and baths for a Japanese onsen (thermal bath) business. It uses the HTTP protocol and the JSON format and it is based on a REST / CRUD pattern and manages two main domains:

The clients and the Baths

# API

You can find the entire description of Our API here [api.md](api.md)

# Web infrastructure

## VM installation

### Initial configuration
To install and configure the VM, we used the great documentation of our units.

You can find it here : https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/main/11.03-ssh-and-scp/01-course-material/README.md

### Configure DNZ zone

We worked with Dynu to get our free DNS domain. 

1) Go to : https://www.dynu.com/en-US/
2) Create your account and register your new domain. Match your ip with your domain name.
3) Our exemple : dai-pw03-bader-santos.ddnsfree.com 
4) Manage your DSN Record, if not added, check if there is a record type for A or AAAA

![img.png](img/dns_record.png)


## Image configuration

You can find our previous project where we explain how to deploy an image to hcr github and various things 

Go to : https://github.com/MauroWasTaken/dai-pw02-bader-santos

## Launch the app

One your are connected to the vm :

```bash
ssh ubuntu@51.103.223.246
```

You can start the application if not started by :

```bash
cd cd dai-pw03-bader-santos/docker/
docker compose up app -d
```


After launching, you can acess to :

Treafik : https://traefik.dai-pw03-bader-santos.ddnsfree.com/

And you can play with the next section.

## Curl and testing the app

To play with the app, use the following :

### CURL exemple

1) Set the BASE url (easier)
``` bash
BASE="https://dai-pw03-bader-santos.ddnsfree.com"

```

2) If not added, you can add client / bath / record history

Bath 1
```
curl -X POST \
-H "Content-Type: application/json" \
-d '{"name":"Hinoki","location":"Kyoto","type":"hot","maintenanceDone":true,"minTemperature":38.0,"maxTemperature":42.0}' \
"$BASE/baths"
```
Bath 2
```
curl -X POST \
-H "Content-Type: application/json" \
-d '{"name":"Rotenburo","location":"Beppu","type":"outdoor","maintenanceDone":true,"minTemperature":36.0,"maxTemperature":41.0}' \
"$BASE/baths"
```

Bath 3
```
curl -X POST \
-H "Content-Type: application/json" \
-d '{"name":"Mizu","location":"Tokyo","type":"indoor","maintenanceDone":false,"minTemperature":34.0,"maxTemperature":39.0}' \
"$BASE/baths"
```

Client 1
```
curl -X POST \
-H "Content-Type: application/json" \
-d '{"firstName":"Alice","lastName":"Tanaka","email":"alice.tanaka+1@example.com","phone":"+41 79 000 00 01"}' \
"$BASE/clients"
```
Client 2
```
curl -X POST \
-H "Content-Type: application/json" \
-d '{"firstName":"Bob","lastName":"Suzuki","email":"bob.suzuki+1@example.com","phone":"+41 79 000 00 02"}' \
"$BASE/clients"
```

Visits history

```
# Visit 1 (client 1 -> bath 1)
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"bathId":1,"visitedAt":"2026-01-08T09:00:00Z"}' \
  "$BASE/clients/1/visits"

# Visit 2 (client 1 -> bath 2)
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"bathId":2,"visitedAt":"2026-01-08T10:30:00Z"}' \
  "$BASE/clients/1/visits"

# Visit 3 (client 2 -> bath 3)
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"bathId":3,"visitedAt":"2026-01-08T11:15:00Z"}' \
  "$BASE/clients/2/visits"

```

See all 

baths :
```
curl -X GET \
"$BASE/baths"
```
clients :
```
curl -X GET \
"$BASE/clients"
```

Exemple of one of the Results : (note : we used python to format nicely)
![img_1.png](img/curl_client_bath.png)


Find with last/first name
```
curl -X GET \
"$BASE/clients?firstName=Alice"

curl -X GET \
"$BASE/clients?lastName=Suzuki"

curl -X GET \
"$BASE/clients?firstName=Bob&lastName=Suzuki"
```

Modify a bath :
```
curl -X PUT \
-H "Content-Type: application/json" \
-d '{"id":999,"name":"Hinoki v2","location":"Kyoto Station","type":"indoor","maintenanceDone":false,"minTemperature":36.0,"maxTemperature":41.5,"isActive":true}' \
"$BASE/baths/1"
```
Modify a client
```
curl -X PUT \
-H "Content-Type: application/json" \
-d '{"firstName":"Alice","lastName":"Tanaka-Updated","email":"alice.tanaka+1@example.com","phone":"+41 79 000 00 99"}' \
"$BASE/clients/1"
```

Delete a bath :
```
curl -X DELETE \
"$BASE/baths/1"
```

# Cache strategy

Baths represent physical installations (hot/cold/indoor/outdoor baths). They are
typically created once and then rarely modified (mostly when a maintenance
is done, or when operational parameters are adjusted).

This makes baths a good candidate for caching, because:

Read operations (GET /baths, GET /baths/{id}) happen frequently.

Write operations (POST, PUT, DELETE) happen less often.

We therefore implemented a caching strategy that optimizes reads while keeping
data consistent.decided to focus our cache strategy why the model "validation model". In our app, the baths will be creater once, maybe updated, but mostly updated when we need a maintenance or we did a maintenance. So this cache strategy is very good because the baths are rarely modified and this allow us to gain performance.

## Implementation

### Create a bath (`POST /baths`)

- Store a timestamp for the created bath: `cache[id] = now`
- Invalidate the collection cache: `cache.remove(-1)`
- Return `Last-Modified: now`

### Update a bath (`PUT /baths/{id}`)

- Require `If-Unmodified-Since` to prevent overwriting changes:
    - If it does not match `cache[id]` → return `412 Precondition Failed`
- If the update is accepted:
    - Update timestamp: `cache[id] = now`
    - Invalidate the collection cache: `cache.remove(-1)`
    - Return `Last-Modified: now`

### Delete a bath (`DELETE /baths/{id}`)

- Require `If-Unmodified-Since`:
    - If it does not match `cache[id]` → return `412 Precondition Failed`
- If the delete is accepted:
    - Remove timestamp for this bath: `cache.remove(id)`
    - Invalidate the collection cache: `cache.remove(-1)`
    - Return `204 No Content`

## How to test