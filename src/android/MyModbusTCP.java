package cordova.plugin.mymodbustcp;

import android.util.Log;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.wimpi.modbus.Modbus;
import net.wimpi.modbus.io.ModbusTCPTransaction;
import net.wimpi.modbus.msg.*;
import net.wimpi.modbus.net.TCPMasterConnection;
import net.wimpi.modbus.procimg.Register;

public class MyModbusTCP extends CordovaPlugin {

    private int timeout = 500;
    private int retries = 1;

    private static final int PLC_DELAY_MS = 600; // delay óptimo para tu PLC

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.i("ModbusPlugin", "============================================================");
        Log.i("ModbusPlugin", "execute() → INICIO | Acción: " + action);

        String ip = args.getString(0);
        Log.i("ModbusPlugin", "execute() → IP recibida: " + ip);

        // Validación de IP (toca el puerto 502)
        Log.i("ModbusPlugin", "execute() → Llamando a validateIp()...");
        if (!validateIp(ip, callbackContext)) {
            Log.e("ModbusPlugin", "execute() → validateIp() devolvió false");
            return false;
        }

        // Delay para permitir que el PLC libere conexiones previas
        Log.i("ModbusPlugin", "execute() → Llamando a safeDelay() tras validateIp()");
        safeDelay();

        switch (action) {
            case "readHoldingRegister":
                Log.i("ModbusPlugin", "execute() → Acción: readHoldingRegister");
                readHoldingRegister(ip, args.getString(1), args.getString(2), callbackContext);
                return true;

            case "readCoil":
                Log.i("ModbusPlugin", "execute() → Acción: readCoil");
                readCoil(ip, args.getString(1), callbackContext);
                return true;

            case "writeHoldingRegister":
                Log.i("ModbusPlugin", "execute() → Acción: writeHoldingRegister");
                writeHoldingRegister(ip, args.getString(1), args.getString(2), callbackContext);
                return true;

            case "writeCoil":
                Log.i("ModbusPlugin", "execute() → Acción: writeCoil");
                writeCoil(ip, args.getString(1), args.getString(2), callbackContext);
                return true;

            default:
                Log.e("ModbusPlugin", "execute() → Acción no soportada: " + action);
                callbackContext.error("Acción no soportada: " + action);
                return false;
        }
    }

    // -------------------------------------------------------------------------
    // VALIDACIÓN DE IP (toca el puerto 502)
    // -------------------------------------------------------------------------
    private boolean validateIp(String ip, CallbackContext callbackContext) {
        Log.i("ModbusPlugin", "*******************************************************************");
        Log.i("ModbusPlugin", "validateIp() → INICIO");
        Log.i("ModbusPlugin", "validateIp() → Validando: " + ip);

        // 1. Validación de formato
        if (ip == null || ip.trim().isEmpty()) {
            Log.e("ModbusPlugin", "validateIp() → IP vacía o nula");
            callbackContext.error("IP inválida o vacía");
            return false;
        }

        String trimmed = ip.trim();
        String[] parts = trimmed.split("\\.");
        if (parts.length != 4) {
            Log.e("ModbusPlugin", "validateIp() → Formato incorrecto");
            callbackContext.error("IP inválida: debe tener 4 octetos");
            return false;
        }

        try {
            for (String part : parts) {
                int value = Integer.parseInt(part);
                if (value < 0 || value > 255) {
                    Log.e("ModbusPlugin", "validateIp() → Octeto fuera de rango: " + part);
                    callbackContext.error("IP inválida: octeto fuera de rango");
                    return false;
                }
            }
        } catch (NumberFormatException e) {
            Log.e("ModbusPlugin", "validateIp() → Caracter no numérico: " + e.getMessage());
            callbackContext.error("IP inválida: contiene caracteres no numéricos");
            return false;
        }

        // 2. Comprobación de comunicación real (toca el puerto 502)
        int timeoutMs = timeout;
        Log.i("ModbusPlugin", "validateIp() → Intentando conectar a " + trimmed + ":" + Modbus.DEFAULT_PORT);

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
            Log.i("ModbusPlugin", "validateIp() → FIN");
            Log.i("ModbusPlugin", "*******************************************************************");
        }
    }

    // -------------------------------------------------------------------------
    // DELAY ENTRE VALIDACIÓN Y MODBUS
    // -------------------------------------------------------------------------
    private void safeDelay() {
        Log.i("ModbusPlugin", "safeDelay() → INICIO | Esperando " + PLC_DELAY_MS + " ms...");
        try {
            Thread.sleep(PLC_DELAY_MS);
        } catch (InterruptedException e) {
            Log.e("ModbusPlugin", "safeDelay() → Interrumpido: " + e.getMessage());
        }
        Log.i("ModbusPlugin", "safeDelay() → FIN");
    }

    // -------------------------------------------------------------------------
    // READ HOLDING REGISTER
    // -------------------------------------------------------------------------
    private void readHoldingRegister(String ip, String offset, String number, CallbackContext callbackContext) {

        Log.i("ModbusPlugin", "readHoldingRegister() → INICIO");
        Log.i("ModbusPlugin", "readHoldingRegister() → IP: " + ip + " | offset: " + offset + " | number: " + number);

        TCPMasterConnection con = null;

        try {
            int port = Modbus.DEFAULT_PORT;
            int ref = Integer.parseInt(offset);
            int count = Integer.parseInt(number);

            InetAddress addr = Inet4Address.getByName(ip);

            Log.i("ModbusPlugin", "readHoldingRegister() → Creando conexión TCP...");
            con = new TCPMasterConnection(addr);
            con.setTimeout(timeout);
            con.setPort(port);

            Log.i("ModbusPlugin", "readHoldingRegister() → Conectando...");
            con.connect();

            ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(ref, count);
            ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
            trans.setRetries(retries);
            trans.setRequest(req);

            Log.i("ModbusPlugin", "readHoldingRegister() → Ejecutando transacción...");
            trans.execute();

            ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();

            JSONArray response = new JSONArray();
            Register[] registers = res.getRegisters();

            for (int i = 0; i < registers.length; i++) {
                int value = registers[i].getValue();
                Log.i("ModbusPlugin", "readHoldingRegister() → Registro[" + (ref + i) + "] = " + value);
                response.put(value);
            }

            callbackContext.success(response);

        } catch (Exception exc) {
            Log.e("ModbusPlugin", "readHoldingRegister() → ERROR: " + exc.getMessage());
            callbackContext.error("ERROR: " + exc.getLocalizedMessage());

        } finally {
            if (con != null) {
                try {
                    con.close();
                    Log.i("ModbusPlugin", "readHoldingRegister() → Conexión cerrada");
                } catch (Exception e) {
                    Log.e("ModbusPlugin", "readHoldingRegister() → Error cerrando conexión: " + e.getMessage());
                }
            }
            Log.i("ModbusPlugin", "readHoldingRegister() → FIN");
            safeDelay();
        }
    }

    // -------------------------------------------------------------------------
    // READ COIL
    // -------------------------------------------------------------------------
    private void readCoil(String ip, String offset, CallbackContext callbackContext) {

        Log.i("ModbusPlugin", "readCoil() → INICIO");
        Log.i("ModbusPlugin", "readCoil() → IP: " + ip + " | offset: " + offset);

        TCPMasterConnection con = null;

        try {
            int port = Modbus.DEFAULT_PORT;
            int ref = Integer.parseInt(offset);

            InetAddress addr = Inet4Address.getByName(ip);

            Log.i("ModbusPlugin", "readCoil() → Creando conexión TCP...");
            con = new TCPMasterConnection(addr);
            con.setTimeout(timeout);
            con.setPort(port);

            Log.i("ModbusPlugin", "readCoil() → Conectando...");
            con.connect();

            ReadCoilsRequest req = new ReadCoilsRequest(ref, 1);
            ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
            trans.setRetries(retries);
            trans.setRequest(req);

            Log.i("ModbusPlugin", "readCoil() → Ejecutando transacción...");
            trans.execute();

            ReadCoilsResponse res = (ReadCoilsResponse) trans.getResponse();

            boolean value = res.getCoilStatus(0);
            Log.i("ModbusPlugin", "readCoil() → Valor leído: " + value);

            callbackContext.success(value ? "1" : "0");

        } catch (Exception exc) {
            Log.e("ModbusPlugin", "readCoil() → ERROR: " + exc.getMessage());
            callbackContext.error("ERROR: " + exc.getLocalizedMessage());

        } finally {
            if (con != null) {
                try {
                    con.close();
                    Log.i("ModbusPlugin", "readCoil() → Conexión cerrada");
                } catch (Exception e) {
                    Log.e("ModbusPlugin", "readCoil() → Error cerrando conexión: " + e.getMessage());
                }
            }
            Log.i("ModbusPlugin", "readCoil() → FIN");
            safeDelay();
        }
    }

    // -------------------------------------------------------------------------
    // WRITE HOLDING REGISTER
    // -------------------------------------------------------------------------
    private void writeHoldingRegister(String ip, String offset, String value, CallbackContext callbackContext) {

        Log.i("ModbusPlugin", "writeHoldingRegister() → INICIO");
        Log.i("ModbusPlugin", "writeHoldingRegister() → IP: " + ip + " | offset: " + offset + " | value: " + value);

        TCPMasterConnection con = null;

        try {
            int port = Modbus.DEFAULT_PORT;
            int ref = Integer.parseInt(offset);
            int val = Integer.parseInt(value);

            InetAddress addr = Inet4Address.getByName(ip);

            Log.i("ModbusPlugin", "writeHoldingRegister() → Creando conexión TCP...");
            con = new TCPMasterConnection(addr);
            con.setTimeout(timeout);
            con.setPort(port);

            Log.i("ModbusPlugin", "writeHoldingRegister() → Conectando...");
            con.connect();

            WriteSingleRegisterRequest req = new WriteSingleRegisterRequest(ref, new SimpleRegister(val));
            ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
            trans.setRetries(retries);
            trans.setRequest(req);

            Log.i("ModbusPlugin", "writeHoldingRegister() → Ejecutando transacción...");
            trans.execute();

            callbackContext.success("OK");

        } catch (Exception exc) {
            Log.e("ModbusPlugin", "writeHoldingRegister() → ERROR: " + exc.getMessage());
            callbackContext.error("ERROR: " + exc.getLocalizedMessage());

        } finally {
            if (con != null) {
                try {
                    con.close();
                    Log.i("ModbusPlugin", "writeHoldingRegister() → Conexión cerrada");
                } catch (Exception e) {
                    Log.e("ModbusPlugin", "writeHoldingRegister() → Error cerrando conexión: " + e.getMessage());
                }
            }
            Log.i("ModbusPlugin", "writeHoldingRegister() → FIN");
            safeDelay();
        }
    }

    // -------------------------------------------------------------------------
    // WRITE COIL
    // -------------------------------------------------------------------------
    private void writeCoil(String ip, String offset, String value, CallbackContext callbackContext) {

        Log.i("ModbusPlugin", "writeCoil() → INICIO");
        Log.i("ModbusPlugin", "writeCoil() → IP: " + ip + " | offset: " + offset + " | value: " + value);

        TCPMasterConnection con = null;

        try {
            int port = Modbus.DEFAULT_PORT;
            int ref = Integer.parseInt(offset);
            boolean val = value.equals("1");

            InetAddress addr = Inet4Address.getByName(ip);

            Log.i("ModbusPlugin", "writeCoil() → Creando conexión TCP...");
            con = new TCPMasterConnection(addr);
            con.setTimeout(timeout);
            con.setPort(port);

            Log.i("ModbusPlugin", "writeCoil() → Conectando...");
            con.connect();

            WriteCoilRequest req = new WriteCoilRequest(ref, val);
            ModbusTCPTransaction trans = new ModbusTCPTransaction(con);
            trans.setRetries(retries);
            trans.setRequest(req);

            Log.i("ModbusPlugin", "writeCoil() → Ejecutando transacción...");
            trans.execute();

            callbackContext.success("OK");

        } catch (Exception exc) {
            Log.e("ModbusPlugin", "writeCoil() → ERROR: " + exc.getMessage());
            callbackContext.error("ERROR: " + exc.getLocalizedMessage());

        } finally {
            if (con != null) {
                try {
                    con.close();
                    Log.i("ModbusPlugin", "writeCoil() → Conexión cerrada");
                } catch (Exception e) {
                    Log.e("ModbusPlugin", "writeCoil() → Error cerrando conexión: " + e.getMessage());
                }
            }
            Log.i("ModbusPlugin", "writeCoil() → FIN");
            safeDelay();
        }
    }
}