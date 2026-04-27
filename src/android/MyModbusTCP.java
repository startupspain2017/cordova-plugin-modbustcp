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
		// 1. Validación de formato
		if (ip == null || ip.trim().isEmpty()) {
			callbackContext.error("IP inválida o vacía");
			return false;
		}

		String trimmed = ip.trim();
		String[] parts = trimmed.split("\\.");
		if (parts.length != 4) {
			callbackContext.error("IP inválida: debe tener 4 octetos");
			return false;
		}

		try {
			for (String part : parts) {
				int value = Integer.parseInt(part);
				if (value < 0 || value > 255) {
					callbackContext.error("IP inválida: octeto fuera de rango (0-255)");
					return false;
				}
			}
		} catch (NumberFormatException e) {
			callbackContext.error("IP inválida: contiene caracteres no numéricos");
			return false;
		}

		// 2. Comprobación de comunicación con timeout real
		SocketChannel socketChannel = null;
		try {
			InetSocketAddress address = new InetSocketAddress(trimmed, Modbus.DEFAULT_PORT);

			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(address);

			long start = System.currentTimeMillis();
			long timeoutMs = 500;

			while (!socketChannel.finishConnect()) {
				if (System.currentTimeMillis() - start > timeoutMs) {
					callbackContext.error("IP válida pero sin respuesta (timeout)");
					return false;
				}
				Thread.sleep(10);
			}

			// Si llega aquí → IP válida y comunicación OK
			return true;

		} catch (Exception e) {
			callbackContext.error("IP válida pero sin comunicación: " + e.getLocalizedMessage());
			return false;

		} finally {
			try {
				if (socketChannel != null) socketChannel.close();
			} catch (Exception ignored) {}
		}
	}


	private void readHoldingRegister(String ip, String offset, String number, CallbackContext callbackContext) {
		TCPMasterConnection con = null; // the connection
        try {
			/* The important instances of the classes mentioned before */
			ModbusTCPTransaction trans = null; // the transaction
			ReadMultipleRegistersRequest reqMultiple = null; //
			ReadMultipleRegistersResponse resMultiple = null;

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
			reqMultiple = new ReadMultipleRegistersRequest(ref, count);

			// 4. Prepare the transaction
			trans = new ModbusTCPTransaction(con);
			trans.setRetries(retries);
			trans.setRequest(reqMultiple);

			// 5. Execute the transaction
			trans.execute();
			resMultiple = (ReadMultipleRegistersResponse) trans.getResponse();

			JSONArray myResponse = new JSONArray();
			Register[] registers = resMultiple.getRegisters();
			for (int i = 0; i < registers.length; i++) {
				myResponse.put(registers[i].getValue());
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
                    Log.i("readHoldingRegister","Error closing connection: " + e.getLocalizedMessage());
                }
            }
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
		SocketChannel socketChannel = null;

		try {
			InetSocketAddress address = new InetSocketAddress(ip, Modbus.DEFAULT_PORT);

			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false); // modo no bloqueante
			socketChannel.connect(address);

			long start = System.currentTimeMillis();
			long timeoutMs = 500;

			while (!socketChannel.finishConnect()) {
				if (System.currentTimeMillis() - start > timeoutMs) {
					callbackContext.error("ERROR: Timeout");
					return;
				}
				Thread.sleep(10);
			}

			callbackContext.success("PING OK");

		} catch (Exception e) {
			callbackContext.error("ERROR: " + e.getLocalizedMessage());
		} finally {
			try {
				if (socketChannel != null) socketChannel.close();
			} catch (Exception ignored) {}
		}
	}

}