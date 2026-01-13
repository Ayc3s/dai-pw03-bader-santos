package ch.heigvd;

import ch.heigvd.baths.Bath;
import ch.heigvd.baths.BathsController;
import ch.heigvd.baths.Measurement;
import ch.heigvd.clients.Client;
import ch.heigvd.clients.ClientsController;
import ch.heigvd.clients.Visit;
import io.javalin.Javalin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
  public static final int PORT = 8080;

  public static void main(String[] args) {
    Javalin app =
        Javalin.create();

    ConcurrentMap<Integer, Client> clients = new ConcurrentHashMap<>();
    ConcurrentMap<Integer, Bath> baths = new ConcurrentHashMap<>();

    ConcurrentMap<Integer, CopyOnWriteArrayList<Visit>> visitsByClientId = new ConcurrentHashMap<>();
    ConcurrentMap<Integer, CopyOnWriteArrayList<Measurement>> measurementsByBathId = new ConcurrentHashMap<>();

    AtomicInteger clientIdSeq = new AtomicInteger(1);
    AtomicInteger bathIdSeq = new AtomicInteger(1);
    AtomicInteger visitIdSeq = new AtomicInteger(1);

    // Controllers
    ClientsController clientsController = new ClientsController(clients, baths, visitsByClientId, clientIdSeq, visitIdSeq);
    BathsController bathsController = new BathsController(baths, measurementsByBathId, bathIdSeq);

    // Client management
    app.post("/clients", clientsController::create);
    app.put("/clients/{id}", clientsController::update);
    app.get("/clients", clientsController::getMany);
    app.get("/clients/{id}", clientsController::getOne);
    app.delete("/clients/{id}", clientsController::delete);
    app.post("/clients/{id}/visits", clientsController::recordVisit);
    app.get("/clients/{id}/visits", clientsController::getVisitHistory);

    // Bath management
    app.post("/baths", bathsController::create);
    app.get("/baths", bathsController::getMany);
    app.get("/baths/{id}", bathsController::getOne);
    app.put("/baths/{id}", bathsController::update);
    app.delete("/baths/{id}", bathsController::delete);
    app.post("/baths/{id}/measurements", bathsController::recordMeasurement);

    app.start(PORT);
  }
}
