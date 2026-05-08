package cordova.plugin.mymodbustcp;

import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.*;
import java.util.ArrayList;
import java.io.*;
import net.wimpi.modbus.*;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.io.*;
import net.wimpi.modbus.net.*;
import net.wimpi.modbus.procimg.*;
import net.wimpi.modbus.util.*;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

/**
 * This class echoes a string called from JavaScript.
 */
public class MyModbusTCP extends CordovaPlugin {

	private int timeout = 500;
	private int retries = 1;
 
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (action.equals("readHoldingRegister")) {
			cordova.getThreadPool().execute(() -> {
				try {
					String ip = args.getString(0);
					String offset = args.getString(1);
					String number = args.getString(2);
					if (!validateIp(ip, callbackContext)) return;
					this.readHoldingRegister(ip, offset, number, callbackContext);
				} catch (Exception e) {
					callbackContext.error("Thread error: " + e.getLocalizedMessage());
				}
			});
			return true;
		} else if (action.equals("readCoil")) {
			cordova.getThreadPool().execute(() -> {
				try {
					String ip = args.getString(0);
					String offset = args.getString(1);
					String number = args.getString(2);
					if (!validateIp(ip, callbackContext)) return;
					this.readCoil(ip, offset, number, callbackContext);
				} catch (Exception e) {
					callbackContext.error("Thread error: " + e.getLocalizedMessage());
				}
			});
			return true;
		} else if (action.equals("writeHoldingRegister")) {
			cordova.getThreadPool().execute(() -> {
				try {
					String ip = args.getString(0);
					String offset = args.getString(1);
					String values = args.getString(2);
					if (!validateIp(ip, callbackContext)) return;
					this.writeHoldingRegister(ip, offset, values, callbackContext);
				} catch (Exception e) {
					callbackContext.error("Thread error: " + e.getLocalizedMessage());
				}
			});
			return true;
		} else if (action.equals("writeCoil")) {
			cordova.getThreadPool().execute(() -> {
				try {
					String ip = args.getString(0);
					String offset = args.getString(1);
					String values = args.getString(2);
					if (!validateIp(ip, callbackContext)) return;
					this.writeCoil(ip, offset, values, callbackContext);
				} catch (Exception e) {
					callbackContext.error("Thread error: " + e.getLocalizedMessage());
				}
			});
			return true;
		} else if (action.equals("ping")) {
			cordova.getThreadPool().execute(() -> {
				try {
					String ip = args.getString(0);
					if (!validateIp(ip, callbackContext)) return;
					this.ping(ip, callbackContext);
				} catch (Exception e) {
					callbackContext.error("Thread error: " + e.getLocalizedMessage());
				}
			});
			return true;
		}
		return false;
	}

	private boolean validateIp(String ip, CallbackContext callbackContext) {
		Log.i("ModbusPlugin", "*******************************************************************");
		Log.i("ModbusPlugin", "******************************** INICIO ********************************");
		Log.i("ModbusPlugin", "*******************************************************************");
		Log.i("ModbusPlugin", "validateIp() → Iniciando validación para: " + ip);

		// 1. Validación de formato
		if (ip == null || ip.trim().isEmpty()) {
			Log.e("ModbusPlugin", "validateIp() → IP vacía o nula");
			callbackContext.error("IP inválida o vacía");
			return false;
		}

		String trimmed = ip.trim();
		String[] parts = trimmed.split("\\.");
		if (parts.length != 4) {
			Log.e("ModbusPlugin", "validateIp() → Formato incorrecto, no tiene 4 octetos");
			callbackContext.error("IP inválida: debe tener 4 octetos");
			return false;
		}

		try {
			for (String part : parts) {
				int value = Integer.parseInt(part);
				if (value < 0 || value > 255) {
					Log.e("ModbusPlugin", "validateIp() → Octeto fuera de rango: " + part);
					callbackContext.error("IP inválida: octeto fuera de rango (0-255)");
					return false;
				}
			}
		} catch (NumberFormatException e) {
			Log.e("ModbusPlugin", "validateIp() → Caracter no numérico en IP: " + e.getMessage());
			callbackContext.error("IP inválida: contiene caracteres no numéricos");
			return false;
		}

		// 2. Comprobación de comunicación con timeout real
		int timeoutMs = timeout; // tu variable global
		Log.i("ModbusPlugin", "validateIp() → Intentando conectar a " + trimmed + ":" + Modbus.DEFAULT_PORT + " con timeout " + timeoutMs + " ms");

		long start = System.currentTimeMillis();

		try (Socket socket = new Socket()) {

			SocketAddress socketAddress = new InetSocketAddress(trimmed, Modbus.DEFAULT_PORT);

			socket.connect(socketAddress, timeoutMs);

			long elapsed = System.currentTimeMillis() - start;
			Log.i("ModbusPlugin", "validateIp() → Conexión OK en " + elapsed + " ms");

			return true;

		} catch (Exception e) {

			long elapsed = System.currentTimeMillis() - start;
			Log.e("ModbusPlugin", "validateIp() → ERROR tras " + elapsed + " ms: " + e.getMessage());

			callbackContext.error("IP válida pero sin comunicación: " + e.getLocalizedMessage());
			return false;
		} finally {
			Log.i("ModbusPlugin", "*******************************************************************");
			Log.i("ModbusPlugin", "******************************** FIN ********************************");
			Log.i("ModbusPlugin", "*******************************************************************");
		}
	}

	private void readHoldingRegister(String ip, String offset, String number, CallbackContext callbackContext) {
		Log.i("ModbusPlugin", "------------------------------------------------------------");
		Log.i("ModbusPlugin", "readHoldingRegister() → INICIO");
		Log.i("ModbusPlugin", "IP: " + ip + " | offset: " + offset + " | number: " + number);

		TCPMasterConnection con = null;

		try {
			int port = Modbus.DEFAULT_PORT;
			int ref = Integer.parseInt(offset);
			int count = Integer.parseInt(number);

			Log.i("ModbusPlugin", "readHoldingRegister() → Resolviendo dirección IP...");
			InetAddress addr = InetAddress.getByName(ip);
			Log.i("ModbusPlugin", "readHoldingRegister() → IP resuelta: " + addr.getHostAddress());

			Log.i("ModbusPlugin", "readHoldingRegister() → Creando conexión JAMOD...");
			con = new TCPMasterConnection(addr);
			con.setTimeout(timeout);
			con.setPort(port);

			Log.i("ModbusPlugin", "readHoldingRegister() → Conectando a " + ip + ":" + port + " con timeout " + timeout + " ms...");
			long startConnect = System.currentTimeMillis();
			con.connect();
			long elapsedConnect = System.currentTimeMillis() - startConnect;
			Log.i("ModbusPlugin", "readHoldingRegister() → Conexión establecida en " + elapsedConnect + " ms");

			Log.i("ModbusPlugin", "readHoldingRegister() → Preparando petición Modbus...");
			ReadMultipleRegistersRequest reqMultiple = new ReadMultipleRegistersRequest(ref, count);

			Log.i("ModbusPlugin", "readHoldingRegister() → Creando transacción...");
			ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
			trans.setRetries(retries);
			trans.setRequest(reqMultiple);

			Log.i("ModbusPlugin", "readHoldingRegister() → Ejecutando transacción...");
			long startTrans = System.currentTimeMillis();
			trans.execute();
			long elapsedTrans = System.currentTimeMillis() - startTrans;
			Log.i("ModbusPlugin", "readHoldingRegister() → Transacción completada en " + elapsedTrans + " ms");

			ReadMultipleRegistersResponse resMultiple = (ReadMultipleRegistersResponse) trans.getResponse();

			Log.i("ModbusPlugin", "readHoldingRegister() → Procesando respuesta...");
			Register[] registers = resMultiple.getRegisters();
			JSONArray myResponse = new JSONArray();

			for (int i = 0; i < registers.length; i++) {
				int value = registers[i].getValue();
				Log.i("ModbusPlugin", "readHoldingRegister() → Registro[" + (ref + i) + "] = " + value);
				myResponse.put(value);
			}

			Log.i("ModbusPlugin", "readHoldingRegister() → ÉXITO, devolviendo valores al JS");
			callbackContext.success(myResponse);

		} catch (Exception exc) {
			Log.e("ModbusPlugin", "readHoldingRegister() → ERROR: " + exc.getMessage(), exc);
			callbackContext.error("ERROR: " + exc.getLocalizedMessage());

		} finally {
			if (con != null) {
				try {
					Log.i("ModbusPlugin", "readHoldingRegister() → Cerrando conexión...");
					con.close();
					Log.i("ModbusPlugin", "readHoldingRegister() → Conexión cerrada correctamente");
				} catch (Exception e) {
					Log.e("ModbusPlugin", "readHoldingRegister() → Error cerrando conexión: " + e.getMessage());
				}
			}

			Log.i("ModbusPlugin", "readHoldingRegister() → FIN");
			Log.i("ModbusPlugin", "------------------------------------------------------------");
		}
	}

	private void readCoil(String ip, String offset, String number, CallbackContext callbackContext) {
        TCPMasterConnection con = null; // the connection
		try {
			/* The important instances of the classes mentioned before */
			ModbusTCPTransaction trans = null; // the transaction
			ReadCoilsRequest reqMultiple = null; //
			ReadCoilsResponse resMultiple = null;

			/* Variables for storing the parameters */
			InetAddress addr = null; // the slave's address
			int port = Modbus.DEFAULT_PORT;
			int ref = Integer.parseInt(offset); // the reference; offset where to start reading from
			int count = Integer.parseInt(number); // the number of DI's to read

			// 2. Open the connection
			addr = InetAddress.getByName(ip);

			con = new TCPMasterConnection(addr);
			con.setTimeout(timeout);
			con.setPort(port);
			con.connect();

			// 3. Prepare the request
			reqMultiple = new ReadCoilsRequest(ref, count);

			// 4. Prepare the transaction
			trans = new ModbusTCPTransaction(con);
			trans.setRetries(retries);
			trans.setRequest(reqMultiple);

			// 5. Execute the transaction
			trans.execute();
			resMultiple = (ReadCoilsResponse) trans.getResponse();

			JSONArray myResponse = new JSONArray();
			BitVector coils = resMultiple.getCoils();
			int limit = Math.min(count, coils.size());
			for (int i = 0; i < limit; i++) {
				myResponse.put(coils.getBit(i));
			}

			callbackContext.success(myResponse);
		} catch (Exception exc) {
			callbackContext.error("ERROR: " + exc.getLocalizedMessage());
		} finally {
            // 6. Close the connection
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    Log.i("readCoil","Error closing connection: " + e.getLocalizedMessage());
                }
            }
        }
	}

	private void writeHoldingRegister(String ip, String offset, String values, CallbackContext callbackContext) {
		TCPMasterConnection con = null; // the connection
        try {
			/* The important instances of the classes mentioned before */
			ModbusTCPTransaction trans = null; // the transaction
			WriteMultipleRegistersRequest reqWrite = null; // the request
			WriteMultipleRegistersResponse resWrite = null;

			/* Variables for storing the parameters */
			InetAddress addr = null; // the slave's address
			int port = Modbus.DEFAULT_PORT;
			int ref = Integer.parseInt(offset); // the reference; offset where to start reading from

			// 2. Open the connection
			addr = InetAddress.getByName(ip);

			con = new TCPMasterConnection(addr);
			con.setTimeout(timeout);
			con.setPort(port);
			con.connect();

			Register[] registers = new Register[values.split(",").length];
			String[] split = values.split(",");
			for (int i = 0; i < split.length; i++) {
				registers[i] = new SimpleRegister(Integer.parseInt(split[i]));
			}

			// 3. Prepare the request
			reqWrite = new WriteMultipleRegistersRequest(ref, registers);

			// 4. Prepare the transaction
			trans = new ModbusTCPTransaction(con);
			trans.setRetries(retries);
			trans.setRequest(reqWrite);

			// 5. Execute the transaction
			trans.execute();
			resWrite = (WriteMultipleRegistersResponse) trans.getResponse();

			callbackContext.success("OK ---> [" + offset + ", " + values + "]");
		} catch (Exception exc) {
			callbackContext.error("ERROR: " + exc.getLocalizedMessage());
		} finally {
            // 6. Close the connection
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    Log.i("writeHoldingRegister","Error closing connection: " + e.getLocalizedMessage());
                }
            }
        }
	}

	private void writeCoil(String ip, String offset, String values, CallbackContext callbackContext) {
		TCPMasterConnection con = null; // the connection
        try {
			/* The important instances of the classes mentioned before */
			ModbusTCPTransaction trans = null; // the transaction
			WriteMultipleCoilsRequest reqWrite = null; // the request
			WriteMultipleCoilsResponse resWrite = null;

			/* Variables for storing the parameters */
			InetAddress addr = null; // the slave's address
			int port = Modbus.DEFAULT_PORT;
			int ref = Integer.parseInt(offset); // the reference; offset where to start reading from

			// 2. Open the connection
			addr = InetAddress.getByName(ip);

			con = new TCPMasterConnection(addr);
			con.setTimeout(timeout);
			con.setPort(port);
			con.connect();

			BitVector registers = new BitVector(values.split(",").length);
			String[] split = values.split(",");
			for (int i = 0; i < split.length; i++) {
				registers.setBit(i, Boolean.parseBoolean(split[i]));
			}

			// 3. Prepare the request
			reqWrite = new WriteMultipleCoilsRequest(ref, registers);

			// 4. Prepare the transaction
			trans = new ModbusTCPTransaction(con);
			trans.setRetries(retries);
			trans.setRequest(reqWrite);

			// 5. Execute the transaction
			trans.execute();
			resWrite = (WriteMultipleCoilsResponse) trans.getResponse();

			callbackContext.success("OK ---> [" + offset + ", " + values + "]");
		} catch (Exception exc) {
			callbackContext.error("ERROR: " + exc.getLocalizedMessage());
		} finally {
            // 6. Close the connection
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    Log.i("writeCoil","Error closing connection: " + e.getLocalizedMessage());
                }
            }
        }
	}

	private void ping(String ip, CallbackContext callbackContext) {
		Socket socket = null;
		try {
			SocketAddress socketAddress = new InetSocketAddress(ip, Modbus.DEFAULT_PORT);

			socket = new Socket();

			long start = System.currentTimeMillis();
			socket.connect(socketAddress, timeout); // timeout en ms
			long elapsed = System.currentTimeMillis() - start;

			callbackContext.success("PING OK (" + elapsed + " ms)");

		} catch (Exception exc) {
			callbackContext.error("ERROR: " + exc.getLocalizedMessage());

		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
					Log.i("ping", "Error closing socket: " + e.getLocalizedMessage());
				}
			}
		}
	}
}