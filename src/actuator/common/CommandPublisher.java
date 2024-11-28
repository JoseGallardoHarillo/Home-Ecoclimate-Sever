package actuator.common;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import sensor.common.Reading;

/**
 * Abstract class to be implemented by each sensor reading type
 * to send the corresponding MQTT commands separated by
 * group ID.
 * 
 * @param <T> Any resource representing a sensor type
 */
public abstract class CommandPublisher<T extends Reading> {

	protected final MqttClient client;
	
	protected CommandPublisher(MqttClient client) {
		super();
		this.client = client;
	}
	
	/**
	 * @param series Data series grouped by group ID and sensor
	 * @return General index calculated from the data series
	 */
	abstract protected long getIndex(Stream<? extends List<T>> series);
	
	/**
	 * Method to be implemented (but not used directly) to send
	 * the MQTT message that represents the appropriate command.
	 * The values must not represent a numerical value larger than
	 * 8 bits, whether signed or unsigned.
	 * 
	 * @param groupId Group ID to which the index belongs
	 * @param index Index calculated by <b>getIndex</b>
	 * @return Asynchronous operation after sending the command according to the index
	 */
	abstract protected Future<Void> handleIndex(String groupId, short index);

	/**
	 * Utility method to be used only by classes that implement
	 * this abstract class.
	 * 
	 * @param cmdType Command type
	 * @param content Command value
	 * @return Return value of the MQTT message publish
	 */
	final protected Future<Integer> publish(String cmdType, String content) {
		return client.publish(
			cmdType,
			Buffer.buffer(content),
			MqttQoS.AT_LEAST_ONCE,
			false,
			false
		);
	}
	
	
	/**
	 * @param series Raw data series taken from the database
	 * @return Asynchronous operation after sending the appropriate commands
	 */
	final public Future<Void> sendCmds(Set<T> series) {
		return series.stream()
			.collect(getSensorReadingMapByIdsCollector())
			.entrySet()
			.stream()
			.map(this::sendSingleCmd)
			.reduce(
				Future.succeededFuture(),
				(x, y) -> CompositeFuture.join(x, y).map((Void) null)
			);
	}
	
	private Future<Void> sendSingleCmd(Map.Entry<String, ? extends Map<String, ? extends List<T>>> group) {
		short index = saturatingCastToUnsignedByte(getIndex(group.getValue()
			.entrySet()
			.stream()
			.map(Map.Entry::getValue)
		));
		
		return handleIndex(group.getKey(), index);
	}
	
	private static <T extends Reading> Collector<T, ?, Map<String, Map<String, List<T>>>>
		getSensorReadingMapByIdsCollector()
	{
		return Collectors.groupingBy(
			Reading::groupId,
			Collectors.groupingBy(Reading::sensorId)
		);
	}
	
	protected static short saturatingCastToUnsignedByte(long value) {
		return value > 255 ? 255 : (short) value;
	}
	
}
