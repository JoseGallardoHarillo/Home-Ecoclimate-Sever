package sensor.common;

import java.util.Objects;

/**
 * Abstract class that defines the minimum standard methods for any sensor record
 * from the board.
 */
public abstract class Reading {
	
	protected String groupId;
	protected String sensorId;
	protected long time;
	
	protected Reading(String groupId, String sensorId, long time) {
		super();
		this.groupId = groupId;
		this.sensorId = sensorId;
		this.time = time;
	}
	
	/**
	 * @return Identifier of the sensor group that sends the entry to the record
	 */
	final public String groupId() {
		return groupId;
	}
	
	/**
	 * @return Identifier of the sensor that sends the entry to the record
	 */
	final public String sensorId() {
		return sensorId;
	}
	
	/**
	 * @return Timestamp of the record entry sent by the sensor
	 */
	final public long time() {
		return time;
	}
	
	/**
	 * @param <T> Subclass of this abstract class
	 * @return Reading with the time modified to the current hour
	 */
	abstract public <T extends Reading> T withCurrentTime();
	
	/**
	 * @param tableName Name of the table where to insert this reading
	 * @return MySQL query to insert this record into the database.
	 */
	abstract public String asSQLInsertQuery(String tableName);
	
	final public int hashCode() {
		return Objects.hash(groupId(), sensorId(), time());
	}
	
	final public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reading other = (Reading) obj;
		return Objects.equals(groupId(), other.groupId())
				&& Objects.equals(sensorId(), other.sensorId()) && time() == other.time();
	}
	
}
