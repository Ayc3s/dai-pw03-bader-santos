package ch.heigvd.baths;

import io.javalin.http.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BathsController {
    private final ConcurrentMap<Integer, Bath> baths;
    private final ConcurrentMap<Integer, CopyOnWriteArrayList<Measurement>> measurementsByBathId;
    private final AtomicInteger bathIdSeq;

    public BathsController(
            ConcurrentMap<Integer, Bath> baths,
            ConcurrentMap<Integer, CopyOnWriteArrayList<Measurement>> measurementsByBathId, AtomicInteger bathIdSeq) {
        this.baths = baths;
        this.measurementsByBathId = measurementsByBathId;
        this.bathIdSeq = bathIdSeq;
    }

    // POST /baths
    public void create(Context ctx) {
        Bath input =
                ctx.bodyValidator(Bath.class)
                        .check(b -> b.name() != null, "Missing name")
                        .check(b -> b.location() != null, "Missing location")
                        .check(b -> b.type() != null, "Missing type")
                        .check(b -> b.maintenanceDone() != null, "Missing maintenanceDone")
                        .check(b -> b.minTemperature() != null, "Missing minTemperature")
                        .check(b -> b.maxTemperature() != null, "Missing maxTemperature")
                        .get();

        validateBathType(input.type()); // hot, cold, ...

        if (input.minTemperature() > input.maxTemperature()) {
            throw new BadRequestResponse("minTemperature must be <= maxTemperature");
        }

        int id = bathIdSeq.getAndIncrement();

        Bath created =
                new Bath(
                        id,
                        input.name(),
                        input.location(),
                        input.type(),
                        input.maintenanceDone(),
                        input.minTemperature(),
                        input.maxTemperature(),
                        true);

        baths.put(id, created);

        ctx.status(HttpStatus.CREATED);
        ctx.json(created);
    }


    // GET /baths/{id}
    public void getOne(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        Bath bath = baths.get(id);
        if (bath == null) {
            throw new NotFoundResponse();
        }

        ctx.json(bath);
    }

    // GET /baths
    public void getMany(Context ctx) {
        ctx.json(baths.values());
    }

    // PUT /baths/{id}
    public void update(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        if (!baths.containsKey(id)) throw new NotFoundResponse();

        Bath input = ctx.bodyAsClass(Bath.class);

        Bath updated = new Bath(
                id,
                input.name(),
                input.location(),
                input.type(),
                input.maintenanceDone(),
                input.minTemperature(),
                input.maxTemperature(),
                input.isActive()
        );

        validateBathType(input.type()); // hot, cold, ...

        baths.put(id, updated);
        ctx.status(HttpStatus.OK).json(updated);
    }


    // DELETE /baths/{id}
    public void delete(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        if (!baths.containsKey(id)) {
            throw new NotFoundResponse();
        }

        baths.remove(id);
        measurementsByBathId.remove(id);

        ctx.status(HttpStatus.NO_CONTENT);
    }

    // POST /baths/{id}/measurements
    public void recordMeasurement(Context ctx) {
        Integer bathId = ctx.pathParamAsClass("id", Integer.class).get();

        Bath bath = baths.get(bathId);
        if (bath == null) {
            throw new NotFoundResponse();
        }

        MeasurementRequest req =
                ctx.bodyValidator(MeasurementRequest.class)
                        .check(r -> r.temperature() != null, "Missing temperature")
                        .check(r -> r.measuredAt() != null, "Missing measuredAt")
                        .get();

        Measurement m = new Measurement(bathId, req.temperature(), req.measuredAt());

        measurementsByBathId
                .computeIfAbsent(bathId, ignored -> new CopyOnWriteArrayList<>())
                .add(m);

        ctx.status(HttpStatus.CREATED).json(m);
    }

    private static void validateBathType(String type) {
        if (type == null) {
            throw new BadRequestResponse("Missing type");
        }

        String t = type.trim().toLowerCase();
        if (!t.equals("hot") && !t.equals("cold") && !t.equals("indoor") && !t.equals("outdoor")) {
            throw new BadRequestResponse("type must be one of: hot, cold, indoor, outdoor");
        }
    }

}