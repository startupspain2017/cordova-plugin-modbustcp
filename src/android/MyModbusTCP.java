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
		if (ip == null || ip.trim().isEmpty()) {
			callbackContext.error("IP inválida o vacía");
			return false;
		}
		try {
			InetAddress.getByName(ip);
			return true;
		} catch (Exception e) {
			callbackContext.error("IP inválida: " + e.getLocalizedMessage());
			return false;
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
		Socket socket = null;
		try {
			InetAddress addr = InetAddress.getByName(ip);
			SocketAddress socketAddress = new InetSocketAddress(addr, Modbus.DEFAULT_PORT);
			socket = new Socket();
			socket.connect(socketAddress, timeout);
			callbackContext.success("PING OK");
		} catch (Exception exc) {
			callbackContext.error("ERROR: " + exc.getLocalizedMessage());
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
					Log.i("ping","Error closing socket: " + e.getLocalizedMessage());
				}
			}
		}
	}}