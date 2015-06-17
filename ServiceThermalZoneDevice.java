//package echowand.sample;

import echowand.common.EPC;
import echowand.info.TemperatureSensorInfo;
import echowand.logic.TooManyObjectsException;
import echowand.net.Inet4Subnet;
import echowand.net.SubnetException;
import echowand.object.LocalObject;
import echowand.object.LocalObjectDateTimeDelegate;
import echowand.object.LocalObjectDefaultDelegate;
import echowand.object.LocalObjectDelegate;
import echowand.object.ObjectData;
import echowand.service.Core;
import echowand.service.LocalObjectConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An ECHONET Lite Device supporting thermal zone sensors.
 * @author ymakino
 */
public class ServiceThermalZoneDevice {
    public static final String SENSOR_FILENAME = "temperature.txt";
    public static class ThermalZoneDelegate extends LocalObjectDefaultDelegate {

        private File file;

        public ThermalZoneDelegate(File file) {
            this.file = file;
        }

        @Override
        public void getData(LocalObjectDelegate.GetState result, LocalObject object, EPC epc) {
            if (epc == EPC.xE0) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String line = br.readLine();
                    short value = Short.parseShort(line);
                    System.out.println(file  + ": " + line + "(" + (value) + ")");
                   byte b1, b2; 
		    if(value > 32766){
			b1 = (byte) 0x7F;
			b2 = (byte) 0xFF;
		    }else if(value < -2732){
			b1 = (byte) 0x80;
			b2 = (byte) 0x00;
		    }else {
	       	    	b1 = (byte) ((value >> 8) & 0xff);
                    	b2 = (byte) (value & 0xff);
                    } 
		    result.setGetData(new ObjectData(b1, b2));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    object.setInternalData(EPC.x88, new ObjectData((byte)0x41));
                    result.setFail();
                }
            }
        }
    }
    
    public static LocalObjectConfig createThermalZoneConfig() {
        TemperatureSensorInfo info = new TemperatureSensorInfo();
	//EPC, gettable, settable, observable, dataSize
        info.add(EPC.x97, true, false, false, 2);
        info.add(EPC.x98, true, false, false, 4);

	LocalObjectConfig config = new LocalObjectConfig(info);
        config.addDelegate(new LocalObjectDateTimeDelegate());
        config.addDelegate(new ThermalZoneDelegate(new File(SENSOR_FILENAME)));

        return config;
    }
    
    public static void main(String[] args) throws InterruptedException, SocketException, SubnetException, TooManyObjectsException {
        if (args.length > 1) {
            System.err.println("Usage: ServiceThermalZoneDevice [ifname]");
            return;
        }
        
        Core core;
        
        if (args.length == 0) {
            core = new Core(Inet4Subnet.startSubnet());
        } else {
            NetworkInterface nif = NetworkInterface.getByName(args[0]);
            core = new Core(Inet4Subnet.startSubnet(nif));
        }
        
        core.addLocalObjectConfig(createThermalZoneConfig());
        
        core.startService();
    }
}
