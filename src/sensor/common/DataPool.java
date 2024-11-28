package sensor.common;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.Future;

/**
 * Indica las operaciones necesarias sobre la base de datos sin depender
 * de ninguna implementación concreta. Todos los valores devueltos son
 * inmutables.
 *
 * @param <T> Cualquier recurso que represente un tipo de sensor
 */
public interface DataPool<T extends Reading> {

	/**
	 * @return Todos los registros del recurso <b>T</b>
	 */
	public Future<Set<T>> getAll();
	
	/**
	 * @param lastSeconds Segundos a abarcar
	 * @return Todos los registros del recurso <b>T</b> de los últimos
	 * <b>lastSeconds</b> segundos.
	 */
	default public Future<Set<T>> getLast(long lastSeconds) {
		return getAll().map(r -> {
			long lastTime = Instant.now().getEpochSecond() - lastSeconds;
			return r.stream().filter(m -> m.time() >= lastTime).collect(Collectors.toSet());
		});
	}
	
	/**
	 * @param groupId	Identificador de grupo de la entrada
	 * @param sensorId	Identificador de sensor de la entrada
	 * @return Todos los registros del recurso <b>T</b> con el identificador indicado
	 */
	default public Future<Set<T>> getById(String groupId, String sensorId) {
		return getAll().map(
			r -> r.stream().filter(e ->
					e.groupId().equals(groupId)
					&& e.sensorId().equals(sensorId)
				)
				.collect(Collectors.toSet())
		);
	}
	
	/**
	 * @param groupId	Identificador de grupo de la entrada
	 * @param sensorId	Identificador de sensor de la entrada
	 * @param time		Marca de tiempo de la entrada
	 * @return Registro del recurso <b>T</b> con el identificador
	 * y la marca de tiempo indicados
	 */
	default public Future<Optional<T>> getByIdAndTime(
		String groupId,
		String sensorId,
		long time
	) {
		return getAll().map(
			r -> r.stream().filter(
					e -> e.groupId().equals(groupId)
						&& e.sensorId().equals(sensorId)
						&& e.time() == time
				)
				.findAny()
		);
	}
	
	/**
	 * @param elem Entrada a añadir al registro
	 * @return <b>true</b> si se ha añadido el elemento, <b>false</b> si no
	 */
	public Future<Void> add(T elem);

}
