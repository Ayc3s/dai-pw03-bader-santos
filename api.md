# Onsen Management - API

The Onsen Management API allows to manage clients and baths for a Japanese _onsen_ (thermal bath) business. It uses the HTTP protocol and the JSON format and it is based on a REST / CRUD pattern and manages two main domains:

- **Clients**, including client accounts and visit history.
- **Baths**, including bath administration and temperature measurements.

It has the following operations:

Client management :
- Create a client account.
- Update client
- Delete a client account.
- Record a visit to a bath.
- Retrieve a client’s visit history.

Bath management:
- Create a bath.
- Update bath information.
- Delete a bath.
- Record temperature measurements for a bath.

## Endpoints

### Client management

#### Create a new client

- `POST /clients`

Create a new client account.

#### Request

The request body must contain a JSON object with the following properties:

- `firstName` – The first name of the client
- `lastName` – The last name of the client
- `email` – The email address of the client
- `phone` – The phone number of the client

#### Response

The response body contains a JSON object with the following properties:

- `id` – The unique identifier of the client
- `firstName` – The first name of the client
- `lastName` – The last name of the client
- `email` – The email address of the client
- `phone` – The phone number of the client
#### Status codes

- `201` (Created) – The client has been successfully created
- `400` (Bad Request) – The request body is invalid
- `409` (Conflict) – The client already exists

### Update a client

- `PUT /clients/{id}`

Update a client by its ID.

#### Request

The request path must contain the ID of the client.

The request body must contain a JSON object with the following properties:

- `firstName` – The first name of the client
- `lastName` – The last name of the client
- `email` – The email address of the client
- `phone` – The phone number of the client
#### Response

The response body contains a JSON object with the following properties:

- `id` – The unique identifier of the client
- `firstName` – The first name of the client
- `lastName` – The last name of the client
- `email` – The email address of the client
- `phone` – The phone number of the client

#### Status codes

- `200` (OK) – The client has been successfully updated
- `400` (Bad Request) – The request body is invalid
- `404` (Not Found) – The client does not exist
- `409` (Conflict) – The email already exists
### Delete a client

- `DELETE /clients/{id}`

Delete a client by its ID.
#### Request

The request path must contain the ID of the client.
#### Response

The response body is empty.
#### Status codes

- `204` (No Content) – The client has been successfully deleted
- `404` (Not Found) – The client does not exist
### Record a visit

- `POST /clients/{id}/visits`

Record a visit of a client to a bath.
#### Request

The request path must contain the ID of the client.

The request body must contain a JSON object with the following properties:

- `bathId` – The unique identifier of the bath
- `visitedAt` – The visit date and time
#### Response

The response body contains a JSON object with the following properties:
- `id` – The unique identifier of the visit
- `clientId` – The client identifier
- `bathId` – The bath identifier
- `visitedAt` – The visit date and time
#### Status codes

- `201` (Created) – The visit has been successfully recorded
- `400` (Bad Request) – The request body is invalid
- `404` (Not Found) – The client or bath does not exist

### Get visit history

- `GET /clients/{id}/visits`

Get the visit history of a client.
#### Request

The request path must contain the ID of the client.
#### Response

The response body contains a JSON array with the following properties:

- `id` – The visit identifier
- `bathId` – The bath identifier
- `visitedAt` – The visit date and time
#### Status codes

- `200` (OK) – The visit history has been successfully retrieved
- `404` (Not Found) – The client does not exist
    
## Baths

### Create a new bath

- `POST /baths`

Create a new bath.

#### Request

The request body must contain a JSON object with the following properties:

- `name` – The name of the bath
- `location` – The location of the bath
- `type` – The type of the bath (hot, cold, indoor, outdoor)
- `maintenanceDone` – Indicates if maintenance has been performed
- `minTemperature` – Minimum allowed temperature
- `maxTemperature` – Maximum allowed temperature
#### Response

The response body contains a JSON object with the following properties:

- `id` – The unique identifier of the bath
- `name` – The name of the bath
- `location` – The location of the bath
- `type` – The type of the bath
- `maintenanceDone` – Maintenance status
- `minTemperature` – Minimum temperature
- `maxTemperature` – Maximum temperature
- `isActive` – Indicates if the bath is active
#### Status codes

- `201` (Created) – The bath has been successfully created
- `400` (Bad Request) – The request body is invalid
### Get many baths

- `GET /baths`

Get many baths.

#### Response

The response body contains a JSON array with the following properties:

- `id`
- `name`
- `location`
- `type`
- `isActive`

#### Status codes

- `200` (OK) – The baths have been successfully retrieved

### Get one bath

- `GET /baths/{id}`

Get one bath by its ID.

#### Request

The request path must contain the ID of the bath.

#### Response

The response body contains a JSON object describing the bath.

#### Status codes

- `200` (OK) – The bath has been successfully retrieved
- `404` (Not Found) – The bath does not exist
### Update a bath

- `PUT /baths/{id}`

Update a bath by its ID.

#### Request

The request path must contain the ID of the bath.

The request body must contain one or more of the following properties:

- `name`
- `location`
- `type`
- `maintenanceDone`
- `minTemperature`
- `maxTemperature`
- `isActive`

#### Response

The response body contains the updated bath.

#### Status codes

- `200` (OK) – The bath has been successfully updated
- `400` (Bad Request) – The request body is invalid
- `404` (Not Found) – The bath does not exist


### Delete a bath

- `DELETE /baths/{id}`

Delete a bath by its ID.

#### Request

The request path must contain the ID of the bath.

#### Response

The response body is empty.

#### Status codes

- `204` (No Content) – The bath has been successfully deleted
- `404` (Not Found) – The bath does not exist
### Record a temperature measurement

- `POST /baths/{id}/measurements`

Record a temperature measurement for a bath.

#### Request

The request path must contain the ID of the bath.

The request body must contain a JSON object with the following properties:

- `temperature` – The measured temperature
- `measuredAt` – The measurement timestamp (ISO 8601)
#### Response

The response body contains a JSON object with the following properties:

- `bathId` – The bath identifier
- `temperature` – The measured temperature
- `measuredAt` – The measurement timestamp

#### Status codes

- `201` (Created) – The measurement has been successfully recorded
- `400` (Bad Request) – The request body is invalid
- `404` (Not Found) – The bath does not exist