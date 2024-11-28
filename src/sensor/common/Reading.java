package sensor.common;

import java.util.Objects;

/**
 * Clase abstracta que indica los métodos estándar mínimos de cualquier registro
 * de un sensor de la placa.
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
	 * @return Identificador del grupo de sensores que envía la entrada al registro
	 */
	final public String groupId() {
		return groupId;
	}
	
	/**
	 * @return Identificador del sensor que envía la entrada al registro
	 */
	final public String sensorId() {
		return sensorId;
	}
	
	/**
	 * @return Marca de tiempo de la entrada del registro enviada por el sensor
	 */
	final public long time() {
		return time;
	}
	
	/**
	 * @param <T> Subclase de esta clase abstracta
	 * @return Lectura con el tiempo modificado a la hora actual
	 */
	abstract public <T extends Reading> T withCurrentTime();
	
	/**
	 * @param tableName Nombre de la tabla donde insertar esta lectura
	 * @return Consulta de MySQL para insertar este registro en la base de datos.
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
