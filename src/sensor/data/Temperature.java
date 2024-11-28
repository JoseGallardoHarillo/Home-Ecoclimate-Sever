package sensor.data;

import java.time.Instant;

import io.vertx.sqlclient.Row;
import sensor.common.Reading;

public final class Temperature extends Reading {
	
	private double celsius;
	
	public Temperature(String groupId, String sensorId, long time, double celsius) {
		super(groupId, sensorId, time);
		this.celsius = celsius;
	}

	public Temperature(Row sqlRow) {
		this(
			sqlRow.getString("groupId"),
			sqlRow.getString("sensorId"),
			sqlRow.getLong("time"),
			sqlRow.getDouble("celsius")
		);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Temperature withCurrentTime() {
		return new Temperature(
			groupId(),
			sensorId(),
			Instant.now().getEpochSecond(),
			celsius()
		);
	}
	
	public double celsius() {
		return celsius;
	}
	
	public String asSQLInsertQuery(String tableName) {
		return "INSERT INTO " + tableName
			+ " VALUES ('"
			+ groupId() + "', '"
			+ sensorId() + "', "
			+ time() + ", "
			+ celsius() + ");";
	}
	
	@Override
	public String toString() {
		return "Temperature [groupId=" + groupId
			+ ", sensorId=" + sensorId
			+ ", time=" + time
			+ ", celsius=" + celsius
			+ "]";
	}
	
}
