package actuator;

import java.util.List;
import java.util.stream.Stream;

import actuator.common.CommandPublisher;
import io.vertx.core.Future;
import io.vertx.mqtt.MqttClient;
import sensor.data.Humidity;

public final class HumidCmdPublisher extends CommandPublisher<Humidity> {

	final private static double MAX_HUMIDITY_THRESHOLD = 90.0;
	final private static double MIN_HUMIDITY_THRESHOLD = 30.0;
	
	public HumidCmdPublisher(MqttClient client) {
		super(client);
	}

	@Override
	protected long getIndex(Stream<? extends List<Humidity>> series) {
		return series
			.flatMapToDouble(m ->
				m.stream()
					.mapToDouble(Humidity::ratio)
					.average()
					.stream()
			)
			.min()
			.stream()
			.mapToLong(h -> 
				h >= MAX_HUMIDITY_THRESHOLD
					? 255
					: h <= MIN_HUMIDITY_THRESHOLD
						? 0
						: Math.round(
							(h - MIN_HUMIDITY_THRESHOLD) * 255
							/ (MAX_HUMIDITY_THRESHOLD - MIN_HUMIDITY_THRESHOLD)
						)
			)
			.findFirst()
			.orElse(0);
	}

	@Override
	protected Future<Void> handleIndex(String groupId, short fanSpeed) {
		return publish(
				groupId + "/fanSpeed",
				Short.toString(fanSpeed)
			)
			.map((Void) null);
	}

}
