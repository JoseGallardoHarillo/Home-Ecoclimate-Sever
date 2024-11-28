package sensor.data;

import java.time.Instant;

import io.vertx.sqlclient.Row;
import sensor.common.Reading;

public final class Humidity extends Reading {

	private double ratio;
	
	public Humidity(String groupId, String sensorId, long time, double ratio) {
		super(groupId, sensorId, time);
		this.ratio = ratio;
	}
	
	public Humidity(Row sqlRow) {
		this(
			sqlRow.getString("groupId"),
			sqlRow.getString("sensorId"),
			sqlRow.getLong("time"),
			sqlRow.getDouble("ratio")
		);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Humidity withCurrentTime() {
		return new Humidity(
			groupId(),
			sensorId(),
			Instant.now().getEpochSecond(),
			ratio()
		);
	}

	public double ratio() {
		return ratio;
	}
	
	public String asSQLInsertQuery(String tableName) {
		return "INSERT INTO " + tableName
			+ " VALUES ('"
			+ groupId() + "', '"
			+ sensorId() + "', "
			+ time() + ", "
			+ ratio() + ");";
	}

	@Override
	public String toString() {
		return "Humedad [groupId=" + groupId
			+ ", sensorId=" + sensorId
			+ ", time=" + time
			+ ", ratio=" + ratio
			+ "]";
	}

}
