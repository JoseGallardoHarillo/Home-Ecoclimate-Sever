package actuator;

import java.util.List;
import java.util.stream.Stream;

import actuator.common.CommandPublisher;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.mqtt.MqttClient;
import sensor.data.Temperature;

public final class TempCmdPublisher extends CommandPublisher<Temperature> {

	final private static double TEMPERATURE_THRESHOLD = 24.0;
	final private static short MIN_TEMP_INDEX_FOR_AC = 60;
	final private static byte MIN_TEMP_INDEX_FOR_HEATER = -60;
	final private static byte MAX_WB_ANGLE = Byte.MAX_VALUE;
	
	public TempCmdPublisher(MqttClient client) {
		super(client);
	}

	@Override
	final protected long getIndex(Stream<? extends List<Temperature>> series) {
		return series
			.flatMapToDouble(m ->
				m.stream()
					.mapToDouble(Temperature::celsius)
					.average()
					.stream()
			)
			.min()
			.stream()
			.mapToLong(t -> Math.round((t - TEMPERATURE_THRESHOLD) * 10))
			.findFirst()
			.orElse(0);
	}

	@Override
	final protected Future<Void> handleIndex(String groupId, short tempCmdIndex) {
		byte tempIndex;
		short angle;
		if (tempCmdIndex >= MIN_TEMP_INDEX_FOR_AC) {
			tempIndex = (byte) (tempCmdIndex - 50);
			angle = MAX_WB_ANGLE;
		} else {
			tempIndex = tempCmdIndex > MIN_TEMP_INDEX_FOR_HEATER
				? 0
				: (byte) (tempCmdIndex + 50);
			angle = (short) (tempCmdIndex * 4);
		}

		return CompositeFuture.join(
				publish(
					groupId + "/tempIndex",
					Byte.toString(tempIndex)
				),
				publish(
					groupId + "/angle",
					Short.toString(saturatingCastToUnsignedByte(angle))
				)
			)
			.map((Void) null);
	}

}
