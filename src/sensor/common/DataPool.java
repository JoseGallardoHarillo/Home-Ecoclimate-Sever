package sensor.common;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.Future;

/**
 * Specifies the operations needed on the database without depending
 * on any concrete implementation. All returned values are immutable.
 *
 * @param <T> Any resource representing a sensor type
 */
public interface DataPool<T extends Reading> {

	/**
	 * @return All records of the resource <b>T</b>
	 */
	public Future<Set<T>> getAll();
	
	/**
	 * @param lastSeconds Seconds to cover
	 * @return All records of the resource <b>T</b> from the last
	 * <b>lastSeconds</b> seconds.
	 */
	default public Future<Set<T>> getLast(long lastSeconds) {
		return getAll().map(r -> {
			long lastTime = Instant.now().getEpochSecond() - lastSeconds;
			return r.stream().filter(m -> m.time() >= lastTime).collect(Collectors.toSet());
		});
	}
	
	/**
	 * @param groupId	Group identifier of the entry
	 * @param sensorId	Sensor identifier of the entry
	 * @return All records of the resource <b>T</b> with the specified identifier
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
	 * @param groupId	Group identifier of the entry
	 * @param sensorId	Sensor identifier of the entry
	 * @param time		Timestamp of the entry
	 * @return Record of the resource <b>T</b> with the specified identifier
	 * and timestamp
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
	 * @param elem Entry to be added to the record
	 * @return <b>true</b> if the element was added, <b>false</b> if not
	 */
	public Future<Void> add(T elem);

}
