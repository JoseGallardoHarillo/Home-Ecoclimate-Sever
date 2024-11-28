package sensor.common;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Administrador de la API REST junto con la base de datos para cualquier recurso
 * que represente la serie de datos de un sensor.
 * 
 * @param <T> Cualquier recurso que represente un tipo de sensor
 */
public class ResourceHandler<T extends Reading> {

	final private Class<T> resource;
	final private DataPool<T> data;
	final Function<Set<T>, Future<Void>> sensorSeriesToMQTT;

	final private Gson gson = new GsonBuilder().create();

	public ResourceHandler(
		Class<T> resource,
		DataPool<T> data,
		Function<Set<T>, Future<Void>> sensorSeriesToMQTT
	) {
		super();
		this.data = data;
		this.resource = resource;
		this.sensorSeriesToMQTT = sensorSeriesToMQTT;
	}
	
	/**
	 * Configura una estructura de routing para el recurso <b>T</b>
	 * con la siguiente estructura:
	 * <ul>
	 * <li>
	 * <b>GET /</b> => Recolectar todos los registros de este recurso.
	 * Si se añade un parámetro de query "lastseconds" con el número
	 * de segundos a abarcar en la consulta desde ahora al pasado se
	 * puede limitar el rango temporal.
	 * </li>
	 * <li>
	 * <b>GET /:groupId/:sensorId</b> => Recolectar todos los registros de este recurso
	 * con los identificadores de grupo y sensor indicados.
	 * </li>
	 * <li>
	 * <b>GET /:groupId/:sensorId/:time</b> => Devolver el registro de este recurso
	 * con los identificadores de grupo y sensor y la marca de tiempo indicados.
	 * Debe ser único.
	 * </li>
	 * <li>
	 * <b>POST /</b> => Añadir un registro del tipo <b>T</b>. No requiere
	 * del campo "time" en el cuerpo JSON, aunque si se envía igualmente
	 * será reescrito por la marca de tiempo del servidor. Cada vez que es
	 * llamado envía un comando como mensaje MQTT para actualizar el estado
	 * requerido de los dispositivos de climatización.
	 * </li>
	 * </ul>
	 * 
	 * @param vertx Instancia de Vert.X desde la que configurar el router
	 * @return Estructura de routing con los handlers ya configurados
	 */
	public Router getRouter(Vertx vertx) {
		Router router = Router.router(vertx);
		router.route("/*").handler(BodyHandler.create());
		router.get("/").handler(this::getAll);
		router.get("/:groupId/:sensorId").handler(this::getId);
		router.get("/:groupId/:sensorId/:time").handler(this::getIdTime);
		router.post().handler(this::addRecord);
		
		return router;
	}
	
	/**
	 * @return El nombre del tipo de recurso <b>T</b>
	 */
	public String resourceName() {
		return resource.getSimpleName();
	}

	private <P> Future<Void> sendJSONResponse(HttpServerResponse response, Optional<P> payload) {
		return payload
			.map(queryResult ->
				response
					.putHeader("content-type", "application/json; charset=utf-8")
					.setStatusCode(200)
					.end(gson.toJson(queryResult))
			)
			.orElseGet(() ->
				response
					.putHeader("content-type", "application/json; charset=utf-8")
					.setStatusCode(204)
					.end()
			);
	}
	
	private <P extends Collection<T>> Future<Void> sendJSONCollectionResponse(HttpServerResponse response, P payload) {
		return sendJSONResponse(response, Optional.of(payload).filter(p -> !p.isEmpty()));
	}
	
	private Future<Void> getAll(RoutingContext routingContext) {
		Future<Set<T>> query;
		
		try {
			long lastSeconds = Long.parseLong(routingContext.request().getParam("lastseconds"));
			query = data.getLast(lastSeconds);
		} catch (Exception ex) {
			query = data.getAll();
		}
		
		return query.flatMap(
			r -> sendJSONCollectionResponse(routingContext.response(), r)
		);
	}

	private Future<Void> getId(RoutingContext routingContext) {
		String groupId = routingContext.request().getParam("groupId");
		String sensorId = routingContext.request().getParam("sensorId");
		
		return data.getById(groupId, sensorId).flatMap(
			r -> sendJSONCollectionResponse(routingContext.response(), r)
		);
	}

	private Future<Void> getIdTime(RoutingContext routingContext) {
		String groupId = routingContext.request().getParam("groupId");
		String sensorId = routingContext.request().getParam("sensorId");
		long time = Long.parseLong(routingContext.request().getParam("time"));
		
		return data.getByIdAndTime(groupId, sensorId, time).flatMap(
			r -> sendJSONResponse(routingContext.response(), r)
		);
	}

	private Future<Void> addRecord(RoutingContext routingContext) {
		Future<T> start;
		try {
			start = Future.succeededFuture(
				bodyToJSON(routingContext.getBodyAsString()).withCurrentTime()
			);
		} catch (JsonSyntaxException ex) {
			start = Future.failedFuture(ex);
		}
		
		return start
			.flatMap(data::add)
			.map(201)
			.otherwise(ex -> ex instanceof JsonSyntaxException ? 400 : 500)
			.flatMap(statusCode ->
				routingContext
					.response()
					.setStatusCode(statusCode)
					.end()
			)
			.flatMap(x -> data.getLast(60))
			.flatMap(sensorSeriesToMQTT);
	}
	
	private T bodyToJSON(String body) throws JsonSyntaxException {
		if (body == null || body.equals(""))
			throw new JsonSyntaxException("The payload for adding an entry cannot be empty");
		
		return gson.fromJson(body, resource);
	}
	
}
