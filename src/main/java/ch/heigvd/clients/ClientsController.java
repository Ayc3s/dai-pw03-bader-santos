package ch.heigvd.clients;

import ch.heigvd.baths.Bath;
import io.javalin.http.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientsController {
  private final ConcurrentMap<Integer, Client> clients;
  private final ConcurrentMap<Integer, Bath> baths;
  private final ConcurrentMap<Integer, CopyOnWriteArrayList<Visit>> visitsByClientId;
  private final AtomicInteger clientIdSeq;
  private final AtomicInteger visitIdSeq;

  public ClientsController(
      ConcurrentMap<Integer, Client> clients,
      ConcurrentMap<Integer, Bath> baths,
      ConcurrentMap<Integer, CopyOnWriteArrayList<Visit>> visitsByClientId,
      AtomicInteger clientIdSeq,
      AtomicInteger visitIdSeq) {
    this.clients = clients;
    this.baths = baths;
    this.visitsByClientId = visitsByClientId;
    this.clientIdSeq = clientIdSeq;
    this.visitIdSeq = visitIdSeq;
  }

  public void create(Context ctx) {
    Client input =
        ctx.bodyValidator(Client.class)
            .check(c -> c.firstName() != null, "Missing firstName")
            .check(c -> c.lastName() != null, "Missing lastName")
            .check(c -> c.email() != null, "Missing email")
            .check(c -> c.phone() != null, "Missing phone")
            .get();

    // Unique email
    for (Client existing : clients.values()) {
      if (input.email().equalsIgnoreCase(existing.email())) {
        throw new ConflictResponse();
      }
    }

    int id = clientIdSeq.getAndIncrement();
    Client created = new Client(id, input.firstName(), input.lastName(), input.email(), input.phone());
    clients.put(id, created);

    ctx.status(HttpStatus.CREATED);
    ctx.json(created);
  }

  public void update(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    Client current = clients.get(id);
    if (current == null) {
      throw new NotFoundResponse();
    }

    Client input =
        ctx.bodyValidator(Client.class)
            .check(c -> c.firstName() != null, "Missing firstName")
            .check(c -> c.lastName() != null, "Missing lastName")
            .check(c -> c.email() != null, "Missing email")
            .check(c -> c.phone() != null, "Missing phone")
            .get();

    // Email unique
    for (Client existing : clients.values()) {
      if (existing.id() != null
          && !existing.id().equals(id)
          && input.email().equalsIgnoreCase(existing.email())) {
        throw new ConflictResponse();
      }
    }

    Client updated = new Client(id, input.firstName(), input.lastName(), input.email(), input.phone());
    clients.put(id, updated);

    ctx.status(HttpStatus.OK);
    ctx.json(updated);
  }

    public void getOne(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        Client client = clients.get(id);

        if (client == null) {
            throw new NotFoundResponse();
        }

        ctx.json(client);
    }

    public void getMany(Context ctx) {
        String firstName = ctx.queryParam("firstName");
        String lastName = ctx.queryParam("lastName");

        List<Client> clients = new ArrayList<>();

        for (Client client : this.clients.values()) {
            if (firstName != null && !client.firstName().equalsIgnoreCase(firstName)) {
                continue;
            }

            if (lastName != null && !client.lastName().equalsIgnoreCase(lastName)) {
                continue;
            }

            clients.add(client);
        }

        ctx.json(clients);
    }


  public void delete(Context ctx) {
    Integer id = ctx.pathParamAsClass("id", Integer.class).get();

    Client removed = clients.remove(id);
    if (removed == null) {
      throw new NotFoundResponse();
    }
    ctx.status(HttpStatus.NO_CONTENT);
  }

  public void recordVisit(Context ctx) {
    Integer clientId = ctx.pathParamAsClass("id", Integer.class).get();

    if (!clients.containsKey(clientId)) {
      throw new NotFoundResponse();
    }

    VisitRequest req =
        ctx.bodyValidator(VisitRequest.class)
            .check(r -> r.bathId() != null, "Missing bathId")
            .check(r -> r.visitedAt() != null, "Missing visitedAt")
            .get();

    if (!baths.containsKey(req.bathId())) {
      throw new NotFoundResponse("Client or bath does not exist");
    }

    int visitId = visitIdSeq.getAndIncrement();
    Visit visit = new Visit(visitId, clientId, req.bathId(), req.visitedAt());

    visitsByClientId.computeIfAbsent(clientId, ignored -> new CopyOnWriteArrayList<>()).add(visit);

    ctx.status(HttpStatus.CREATED);
    ctx.json(visit);
  }

  public void getVisitHistory(Context ctx) {
    Integer clientId = ctx.pathParamAsClass("id", Integer.class).get();

    if (!clients.containsKey(clientId)) {
      throw new NotFoundResponse();
    }

    List<Visit> visits =
        visitsByClientId.getOrDefault(clientId, new CopyOnWriteArrayList<>());

    List<VisitHistoryItem> response = new ArrayList<>();
    for (Visit v : visits) {
      response.add(new VisitHistoryItem(v.id(), v.bathId(), v.visitedAt()));
    }

    ctx.status(HttpStatus.OK);
    ctx.json(response);
  }
}
