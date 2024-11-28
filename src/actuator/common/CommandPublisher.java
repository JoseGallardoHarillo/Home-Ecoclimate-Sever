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
 * Clase abstracta a implementar por cada tipo de lectura de sensor
 * para enviar los comandos MQTT que correspondan separados por
 * ID de grupo.
 * 
 * @param <T> Cualquier recurso que represente un tipo de sensor
 */
public abstract class CommandPublisher<T extends Reading> {

	protected final MqttClient client;
	
	protected CommandPublisher(MqttClient client) {
		super();
		this.client = client;
	}
	
	/**
	 * @param series Serie de datos agrupados por el ID de grupo y sensor
	 * @return Índice general calculado a partir de la serie de datos
	 */
	abstract protected long getIndex(Stream<? extends List<T>> series);
	
	/**
	 * Método a implementar (pero no usar directamente) para realizar
	 * el envío del mensaje MQTT que representa el comando apropiado.
	 * Los valores no deben representar un valor numérico con un tamaño
	 * mayor de 8 bits, sea con o sin signo.
	 * 
	 * @param groupId ID de grupo al que pertenece el índice
	 * @param index Índice calculado por <b>getIndex</b>
	 * @return Operación asíncrona tras enviar el comando según el índice
	 */
	abstract protected Future<Void> handleIndex(String groupId, short index);

	/**
	 * Método de utilidad a ser usado solo por las clases que implementen
	 * esta clase abstracta.
	 * 
	 * @param cmdType Tipo de comando
	 * @param content Valor del comando
	 * @return Valor de retorno de la publicación del mensaje MQTT
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
	 * @param series Serie de datos tomados en bruto desde la base de datos
	 * @return Operación asíncrona tras enviar los comandos apropiados
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
