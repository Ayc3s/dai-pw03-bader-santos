package ch.heigvd.baths;

import io.javalin.http.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class BathsController {
    private final ConcurrentMap<Integer, Bath> baths;
    private final ConcurrentMap<Integer, CopyOnWriteArrayList<Measurement>> measurementsByBathId;
    private final AtomicInteger bathIdSeq;

    private final ConcurrentMap<Integer, LocalDateTime> bathsCache;

    // This is a magic number used to store the baths' list last modification date
    // As the ID for baths starts from 1, it is safe to reserve the value -1 for all baths
    private final Integer RESERVED_ID_TO_IDENTIFY_ALL_BATHS = -1;

    public BathsController(
            ConcurrentMap<Integer, Bath> baths,
            ConcurrentMap<Integer, CopyOnWriteArrayList<Measurement>> measurementsByBathId,
            AtomicInteger bathIdSeq,
            ConcurrentMap<Integer, LocalDateTime> bathsCache) {
        this.baths = baths;
        this.measurementsByBathId = measurementsByBathId;
        this.bathIdSeq = bathIdSeq;
        this.bathsCache = bathsCache;
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

        validateBathType(input.type());

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

        // Store the last modification date of this bath in the cache
        LocalDateTime now = LocalDateTime.now();
        bathsCache.put(created.id(), now);

        // Invalidate the cache for the collection of all baths
        bathsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_BATHS);

        ctx.status(HttpStatus.CREATED);
        ctx.header("Last-Modified", String.valueOf(now));
        ctx.json(created);
    }

    // GET /baths/{id}
    public void getOne(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        // Get the last known modification date of the baths
        LocalDateTime lastKnownModification =
                ctx.headerAsClass("If-Modified-Since", LocalDateTime.class).getOrDefault(null);

        // Check if the baths has been modified since the last known modification date
        if (lastKnownModification != null && bathsCache.get(id).equals(lastKnownModification)) {
            throw new NotModifiedResponse();
        }

        Bath bath = baths.get(id);
        if (bath == null) {
            throw new NotFoundResponse();
        }

        LocalDateTime now;
        if (bathsCache.containsKey(bath.id())) {
            // If it is already in the cache, get the last modification date
            now = bathsCache.get(bath.id());
            // Otherwise, set to the current date
        } else {
            now = LocalDateTime.now();
            bathsCache.put(bath.id(), now);
        }

        ctx.header("Last-Modified", String.valueOf(now));
        ctx.json(bath);
    }

    // GET /baths
    public void getMany(Context ctx) {
        // Get the last known modification date of all baths
        LocalDateTime lastKnownModification =
                ctx.headerAsClass("If-Modified-Since", LocalDateTime.class).getOrDefault(null);

        // Check if all baths have been modified since the last known modification date
        if (lastKnownModification != null
                && bathsCache.get(RESERVED_ID_TO_IDENTIFY_ALL_BATHS).equals(lastKnownModification)) {
            throw new NotModifiedResponse();
        }

        LocalDateTime now;
        if (bathsCache.containsKey(RESERVED_ID_TO_IDENTIFY_ALL_BATHS)) {
            // If it is already in the cache, get the last modification date
            now = bathsCache.get(RESERVED_ID_TO_IDENTIFY_ALL_BATHS);
        } else {
            // Otherwise, set to the current date
            now = LocalDateTime.now();
            bathsCache.put(RESERVED_ID_TO_IDENTIFY_ALL_BATHS, now);
        }

        // Add the last modification date to the response
        ctx.header("Last-Modified", String.valueOf(now));
        ctx.json(baths.values());
    }

    // PUT /baths/{id}
    public void update(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        // Get the last known modification date of the baths
        LocalDateTime lastKnownModification =
                ctx.headerAsClass("If-Unmodified-Since", LocalDateTime.class).getOrDefault(null);

        // Check if the baths has been modified since the last known modification date
        if (lastKnownModification != null && !bathsCache.get(id).equals(lastKnownModification)) {
            throw new PreconditionFailedResponse();
        }

        if (!baths.containsKey(id)) {
            throw new NotFoundResponse();
        }

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

        validateBathType(input.type());

        if (updated.minTemperature() > updated.maxTemperature()) {
            throw new BadRequestResponse("minTemperature must be <= maxTemperature");
        }

        baths.put(id, updated);

        // Store the last modification date of this bath in the cache
        LocalDateTime now = LocalDateTime.now();
        bathsCache.put(updated.id(), now);

        // Invalidate the cache for the collection of all baths
        bathsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_BATHS);

        ctx.status(HttpStatus.OK);
        ctx.header("Last-Modified", String.valueOf(now));
        ctx.json(updated);
    }

    // DELETE /baths/{id}
    public void delete(Context ctx) {
        Integer id = ctx.pathParamAsClass("id", Integer.class).get();

        LocalDateTime lastKnownModification =
                ctx.headerAsClass("If-Unmodified-Since", LocalDateTime.class).getOrDefault(null);

        if (lastKnownModification != null && !bathsCache.get(id).equals(lastKnownModification)) {
            throw new PreconditionFailedResponse();
        }

        if (!baths.containsKey(id)) {
            throw new NotFoundResponse();
        }

        baths.remove(id);
        measurementsByBathId.remove(id);

        // Invalidate the cache for this bath
        bathsCache.remove(id);

        // Invalidate the cache for the collection of all baths
        bathsCache.remove(RESERVED_ID_TO_IDENTIFY_ALL_BATHS);

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